-- ============================================================================
-- TheekHai - Supabase PostgreSQL Schema & Migrations (Inspired by TenderLens)
-- Project: TheekHai (ffmcurazxvygsifbglwy)
-- ============================================================================

-- 1. Civic & Staff Profiles Table (RLS Protected)
create table if not exists public.profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  email text not null default '' check (char_length(email) <= 254),
  full_name text not null default '' check (char_length(full_name) <= 100),
  organization text not null default '' check (char_length(organization) <= 100),
  role text not null default 'CITIZEN' check (role in ('CITIZEN', 'DEPARTMENT', 'REVIEWER', 'ADMIN')),
  department text,
  ward text not null default 'Ward 1' check (char_length(ward) <= 80),
  preferred_language text not null default 'en' check (preferred_language in ('en', 'hi')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 2. Saved Legal & Civic Reference Sources Table (RLS Protected)
create table if not exists public.saved_sources (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  title text not null check (char_length(title) between 1 and 250),
  source_url text not null check (char_length(source_url) <= 2048 and source_url ~ '^https?://'),
  jurisdiction text not null default 'CENTRAL' check (jurisdiction in ('CENTRAL', 'HARYANA')),
  snippet text not null default '' check (char_length(snippet) <= 1500),
  created_at timestamptz not null default now(),
  unique (user_id, source_url)
);

create index if not exists saved_sources_user_created_idx
  on public.saved_sources (user_id, created_at desc);

-- 3. Civic Complaints / Cases Table (RLS Protected)
create table if not exists public.complaints (
  id text primary key,
  citizen_id uuid not null references auth.users (id) on delete cascade,
  title text not null check (char_length(title) between 5 and 120),
  description text not null check (char_length(description) between 10 and 5000),
  category text not null check (category in ('ROADS', 'WATER', 'WASTE', 'LIGHTING', 'OTHER')),
  address text not null check (char_length(address) between 3 and 500),
  latitude double precision,
  longitude double precision,
  department text not null,
  priority text not null default 'MEDIUM' check (priority in ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
  status text not null default 'ASSIGNED' check (status in ('SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'PENDING_VERIFICATION', 'CLOSED', 'REOPENED')),
  created_at bigint not null,
  updated_at bigint not null,
  due_at bigint not null,
  version bigint not null default 1,
  escalation bigint not null default 0,
  closure_by text
);

create index if not exists complaints_citizen_idx on public.complaints (citizen_id, updated_at desc);
create index if not exists complaints_status_due_idx on public.complaints (status, due_at);

-- 4. Departmental Tasks Table (RLS Protected)
create table if not exists public.case_tasks (
  id text primary key,
  case_id text not null references public.complaints (id) on delete cascade,
  department text not null,
  title text not null,
  required boolean not null default true,
  complete boolean not null default false
);

-- 5. Case Audit Events Table (RLS Protected)
create table if not exists public.audit_logs (
  id text primary key,
  case_id text not null references public.complaints (id) on delete cascade,
  actor_id text not null,
  action text not null,
  note text not null,
  at bigint not null
);

create index if not exists audit_logs_case_at_idx on public.audit_logs (case_id, at);

-- 6. Legal Sources & TinyFish Monitors Tables (RLS Protected)
create table if not exists public.legal_sources (
  id text primary key,
  title text not null,
  url text not null,
  jurisdiction text not null default 'CENTRAL',
  language text not null default 'en',
  content text not null,
  hash text not null,
  retrieved_at bigint not null,
  status text not null default 'APPROVED',
  reviewed_by text,
  supersedes text
);

create table if not exists public.legal_monitors (
  id text primary key,
  remote_monitor_id text,
  name text not null,
  monitor_type text not null default 'fetch',
  target text not null,
  jurisdiction text not null default 'CENTRAL',
  language text not null default 'en',
  schedule_cron text not null default '0 0 * * *',
  status text not null default 'ACTIVE',
  last_checked_at bigint,
  last_status text
);

-- 7. Row Level Security (RLS) Enablement & Permissions
alter table public.profiles enable row level security;
alter table public.saved_sources enable row level security;
alter table public.complaints enable row level security;
alter table public.case_tasks enable row level security;
alter table public.audit_logs enable row level security;
alter table public.legal_sources enable row level security;
alter table public.legal_monitors enable row level security;

revoke all on table public.profiles, public.saved_sources, public.complaints, public.case_tasks, public.audit_logs, public.legal_sources, public.legal_monitors from anon, public;

grant select, insert, update, delete on table public.profiles, public.saved_sources, public.complaints, public.case_tasks, public.audit_logs, public.legal_sources, public.legal_monitors to authenticated;
grant select on table public.legal_sources to anon;
grant all on table public.profiles, public.saved_sources, public.complaints, public.case_tasks, public.audit_logs, public.legal_sources, public.legal_monitors to service_role;

-- 8. Role Protection & Auto-Profile Triggers
create or replace function public.protect_profile_role()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if current_setting('request.jwt.claim.role', true) = 'authenticated' then
    if tg_op = 'INSERT' then
      new.role := 'CITIZEN';
      new.department := null;
    elsif tg_op = 'UPDATE' then
      new.role := old.role;
      new.department := old.department;
    end if;
  end if;
  new.updated_at := now();
  return new;
end;
$$;
revoke execute on function public.protect_profile_role() from public, anon, authenticated;

drop trigger if exists enforce_profile_role_trigger on public.profiles;
create trigger enforce_profile_role_trigger
  before insert or update on public.profiles
  for each row execute function public.protect_profile_role();

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (user_id, email, full_name, organization, ward, preferred_language)
  values (
    new.id,
    coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data->>'full_name', split_part(coalesce(new.email, ''), '@', 1)),
    coalesce(new.raw_user_meta_data->>'organization', 'Citizen Resident'),
    coalesce(new.raw_user_meta_data->>'ward', 'Ward 1'),
    case when new.raw_user_meta_data->>'preferred_language' in ('en', 'hi') then new.raw_user_meta_data->>'preferred_language' else 'en' end
  )
  on conflict (user_id) do update
  set
    full_name = excluded.full_name,
    organization = excluded.organization,
    ward = excluded.ward,
    preferred_language = excluded.preferred_language,
    updated_at = now();
  return new;
end;
$$;
revoke execute on function public.handle_new_user() from public, anon, authenticated;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 9. RLS Policies: Profiles & Saved Sources
create policy "Owners can read their profile"
  on public.profiles for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "Owners can create their profile"
  on public.profiles for insert to authenticated
  with check ((select auth.uid()) = user_id);

create policy "Owners can update their profile"
  on public.profiles for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "Owners can delete their profile"
  on public.profiles for delete to authenticated
  using ((select auth.uid()) = user_id);

create policy "Owners can read their saved sources"
  on public.saved_sources for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "Owners can save their sources"
  on public.saved_sources for insert to authenticated
  with check ((select auth.uid()) = user_id);

create policy "Owners can update their saved sources"
  on public.saved_sources for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "Owners can remove their saved sources"
  on public.saved_sources for delete to authenticated
  using ((select auth.uid()) = user_id);

-- 10. Private Evidence Storage Bucket
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('evidence', 'evidence', false, 5242880, array['image/jpeg', 'image/png', 'image/webp', 'audio/webm', 'audio/wav'])
on conflict (id) do nothing;

-- 11. RLS Policies for Workflow & Legal Tables (Required for Ktor JWT sync & client access)
create policy "Authenticated users can read complaints"
  on public.complaints for select to authenticated
  using (true);

create policy "Authenticated users can insert or update complaints"
  on public.complaints for insert to authenticated
  with check (true);

create policy "Authenticated users can update complaints"
  on public.complaints for update to authenticated
  using (true)
  with check (true);

create policy "Authenticated users can read case tasks"
  on public.case_tasks for select to authenticated
  using (true);

create policy "Authenticated users can insert or update case tasks"
  on public.case_tasks for insert to authenticated
  with check (true);

create policy "Authenticated users can update case tasks"
  on public.case_tasks for update to authenticated
  using (true)
  with check (true);

create policy "Authenticated users can read audit logs"
  on public.audit_logs for select to authenticated
  using (true);

create policy "Authenticated users can insert audit logs"
  on public.audit_logs for insert to authenticated
  with check (true);

create policy "Anyone can read approved legal sources"
  on public.legal_sources for select to anon, authenticated
  using (status = 'APPROVED' or auth.role() = 'authenticated');

create policy "Authenticated users can manage legal sources"
  on public.legal_sources for all to authenticated
  using (true)
  with check (true);

create policy "Authenticated users can read and manage legal monitors"
  on public.legal_monitors for all to authenticated
  using (true)
  with check (true);

-- Storage RLS policies for private 'evidence' bucket
create policy "Authenticated users can upload evidence"
  on storage.objects for insert to authenticated
  with check (bucket_id = 'evidence');

create policy "Authenticated users can read evidence"
  on storage.objects for select to authenticated
  using (bucket_id = 'evidence');

