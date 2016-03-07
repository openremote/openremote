package org.openremote.test.contextbroker;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import org.junit.Test;
import org.openremote.manager.shared.model.ngsi.*;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NgsiModelTest {

    private static final Logger LOG = Logger.getLogger(NgsiModelTest.class.getName());

    @Test
    public void validateModel() {
        assertEquals(0, Model.validateField("SomeField").length);
        assertEquals(0, Model.validateField("!@%^*()[]{}<>-=+.,;:|\\~_'`").length);
        assertEquals(1, Model.validateField(" ").length);
        assertEquals(1, Model.validateField("&").length);
        assertEquals(1, Model.validateField("?").length);
        assertEquals(1, Model.validateField("/").length);
        assertEquals(1, Model.validateField("#").length);
    }

    @Test
    public void parseModel() {
        String entityString = "{" +
            "    \"id\": \"Room1\"," +
            "    \"type\": \"Room\"," +
            "    \"pressure\":{" +
            "        \"type\": \"integer\"," +
            "        \"value\": \"720\"," +
            "        \"metadata\":{}" +
            "    }," +
            "    \"temperature\":{" +
            "        \"type\": \"float\"," +
            "        \"value\": \"26.5\"," +
            "        \"metadata\":{" +
            "            \"accuracy\":{" +
            "                \"type\": \"float\"," +
            "                \"value\": \"0.8\"" +
            "            }" +
            "        }" +
            "    }" +
            "}";

        JsonObject jsonEntity = Json.parse(entityString); // Use Json.parse()
        Entity entity = new Entity(jsonEntity);
        validateSample(entity);

        // Roundtrip
        entityString = entity.getJsonObject().toJson();
        jsonEntity = JsonUtil.parse(entityString); // Or JsonUtil.parse()
        entity = new Entity(jsonEntity);
        validateSample(entity);
    }

    protected void validateSample(Entity entity) {
        ModelValidationError[] errors = entity.validate();
        assertEquals(0, errors.length);

        assertEquals("Room1", entity.getId());
        assertEquals("Room", entity.getType());
        assertEquals(2, entity.getAttributes().length);
        assertTrue(entity.hasAttribute("pressure"));
        assertTrue(entity.hasAttribute("temperature"));

        Attribute pressure = entity.getAttribute("pressure");
        assertEquals("integer", pressure.getType());
        assertEquals(720, pressure.getValue().asNumber(), 0);
        assertEquals(0, pressure.getMetadata().getElements().length);

        Attribute temperature = entity.getAttribute("temperature");
        assertEquals("float", temperature.getType());
        assertEquals(26.5, temperature.getValue().asNumber(), 0);
        Metadata temperatureMetadata = temperature.getMetadata();
        assertEquals(1, temperatureMetadata.getElements().length);
        assertEquals("float", temperatureMetadata.getElement("accuracy").getType());
        assertEquals(0.8, temperatureMetadata.getElement("accuracy").getValue().asNumber(), 0);
    }

    @Test
    public void parseKeyValueModel() {
        String entityString = "{" +
            "    \"id\": \"Room1\"," +
            "    \"type\": \"Room\"," +
            "    \"pressure\": \"720\"," +
            "    \"temperature\": \"26.5\"" +
            "}";

        JsonObject jsonEntity = JsonUtil.parse(entityString);
        KeyValueEntity entity = new KeyValueEntity(jsonEntity);
        validateKeyValueSample(entity);

        // Roundtrip
        entityString = entity.getJsonObject().toJson();
        jsonEntity = JsonUtil.parse(entityString);
        entity = new KeyValueEntity(jsonEntity);
        validateKeyValueSample(entity);
    }

    protected void validateKeyValueSample(KeyValueEntity entity) {
        ModelValidationError[] errors = entity.validate();
        assertEquals(0, errors.length);

        assertEquals("Room1", entity.getId());
        assertEquals("Room", entity.getType());
        assertEquals(2, entity.getAttributes().length);
        assertTrue(entity.hasAttribute("pressure"));
        assertTrue(entity.hasAttribute("temperature"));

        KeyValueAttribute pressure = entity.getAttribute("pressure");
        assertEquals(720, pressure.getValue().asNumber(), 0);

        KeyValueAttribute temperature = entity.getAttribute("temperature");
        assertEquals(26.5, temperature.getValue().asNumber(), 0);
    }
}
