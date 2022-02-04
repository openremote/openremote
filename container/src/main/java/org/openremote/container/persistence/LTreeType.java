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
package org.openremote.container.persistence;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.openremote.model.Constants;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

public class LTreeType implements UserType {

    public static final String TYPE = Constants.PERSISTENCE_LTREE_TYPE;

    @Override
    public int[] sqlTypes() {
        return  new int[] {Types.OTHER};
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class returnedClass() {
        return String[].class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        String ltreeStr = rs.getString(names[0]);
        return ltreeStr != null ? ltreeStr.split("\\.") : null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        st.setObject(index, value != null ? String.join(".", (String[])value) : null, Types.OTHER);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        String[] v = (String[])value;
        return v == null ? null : Arrays.copyOf(v, v.length);
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (String[])value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner)
        throws HibernateException {
        // TODO Auto-generated method stub
        return deepCopy(original);
    }

}
