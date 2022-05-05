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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaString;
import org.openremote.model.Constants;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.attribute.AttributeLink;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.console.ConsoleProviders;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.CronExpressionParser;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.value.impl.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Period;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@TsIgnore
public final class ValueType {

    /* SOME CUSTOM TYPES TO AVOID GENERIC TYPE SO THESE CAN BE CONSUMED IN VALUE DESCRIPTORS */
    public static class StringMap extends HashMap<String, String> {}
    public static class IntegerMap extends HashMap<String, Integer> {}
    public static class DoubleMap extends HashMap<String, Double> {}
    public static class BooleanMap extends HashMap<String, Double> {}
    public static class MultivaluedStringMap extends HashMap<String, List<String>> {}

    public static final ValueDescriptor<Boolean> BOOLEAN = new ValueDescriptor<>("boolean", Boolean.class);

    public static final ValueDescriptor<BooleanMap> BOOLEAN_MAP = new ValueDescriptor<>("booleanMap", BooleanMap.class);

    public static final ValueDescriptor<Integer> INTEGER = new ValueDescriptor<>("integer", Integer.class);

    public static final ValueDescriptor<Long> LONG = new ValueDescriptor<>("long", Long.class);

    public static final ValueDescriptor<BigInteger> BIG_INTEGER = new ValueDescriptor<>("bigInteger", BigInteger.class);

    public static final ValueDescriptor<IntegerMap> INTEGER_MAP = new ValueDescriptor<>("integerMap", IntegerMap.class);

    public static final ValueDescriptor<Double> NUMBER = new ValueDescriptor<>("number", Double.class);

    public static final ValueDescriptor<DoubleMap> NUMBER_MAP = new ValueDescriptor<>("numberMap", DoubleMap.class);

    public static final ValueDescriptor<BigDecimal> BIG_NUMBER = new ValueDescriptor<>("bigNumber", BigDecimal.class);

    public static final ValueDescriptor<String> TEXT = new ValueDescriptor<>("text", String.class);

    public static final ValueDescriptor<StringMap> TEXT_MAP = new ValueDescriptor<>("textMap", StringMap.class);

    public static final ValueDescriptor<MultivaluedStringMap> MULTIVALUED_TEXT_MAP = new ValueDescriptor<>("multivaluedTextMap", MultivaluedStringMap.class);

    public static final ValueDescriptor<ObjectNode> JSON_OBJECT = new ValueDescriptor<>("JSONObject", ObjectNode.class);

    public static final ValueDescriptor<ArrayNode> JSON_ARRAY = new ValueDescriptor<>("JSONArray", ArrayNode.class);

    public static final ValueDescriptor<BaseJsonNode> JSON = new ValueDescriptor<>("JSON", BaseJsonNode.class);

    public static final ValueDescriptor<Integer> POSITIVE_INTEGER = new ValueDescriptor<>("positiveInteger", Integer.class,
        new ValueConstraint.Min(0)
    );

    public static final ValueDescriptor<Integer> NEGATIVE_INTEGER = new ValueDescriptor<>("negativeInteger", Integer.class,
        new ValueConstraint.Max(0)
    );

    public static final ValueDescriptor<Double> POSITIVE_NUMBER = new ValueDescriptor<>("positiveNumber", Double.class,
        new ValueConstraint.Min(0)
    );

    public static final ValueDescriptor<Double> NEGATIVE_NUMBER = new ValueDescriptor<>("negativeNumber", Double.class,
        new ValueConstraint.Max(0)
    );

    public static final ValueDescriptor<Integer> INT_BYTE = new ValueDescriptor<>("integerByte", Integer.class,
        new ValueConstraint.Min(0),
        new ValueConstraint.Max(255)
    );

    public static final ValueDescriptor<Byte> BYTE = new ValueDescriptor<>("byte", Byte.class);

    public static final ValueDescriptor<Long> TIMESTAMP = new ValueDescriptor<>("timestamp", Long.class);

    public static final ValueDescriptor<String> TIMESTAMP_ISO8601 = new ValueDescriptor<>("timestampISO8601", String.class,
        new ValueConstraint.Pattern(Constants.ISO8601_DATETIME_REGEXP)
    );

    public static final ValueDescriptor<Date> DATE_AND_TIME = new ValueDescriptor<>("dateAndTime", Date.class);

    public static final ValueDescriptor<Duration> DURATION_TIME_ISO8601 = new ValueDescriptor<>("timeDurationISO8601", Duration.class,
        new ValueConstraint.Pattern(Constants.ISO8601_DURATION_REGEXP)
    );

    public static final ValueDescriptor<Period> DURATION_PERIOD_ISO8601 = new ValueDescriptor<>("periodDurationISO8601", Period.class,
        new ValueConstraint.Pattern(Constants.ISO8601_DURATION_REGEXP)
    );

