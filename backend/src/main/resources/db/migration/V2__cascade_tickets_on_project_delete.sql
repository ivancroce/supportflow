-- Replace the tickets -> projects FK with one that cascades on delete.
-- Deleting a project should atomically remove all of its tickets at the DB level,
-- so the API can offer a single DELETE /api/projects/{id} without orphaning rows.

alter table tickets
    drop constraint tickets_project_id_fkey;

alter table tickets
    add constraint tickets_project_id_fkey
        foreign key (project_id) references projects (id)
        on delete cascade;
