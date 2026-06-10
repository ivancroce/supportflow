# SupportFlow

A personal, per-project issue & documentation hub for the developer's own client work. Tickets are
captured per client project and fan out into Jira, Qase, and Confluence. This glossary fixes the
language used across the codebase and docs.

## Language

### Core

**Owner**:
The single human who uses SupportFlow — the developer doing the client work. There is no other
role: no agents, no team, no end-users or customers signing in. The UI speaks in the first person
("your projects", "your queue") and never labels the logged-in person.
_Avoid_: Agent, user (in UI copy), team member, requester, customer (nobody else logs in).

**Project**:
One client/website. Owns its tickets and its own Jira/Qase/Confluence targets.
_Avoid_: Client (the human is the client; the Project is the work), workspace.

**Ticket**:
A single captured issue, bug, question, or feedback item belonging to one Project.
_Avoid_: Case, issue (Jira owns "issue"), task.

**Escalation**:
The deliberate, human-triggered act of pushing a Ticket out into the external toolchain (Jira +
Qase + Confluence). Only bugs are escalated.
_Avoid_: Sync, export, push (push is the Phase-1 direction, not the act).

### AI advisor

**AI Advisor**:
The agent-facing, human-in-the-loop assistant that, on a Ticket, produces a Classification and a
Triage Note. It never diagnoses or fixes the bug.
_Avoid_: Assistant, bot, copilot.

**Classification**:
The Advisor's suggested type, category, and priority for a Ticket — a Suggestion until accepted.
_Avoid_: Tagging, labelling, prediction.

**Triage Note**:
The Advisor's drafted structured summary of a Ticket (restated problem, suspected area/severity,
missing info). A draft the developer reviews; it is not customer-facing and does not propose a fix.
_Avoid_: Reply, answer, solution, draft response.

**Suggestion**:
Anything the Advisor proposes (Classification or Triage Note) that has NOT yet been accepted. Stored
separately from the Ticket's real fields. Becomes part of the Ticket only when accepted.
_Avoid_: Decision, result.

**Known Issue**:
A Confluence page created at Escalation as an "Investigating" stub for a bug. Its body is seeded from
the Ticket (subject + description) and, when an Advisor Suggestion has been cached, the Triage Note.
_Avoid_: Article, doc, KB entry.

## Flagged ambiguities

- **"Issue"** — reserved for Jira ("Jira issue"). Inside SupportFlow the unit is always a **Ticket**.
- **"Priority"** — a Ticket field. The Advisor only *suggests* it; the field's real value changes
  only on accept. "Suggested priority" and "ticket priority" are distinct.

## Example dialogue

> **Dev:** When I open a new ticket, the advisor classifies it?
> **Domain expert:** It produces a Classification — suggested type, category, priority — plus a
> Triage Note. Both are Suggestions until you accept them.
> **Dev:** So if it says HIGH priority, the ticket is HIGH?
> **Domain expert:** No. The ticket's priority only changes when you accept the Suggestion. The
> Advisor's pick lives in the suggestion cache, separate from the Ticket.
> **Dev:** And if it's a real bug?
> **Domain expert:** You escalate it. That creates the Jira issue, the Qase case, and a Known Issue
> page in Confluence — the Known Issue body comes from the ticket text, plus the Triage Note if the
> Advisor has run on it.
