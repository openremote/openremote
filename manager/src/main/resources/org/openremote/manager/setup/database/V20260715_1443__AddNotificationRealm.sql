-- Adds and backfills the NOTIFICATION.realm column. NOTIFICATION/ASSET are unqualified so they resolve to the
-- manager schema (the connection's default schema); Keycloak's tables are in "public" so they're qualified, as the
-- migration connection's search_path may not include it.

-- add the realm column (initially nullable so existing rows can be backfilled)
ALTER TABLE NOTIFICATION ADD COLUMN realm VARCHAR(255);

-- backfill the realm of existing notifications from their target, mirroring NotificationService.resolveTargetRealm:
--   REALM target -> the target id is the realm name itself
--   ASSET target -> the asset's realm
--   USER  target -> the user's realm
UPDATE NOTIFICATION
    SET realm = TARGET_ID
    WHERE realm IS NULL AND TARGET = 'REALM';

UPDATE NOTIFICATION n
    SET realm = a.REALM
    FROM ASSET a
    WHERE n.realm IS NULL AND n.TARGET = 'ASSET' AND a.ID = n.TARGET_ID;

UPDATE NOTIFICATION n
    SET realm = r.name
    FROM public.user_entity u
    JOIN public.realm r ON r.id = u.realm_id
    WHERE n.realm IS NULL AND n.TARGET = 'USER' AND u.id = n.TARGET_ID;

-- any rows whose target could not be resolved (CUSTOM targets, or a since-deleted asset/user) fall back to the
-- default realm, matching resolveTargetRealm's fallback, so the NOT NULL constraint can be applied
UPDATE NOTIFICATION SET realm = 'master' WHERE realm IS NULL;

-- enforce NOT NULL to match the entity (@NotNull / @Column(nullable = false))
ALTER TABLE NOTIFICATION ALTER COLUMN realm SET NOT NULL;
