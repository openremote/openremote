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
package org.openremote.model;

public interface Constants {

    String KEYCLOAK_CLIENT_ID = "openremote";
    String MASTER_REALM = "master";
    String MASTER_REALM_ADMIN_USER = "admin";
    String REALM_ADMIN_ROLE = "admin";
    String READ_LOGS_ROLE = "read:logs";
    String READ_USERS_ROLE = "read:users";
    String READ_ADMIN_ROLE = "read:admin";
    String READ_MAP_ROLE = "read:map";
    String READ_ASSETS_ROLE = "read:assets";
    String READ_RULES_ROLE = "read:rules";
    String READ_APPS_ROLE = "read:apps";
    String WRITE_USER_ROLE = "write:user";
    String WRITE_ADMIN_ROLE = "write:admin";
    String WRITE_LOGS_ROLE = "write:logs";
    String WRITE_ASSETS_ROLE = "write:assets";
    String WRITE_ATTRIBUTES_ROLE = "write:attributes";
    String WRITE_RULES_ROLE = "write:rules";
    String AUTH_CONTEXT = "AUTH_CONTEXT";
    int ACCESS_TOKEN_LIFESPAN_SECONDS = 60; // 1 minute
    String PERSISTENCE_SEQUENCE_ID_GENERATOR = "SEQUENCE_ID_GENERATOR";
    String PERSISTENCE_UNIQUE_ID_GENERATOR = "UNIQUE_ID_GENERATOR";
    String PERSISTENCE_JSON_VALUE_TYPE = "json_value";
    String PERSISTENCE_JSON_OBJECT_TYPE = "json_object";
    String PERSISTENCE_JSON_ARRAY_TYPE = "json_array";
    String PERSISTENCE_STRING_ARRAY_TYPE = "string_array";
    String NAMESPACE = "urn:openremote";
    String PROTOCOL_NAMESPACE = NAMESPACE + ":protocol";
    String ASSET_NAMESPACE = NAMESPACE + ":asset";
    String ASSET_META_NAMESPACE = ASSET_NAMESPACE + ":meta";
    String DEFAULT_DATETIME_FORMAT ="dd. MMM yyyy HH:mm:ss zzz";
    String DEFAULT_DATETIME_FORMAT_MILLIS ="dd. MMM yyyy HH:mm:ss:SSS zzz";
    String DEFAULT_DATE_FORMAT ="dd. MMM yyyy";
    String DEFAULT_TIME_FORMAT ="HH:mm:ss";

    String SETUP_EMAIL_USER = "SETUP_EMAIL_USER";
    String SETUP_EMAIL_HOST = "SETUP_EMAIL_HOST";
    String SETUP_EMAIL_PASSWORD = "SETUP_EMAIL_PASSWORD";
    String SETUP_EMAIL_PORT = "SETUP_EMAIL_PORT";
    int SETUP_EMAIL_PORT_DEFAULT = 25;
    String SETUP_EMAIL_TLS = "SETUP_EMAIL_TLS";
    boolean SETUP_EMAIL_TLS_DEFAULT = true;
    String SETUP_EMAIL_FROM = "SETUP_EMAIL_FROM";
    String SETUP_EMAIL_FROM_DEFAULT = "no-reply@openremote.io";
    String REQUEST_HEADER_REALM = "Auth-Realm";

    String UNITS_TEMPERATURE_CELSIUS = "CELSIUS";
    String UNITS_TEMPERATURE_FAHRENHEIT = "FAHRENHEIT";
    String UNITS_TIME_HOURS = "HOURS";
    String UNITS_TIME_MINUTES = "MINUTES";
    String UNITS_TIME_SECONDS = "SECONDS";
    String UNITS_TIME_MILLISECONDS = "MILLISECONDS";
    String UNITS_SPEED_KNOTS = "SPEED_KNOTS";
    String UNITS_SPEED_KILOMETERS_HOUR = "SPEED_KMPH";
    String UNITS_SPEED_MILES_HOUR = "SPEED_MIPH";
    String UNITS_SPEED_METRES_SECOND = "SPEED_MPS";
    String UNITS_SOUND_DECIBELS = "DECIBELS";
    String UNITS_DISTANCE_KILOMETRES = "DISTANCE_KM";
    String UNITS_DISTANCE_MILES = "DISTANCE_MI";
    String UNITS_DISTANCE_METRES = "DISTANCE_M";
    String UNITS_DISTANCE_CENTIMETRES = "DISTANCE_CM";
    String UNITS_DISTANCE_MILLIMETRES = "DISTANCE_MM";
    String UNITS_FLOW_LPM = "FLOW_LPM";
    String UNITS_EUR_PER_KILOWATT_HOUR = "EUR_PER_KWH";
    String UNITS_EUR_PER_MONTH = "EUR_PER_MONTH";
    String UNITS_GBP_PER_KILOWATT_HOUR = "GBP_PER_KWH";
    String UNITS_GBP_PER_MONTH = "GBP_PER_MONTH";
    String UNITS_CURRENCY_EUR = "CURRENCY_EUR";
    String UNITS_CURRENCY_GBP = "CURRENCY_GBP";
    String UNITS_POWER_KILOWATT = "POWER_KW";
    String UNITS_POWER_WATT = "POWER_W";
    String UNITS_POWER_KILOWATT_PEAK = "POWER_KWP";
    String UNITS_POWER_PER_SQUARE_M = "POWER_PER_M2";
    String UNITS_ENERGY_KILOWATT_HOUR = "ENERGY_KWH";
    String UNITS_KILOGRAM_CARBON_PER_KILOWATT_HOUR = "KG_CARBON_PER_KWH";
    String UNITS_MASS_KILOGRAM = "MASS_KG";
    String UNITS_ANGLE_DEGREES = "ANGLE_DEGREES";
    String UNITS_ANGLE_RADIANS = "ANGLE_RADIANS";
    String UNITS_PERCENTAGE = "PERCENTAGE";
    String UNITS_DENSITY_KILOGRAMS_CUBIC_M = "DENSITY_KG_M3";
    String UNITS_DENSITY_MICROGRAMS_CUBIC_M = "DENSITY_UG_M3";
    String UNITS_ON_OFF = "ON_OFF";
    String UNITS_PRESSED_RELEASED = "PRESSED_RELEASED";
    String UNITS_COUNT_PER_HOUR = "COUNT_PER_HOUR";
    String UNITS_COUNT_PER_MINUTE = "COUNT_PER_MINUTE";
    String UNITS_COUNT_PER_SECOND = "COUNT_PER_SECOND";
}
