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
package org.openremote.model.util;

import java.io.Serializable;
import java.util.Objects;

public class Triplet<A, B, C> implements Serializable {
    public A value1;
    public B value2;
    public C value3;

    public Triplet(A value1, B value2, C value3) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
    }

    public A getValue1() {
        return value1;
    }

    public B getValue2() {
        return value2;
    }

    public C getValue3() {
        return value3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Triplet) {
            Triplet<?,?,?> other = (Triplet<?,?,?>) o;
            return (Objects.equals(value1, other.value1))
                    && (Objects.equals(value2, other.value2))
                    && (Objects.equals(value3, other.value3));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value1, value2, value3);
    }

    @Override
    public String toString() {
        return value1 + ":" + value2 + ":" + value3;
    }
}
