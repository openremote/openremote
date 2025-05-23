/*
 * Copyright 2025, OpenRemote Inc.
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

package org.openremote.model.util;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.openremote.model.IdentifiableEntity;

import java.io.Serializable;

public class HibernateUniqueIdentifierGeneratorAssignable implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        if (object instanceof IdentifiableEntity) {
            IdentifiableEntity<?> identifiableEntity = (IdentifiableEntity<?>) object;
            if (identifiableEntity.getId() != null)
                return identifiableEntity.getId();
        }
        return UniqueIdentifierGenerator.generateId();
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }
}
