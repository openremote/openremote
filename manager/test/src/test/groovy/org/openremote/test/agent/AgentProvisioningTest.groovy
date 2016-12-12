package org.openremote.test.agent

import elemental.json.Json
import elemental.json.JsonObject
import org.openremote.model.Attributes
import org.openremote.test.GwtClientTrait
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

class AgentProvisioningTest extends Specification implements ManagerContainerTrait, GwtClientTrait {

    def agentAttributesJson = '''{
    "hueBridge456": {
      "type": "String",
      "value": "urn:openremote:protocol:hue",
      "metadata": [
        {
          "name": "urn:openremote:protocol:hue:host",
          "value": "192.168.123.123"
        },
        {
          "name": "urn:openremote:protocol:hue:username",
          "value": "1028d66426293e821ecfd9ef1a0731df"
        },
        {
          "name": "urn:openremote:protocol:hue:pollInterval",
          "value": 500
        }
      ]
    },
    "zwave123": {
      "type": "String",
      "value": "urn:openremote:protocol:zwave",
      "metadata": [
        {
          "name": "urn:openremote:asset:meta:description",
          "value": "Aeon Labs ZWave Adapter in USB Port 123"
        },
        {
          "name": "urn:openremote:protocol:zwave:usbport",
          "value": "/dev/cu.usbmodem1411"
        },
        {
          "name": "urn:openremote:protocol:zwave:checkAliveInterval",
          "value": 3000
        }
      ]
    }
    }'''

    def thingAttributesJson = '''{
    "light1Toggle": {
      "type": "Boolean",
      "value": true,
      "metadata": [
        {
          "name": "urn:openremote:asset:meta:description",
          "value": "The switch for the light in the living room"
        },
        {
          "name": "urn:openremote:agent:link",
          "value": [
            "ekOo0puKRdGtBhDOOFzBfA",
            "hueBridge456"
          ]
        },
        {
          "name": "urn:openremote:protocol:hue:lightId",
          "value": 3
        },
        {
          "name": "urn:openremote:protocol:hue:command",
          "value": "switch"
        }
      ]
    },
    "light1Dimmer": {
      "type": "Integer",
      "value": 55,
      "metadata": [
        {
          "name": "urn:openremote:asset:meta:description",
          "value": "The dimmer for the light in the living room"
        },
        {
          "name": "urn:openremote:asset:meta:rangeMin",
          "value": 0
        },
        {
          "name": "urn:openremote:asset:meta:rangeMax",
          "value": 100
        },
        {
          "name": "urn:openremote:agent:link",
          "value": [
            "ekOo0puKRdGtBhDOOFzBfA",
            "hueBridge456"
          ]
        },
        {
          "name": "urn:openremote:protocol:hue:lightId",
          "value": 3
        },
        {
          "name": "urn:openremote:protocol:hue:command",
          "value": "brightness"
        }
      ]
    },
    "light1Color": {
      "type": "Color",
      "value": {
        "red": 88,
        "green": 123,
        "blue": 88
      },
      "metadata": [
        {
          "name": "urn:openremote:asset:meta:description",
          "value": "The color of the living room light"
        },
        {
          "name": "urn:openremote:agent:link",
          "value": [
            "ekOo0puKRdGtBhDOOFzBfA",
            "hueBridge456"
          ]
        },
        {
          "name": "urn:openremote:protocol:hue:lightId",
          "value": 3
        },
        {
          "name": "urn:openremote:protocol:hue:command",
          "value": "color"
        }
      ]
    },
    "light1PowerConsumption": {
      "type": "Integer",
      "value": 123,
      "metadata": [
        {
          "name": "urn:openremote:asset:meta:description",
          "value": "The total power consumption of the living room light"
        },
        {
          "name": "urn:openremote:asset:meta:readOnly",
          "value": true
        },
        {
          "name": "urn:openremote:asset:meta:format",
          "value": "%3d kWh"
        },
        {
          "name": "urn:openremote:agent:link",
          "value": [
            "ekOo0puKRdGtBhDOOFzBfA",
            "zwave123"
          ]
        },
        {
          "name": "urn:openremote:protocol:zwave:nodeId",
          "value": 5
        },
        {
          "name": "urn:openremote:protocol:zwave:command",
          "value": "STATUS"
        }
      ]
    }
    }'''

    def "Create an agent and thing configuration"() {

/*
        given: "a server container"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())

*/
        when: "agent attributes are parsed"
        def agentAttributes = new Attributes(Json.instance().parse(agentAttributesJson) as JsonObject)


        then: "the result should match"
        agentAttributes.get("hueBridge456").getValueAsString() == "urn:openremote:protocol:hue"

/*
        and: "the server should be stopped"
        stopContainer(container);
*/
    }
}
