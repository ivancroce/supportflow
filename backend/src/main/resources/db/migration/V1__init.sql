create table users (
    id          uuid primary key default gen_random_uuid(),
    email       varchar(320) not null unique,
    name        varchar(255),
    avatar_url  varchar(1024),
    provider    varchar(32)  not null,
    created_at  timestamptz  not null default now()
);

create table projects (
    id                    uuid primary key default gen_random_uuid(),
    owner_id              uuid not null references users (id),
    name                  varchar(255) not null,
    client_name           varchar(255),
    description           text,
    jira_project_key      varchar(64),
    qase_project_code     varchar(64),
    confluence_space_key  varchar(64),
    archived              boolean not null default false,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now()
);

create index idx_projects_owner on projects (owner_id);

create table tickets (
    id                  uuid primary key default gen_random_uuid(),
    project_id          uuid not null references projects (id),
    subject             varchar(500) not null,
    description         text,
    status              varchar(32) not null default 'OPEN'
                            check (status in ('OPEN','IN_PROGRESS','PENDING','RESOLVED','CLOSED')),
    priority            varchar(32) not null default 'MEDIUM'
                            check (priority in ('LOW','MEDIUM','HIGH','URGENT')),
    type                varchar(32) not null default 'QUESTION'
                            check (type in ('QUESTION','BUG','FEATURE_REQUEST')),
    category            varchar(128),
    escalated           boolean not null default false,
    jira_issue_key      varchar(64),
    qase_case_id        varchar(64),
    confluence_page_id  varchar(64),
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create index idx_tickets_project on tickets (project_id);
create index idx_tickets_status on tickets (status);

create table messages (
    id          uuid primary key default gen_random_uuid(),
    ticket_id   uuid not null references tickets (id),
    author_id   uuid references users (id),
    body        text not null,
    is_ai_draft boolean not null default false,
    created_at  timestamptz not null default now()
);

create index idx_messages_ticket on messages (ticket_id);

create table ai_suggestions (
    ticket_id           uuid primary key references tickets (id),
    suggested_type      varchar(32),
    suggested_category  varchar(128),
    suggested_priority  varchar(32),
    draft_note          text,
    generated_at        timestamptz not null default now()
);
