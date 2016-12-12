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
package org.openremote.manager.shared.connector;


import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;

public class ConnectorUtil {
    protected ConnectorUtil() {}

    public static Attribute buildConnectorSetting(String attributeName, AttributeType type, String displayName, String description, boolean required) {
        return buildConnectorSetting(attributeName, type, displayName, description, required, null, null);
    }

    public static Attribute buildConnectorSetting(String attributeName, AttributeType type, String displayName, String description, boolean required, String defaultValue, String defaultValueNote) {
        Attribute attribute = new Attribute(attributeName, type);
        /*
        attribute.setMetadata(new Metadata(Json.createObject()));

        if (displayName != null && displayName.length() > 0) {
            attribute.getMetadata().putElement(
                    new MetadataElement("label", Json.createObject())
                            .setType(AttributeType.STRING.getValue())
                            .setValue(Json.create(displayName))
            );
        }

        if (description != null && description.length() > 0) {
            attribute.getMetadata().putElement(
                    new MetadataElement("description", Json.createObject())
                            .setType(AttributeType.STRING.getValue())
                            .setValue(Json.create(description))
            );
        }

        if (defaultValue != null && defaultValue.length() > 0) {
            attribute.getMetadata().putElement(
                    new MetadataElement("defaultValue", Json.createObject())
                            .setType(type.getValue())
                            .setValue(Json.create(defaultValue))
            );
        }

        if (defaultValueNote != null && defaultValueNote.length() > 0) {
            attribute.getMetadata().putElement(
                    new MetadataElement("defaultValueNote", Json.createObject())
                            .setType(AttributeType.STRING.getValue())
                            .setValue(Json.create(defaultValueNote))
            );
        }

        if (required) {
            attribute.getMetadata().putElement(
                    new MetadataElement("required", Json.createObject())
                            .setType(AttributeType.BOOLEAN.getValue())
                            .setValue(Json.create(true))
            );
        }
        */

        return attribute;
    }

//    /**
//     * Will synchronise the stored agent settings with the supplied connector. Existing agent settings
//     * that match a connector setting will be preserved.
//     */
//    public static void synchroniseAgentSettings(Agent agent, Connector connector) {
//        Attributes connectorSettings = connector.getAgentSettings();
//
//        if (connectorSettings == null) {
//            agent.setConnectorSettings(null);
//            return;
//        }
//
//        Attributes agentSettings = new Attributes(agent.getConnectorSettings());
//        Attributes newSettings = new Attributes();
//
//        for (Attribute attribute : connectorSettings.get()) {
//            Attribute existingAttribute = agentSettings.get(attribute.getName());
//
//            if (existingAttribute != null && existingAttribute.getType().equals(attribute.getType())) {
//                newSettings.add(existingAttribute);
//            } else {
//                Attribute newAttribute = createSettingAttribute(attribute);
//                if (newAttribute != null) {
//                    newSettings.add(newAttribute);
//                }
//            }
//        }
//
//        agent.setConnectorSettings(newSettings.getJsonObject());
//    }
//
//    protected static Attribute createSettingAttribute(Attribute connectorAttribute) {
//        Metadata metadata = connectorAttribute.getMetadata();
//        if (!metadata.hasElement("required")) {
//            return null;
//        }
//
//        JsonValue value = Json.createNull();
//
//        if (metadata.hasElement("defaultValue")) {
//            value = metadata.getElement("defaultValue").getValue();
//        }
//
//        return new Attribute(connectorAttribute.getName(), connectorAttribute.getType(), value);
//    }

}
