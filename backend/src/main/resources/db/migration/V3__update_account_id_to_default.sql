-- V3 Migration: Update existing table data to default Account ID '65dedb05-5255-42ec-97f7-a9595bb74c2a'

-- 1. Ensure target default account exists in accounts table
INSERT INTO accounts (id, company_name, account_key, created_at, updated_at)
VALUES ('65dedb05-5255-42ec-97f7-a9595bb74c2a'::uuid, 'Demo Account', 'demo-account-65dedb05', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 2. Update users table to target account_id
UPDATE users 
SET account_id = '65dedb05-5255-42ec-97f7-a9595bb74c2a'::uuid 
WHERE account_id IS NULL OR account_id != '65dedb05-5255-42ec-97f7-a9595bb74c2a'::uuid;

-- 3. Update account_knowledge_configs table to target account_id
UPDATE account_knowledge_configs 
SET account_id = '65dedb05-5255-42ec-97f7-a9595bb74c2a'::uuid 
WHERE account_id IS NULL OR account_id != '65dedb05-5255-42ec-97f7-a9595bb74c2a'::uuid;

-- 4. Update vector_chunks table to target account_id
UPDATE vector_chunks 
SET account_id = '65dedb05-5255-42ec-97f7-a9595bb74c2a'::uuid 
WHERE account_id IS NULL OR account_id != '65dedb05-5255-42ec-97f7-a9595bb74c2a'::uuid;
