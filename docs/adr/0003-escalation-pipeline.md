# Escalation requires all three targets, seeds from the ticket, and retries partial failures idempotently

Escalating a Ticket as a bug pushes one record into each external tool: a **Jira issue** (track the
fix), a **Qase case** (regression test), and a **Confluence Known Issue** page (investigating stub).
Phase 1 is one-way push only. The pipeline makes three sequential external HTTP calls, each of which
can fail on its own, so the design fixes how preconditions, content, and partial failure are handled.

## Decisions

- **Fail fast on configuration.** Escalation requires the Ticket's Project to have all three targets
  set (`jiraProjectKey`, `qaseProjectCode`, `confluenceSpaceKey`). If any is missing, the request is
  rejected with a `ProblemDetail` **before any external call is made** — no half-escalation by
  config. Keeps the headline demo ("all three appear at once") coherent.

- **Content comes from the Ticket, not from an "accept" step.** The Jira description and Confluence
  body are seeded from the Ticket's subject + description, plus the cached Triage Note
  (`AiSuggestion.draftNote`) appended as a "Triage summary" section **when one exists**. Escalation is
  manual and must work even if the Advisor never ran, failed, or hit quota, so the Triage Note is
  optional enrichment — never a precondition. No Gemini call happens at escalation. This supersedes
  the wording in ADR 0001 / `CONTEXT.md` that spoke of an "accepted Triage Note": there is no accept
  mechanism and no Triage Note field on the Ticket, so "accepted" was a phantom concept.

- **Jira is the anchor.** Calls run Jira → Qase → Confluence so the Qase case and Confluence page can
  reference the Jira issue key. Every record also links back to the SupportFlow Ticket via a
  configurable base URL (`APP_BASE_URL`, default `http://localhost:5173`) templated as
  `{base}/tickets/{ticketId}` — a placeholder until the app is hosted, swapped via env var with no
  code change.

- **Idempotent partial-failure retry.** Each external id is persisted in its own short transaction as
  soon as that call succeeds. Re-invoking escalate **skips any tool already linked** (id present) and
  only attempts the missing ones, so a retry never double-creates a Jira issue. The Ticket's
  `escalated` flag flips true only once all three ids exist; a partially escalated Ticket has
  `escalated=false` with some ids set. This reuses the three existing nullable id columns — no
  migration, no new status enum.

- **Always-200 per-tool envelope.** `POST /api/tickets/{id}/escalate` returns an `EscalationResult`:
  an overall status (`ESCALATED` / `PARTIAL` / `FAILED`) plus a per-tool outcome list
  (`CREATED` / `ALREADY_LINKED` / `FAILED` with key, url, and error) plus the updated ticket. This
  mirrors the existing `AiSuggestionEnvelope` pattern (200 + status in the body) so the caller has one
  uniform success path and can render badges + a Retry affordance.

- **`type=BUG` is set up front.** Clicking "Escalate as bug" is the declaration that the Ticket is a
  bug, so `type` is set to `BUG` in the opening transaction and persists even if the external calls
  fail. The Ticket's `status` is left untouched.

- **SupportFlow priority is not mapped onto Jira's priority field.** Jira priority scheme names vary
  per project configuration; setting an unknown priority would fail issue creation. The SupportFlow
  priority is written into the Jira description text instead.

## Consequences

- `Ticket.markEscalated(jira, qase, confluence)` (which set all three ids at once) is replaced by
  per-tool record methods plus a method that flips `escalated`/`type`, so partial progress is
  representable.
- External HTTP calls run **outside** any DB transaction (as in `AiAdvisorService`) so a slow upstream
  never pins a JDBC connection; only the short load/validate and persist steps are transactional.
- The three integration clients follow the `GeminiClient` seam: an interface + a `Real…Client` under
  `supportflow.integrations.client` (real by default) + a test-only fake, so partial-failure and retry
  branches are coverable without real Atlassian/Qase accounts.
- If a future, hosted version wants a real suggest-and-accept step, the content seam already exists:
  swap the "ticket text + cached Triage Note" source for an accepted-note field without changing the
  pipeline.
