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

import com.vladmihalcea.hibernate.type.MutableType;
import com.vladmihalcea.hibernate.type.util.Configuration;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.jdbc.BigIntJdbcType;

import java.time.Instant;

public class EpochMillisInstantType extends MutableType<Instant, BigIntJdbcType, InstantJavaType> {

    public static final String TYPE_NAME = "epoch-millis-instant";

    public EpochMillisInstantType() {
        super(
            Instant.class,
            BigIntJdbcType.INSTANCE,
            InstantJavaType.INSTANCE
        );
    }

    public EpochMillisInstantType(Configuration configuration) {
        super(
            Instant.class,
            BigIntJdbcType.INSTANCE,
            InstantJavaType.INSTANCE,
            configuration
        );
    }

    public EpochMillisInstantType(org.hibernate.type.spi.TypeBootstrapContext typeBootstrapContext) {
        this(new Configuration(typeBootstrapContext.getConfigurationSettings()));
    }

    public String getName() {
        return TYPE_NAME;
    }
}
