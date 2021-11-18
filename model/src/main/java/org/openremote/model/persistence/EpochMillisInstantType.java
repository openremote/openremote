/*
 * Copyright 2021, OpenRemote Inc.
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

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.LiteralType;
import org.hibernate.type.VersionType;
import org.hibernate.type.descriptor.java.InstantJavaDescriptor;
import org.hibernate.type.descriptor.sql.BigIntTypeDescriptor;

import java.time.Instant;
import java.util.Comparator;

public class EpochMillisInstantType extends AbstractSingleColumnStandardBasicType<Instant>
    implements VersionType<Instant>, LiteralType<Instant> {

    public static final String TYPE_NAME = "epoch-millis-instant";

    public EpochMillisInstantType() {
        super( BigIntTypeDescriptor.INSTANCE, InstantJavaDescriptor.INSTANCE );
    }

    @Override
    public String objectToSQLString(Instant value, Dialect dialect) throws Exception {
        return Long.toString(value.toEpochMilli());
    }

    @Override
    public Instant seed(SharedSessionContractImplementor session) {
        return Instant.now();
    }

    @Override
    public Instant next(Instant current, SharedSessionContractImplementor session) {
        return Instant.now();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Comparator<Instant> getComparator() {
        return ComparableComparator.INSTANCE;
    }

    @Override
    public String getName() {
        return TYPE_NAME;
    }
}
