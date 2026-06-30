-- add the realm column to the notification table (initially nullable so existing rows can be backfilled)
ALTER table NOTIFICATION
    ADD COLUMN realm VARCHAR(255);

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
    FROM user_entity u
    JOIN realm r ON r.id = u.realm_id
    WHERE n.realm IS NULL AND n.TARGET = 'USER' AND u.id = n.TARGET_ID;

-- any rows whose target could not be resolved (CUSTOM targets, or a since-deleted asset/user) fall back to the
-- default realm, matching resolveTargetRealm's fallback, so the NOT NULL constraint can be applied
UPDATE NOTIFICATION SET realm = 'master' WHERE realm IS NULL;

-- enforce NOT NULL to match the entity (@NotNull / @Column(nullable = false))
ALTER table NOTIFICATION
    ALTER COLUMN realm SET NOT NULL;

-- add read:notifications and write:notifications Keycloak client roles for all openremote clients
DO $$
DECLARE
    v_client RECORD;
BEGIN
    FOR v_client IN
        SELECT id, realm_id
        FROM client
        WHERE client_id = 'openremote'
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM keycloak_role
            WHERE name = 'read:notifications' AND client = v_client.id
        ) THEN
            INSERT INTO keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client)
            VALUES (gen_random_uuid()::varchar(36), v_client.id, true, 'Read notifications', 'read:notifications', v_client.realm_id, v_client.id);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM keycloak_role
            WHERE name = 'write:notifications' AND client = v_client.id
        ) THEN
            INSERT INTO keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client)
            VALUES (gen_random_uuid()::varchar(36), v_client.id, true, 'Write notification data', 'write:notifications', v_client.realm_id, v_client.id);
        END IF;
    END LOOP;
END $$;