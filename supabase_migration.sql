-- ============================================================
-- Supabase Full Reset + New Architecture Setup
-- ============================================================
-- Run this in the Supabase SQL Editor.
-- This nukes everything in the public schema and creates a
-- single `entries` table with a permissive RLS policy.
-- ============================================================

-- 1. Drop ALL policies on all tables in public schema
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT schemaname, tablename, policyname
        FROM pg_policies
        WHERE schemaname = 'public'
    ) LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I', r.policyname, r.schemaname, r.tablename);
    END LOOP;
END $$;

-- 2. Drop ALL functions in public schema
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT ns.nspname, p.proname, pg_get_function_identity_arguments(p.oid) AS args
        FROM pg_proc p
        JOIN pg_namespace ns ON p.pronamespace = ns.oid
        WHERE ns.nspname = 'public'
    ) LOOP
        EXECUTE format('DROP FUNCTION IF EXISTS %I.%I(%s) CASCADE', r.nspname, r.proname, r.args);
    END LOOP;
END $$;

-- 3. Drop ALL tables in public schema
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
    ) LOOP
        EXECUTE format('DROP TABLE IF EXISTS public.%I CASCADE', r.tablename);
    END LOOP;
END $$;

-- 4. Drop ALL views in public schema
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT viewname
        FROM pg_views
        WHERE schemaname = 'public'
    ) LOOP
        EXECUTE format('DROP VIEW IF EXISTS public.%I CASCADE', r.viewname);
    END LOOP;
END $$;

-- 5. Drop ALL types in public schema (enums, composites)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT typname
        FROM pg_type t
        JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE n.nspname = 'public'
          AND t.typtype IN ('e', 'c')
    ) LOOP
        EXECUTE format('DROP TYPE IF EXISTS public.%I CASCADE', r.typname);
    END LOOP;
END $$;

-- ============================================================
-- New Schema: Single table, no auth, permissive RLS
-- ============================================================

-- 6. Create the `entries` table
CREATE TABLE entries (
    id TEXT PRIMARY KEY,
    encrypted_payload TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 7. Enable RLS (Supabase requires it)
ALTER TABLE entries ENABLE ROW LEVEL SECURITY;

-- 8. Permissive policy — always true
--    Silences the "no RLS policy" warning.
--    Security is from client-side AES-256-GCM encryption, not RLS.
CREATE POLICY "allow_all" ON entries
    FOR ALL
    USING (true)
    WITH CHECK (true);
