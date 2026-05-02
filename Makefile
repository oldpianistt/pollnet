# PollNet — convenience targets. All commands assume you're in the repo root.
#
# Run `make` (no args) to see the full list with descriptions.

SHELL := /bin/bash

DEV_COMPOSE  := docker compose
PROD_COMPOSE := docker compose -f docker-compose.prod.yml
TLS_COMPOSE  := docker compose -f docker-compose.prod.yml -f docker-compose.tls.yml

.DEFAULT_GOAL := help
.PHONY: help up up-all down logs ps backend frontend test test-be test-fe \
        prod prod-down prod-logs tls bootstrap-invite reset clean

help: ## Show this help
	@awk 'BEGIN {FS = ":.*##"; printf "PollNet make targets:\n\n"} \
	     /^[a-zA-Z0-9_-]+:.*##/ {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# ───────────── Development (postgres + redis in docker, BE/FE on host) ─────────────

up: ## Start postgres + redis only (preferred dev workflow)
	$(DEV_COMPOSE) up -d postgres redis

up-all: ## Start the whole dev stack inside docker (postgres + redis + backend + frontend dev)
	$(DEV_COMPOSE) up --build

down: ## Stop the dev stack (volumes preserved)
	$(DEV_COMPOSE) down

ps: ## Show docker container status
	$(DEV_COMPOSE) ps

logs: ## Tail backend logs (assumes container running, e.g. via `make up-all`)
	$(DEV_COMPOSE) logs -f backend

backend: ## Run the backend on the host (port 8090). Needs `make up` first.
	cd backend && mvn spring-boot:run

frontend: ## Run the frontend dev server on the host (port 5173). Needs `make up` first.
	cd frontend && npm install && npm run dev

# ───────────── Tests ─────────────

test: test-be test-fe ## Run all tests (backend integration + frontend unit)

test-be: ## Backend Testcontainers integration tests (postgres + redis spun up automatically)
	cd backend && mvn -B -ntp test

test-fe: ## Frontend Vitest unit tests
	cd frontend && npm test

# ───────────── Production ─────────────

prod: ## Start the production stack (postgres + redis + backend + nginx web)
	@test -f .env || (echo "Create .env first: cp .env.example .env && edit it" && exit 1)
	$(PROD_COMPOSE) up -d --build

prod-down: ## Stop the production stack
	$(PROD_COMPOSE) down

prod-logs: ## Tail prod backend logs
	$(PROD_COMPOSE) logs -f backend

tls: ## Start the production stack with TLS (requires nginx-tls.conf + certs on host)
	@test -f frontend/nginx-tls.conf || (echo "Copy nginx-tls.conf.example → nginx-tls.conf and edit YOUR_DOMAIN_HERE" && exit 1)
	$(TLS_COMPOSE) up -d --build

# ───────────── First-time setup helper ─────────────

bootstrap-invite: ## Create a placeholder user + bootstrap invite token so you can register the first real account
	@docker exec pollnet-postgres psql -U pollnet -d pollnet -v ON_ERROR_STOP=1 -c "\
		WITH p AS (INSERT INTO users (id, username, email, password_hash, invite_quota, invite_quota_reset_at) \
		           VALUES (gen_random_uuid(), 'bootstrap', 'bootstrap@local', 'unused', 10, NOW()) \
		           ON CONFLICT (username) DO UPDATE SET username='bootstrap' RETURNING id) \
		INSERT INTO invitations (id, token, inviter_id, expires_at) \
		SELECT gen_random_uuid(), 'BOOTSTRAP_INVITE', id, NOW() + INTERVAL '7 days' FROM p \
		ON CONFLICT (token) DO NOTHING RETURNING token;"
	@echo ""
	@echo "Bootstrap invite ready. Register at:"
	@echo "  http://localhost:5173/register?invite=BOOTSTRAP_INVITE      (dev)"
	@echo "  http://localhost:\$$WEB_PORT/register?invite=BOOTSTRAP_INVITE  (prod)"

# ───────────── Reset ─────────────

reset: ## Stop everything AND wipe the postgres volume (DESTRUCTIVE — deletes all data)
	@read -p "This deletes the postgres volume. Continue? [y/N] " ans && [ "$$ans" = "y" ]
	$(DEV_COMPOSE)  down -v
	$(PROD_COMPOSE) down -v 2>/dev/null || true

clean: ## Remove generated artifacts (target/, dist/, node_modules/)
	rm -rf backend/target frontend/dist frontend/node_modules
