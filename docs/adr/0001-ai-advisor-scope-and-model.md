# AI advisor classifies and drafts, but never diagnoses or fixes the bug

The AI advisor is scoped to two jobs on a ticket: **classify** (suggest type/category/priority) and
**draft a triage note** (restate the problem, suspected area/severity, missing info). It deliberately
does **not** attempt to diagnose the bug or propose a fix. Reason: SupportFlow is a triage and
escalation tool, not a debugging assistant — "how to fix it" is the highest-hallucination,
least-verifiable, lowest-value output here, and a future reader will reasonably wonder why the AI
stops short of suggesting solutions. Every suggestion is human-reviewed (suggest-and-accept) before it
touches the ticket or feeds an escalation. The model is **Gemini 2.5 Flash (free tier) for now**;
one cached call per ticket is sufficient for this scope, and the model is straightforward to revisit
since suggestions are isolated behind the `AiSuggestion` cache.

## Consequences

- "Search existing Known Issues for a match" (grounded retrieval) is intentionally out of scope until
  a corpus of Confluence pages exists — a later enhancement, not Phase 2.
- The accepted triage note seeds both the Jira issue description and the Confluence Known-Issue page,
  so the reviewed text is reused without extra model calls.
