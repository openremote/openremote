/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.container.persistence;

import org.apache.commons.io.IOUtils;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;
import org.hibernate.tool.hbm2ddl.SingleLineSqlCommandExtractor;

import java.io.IOException;
import java.io.Reader;

/**
 * Load and parse import SQL files, either the whole file as a single statement if its first line
 * is <code>-- importOneStatementOnly</code>, or as a semicolon-separated list of statements.
 */
@SuppressWarnings("deprecation")
public class EnhancedImportSqlCommandExtractor implements ImportSqlCommandExtractor {

    @Override
    public String[] extractCommands(Reader reader) {
        try {
            String sql = IOUtils.toString(reader);
            if (sql.startsWith("-- importOneStatementOnly")) {
                return new SingleLineSqlCommandExtractor().extractCommands(reader);
            } else {
                return new MultipleLinesSqlCommandExtractor().extractCommands(reader);
            }
        } catch (IOException e) {
            throw new org.hibernate.tool.hbm2ddl.ImportScriptException("Error during import script parsing.", e);
        }
    }
}
