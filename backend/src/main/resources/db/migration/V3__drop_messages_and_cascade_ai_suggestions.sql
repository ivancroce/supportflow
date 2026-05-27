-- Drop the messages table: SupportFlow is single-user and the AI's Triage Note lives on
-- ai_suggestions, so a per-ticket message thread is unused. See docs/PROJECT_PLAN.md.
drop table if exists messages;

-- Make ai_suggestions.ticket_id cascade on delete so DELETE /api/tickets/{id} stays atomic
-- once a cached suggestion exists. Mirrors V2's approach for tickets -> projects.
alter table ai_suggestions
    drop constraint ai_suggestions_ticket_id_fkey;

alter table ai_suggestions
    add constraint ai_suggestions_ticket_id_fkey
        foreign key (ticket_id) references tickets (id)
        on delete cascade;