    public static final ValueDescriptor<PeriodAndDuration> PERIOD_ISO8601 = new ValueDescriptor<>("timeAndPeriodDurationISO8601", PeriodAndDuration.class,
        new ValueConstraint.Pattern(Constants.ISO8601_DURATION_REGEXP)
    );

    public static final ValueDescriptor<String> EMAIL = new ValueDescriptor<>("email", String.class,
        new ValueConstraint.Pattern(Constants.EMAIL_REGEXP)
    );

    public static final ValueDescriptor<String> UUID = new ValueDescriptor<>("UUID", String.class, 
        new ValueConstraint.Pattern(Constants.UUID_REGEXP)
    );

    public static final ValueDescriptor<String> ASSET_ID = new ValueDescriptor<>("assetID", String.class,
        new ValueConstraint.Pattern(Constants.ASSET_ID_REGEXP)
    );

    /**
     * Allowed values constraint is added at runtime based on {@link org.openremote.model.util.ValueUtil}
     */
    public static final ValueDescriptor<String> ASSET_TYPE = new ValueDescriptor<>("assetType", String.class);

    public static final ValueDescriptor<Integer> DIRECTION = new ValueDescriptor<>("direction", Integer.class,
        new ValueConstraint.Min(0),
        new ValueConstraint.Max(359)
    );

    public static final ValueDescriptor<Integer> PORT = new ValueDescriptor<>("TCP_IPPortNumber", Integer.class,
        new ValueConstraint.Min(1),
        new ValueConstraint.Max(65536)
    );

    public static final ValueDescriptor<String> HOSTNAME_OR_IP_ADDRESS = new ValueDescriptor<>("hostOrIPAddress", String.class,
        new ValueConstraint.Pattern(Constants.HOSTNAME_OR_IP_REGEXP)
    );

    public static final ValueDescriptor<String> IP_ADDRESS = new ValueDescriptor<>("IPAddress", String.class,
        new ValueConstraint.Pattern(Constants.IP_REGEXP)
    );

    public static final ValueDescriptor<AttributeLink> ATTRIBUTE_LINK = new ValueDescriptor<>("attributeLink", AttributeLink.class);

    public static final ValueDescriptor<AttributeRef> ATTRIBUTE_REF = new ValueDescriptor<>("attributeReference", AttributeRef.class);

    public static final ValueDescriptor<AttributeState> ATTRIBUTE_STATE = new ValueDescriptor<>("attributeState", AttributeState.class);

    public static final ValueDescriptor<GeoJSONPoint> GEO_JSON_POINT = new ValueDescriptor<>("GEO_JSONPoint", GeoJSONPoint.class);

    public static final ValueDescriptor<CalendarEvent> CALENDAR_EVENT = new ValueDescriptor<>("calendarEvent", CalendarEvent.class);

    public static final ValueDescriptor<AttributeExecuteStatus> EXECUTION_STATUS = new ValueDescriptor<>("executionStatus", AttributeExecuteStatus.class);

    public static final ValueDescriptor<ConnectionStatus> CONNECTION_STATUS = new ValueDescriptor<>("connectionStatus", ConnectionStatus.class);

    public static final ValueDescriptor<ConsoleProviders> CONSOLE_PROVIDERS = new ValueDescriptor<>("consoleProviders", ConsoleProviders.class);

    public static final ValueDescriptor<ColourRGB> COLOUR_RGB = new ValueDescriptor<>("colourRGB", ColourRGB.class);

    public static final ValueDescriptor<OAuthGrant> OAUTH_GRANT = new ValueDescriptor<>("oAuthGrant", OAuthGrant.class);

    public static final ValueDescriptor<UsernamePassword> USERNAME_AND_PASSWORD = new ValueDescriptor<>("usernameAndPassword", UsernamePassword.class);

    public static final ValueDescriptor<ValueFormat> VALUE_FORMAT = new ValueDescriptor<>("valueFormat", ValueFormat.class);

    public static final ValueDescriptor<ValueConstraint> VALUE_CONSTRAINT = new ValueDescriptor<>("valueConstraint", ValueConstraint.class);

    public static final ValueDescriptor<AgentLink> VALUE_AGENT_LINK = new ValueDescriptor<>("agentLink", AgentLink.class);

    public static final ValueDescriptor<CronExpressionParser> CRON_EXPRESSION = new ValueDescriptor<>("CRONExpression", CronExpressionParser.class);

    public static final ValueDescriptor<String> HTTP_URL = new ValueDescriptor<>("HTTP_URL", String.class,
        new ValueConstraint.Pattern(Constants.HTTP_URL_REGEXP)
    );

    public static final ValueDescriptor<String> WS_URL = new ValueDescriptor<>("WS_URL", String.class,
        new ValueConstraint.Pattern(Constants.WS_URL_REGEXP)
    );

    public static final ValueDescriptor<AssetQuery> ASSET_QUERY = new ValueDescriptor<>("assetQuery", AssetQuery.class);

    protected ValueType() {
    }
}
