# SupportFlow — Implementation Plan

> Status: **Planned, not started.** When resuming, read this file and begin at **Phase 0**.

## Context

SupportFlow is a **personal, per-project issue & documentation hub for your own client work**, built
as a **portfolio/learning project**. You build websites/apps for clients; for each client project,
issues/bugs/feedback are captured in SupportFlow, and from there fan out automatically into your real
engineering toolchain — **Jira** (track the fix), **Qase** (regression test), **Confluence**
(project docs / known issues).

One lightweight front door, many client projects behind it. **You are the only user** — clients and
end-users do not log in. The value: a single unified place across all your client projects, with an
AI advisor and one-click fan-out that removes manual copy-paste between four tools.

Hard constraint: **everything stays on free tiers** (build → deploy → run, no credit card, no spend).

The "wow" demo moment: on a ticket in a client project, you click **"Escalate as bug"** and a linked
Jira issue, a Qase test case, and a Confluence page all appear at once — in _that client's_ Jira
project / Qase project / Confluence space — each linking back to the SupportFlow ticket.

Two distinct uses of Jira/Confluence/Qase, kept in separate layers:

- **A1 — Development workflow (process):** manually use the tools to build SupportFlow itself. No code.
- **B — Runtime feature (product):** the app programmatically pushes records into the tools during
  bug escalation, per project. This is the headline feature.
- **A2 — CI automation of the SDLC: OUT OF SCOPE.**

---

## Locked Decisions

| Topic                | Decision                                                                                                                                             |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| Project type         | Portfolio/learning project (optimize for demo clarity, not scale)                                                                                    |
| Product shape        | **Personal multi-project hub** — one app, many client projects                                                                                       |
| Users                | **Just you** (the developer). No client or end-user logins. Optionally a small team, all same role                                                   |
| Auth                 | Google + GitHub OAuth, **restricted to an email allowlist** (only your account(s) can sign in); app-issued JWT                                       |
| Multi-project        | **Yes** — a `Project` entity (= one client/website); tickets belong to a project                                                                     |
| Tool mapping         | **Per-project config**: each project stores its own Jira project key, Qase project code, Confluence space key (shared API tokens, different targets) |
| AI advisor           | Agent-facing, human-in-the-loop. Auto-runs on ticket open (cached): **classify** type/category/priority + **draft a triage note**. **Suggest-and-accept** (never auto-applies fields). **Never diagnoses/fixes the bug.** Write-only — no retrieval of existing Known Issues (deferred). See `docs/adr/0001`. |
| AI model             | **Google Gemini 2.5 Flash** (free API tier), for now. Confirm exact model id + free limits in AI Studio before coding                                |
| Escalation trigger   | **Only when a ticket is escalated as a BUG** (manual, AI-assisted). Normal tickets never escalate                                                    |
| Escalation direction | **Phase 1 = one-way push**. **Phase 2 (post-hosting) = two-way** webhook sync-back                                                                   |
| Confluence timing    | Page created **at escalation** as a "Known Issue / Investigating" stub                                                                               |
| Repo                 | **Single monorepo** (`/frontend` + `/backend`), one GitHub repo                                                                                      |
| Hosting              | Backend → **Cloud Run**; DB → **Neon Postgres**; Frontend → **Vercel**. All $0, no card                                                              |

---

## Tech Stack

- **Frontend:** TypeScript, React, Vite, Tailwind, shadcn/ui
- **Backend:** Java 21, Maven, Spring Boot, Spring Security (JWT + OAuth2 client: Google + GitHub)
- **DB:** PostgreSQL (Neon free tier)
- **AI:** Google Gemini Flash (free REST API)
- **Integrations:** Jira Cloud, Confluence Cloud, Qase REST APIs
- **Hosting:** Cloud Run (Dockerized backend), Neon (DB), Vercel (frontend)

---

## Data Model (backend)

- **User**: id, email, name, avatarUrl, provider (GOOGLE/GITHUB), createdAt. (Single role — every
  authenticated user is the owner/developer; sign-in gated by allowlist.)
- **Project** (= one client/website): id, name, clientName, description, archived (bool),
  per-project integration config: `jiraProjectKey`, `qaseProjectCode`, `confluenceSpaceKey`,
  createdAt, updatedAt.
