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
package org.openremote.container.persistence;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.openremote.model.IdentifiableEntity;

import java.io.Serializable;

/**
 * Generate a globally unique identifier value if the persisted entity instance is of
 * type {@link IdentifiableEntity} and if its {@link IdentifiableEntity#getId()} method returns <code>null</code>.
 */
public class UniqueIdentifierGenerator implements IdentifierGenerator {

    final protected RandomBasedGenerator generator = Generators.randomBasedGenerator();

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        if (object instanceof IdentifiableEntity) {
            IdentifiableEntity identifiableEntity = (IdentifiableEntity) object;
            if (identifiableEntity.getId() != null)
                return identifiableEntity.getId();
        }
        return generator.generate().toString();
    }
}
