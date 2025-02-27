package org.openremote.manager.setup.database;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20250210_01__AddNotificationRealm extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        
        context.getConnection().createStatement().execute(
            "ALTER TABLE NOTIFICATION ADD COLUMN IF NOT EXISTS REALM VARCHAR(255)"
        );

        context.getConnection().createStatement().execute(
            "UPDATE NOTIFICATION n " + 
            "SET REALM =(SELECT a.REALM FROM ASSET a WHERE a.ID = n.TARGET_ID) " +
            "WHERE n.TARGET = 'ASSET'"
        );

        context.getConnection().createStatement().execute(
            "UPDATE NOTIFICATION n " + 
            "SET REALM = n.SOURCE_ID " +
            "WHERE n.SOURCE = 'REALM_RULESET'"
        );
    }
}
