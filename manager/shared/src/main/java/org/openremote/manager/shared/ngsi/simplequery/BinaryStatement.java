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
package org.openremote.manager.shared.ngsi.simplequery;

public class BinaryStatement extends Statement {

    public enum Operator {
        EQUAL("=="),
        UNEQUAL("!="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_OR_EQUAL_THAN(">="),
        LESS_OR_EQUAL_THAN("<=");

        String op;

        Operator(String op) {
            this.op = op;
        }

        @Override
        public String toString() {
            return op;
        }
    }

    public BinaryStatement(String statement) {
        super(statement);
    }

    public BinaryStatement(String attributeName, Operator operator, QueryValue queryValue) {
        this(attributeName + operator + queryValue);
    }
}