- **Ticket**: id, **projectId**, subject, description, status, priority, type, category,
  escalated (bool), `jiraIssueKey`, `qaseCaseId`, `confluencePageId` (nullable until escalated),
  createdAt, updatedAt.
- **Message** / note: id, ticketId, author (User), body, isAiDraft (bool), createdAt.
- **AiSuggestion** (cache): ticketId, suggestedType, suggestedCategory, suggestedPriority,
  draftNote, generatedAt.

**Enums:**

- Status: `OPEN → IN_PROGRESS → PENDING → RESOLVED → CLOSED`
- Priority: `LOW / MEDIUM / HIGH / URGENT`
- Type: `QUESTION / BUG / FEATURE_REQUEST`

> Integration **credentials** (Atlassian email+token, Qase token) are global env vars — one account.
> The **per-project config** stores only _which_ Jira project / Qase project / Confluence space to
> target, so each client's records stay isolated within your single set of accounts.

---

## Backend Architecture (Spring Boot)

- **Auth:** Spring Security OAuth2 login (Google + GitHub). On login, verify the email is in
  `AUTH_ALLOWLIST`; reject otherwise. Create/lookup User, issue app **JWT** for the SPA.
- **Layers:** Controller → Service → Repository (Spring Data JPA). DTOs at the boundary.
- **Integration clients** (`@Service` each, `RestClient`):
  - `GeminiClient` — classify + draft. `GEMINI_API_KEY`.
  - `JiraClient` — create issue in a given project key. Atlassian Basic auth (email + token).
  - `ConfluenceClient` — create page in a given space key. Same Atlassian token.
  - `QaseClient` — create test case in a given project code. Qase token header.
- **EscalationService** — for "Escalate as bug": reads the ticket's **Project** config, calls
  Jira → Qase → Confluence into _that project's_ targets, stores returned IDs on the ticket, sets
  `escalated=true`, `type=BUG`. Partial-failure tolerant (record what succeeded; allow retry).
- **AiAdvisorService** — on ticket open, return cached `AiSuggestion` or generate via Gemini.

### Key API endpoints (sketch)

- `GET/POST /api/projects`, `GET/PATCH /api/projects/{id}` (incl. per-project tool config)
- `GET/POST /api/projects/{id}/tickets`, `GET /api/tickets/{id}`, `PATCH /api/tickets/{id}`
- `POST /api/tickets/{id}/messages`
- `GET /api/tickets/{id}/ai-suggestion`
- `POST /api/tickets/{id}/escalate`
- `GET /api/me`

---

## Frontend (React + Vite + shadcn)

- **Auth:** "Sign in with Google / GitHub" → backend (allowlist-gated) → JWT → attached to API calls.
- **Projects view:** list of client projects, create/edit a project incl. its Jira/Qase/Confluence
  mapping. A project switcher in the header.
- **Project board:** tickets for the selected project (filter by status/priority), create ticket.
- **Ticket detail:** thread + **AI advisor panel** (suggested type/category/priority + editable
  draft note), **"Escalate as bug"** button, and after escalation, badges linking to Jira/Qase/Confluence.
- UI: Tailwind + shadcn (Table, Dialog, Badge, Card, Form, Select for the project switcher).

---

## Integration Setup (free-tier, no spend)

1. **Atlassian Cloud (Jira + Confluence)** — one free account (≤10 users). Create one Jira project
   and one Confluence space **per client project** (free tier allows multiple). API token; auth =
   Basic(email:token).
2. **Qase** — free plan. Create one project per client. Generate API token. _(Note: Qase free tier
   limits the number of active projects — fine for a demo with a few clients; check current limit.)_
3. **Gemini** — free API key from Google AI Studio (no card).
4. **Neon** — free Postgres project; copy connection string.
5. **Cloud Run** — free tier (scales to zero). **Vercel** — free frontend hosting.

### Environment variables (never commit; provide a committed `.env.example`)

Backend:

