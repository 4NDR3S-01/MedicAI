-- MedicAI Supabase Schema
-- =============================================
-- 1. Extensions
create extension if not exists "uuid-ossp";
create extension if not exists pgcrypto;
-- create extension if not exists vector; -- optional for embeddings

-- 2. Profiles
create table if not exists public.profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  avatar_url text,
  settings jsonb default '{}'::jsonb,
  created_at timestamptz default now()
);

-- 3. Trigger to auto-create profile
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (user_id, settings)
  values (new.id, jsonb_build_object(
    'theme','system',
    'dailyAiLimit', 20,
    'notificationsEnabled', true,
    'locale','es'
  ));
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- 4. Chat threads
create table if not exists public.ai_threads (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text,
  summary text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- 5. Chat messages
create table if not exists public.ai_messages (
  id uuid primary key default gen_random_uuid(),
  thread_id uuid not null references public.ai_threads(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text check (role in ('user','assistant','system')) not null,
  content text not null,
  meta jsonb,
  created_at timestamptz default now()
);

create index if not exists ai_messages_thread_created_idx on public.ai_messages(thread_id, created_at);
create index if not exists ai_messages_user_created_idx on public.ai_messages(user_id, created_at);

-- 6. Medications
create table if not exists public.medications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  dose numeric,
  unit text,
  instructions text,
  created_at timestamptz default now()
);

-- 7. Reminders
create table if not exists public.reminders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  medication_id uuid references public.medications(id) on delete cascade,
  schedule_type text check (schedule_type in ('FIXED_TIMES','INTERVAL','PRN')) not null,
  times text[],                -- for FIXED_TIMES HH:MM
  interval_hours int,          -- for INTERVAL
  timezone text default 'UTC',
  next_run_at timestamptz,
  enabled boolean default true,
  created_at timestamptz default now()
);

-- 8. Reminder logs
create table if not exists public.reminder_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  reminder_id uuid not null references public.reminders(id) on delete cascade,
  status text check (status in ('taken','skipped','missed')) not null,
  taken_at timestamptz default now(),
  note text
);

create index if not exists reminder_logs_user_taken_idx on public.reminder_logs(user_id, taken_at desc);
create index if not exists reminder_logs_reminder_taken_idx on public.reminder_logs(reminder_id, taken_at desc);

-- 9. Trigger to touch thread updated_at
create or replace function public.touch_ai_thread()
returns trigger language plpgsql as $$
begin
  update public.ai_threads set updated_at = now() where id = new.thread_id;
  return new;
end;
$$;

drop trigger if exists ai_messages_touch_thread on public.ai_messages;
create trigger ai_messages_touch_thread
after insert on public.ai_messages
for each row execute function public.touch_ai_thread();

-- 10. Helper: count user messages today
create or replace function public.user_daily_ai_messages(p_user uuid)
returns integer language sql stable as $$
  select count(*) from public.ai_messages
  where user_id = p_user
    and role = 'user'
    and created_at::date = now()::date;
$$;

-- 11. View overview threads
create or replace view public.v_threads_overview as
select
  t.*,
  (select content from public.ai_messages m where m.thread_id = t.id order by created_at desc limit 1) as last_message,
  (select count(*) from public.ai_messages m2 where m2.thread_id = t.id) as message_count
from public.ai_threads t;

-- 12. Enable RLS
alter table public.profiles enable row level security;
alter table public.ai_threads enable row level security;
alter table public.ai_messages enable row level security;
alter table public.medications enable row level security;
alter table public.reminders enable row level security;
alter table public.reminder_logs enable row level security;

-- 13. Policies
-- Profiles
DROP POLICY IF EXISTS select_own_profile ON public.profiles;
CREATE POLICY select_own_profile ON public.profiles FOR SELECT USING (user_id = auth.uid());
DROP POLICY IF EXISTS update_own_profile ON public.profiles;
CREATE POLICY update_own_profile ON public.profiles FOR UPDATE USING (user_id = auth.uid());

-- Threads
DROP POLICY IF EXISTS crud_threads ON public.ai_threads;
CREATE POLICY crud_threads ON public.ai_threads FOR ALL USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

-- Messages
DROP POLICY IF EXISTS select_messages ON public.ai_messages;
CREATE POLICY select_messages ON public.ai_messages FOR SELECT USING (user_id = auth.uid());
DROP POLICY IF EXISTS insert_messages ON public.ai_messages;
CREATE POLICY insert_messages ON public.ai_messages FOR INSERT WITH CHECK (user_id = auth.uid());
DROP POLICY IF EXISTS update_messages ON public.ai_messages;
CREATE POLICY update_messages ON public.ai_messages FOR UPDATE USING (user_id = auth.uid());
DROP POLICY IF EXISTS delete_messages ON public.ai_messages;
CREATE POLICY delete_messages ON public.ai_messages FOR DELETE USING (user_id = auth.uid());

-- Medications
DROP POLICY IF EXISTS crud_meds ON public.medications;
CREATE POLICY crud_meds ON public.medications FOR ALL USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

-- Reminders
DROP POLICY IF EXISTS crud_reminders ON public.reminders;
CREATE POLICY crud_reminders ON public.reminders FOR ALL USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

-- Reminder logs
DROP POLICY IF EXISTS crud_logs ON public.reminder_logs;
CREATE POLICY crud_logs ON public.reminder_logs FOR ALL USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

-- 14. Safe insert user message enforcing daily limit
create or replace function public.safe_insert_user_message(p_thread uuid, p_content text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_count int;
  v_limit int;
  v_id uuid;
begin
  v_limit := coalesce(((select settings->>'dailyAiLimit' from profiles where user_id = auth.uid())::int), 20);
  v_count := public.user_daily_ai_messages(auth.uid());
  if v_count >= v_limit then
    raise exception 'Límite diario alcanzado (%).', v_limit using errcode = 'P0001';
  end if;

  insert into public.ai_messages(id, thread_id, user_id, role, content)
  values (gen_random_uuid(), p_thread, auth.uid(), 'user', p_content)
  returning id into v_id;
  return v_id;
end;
$$;

grant execute on function public.safe_insert_user_message(uuid,text) to authenticated;

-- 15. Seed example (commented)
-- insert into public.ai_threads(user_id,title) values ('<USER_ID>','Ejemplo');
-- insert into public.ai_messages(thread_id,user_id,role,content) values ((select id from public.ai_threads limit 1),'<USER_ID>','assistant','Hola, soy tu asistente.');

-- END
