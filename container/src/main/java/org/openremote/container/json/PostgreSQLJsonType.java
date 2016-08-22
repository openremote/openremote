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
package org.openremote.container.json;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public abstract class PostgreSQLJsonType implements UserType {

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return assemble(disassemble(value), null);
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.JAVA_OBJECT};
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == null && y == null || x != null && y != null && disassemble(x).equals(disassemble(y));
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return disassemble(x).hashCode();
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return assemble(disassemble(original), owner);
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        String json = resultSet.getString(names[0]);
        return json == null ? null : assemble(json, null);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index,
                            SessionImplementor session) throws HibernateException, SQLException {
        final String json = value == null ? null : disassemble(value).toString();
        PGobject pgo = new PGobject();
        pgo.setType("jsonb");
        pgo.setValue(json);
        st.setObject(index, pgo);
    }
}
