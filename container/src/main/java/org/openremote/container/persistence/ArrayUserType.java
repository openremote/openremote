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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;

/**
 * Map Postgres/JDBC text array to Java String array.
 */
public class ArrayUserType implements UserType {

    protected static final int[] SQL_TYPES = {Types.ARRAY};

    public final int[] sqlTypes() {
        return SQL_TYPES;
    }

    public final Class returnedClass() {
        return String[].class;
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        if (resultSet.wasNull()) {
            return null;
        }
        return resultSet.getArray(names[0]).getArray();
    }

    @Override
    public void nullSafeSet(PreparedStatement statement, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            statement.setNull(index, SQL_TYPES[0]);
        } else {
            String[] castObject = (String[]) value;
            Array array = session.connection().createArrayOf("text", castObject);
            statement.setArray(index, array);
        }
    }

    @Override
    public final Object deepCopy(final Object value) throws HibernateException {
        return value;
    }

    @Override
    public final boolean isMutable() {
        return false;
    }

    @Override
    public final boolean equals(final Object x, final Object y) throws HibernateException {
        if (x == y) {
            return true;
        } else if (x == null || y == null) {
            return false;
        } else {
            return x.equals(y);
        }
    }

    @Override
    public final int hashCode(final Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public final Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
        return original;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (String[]) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }
}