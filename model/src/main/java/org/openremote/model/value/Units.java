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
package org.openremote.model.value;

import org.openremote.model.util.TsIgnore;

import com.fasterxml.jackson.annotation.JsonValue;

@TsIgnore
public enum Units {
    PER("per"),
    SQUARED("squared"),
    CUBED("cubed"),
    PEAK("peak"),
    MEGA("mega"),
    KILO("kilo"),
    CENTI("centi"),
    HECTO("hecto"),
    MICRO("micro"),
    MILLI("milli"),
    PERCENTAGE("percentage"),
    ACRE("acre"),
    HECTARE("hectare"),
    DECIBEL("decibel"),
    DECIBEL_ATTENUATED("decibel_attenuated"),
    KNOT("knot"),
    CELSIUS("celsius"),
    KELVIN("kelvin"),
    FAHRENHEIT("fahrenheit"),
    YEAR("year"),
    MONTH("month"),
    WEEK("week"),
    DAY("day"),
    HOUR("hour"),
    MINUTE("minute"),
    SECOND("second"),
    METRE("metre"),
    INCH("inch"),
    FOOT("foot"),
    YARD("yard"),
    MILE("mile"),
    MILE_SCANDINAVIAN("mile_scandinavian"),
    GRAM("gram"),
    OUNCE("ounce"),
    MASS_POUND("pound"),
    STONE("stone"),
    DEGREE("degree"),
    RADIAN("radian"),
    LITRE("litre"),
    GALLON("gallon"),
    FLUID_OUNCE("fluid_ounce"),
    JOULE("joule"),
    BTU("btu"),
    WATT("watt"),
    LUX("lux"),
    LUMEN("lumen"),
    PASCAL("pascal"),
    BAR("bar"),
    IN_HG("inch_mercury"),
    VOLT("volt"),
    OHM("ohm"),
    AMP("amp"),
    HERTZ("hertz"),
    RPM("rpm"),
    PART_PER_MILLION("ppm"),
    CARBON("carbon"),
    EUR("eur"),
    VAR("var");

    private final String value;

    Units(final String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static Units[] units(Units...units) {
        return units;
    }
}
