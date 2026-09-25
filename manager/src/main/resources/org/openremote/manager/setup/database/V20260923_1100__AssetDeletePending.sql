ALTER TABLE ${schemaName}.asset
    ADD COLUMN IF NOT EXISTS delete_pending boolean NOT NULL DEFAULT false;
