-- V4 Migration: Flatten Schema — Remove Redundant `accounts` Table and Migrate to User-Level Isolation

-- 1. Ensure user_id column exists on account_knowledge_configs (if previously created without it)
ALTER TABLE IF EXISTS account_knowledge_configs ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;

-- 2. Populate user_id from matching user row if null
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'account_knowledge_configs' AND column_name = 'account_id'
    ) THEN
        UPDATE account_knowledge_configs akc
        SET user_id = u.id
        FROM users u
        WHERE u.account_id = akc.account_id AND akc.user_id IS NULL;
    END IF;
END $$;

-- 3. Rename account_knowledge_configs to user_knowledge_configs if table exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = 'account_knowledge_configs' AND table_schema = 'public'
    ) THEN
        ALTER TABLE account_knowledge_configs RENAME TO user_knowledge_configs;
    END IF;
END $$;

-- 4. Ensure user_id is NOT NULL in user_knowledge_configs
ALTER TABLE IF EXISTS user_knowledge_configs ALTER COLUMN user_id SET NOT NULL;

-- 5. Drop obsolete account_id column and FK constraint from user_knowledge_configs if present
ALTER TABLE IF EXISTS user_knowledge_configs DROP CONSTRAINT IF EXISTS account_knowledge_configs_account_id_fkey;
ALTER TABLE IF EXISTS user_knowledge_configs DROP COLUMN IF EXISTS account_id;

-- 6. Add user_id column to vector_chunks and drop account_id
ALTER TABLE IF EXISTS vector_chunks ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE IF EXISTS vector_chunks DROP CONSTRAINT IF EXISTS vector_chunks_account_id_fkey;
ALTER TABLE IF EXISTS vector_chunks DROP COLUMN IF EXISTS account_id;
CREATE INDEX IF NOT EXISTS idx_vector_chunks_user_id ON vector_chunks(user_id);

-- 7. Drop account_id FK column and constraint from users table
ALTER TABLE IF EXISTS users DROP CONSTRAINT IF EXISTS users_account_id_fkey;
ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS account_id;

-- 8. Drop redundant accounts table safely without CASCADE (so child tables are preserved)
DROP TABLE IF EXISTS accounts;
