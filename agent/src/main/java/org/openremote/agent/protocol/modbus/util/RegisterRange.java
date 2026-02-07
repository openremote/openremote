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
package org.openremote.agent.protocol.modbus.util;

/**
 * Represents a range of Modbus registers (used for defining illegal/inaccessible registers).
 */
public class RegisterRange {
    private final int start;
    private final int end;

    public RegisterRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    /**
     * Check if a register address falls within this range.
     * @param register the register address to check
     * @return true if the register is within [start, end] inclusive
     */
    public boolean contains(int register) {
        return register >= start && register <= end;
    }

    @Override
    public String toString() {
        return "RegisterRange{" + start + "-" + end + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterRange that = (RegisterRange) o;
        return start == that.start && end == that.end;
    }

    @Override
    public int hashCode() {
        return 31 * start + end;
    }
}
