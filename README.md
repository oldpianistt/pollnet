# PollNet

Davet bazlı, anket odaklı sosyal ağ. Tek monolith, MVP odaklı.

## Stack

- **Backend**: Java 21, Spring Boot 3.3, PostgreSQL 16, Redis 7, Flyway, JWT
- **Frontend**: React 18 + Vite + TypeScript, TanStack Query, Zustand, Tailwind, Recharts
- **Infra**: Docker Compose (dev + prod), nginx reverse proxy

## Yapı

```
pollnet/
├── backend/             # Spring Boot monolith (Maven)
├── frontend/            # Vite + React + TypeScript
├── docker-compose.yml          # geliştirme
├── docker-compose.prod.yml     # production
└── .env.example                # production secrets şablonu
```

## Hızlı başlangıç

`make` komutu mevcut hedefleri listeler. Tek seferlik kurulum + tek-komut başlatma:

```bash
# Bağımlılıklar: docker, java 21, maven, node 20+
make up                # postgres + redis Docker'da (dev)
make backend           # ayrı terminalde: backend 8090'da
make frontend          # ayrı terminalde: frontend 5173'te
make bootstrap-invite  # ilk kullanıcıyı yaratabilmek için davet token
# tarayıcıda: http://localhost:5173/register?invite=BOOTSTRAP_INVITE
```

Veya her şeyi tek docker komutunda:

```bash
make up-all  # postgres + redis + backend + frontend (frontend dev server Docker içinde)
```

Production tek komut:

```bash
cp .env.example .env && $EDITOR .env   # POSTGRES_PASSWORD + POLLNET_JWT_SECRET zorunlu
make prod
```

Testler:

```bash
make test       # backend Testcontainers + frontend Vitest
make test-be    # sadece backend
make test-fe    # sadece frontend
```

Diğer faydalı:

```bash
make ps         # container durumu
make logs       # backend logları (docker container'a karşı)
make down       # dev stack'i durdur (data korunur)
make reset      # her şeyi sil (DESTRUCTIVE — postgres volume dahil)
make clean      # target/, dist/, node_modules/ sil
```

## Geliştirme detayları

Postgres + Redis Docker'da, backend ve frontend lokal IDE/terminalden çalışır:

```bash
docker compose up -d postgres redis
# backend
cd backend && mvn spring-boot:run
# frontend (yeni terminal)
cd frontend && npm install && npm run dev
```

Servisler:
- Frontend: http://localhost:5173
- Backend:  http://localhost:8090
- Postgres: localhost:55432 (pollnet/pollnet) — non-default port to avoid clashing with host PG
- Redis:    localhost:6379

`docker compose up --build` her şeyi container'da koşturur (frontend dev server da Docker'da).

## Production deploy

Tek VPS hedefli. Frontend prod build'i nginx ile servis edilir, `/api/*` requests aynı origin
üzerinden backend'e reverse-proxy edilir (CORS gerekmez).

```bash
cp .env.example .env
# .env'i düzenle: POSTGRES_PASSWORD ve POLLNET_JWT_SECRET'ı değiştir
#   openssl rand -base64 48   # JWT secret üretmek için
docker compose -f docker-compose.prod.yml up -d --build
```

Yayında:
- http://your-host/        → SPA
- http://your-host/api/*   → Spring Boot (nginx üzerinden)
- Postgres ve backend host portu açık değil; sadece web (varsayılan 80) dışarıya bind.

TLS için: nginx'in önüne caddy/traefik koy ya da `frontend/nginx.conf`'a 443 server bloğu ekle
ve cert mount et.

## İlk kullanıcıyı (root) yaratma

Davet zorunlu olduğu için, ilk hesabı oluştururken bir bootstrap davet gerekir.
Postgres'e doğrudan bir kullanıcı + onun adına bir davet kaydı ekleyip, o davetle UI'dan kayıt olun.
İlk kayıttan sonra bu placeholder hesabı silebilirsiniz.