- `DATABASE_URL` (Neon)
- `JWT_SECRET`
- `AUTH_ALLOWLIST` (comma-separated emails allowed to sign in)
- `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`
- `GITHUB_OAUTH_CLIENT_ID`, `GITHUB_OAUTH_CLIENT_SECRET`
- `GEMINI_API_KEY`
- `ATLASSIAN_EMAIL`, `ATLASSIAN_API_TOKEN` (shared Jira + Confluence)
- `JIRA_BASE_URL`, `CONFLUENCE_BASE_URL` (site URLs; project key / space key are per-project in DB)
- `QASE_API_TOKEN`
- `FRONTEND_ORIGIN` (CORS)

Frontend:

- `VITE_API_BASE_URL`

---

## Development Workflow (A1 — manual SDLC, no app code)

While building SupportFlow, dogfood the three tools (this is process, not code; near-zero cost,
high portfolio value): track build work in **Jira** (epics → stories mirroring the phases below),
document design in **Confluence** (Architecture, Auth, Escalation Design, Runbook, Decision Log),
author manual test cases in **Qase** for the critical flows. Reference Jira keys in commits.
Do **not** automate this via CI (that would be A2, out of scope).

---

## Build Phases

**Phase 0 — Scaffolding**

- Monorepo: `/frontend` (Vite + Tailwind + shadcn) + `/backend` (Spring Initializr, Java 21, Maven, mvnw).
- Neon/Postgres connection; JPA/Flyway schema for User/Project/Ticket/Message.
- Health endpoint + basic React shell.

**Phase 1 — Auth + projects + tickets**

- OAuth login (Google + GitHub) with allowlist gate + JWT.
- Project CRUD (incl. per-project tool config) + project switcher.
- Ticket CRUD + message thread, scoped to a project.

**Phase 2 — AI advisor**

- `GeminiClient` (Gemini 2.5 Flash; confirm model id in AI Studio first) + `AiAdvisorService`.
- Auto-runs on ticket open, result cached in `AiSuggestion` (re-open never re-calls).
- Produces a **Classification** (suggested type/category/priority) + a **Triage Note** (structured
  summary: restated problem, suspected area/severity, missing info). **No bug diagnosis/fix.**
- **Suggest-and-accept:** panel shows suggestions; ticket fields change only when the user accepts.
  Reversible to an on-demand "Ask AI" button if it gets noisy or burns quota.
- The accepted Triage Note is reused at escalation to seed the Jira description + Confluence page
  (see Phase 3) — no extra Gemini calls.
- Out of scope (deferred): retrieval/matching against existing Confluence Known Issues.

**Phase 3 — One-way escalation pipeline (headline feature)**

- Jira + Qase + Confluence clients. `EscalationService` using per-project config.
- "Escalate as bug" button + linked badges. Partial-failure handling + retry.

**Phase 4 — Deploy (all free)**

- Dockerize backend → Cloud Run. DB → Neon. Frontend → Vercel. Env vars + CORS. Cold-start mitigation.

**Phase 5 (future) — Two-way sync-back**

- `POST /webhooks/jira` with signature verification. Jira issue closed → SupportFlow ticket → RESOLVED.
  Requires the public hosted URL from Phase 4.

---

## Verification

- **Local dev:** run backend (`./mvnw spring-boot:run`) + frontend (`npm run dev`). Sign in with an
  allowlisted account; confirm a non-allowlisted account is rejected.
- **Projects:** create two client projects with different Jira/Qase/Confluence mappings; confirm
  tickets are scoped per project.
- **AI:** open a ticket, verify the advisor panel populates (and is cached on re-open).
- **Escalation:** click "Escalate as bug" → confirm a Jira issue, Qase case, and Confluence page are
  created in _that project's_ targets, linked back to the ticket, with IDs stored + badges shown.
- **Deploy smoke test:** repeat escalation against the hosted URL after Phase 4.
- **Free-tier check:** no billing enabled anywhere.

---

## Open Items / Notes

- Gemini free quota is limited — AI suggestions are cached per ticket (required).
- Cloud Run cold starts add first-request latency; acceptable for a demo.
- Qase free tier limits active projects — verify the current cap before relying on many client projects.
- Two-way sync-back (Phase 5) deferred until the app is publicly hosted (webhooks need a public URL).
