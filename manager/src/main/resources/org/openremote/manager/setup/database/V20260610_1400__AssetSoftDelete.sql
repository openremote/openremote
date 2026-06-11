-- Support soft delete for assets. Hard deletes are still used for leaf assets with no datapoints.
ALTER TABLE ${schemaName}.asset
    ADD COLUMN IF NOT EXISTS deleted_on TIMESTAMP WITH TIME ZONE;
