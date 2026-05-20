# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**SupportFlow** — a customer-support helpdesk built as a **portfolio/learning project**. The full
design and phased build plan lives in **`docs/PROJECT_PLAN.md`** — read it before starting work.

Hard constraint: **everything stays on free tiers** (build → deploy → run, no credit card, no spend).

## Tech stack

- **Frontend:** TypeScript, React, Vite, Tailwind, shadcn/ui (`/frontend`)
- **Backend:** Java 21, Maven, Spring Boot, Spring Security — JWT + Google/GitHub OAuth (`/backend`)
- **DB:** PostgreSQL (Neon free tier)
- **AI:** Google Gemini Flash (free REST API) — agent-facing advisor
- **Integrations:** Jira, Confluence, Qase Cloud REST APIs (bug-escalation feature)
- **Hosting:** Cloud Run (backend) + Neon (DB) + Vercel (frontend)

## Structure

Single monorepo: `/frontend` and `/backend`, plus `/docs`.

## Headline feature

Agent clicks "Escalate as bug" → app auto-creates linked records in Jira (bug), Qase (test case),
and Confluence (Known-Issue page), all linking back to the SupportFlow ticket. Phase 1 is one-way
push; two-way webhook sync-back is deferred until the app is hosted.

## Conventions & guardrails

- Keep the backend lean — it deploys to Cloud Run (scales to zero; cold starts matter).
- **Never commit secrets.** All keys/tokens come from env vars; keep a committed `.env.example` and
  a git-ignored `.env`. See the env-var list in `docs/PROJECT_PLAN.md`.
- Gemini free quota is limited — AI suggestions are **cached per ticket**, not regenerated on every view.
- Two distinct uses of Jira/Confluence/Qase, kept separate: **A1** = manually use the tools to build
  the app (process, no code); **B** = the runtime escalation feature (code). **A2** (CI automation of
  the SDLC) is out of scope.

## Working agreement

- Plan thoroughly before building; explain trade-offs and give a recommendation.
- Persist durable design decisions in `docs/` (in-repo), not only in ephemeral planning files.
