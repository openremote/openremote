/*
 * Copyright 2020, OpenRemote Inc.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;

/**
 * A number comparator based on:
 * https://stackoverflow.com/questions/2683202/comparing-the-values-of-two-generic-numbers
 */
public final class NumberComparator implements Comparator<Number> {

    public int compare(Number x, Number y) {
        if(isSpecial(x) || isSpecial(y))
            return Double.compare(x.doubleValue(), y.doubleValue());
        else
            return toBigDecimal(x).compareTo(toBigDecimal(y));
    }

    private static boolean isSpecial(Number x) {
        boolean specialDouble = x instanceof Double
            && (Double.isNaN((Double) x) || Double.isInfinite((Double) x));
        boolean specialFloat = x instanceof Float
            && (Float.isNaN((Float) x) || Float.isInfinite((Float) x));
        return specialDouble || specialFloat;
    }

    private static BigDecimal toBigDecimal(Number number) {
        if(number instanceof BigDecimal)
            return (BigDecimal) number;
        if(number instanceof BigInteger)
            return new BigDecimal((BigInteger) number);
        if(number instanceof Byte || number instanceof Short
            || number instanceof Integer || number instanceof Long)
            return new BigDecimal(number.longValue());
        if(number instanceof Float || number instanceof Double)
            return new BigDecimal(number.doubleValue());

        try {
            return new BigDecimal(number.toString());
        } catch(final NumberFormatException e) {
            throw new RuntimeException("The given number (\"" + number + "\" of class " + number.getClass().getName() + ") does not have a parsable string representation", e);
        }
    }
}
