-- Adds and backfills the NOTIFICATION.realm column. NOTIFICATION/ASSET are unqualified so they resolve to the
-- manager schema (the connection's default schema). USER-targeted and CLIENT-sourced rows require a Keycloak API
-- lookup to resolve correctly; that is handled by the companion Java migration V20260715_1443_1, which also applies
-- the master fallback and the NOT NULL constraint once all resolvable rows are filled.

-- add the realm column (initially nullable so existing rows can be backfilled)
ALTER TABLE NOTIFICATION ADD COLUMN realm VARCHAR(255);

-- backfill the realm of existing notifications from their target, mirroring NotificationService.resolveTargetRealm:
--   REALM target -> the target id is the realm name itself
--   ASSET target -> the asset's realm
UPDATE NOTIFICATION
    SET realm = TARGET_ID
    WHERE realm IS NULL AND TARGET = 'REALM';

UPDATE NOTIFICATION n
    SET realm = a.REALM
    FROM ASSET a
    WHERE n.realm IS NULL AND n.TARGET = 'ASSET' AND a.ID = n.TARGET_ID;

-- backfill from the sender for rows whose target could not be resolved:
--   REALM_RULESET source -> the source id is the realm name itself
--   ASSET_RULESET source -> the source asset's realm
UPDATE NOTIFICATION
    SET realm = SOURCE_ID
    WHERE realm IS NULL AND SOURCE = 'REALM_RULESET';

UPDATE NOTIFICATION n
    SET realm = a.REALM
    FROM ASSET a
    WHERE n.realm IS NULL AND n.SOURCE = 'ASSET_RULESET' AND a.ID = n.SOURCE_ID;
