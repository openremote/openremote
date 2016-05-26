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
package org.openremote.manager.shared.ngsi.simplequery;

public class Query {

    public static final String STATEMENT_SEPARATOR = ";";

    public Statement[] statements;

    public Query() {
    }

    public Query(Statement... statements) {
        this.statements = statements;
    }

    public Query statements(Statement... statements) {
        this.statements = statements;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (statements != null) {
            for (Statement statement : statements) {
                sb.append(statement).append(STATEMENT_SEPARATOR);
            }
        }
        return sb.toString();
    }

    public static Query valueOf(String query) {
        if (query == null)
            return null;
        String[] split = query.split(STATEMENT_SEPARATOR);
        Statement[] statements = new Statement[split.length];
        for (int i = 0; i < statements.length; i++) {
            statements[i] = new Statement(split[i]);
        }
        return new Query(statements);
    }
}
