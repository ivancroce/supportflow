# AI suggestions are cached per ticket and only regenerated on explicit user action

The Advisor's `AiSuggestion` is generated lazily on the first `GET /api/tickets/{id}/ai-suggestion`
and then cached on the ticket forever. It is **not** auto-invalidated when the ticket's subject or
description changes. Refreshing the suggestion requires an explicit `POST .../ai-suggestion/regenerate`
from the user. Reason: Gemini's free tier is the dominant constraint, and the Advisor is already
framed as human-in-the-loop — the same human editing a description is the right party to decide
whether the existing Suggestion is still useful or whether a fresh call is worth a quota unit.
Auto-invalidating on every edit would burn calls on typo fixes and small wording changes; a future
reader will reasonably wonder why the cache looks "stale" relative to the ticket and the answer
(quota dominates freshness) belongs in the record.

## Consequences

- The DTO exposes `generatedAt` so the UI can surface staleness ("Generated 12 minutes ago") and
  nudge the user toward Regenerate when they've meaningfully rewritten the ticket.
- A failed generation does **not** write a marker row — the next GET retries naturally. The risk of
  hammering Gemini is bounded by the single human operator.
- If a future provider has a more generous quota (or is paid), the policy can be relaxed to
  edit-triggered invalidation without changing the storage shape — `replaceSuggestion()` already
  exists on the entity for exactly this seam.
