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

-- 3. Row Level Security (RLS) Enablement & Permissions
alter table public.profiles enable row level security;
alter table public.saved_sources enable row level security;

revoke all on table public.profiles from anon, public;
revoke all on table public.saved_sources from anon, public;

grant select, insert, update, delete on table public.profiles to authenticated;
grant select, insert, update, delete on table public.saved_sources to authenticated;

grant all on table public.profiles to service_role;
grant all on table public.saved_sources to service_role;

-- 4. Role Protection & Auto-Profile Triggers
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
  insert into public.profiles (user_id, email, full_name, ward, preferred_language)
  values (
    new.id,
    coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data->>'full_name', split_part(coalesce(new.email, ''), '@', 1)),
    coalesce(new.raw_user_meta_data->>'ward', 'Ward 1'),
    case when new.raw_user_meta_data->>'preferred_language' in ('en', 'hi') then new.raw_user_meta_data->>'preferred_language' else 'en' end
  )
  on conflict (user_id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 5. RLS Policies: Profiles & Saved Sources
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

-- 6. Private Evidence Storage Bucket
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('evidence', 'evidence', false, 5242880, array['image/jpeg', 'image/png', 'image/webp', 'audio/webm', 'audio/wav'])
on conflict (id) do nothing;
