# Frontend reflects the single-user model; the design handoff is a visual spec only

The `design_handoff_supportflow/` bundle was generated with limited context and assumes a
multi-agent help-desk: it shows an "Agent", a requester/customer per ticket, an assignee column,
"Assigned to me" / "Team queue" nav, a reply thread + composer, and an AI "confidence" score.
SupportFlow's locked model is the opposite — a single Owner (the developer), no customers or
agents logging in, no assignees, no message thread (dropped from the MVP), and an
`AiSuggestionResponse` with no confidence field.

**Decision:** we recreate the handoff's *visual language* faithfully (the SLDS palette, type scale,
spacing, badges, shadows, animations — that is the reason it exists) but bind the UI only to the
real backend model. Concretely:

- First-person "personal hub" copy; no Agent/team/requester language (see the **Owner** term in
  `CONTEXT.md`).
- Board columns are `STATUS · PRIORITY · SUBJECT · AGE`; ticket meta uses type/category/created-at
  instead of customer/company/channel/replies.
- The detail view's left column is the Ticket's editable description, not a customer conversation +
  reply composer.
- The AI Advisor panel shows Classification (type/category/priority) + Triage Note, with no
  confidence pill.
- Two screens the handoff omits are added in the same style: **ticket create** and **project
  settings** (per-project Jira/Qase/Confluence config).
- Status filtering covers all five real statuses (`OPEN, IN_PROGRESS, PENDING, RESOLVED, CLOSED`);
  the handoff only depicted four.

Recording this so a future reader diffing the handoff against the shipped UI understands the
omissions are deliberate, not unfinished work.
