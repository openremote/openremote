-- add a nullable realm column to the notification table
ALTER table NOTIFICATION
    ADD COLUMN realm VARCHAR(255);

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