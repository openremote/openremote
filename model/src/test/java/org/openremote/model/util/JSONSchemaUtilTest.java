package org.openremote.model.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class JSONSchemaUtilTest {

    @BeforeEach
    void setup() {
        ValueUtil.doInitialise();
    }

    private static URL loadResource(String resourcePath) {
        return JSONSchemaUtilTest.class.getResource("/org/openremote/model/util/" + resourcePath + ".json");
    }

    @ParameterizedTest
    @ValueSource(classes = {
            // model.value
            org.openremote.model.value.Units.class,
            org.openremote.model.value.ValueConstraint.class,
            org.openremote.model.value.ValueDescriptor.class,
            org.openremote.model.value.ValueFormat.class,
            org.openremote.model.value.ValueType.BooleanMap.class,
            org.openremote.model.value.ValueType.DoubleMap.class,
            org.openremote.model.value.ValueType.IntegerMap.class,
            org.openremote.model.value.ValueType.ObjectMap.class,
            org.openremote.model.value.ValueType.StringMap.class,
            org.openremote.model.value.ValueType.MultivaluedStringMap.class,
            org.openremote.model.value.ForecastConfiguration.class,
            // model.asset.agent
            org.openremote.model.asset.agent.AgentLink.class,
            org.openremote.model.asset.agent.ConnectionStatus.class,
            // model.attribute
            org.openremote.model.attribute.AttributeExecuteStatus.class,
            org.openremote.model.attribute.AttributeLink.class,
            org.openremote.model.attribute.AttributeRef.class,
            org.openremote.model.attribute.AttributeState.class,
            // model.console
            org.openremote.model.console.ConsoleProviders.class,
            // model.query
            org.openremote.model.query.AssetQuery.class,
            // model.calendar
            org.openremote.model.calendar.CalendarEvent.class,
            // model.value.impl
            org.openremote.model.value.impl.ColourRGB.class,
            org.openremote.model.value.impl.PeriodAndDuration.class,
            // model.value.util
            org.openremote.model.util.CronExpressionParser.class,
            // model.value.geo
            org.openremote.model.geo.GeoJSONPoint.class,
            // model.value.auth
            org.openremote.model.auth.UsernamePassword.class,
            org.openremote.model.auth.OAuthGrant.class,
            // java.lang
            java.lang.Object.class,
            java.lang.String.class,
            java.lang.Integer.class,
            java.lang.Long.class,
            java.lang.Boolean.class,
            java.lang.Double.class,
            java.lang.Byte.class,
            // java.math
            java.math.BigDecimal.class,
            java.math.BigInteger.class,
            // java.util
            java.util.Date.class,
            // java.time
            java.time.Duration.class,
            java.time.Period.class
    })
    public void validateJSONSchemas(Class<?> clazz) throws IOException, JSONException {
        JsonNode expected = ValueUtil.JSON.readTree(loadResource(clazz.getName()));
        JsonNode actual = ValueUtil.getSchema(clazz);
        System.out.println(expected);
        System.out.println(actual);
        assertEquals(expected.toString(), actual.toString(), false);
    }
}
