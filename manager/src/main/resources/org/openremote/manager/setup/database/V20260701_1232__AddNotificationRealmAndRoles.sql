-- add the realm column to the notification table (initially nullable so existing rows can be backfilled)
ALTER table ${schemaName}.NOTIFICATION
    ADD COLUMN realm VARCHAR(255);

-- backfill the realm of existing notifications from their target, mirroring NotificationService.resolveTargetRealm:
--   REALM target -> the target id is the realm name itself
--   ASSET target -> the asset's realm
--   USER  target -> the user's realm
-- NOTE: the manager runs in its own schema (${schemaName}) while Keycloak's tables live in "public", so the Keycloak
-- tables are referenced with an explicit public. prefix (the migration connection's search_path may not include it).
UPDATE ${schemaName}.NOTIFICATION
    SET realm = TARGET_ID
    WHERE realm IS NULL AND TARGET = 'REALM';

UPDATE ${schemaName}.NOTIFICATION n
    SET realm = a.REALM
    FROM ${schemaName}.ASSET a
    WHERE n.realm IS NULL AND n.TARGET = 'ASSET' AND a.ID = n.TARGET_ID;

UPDATE ${schemaName}.NOTIFICATION n
    SET realm = r.name
    FROM public.user_entity u
    JOIN public.realm r ON r.id = u.realm_id
    WHERE n.realm IS NULL AND n.TARGET = 'USER' AND u.id = n.TARGET_ID;

-- any rows whose target could not be resolved (CUSTOM targets, or a since-deleted asset/user) fall back to the
-- default realm, matching resolveTargetRealm's fallback, so the NOT NULL constraint can be applied
UPDATE ${schemaName}.NOTIFICATION SET realm = 'master' WHERE realm IS NULL;

-- enforce NOT NULL to match the entity (@NotNull / @Column(nullable = false))
ALTER table ${schemaName}.NOTIFICATION
    ALTER COLUMN realm SET NOT NULL;

-- add read:notifications and write:notifications Keycloak client roles for all openremote clients, and add them to
-- the existing "read"/"write" composite client roles so users assigned only the broad composites inherit them (as
-- defined by ClientRole.READ / ClientRole.WRITE: read -> read:notifications, write -> read:notifications + write:notifications)
DO $$
DECLARE
    v_client RECORD;
    v_read_notif_id  public.keycloak_role.id%TYPE;
    v_write_notif_id public.keycloak_role.id%TYPE;
    v_read_id        public.keycloak_role.id%TYPE;
    v_write_id       public.keycloak_role.id%TYPE;
BEGIN
    FOR v_client IN
        SELECT id, realm_id
        FROM public.client
        WHERE client_id = 'openremote'
    LOOP
        -- read:notifications leaf role (create if missing, then capture its id)
        SELECT id INTO v_read_notif_id FROM public.keycloak_role WHERE name = 'read:notifications' AND client = v_client.id;
        IF v_read_notif_id IS NULL THEN
            v_read_notif_id := gen_random_uuid()::varchar(36);
            INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client)
            VALUES (v_read_notif_id, v_client.id, true, 'Read notifications', 'read:notifications', v_client.realm_id, v_client.id);
        END IF;

        -- write:notifications leaf role (create if missing, then capture its id)
        SELECT id INTO v_write_notif_id FROM public.keycloak_role WHERE name = 'write:notifications' AND client = v_client.id;
        IF v_write_notif_id IS NULL THEN
            v_write_notif_id := gen_random_uuid()::varchar(36);
            INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client)
            VALUES (v_write_notif_id, v_client.id, true, 'Write notification data', 'write:notifications', v_client.realm_id, v_client.id);
        END IF;

        -- wire the leaf roles into the "read"/"write" composite roles (composite_role links parent -> child role id)
        SELECT id INTO v_read_id FROM public.keycloak_role WHERE name = 'read' AND client = v_client.id;
        SELECT id INTO v_write_id FROM public.keycloak_role WHERE name = 'write' AND client = v_client.id;

        IF v_read_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM public.composite_role WHERE composite = v_read_id AND child_role = v_read_notif_id
        ) THEN
            INSERT INTO public.composite_role (composite, child_role) VALUES (v_read_id, v_read_notif_id);
        END IF;

        IF v_write_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM public.composite_role WHERE composite = v_write_id AND child_role = v_read_notif_id
        ) THEN
            INSERT INTO public.composite_role (composite, child_role) VALUES (v_write_id, v_read_notif_id);
        END IF;

        IF v_write_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM public.composite_role WHERE composite = v_write_id AND child_role = v_write_notif_id
        ) THEN
            INSERT INTO public.composite_role (composite, child_role) VALUES (v_write_id, v_write_notif_id);
        END IF;
    END LOOP;
END $$;
