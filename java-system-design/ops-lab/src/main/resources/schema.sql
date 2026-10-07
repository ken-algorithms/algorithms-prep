drop table if exists ledger_entry;
drop table if exists account;
create table account(id bigint primary key, balance bigint not null check (balance >= 0));
create table ledger_entry(id bigserial primary key, account_id bigint not null, amount bigint not null,
                          created_at timestamptz not null default now());
insert into account select g, 1000000000000 from generate_series(1, 1000) g;
