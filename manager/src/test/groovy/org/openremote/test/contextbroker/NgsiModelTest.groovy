package org.openremote.test.contextbroker

import elemental.json.Json
import elemental.json.JsonObject
import org.openremote.manager.shared.ngsi.Entity
import org.openremote.manager.shared.ngsi.KeyValueEntity
import org.openremote.manager.shared.ngsi.Metadata
import org.openremote.manager.shared.ngsi.Model
import org.openremote.manager.shared.ngsi.ModelValidationError
import spock.lang.Specification

class NgsiModelTest extends Specification {

    def "Invalid characters in NGSI fields"() {
        expect: "number of validation errors"
        Model.validateField(fieldLabel).length == expectedNumberOfErrors;
        where: "field labels are"
        fieldLabel                    || expectedNumberOfErrors
        "SomeField"                   || 0
        "!@%^*()[]{}<>-=+.,;:|\\~_'`" || 0
        " "                           || 1
        "&"                           || 1
        "?"                           || 1
        "/"                           || 1
    }

    def "Parse NGSI entity roundtrip"() {
        given: "an NGSI entity JSON string"
        String jsonString = '''
            {
                "id": "Room1",
                "type": "Room",
                "pressure":{
                    "type": "integer",
                    "value": "720",
                    "metadata":{}
                },
                "temperature":{
                    "type": "float",
                    "value": "26.5",
                    "metadata":{
                        "accuracy":{
                            "type": "float",
                            "value": "0.8"
                        }
                    }
                }
            }
            ''';
        when: "is parsed into JsonObject"
        JsonObject jsonEntity = Json.parse(jsonString);
        and: "is wrapped as an Entity"
        Entity entity = new Entity(jsonEntity);
        then: "it should be valid"
        validateEntity(entity);

        when: "Entity is serialized into JSON string"
        jsonString = entity.getJsonObject().toJson();
        and: "is parsed again into a JsonObject"
        jsonEntity = Json.parse(jsonString);
        and: "is wrapped again as an Entity"
        entity = new Entity(jsonEntity);
        then: "it should still be valid"
        validateEntity(entity);
    }

    void validateEntity(entity) {
        ModelValidationError[] errors = entity.validate();
        assert errors.length == 0;
        assert "Room1" == entity.getId();
        assert "Room" == entity.getType();
        assert entity.getAttributes().length == 2;
        assert entity.hasAttribute("pressure");
        assert entity.hasAttribute("temperature");

        def pressure = entity.getAttribute("pressure");
        assert pressure.getValue().asNumber() == 720;
        assert pressure.getMetadata().getElements().length == 0;

        def temperature = entity.getAttribute("temperature");
        assert temperature.getValue().asNumber() == new Double(26.5);

        Metadata temperatureMetadata = temperature.getMetadata();
        assert temperatureMetadata.getElements().length == 1;
        assert temperatureMetadata.getElement("accuracy").getType() == "float";
        assert temperatureMetadata.getElement("accuracy").getValue().asNumber() == new Double(0.8);
    }

    def "Parse NGSI key-value entity roundtrip"() {
        given: "an NGSI key-value entity JSON string"
        String jsonString = '''
            {
                "id": "Room1",
                "type": "Room",
                "pressure": "720",
                "temperature": "26.5"
            }
            ''';
        when: "is parsed into JsonObject"
        JsonObject jsonEntity = Json.parse(jsonString);
        and: "is wrapped as an KeyValueEntity"
        KeyValueEntity entity = new KeyValueEntity(jsonEntity);
        then: "it should be valid"
        validateKeyValueEntity(entity);

        when: "KeyValueEntity is serialized into JSON string"
        jsonString = entity.getJsonObject().toJson();
        and: "is parsed again into a JsonObject"
        jsonEntity = Json.parse(jsonString);
        and: "is wrapped again as an KeyValueEntity"
        entity = new KeyValueEntity(jsonEntity);
        then: "it should still be valid"
        validateKeyValueEntity(entity);
    }

    void validateKeyValueEntity(entity) {
        ModelValidationError[] errors = entity.validate();
        assert errors.length == 0;
        assert "Room1" == entity.getId();
        assert "Room" == entity.getType();
        assert entity.getAttributes().length == 2;
        assert entity.hasAttribute("pressure");
        assert entity.hasAttribute("temperature");

        def pressure = entity.getAttribute("pressure");
        assert pressure.getValue().asNumber() == 720;

        def temperature = entity.getAttribute("temperature");
        assert temperature.getValue().asNumber() == new Double(26.5);
    }
}