```sql
-- docker exec -it pollnet-postgres psql -U pollnet -d pollnet
WITH placeholder AS (
  INSERT INTO users (id, username, email, password_hash, invite_quota, invite_quota_reset_at)
  VALUES (gen_random_uuid(), 'bootstrap', 'bootstrap@local',
          'unused', 10, NOW())
  RETURNING id
)
INSERT INTO invitations (id, token, inviter_id, expires_at)
SELECT gen_random_uuid(),
       'BOOTSTRAP_TOKEN_PICK_ANYTHING',
       id,
       NOW() + INTERVAL '7 days'
FROM placeholder;
```

Sonra `http://your-host/register?invite=BOOTSTRAP_TOKEN_PICK_ANYTHING` adresinden kayıt olun.

## Smoke test (8090 backend dev server'a karşı)

```bash
# Davet üret + register + login + poll create + vote zinciri
BASE=http://localhost:8090
curl -sS $BASE/actuator/health || echo "(health endpoint disabled by default)"
# Kayıt için yukarıdaki bootstrap SQL'ini çalıştır, sonra:
curl -sS -X POST $BASE/api/auth/register -H 'Content-Type: application/json' \
  -d '{"inviteToken":"BOOTSTRAP_TOKEN_PICK_ANYTHING","username":"alice",
       "email":"alice@x.io","password":"hunter22","displayName":"Alice"}'
```

## API özet

```
POST   /api/auth/register | login | refresh | logout
GET    /api/users/me           PATCH  /api/users/me
GET    /api/users/{username}   GET    /api/users/{username}/polls
POST   /api/users/{username}/follow    DELETE /api/users/{username}/follow
GET    /api/users/{username}/followers | following

POST   /api/polls               # questions[] dahil
GET    /api/polls/{id}
DELETE /api/polls/{id}          # sadece sahibi
POST   /api/polls/{id}/answers  # atomik tüm sorulara cevap
GET    /api/polls/{id}/results  # visibility'ye göre

GET    /api/feed?type=discover|following&cursor=&limit=
GET    /api/polls/{id}/comments    POST same    DELETE /api/comments/{id}

GET    /api/invitations            POST same           GET /api/invitations/quota
```

Rate limit (Redis fixed-window):
- Auth: 5/dk per IP
- Poll create: 10/saat per user
- Vote: 60/dk per user
- Comment: 20/dk per user

## Faz Durumu

- [x] **Faz 1** — Foundation (DB schema, JPA entities, security iskelet, FE skeleton)
- [x] **Faz 2** — Auth + Invite (register/login/refresh/logout, davet üret/listele, monthly quota reset)
- [x] **Faz 3** — Core domain (5 question type strategy, atomic answer submit, visibility-aware results, follow/comment/feed, Redis rate limiter)
- [x] **Faz 4** — Frontend (auth ekranları, feed, poll create wizard, voting + Recharts results, profile, davetler, yorumlar)
- [x] **Faz 5** — Production deploy (multi-stage Docker, nginx reverse proxy, .env şablonu)
- [x] **Faz 6** — Production hardening:
  - Email verification + forgot/reset password (SMTP env-driven, log-fallback)
  - In-app notifications (NEW_FOLLOWER, POLL_ANSWERED, POLL_COMMENTED) + 30s polling bell
  - Search (Postgres trigram, polls + users)
  - Mobile hamburger nav + responsive Layout
  - Frontend route code-splitting (vendor chunks)
  - Backend actuator (health/info/prometheus) + JSON logging in `prod` profile + request-id MDC
  - Backend Testcontainers integration tests (auth, poll/vote, notifications, search)
  - Frontend Vitest unit tests (auth store, axios error helper, cn util)
  - TLS-ready nginx (compose override + certbot sidecar)
  - Sentry slot — backend (`sentry-spring-boot-starter-jakarta`) + frontend (`@sentry/react`); both no-op until DSN env vars are set
  - [`RUNBOOK.md`](RUNBOOK.md) — backups, monitoring, incident playbooks

## Karar kayıtları

- Davet quota: 10/ay/hesap, her ayın 1'inde reset
- Davet token: 7 gün geçerli, tek kullanımlık
- Sonuç görünürlüğü: anket başına yazar seçer (`AFTER_VOTE` default)
- Açık uçlu cevap görünürlüğü: anket başına yazar seçer (`PUBLIC` default)
- Anket süresi: yok, hep açık
- Cevap düzenleme: yok, oy verince kilitli
- Çoklu cevap: yok, bir user bir polle bir cevap seti
