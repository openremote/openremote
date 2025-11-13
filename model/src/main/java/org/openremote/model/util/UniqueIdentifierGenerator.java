/*
 * Copyright 2024, OpenRemote Inc.
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

import com.devskiller.friendly_id.Url62;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.openremote.model.IdentifiableEntity;

/**
 * Generate a globally unique identifier value.
 * <p>
 * This is a Hibernate identifier generator as well: Assigns a random UUID if the persisted entity instance is of
 * type {@link IdentifiableEntity} and if its {@link IdentifiableEntity#getId()} method returns <code>null</code>.
 */
public class UniqueIdentifierGenerator {

    static final protected RandomBasedGenerator randomGenerator = Generators.randomBasedGenerator();
    static final protected NameBasedGenerator nameGenerator = Generators.nameBasedGenerator();

    /**
     * Generates a random UUID value encoded as Base62, 22 characters long.
     */
    public static String generateId() {
        return Url62.encode(randomGenerator.generate());
    }

    /**
     * Generates the same UUID value encoded as Base62, 22 characters long, if the name is the same.
     */
    public static String generateId(String name) {
        return Url62.encode(nameGenerator.generate(name));
    }

    /**
     * Generates the same UUID value encoded as Base62, 22 characters long, if the bytes are the same.
     */
    public static String generateId(byte[] bytes) {
        return Url62.encode(nameGenerator.generate(bytes));
    }
}
