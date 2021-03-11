/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.setup;

import org.openremote.container.Container;
import org.openremote.container.persistence.Database;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.persistence.ManagerPersistenceService;

import javax.persistence.Persistence;
import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Export the SQL schema of {@link PersistenceService} to a file.
 */
public class SchemaExporter {

    private static final Logger LOG = Logger.getLogger(SchemaExporter.class.getName());

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            throw new IllegalArgumentException("Missing target file path argument");
        }
        File schemaFile = new File(args[0]);

        Container container = new Container(
            new ManagerPersistenceService() {

                @Override
                protected void openDatabase(org.openremote.model.Container container, Database database, String username, String password, String connectionUrl) {
                    // Ignore, we don't want to connect to the database when exporting schema
                }

                @Override
                public void start(org.openremote.model.Container container) throws Exception {
                    Properties createSchemaProperties = new Properties(persistenceUnitProperties);
                    createSchemaProperties.put(
                        "javax.persistence.schema-generation.scripts.action",
                        "create"
                    );
                    createSchemaProperties.put(
                        "javax.persistence.schema-generation.scripts.create-target",
                        schemaFile.getAbsolutePath()
                    );

                    if (schemaFile.exists()) {
                        LOG.info("Deleting existing schema file: " + schemaFile.getAbsolutePath());
                        schemaFile.delete();
                    }

                    LOG.info("Exporting database schema for persistence unit: " + persistenceUnitName);
                    Persistence.generateSchema(persistenceUnitName, createSchemaProperties);
                    LOG.fine("Schema export complete: " + schemaFile.getAbsolutePath());
                }
            }
        );

        container.start();
    }
}
