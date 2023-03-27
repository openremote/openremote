/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.model.persistence;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

public class LTreeType implements UserType<String[]> {

    public static final String TYPE = "ltree";

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<String[]> returnedClass() {
        return String[].class;
    }

    @Override
    public boolean equals(String[] x, String[] y) throws HibernateException {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(String[] x) throws HibernateException {
        return Arrays.hashCode(x);
    }

    @Override
    public String[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String ltreeStr = rs.getString(position);
        return ltreeStr != null ? ltreeStr.split("\\.") : null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String[] value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        st.setObject(index, value != null ? String.join(".", value) : null, Types.OTHER);
    }

    @Override
    public String[] deepCopy(String[] v) throws HibernateException {
        return v == null ? null : Arrays.copyOf(v, v.length);
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String[] value) throws HibernateException {
        return value;
    }

    @Override
    public String[] assemble(Serializable cached, Object owner) throws HibernateException {
        return deepCopy((String[])cached);
    }

    @Override
    public String[] replace(String[] original, String[] target, Object owner)
        throws HibernateException {
        // TODO Auto-generated method stub
        return deepCopy(original);
    }

}
