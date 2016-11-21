/* OpenRemote, the Home of the Digital Home.
* Copyright 2008-2010, OpenRemote Inc.
*
* See the contributors.txt file in the distribution for a
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
package org.openremote.controllercommand.db;

import java.sql.Types;

import org.hibernate.dialect.MySQL5Dialect;

/**
 * A customized SQL dialect for MySQL 5.x specific features.
 *
 * @author Dan 2009-2-6
 */
public class CustomMySQL5Dialect extends MySQL5Dialect {

    public CustomMySQL5Dialect() {
        super();
        // maps the boolean type to tinyint(1)
        registerColumnType(Types.BIT, "tinyint(1)");
    }

    /**
     * Sets the MySQL default charset to UTF-8
     */
    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8";
    }

}
