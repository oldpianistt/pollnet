# PollNet — Operations Runbook

Quick reference for running, monitoring, and recovering the production stack.

## Stack topology

```
[client] ─HTTPS→ nginx (web) ──/api/*──→ Spring Boot (backend) ──→ Postgres
                                                       └─→ Redis
```

- Only the `web` container exposes ports to the host (80/443).
- `backend`, `postgres`, `redis` are reachable only inside the Docker network.

## Daily operations

| Task | Command |
|------|---------|
| Bring up | `docker compose -f docker-compose.prod.yml up -d --build` |
| With TLS | `docker compose -f docker-compose.prod.yml -f docker-compose.tls.yml up -d --build` |
| Tail backend logs (JSON in prod) | `docker logs -f pollnet-backend` |
| Tail nginx access | `docker logs -f pollnet-web` |
| Stop everything | `docker compose -f docker-compose.prod.yml down` |
| Rolling restart of backend | `docker compose -f docker-compose.prod.yml up -d --build backend` |
| Open psql shell | `docker exec -it pollnet-postgres psql -U pollnet -d pollnet` |
| Open redis-cli | `docker exec -it pollnet-redis redis-cli` |
| Apply outstanding Flyway migrations | `docker compose ... up backend` (Flyway runs at boot) |

## Monitoring

- **Health probe**: `GET /actuator/health` (always public). `503` if DB or Redis is down.
- **Metrics (Prometheus)**: `GET /actuator/prometheus` (public, scrape it from your monitoring host).
- **Build info**: `GET /actuator/info`.

Alert ideas (pick what your stack supports):

- HTTP 5xx rate from nginx access logs
- `actuator/health` non-200 for > 1 min
- Postgres disk > 80% full
- Redis OOM kill events

### Log conventions

In `prod` Spring profile (set `SPRING_PROFILES_ACTIVE=prod` to enable), backend
emits one JSON object per log line, including:

- `requestId` — propagated to/from the `X-Request-Id` response header
- `userId` — set when the request is authenticated

Pipe `docker logs pollnet-backend` to your log shipper of choice (Loki, Vector,
Datadog agent…). The JSON encoder is `net.logstash.logback.encoder.LogstashEncoder`
so any standard parser works.

## Backups

The Postgres volume (`postgres_data`) is the only stateful component that holds
user data. Redis only holds short-lived tokens / counters and can be rebuilt.

### Daily logical backup (local)

Run on the host (cron):

```cron
30 2 * * * docker exec -t pollnet-postgres \
    pg_dump -U pollnet -d pollnet --format=custom \
    | gzip > /var/backups/pollnet-$(date +\%F).sql.gz
```

Retention: keep ≥ 7 daily, ≥ 4 weekly. Delete older with `find /var/backups -name 'pollnet-*' -mtime +30 -delete`.

### Off-site upload (S3-compatible)

Append `&& aws s3 cp /var/backups/pollnet-$(date +\%F).sql.gz s3://your-bucket/`
to the cron line. Use a write-only IAM role.

### Restore

```bash
# 1. Stop the backend so nothing's writing.
docker compose -f docker-compose.prod.yml stop backend

# 2. Drop & recreate the DB.
docker exec -i pollnet-postgres dropdb -U pollnet pollnet
docker exec -i pollnet-postgres createdb -U pollnet pollnet

# 3. Restore from a logical dump.
gunzip -c pollnet-2026-05-01.sql.gz \
  | docker exec -i pollnet-postgres pg_restore -U pollnet -d pollnet

# 4. Bring the backend back; Flyway will see the schema is current and skip.
docker compose -f docker-compose.prod.yml start backend
```

## Common incidents

### 502 on `/api/*` after deploy

Backend isn't ready yet. nginx with the `resolver` directive will recover on
its own once `backend` resolves. Check:

```bash
docker logs pollnet-backend | tail -50
# look for "Started PollnetApplication" or stack traces
```

### "role pollnet does not exist" on first boot

Postgres volume was reused with a different user/password. Delete the volume
to reset (DESTRUCTIVE — only on a fresh deploy):

```bash
docker compose -f docker-compose.prod.yml down -v
```

### Mail not arriving

`pollnet.mail.enabled=false` → service logs the message instead of sending.
Set `MAIL_ENABLED=true` plus `SMTP_HOST/PORT/USERNAME/PASSWORD` in `.env`,
then restart the backend.

To verify:

```bash
docker logs pollnet-backend | grep "Sent mail"
```

### Rate limit too aggressive

`RateLimiter` uses Redis fixed-window keys. To clear all limits:

```bash
docker exec pollnet-redis redis-cli --scan --pattern 'rl:*' | \
  xargs -r docker exec -i pollnet-redis redis-cli del
```

### Quota didn't reset on the 1st

`QuotaResetJob` runs at `0 0 0 1 * *` (cron). If the host clock or container
TZ is off, manually run:

```bash
docker exec pollnet-postgres psql -U pollnet -d pollnet \
  -c "UPDATE users SET invite_quota = 10, invite_quota_reset_at = NOW();"
```

## Bootstrapping the very first user

Invite is mandatory, so insert one bootstrap row by hand:

```sql
WITH placeholder AS (
  INSERT INTO users (id, username, email, password_hash, invite_quota, invite_quota_reset_at)
  VALUES (gen_random_uuid(), 'bootstrap', 'bootstrap@local', 'unused', 10, NOW())
  RETURNING id
)
INSERT INTO invitations (id, token, inviter_id, expires_at)
SELECT gen_random_uuid(), 'BOOT_TOKEN_PICK_ANYTHING',
       id, NOW() + INTERVAL '7 days' FROM placeholder;
```

Visit `https://your-host/register?invite=BOOT_TOKEN_PICK_ANYTHING`, register your
real account, then optionally delete the placeholder user.

## Smoke test (post-deploy)

```bash
HOST=https://your-host
curl -sS $HOST/actuator/health | jq .
curl -sS -X POST $HOST/api/auth/login -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail":"alice","password":"hunter22"}' | jq .accessToken
```

If both succeed, the public surface, JVM, DB, Redis, and CORS are all healthy.
