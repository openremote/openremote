package org.openremote.controller.deploy.xml;

import org.openremote.container.xml.DOM;
import org.openremote.controller.deploy.DeploymentDefinition;
import org.openremote.controller.deploy.CommandDefinition;
import org.openremote.controller.deploy.SensorDefinition;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import java.util.*;

/**
 * Usage:
 *
 * <pre><code>
 *     ControllerDOM dom = new ControllerDOMParser().parse(deploymentXml);
 * </code></pre>
 */
public class ControllerDOM extends DOM implements DeploymentDefinition {

    public static final String NAMESPACE_URI = "http://www.openremote.org";

    final protected XPath xpath;
    final protected CommandDefinition[] commandDefinitions;
    final protected SensorDefinition[] sensorDefinitions;

    public ControllerDOM(Document dom, XPath xpath) {
        super(dom);
        this.xpath = xpath;

        try {
            List<CommandDefinition> commandDefinitionList = new ArrayList<>();
            ControllerElement[] commandElements = getRoot(xpath).getFirstChild("commands").getChildren();
            for (ControllerElement commandElement : commandElements) {
                Integer commandID = Integer.valueOf(commandElement.getAttribute("id"));
                String protocolType = commandElement.getAttribute("protocol");
                Map<String, String> properties = new LinkedHashMap<>();
                for (ControllerElement propertyElement : commandElement.getChildren("property")) {
                    properties.put(propertyElement.getAttribute("name"), propertyElement.getAttribute("value"));
                }
                commandDefinitionList.add(
                    new CommandDefinition(commandID, protocolType, properties)
                );
            }
            commandDefinitions = commandDefinitionList.toArray(new CommandDefinition[commandDefinitionList.size()]);

            List<SensorDefinition> sensorDefinitionList = new ArrayList<>();
            ControllerElement[] sensorElements = getRoot(xpath).getFirstChild("sensors").getChildren();
            for (ControllerElement sensorElement : sensorElements) {
                Integer sensorID = Integer.valueOf(sensorElement.getAttribute("id"));
                String sensorName = sensorElement.getAttribute("name");
                String sensorType = sensorElement.getAttribute("type").toLowerCase(Locale.ROOT);

                Integer commandRefID = null;
                ControllerElement includeElement = sensorElement.getFirstChild("include");
                if (includeElement != null && "command".equals(includeElement.getAttribute("type"))) {
                    commandRefID = Integer.valueOf(includeElement.getAttribute("ref"));
                }
                if (commandRefID == null) {
                    throw new IllegalStateException(
                        "Missing command reference in sensor definition: " + sensorID + "/" + sensorName
                    );
                }

                CommandDefinition commandDefinition = null;
                for (CommandDefinition cmdDefinition : commandDefinitionList) {
                    if (cmdDefinition.getCommandID() == commandRefID) {
                        commandDefinition = cmdDefinition;
                        break;
                    }
                }
                if (commandDefinition == null) {
                    throw new IllegalStateException(
                        "Unknown command '" + commandRefID + "' referenced in sensor definition: " + sensorID + "/" + sensorName
                    );
                }

                // TODO This is horrible, not uniform
                Map<String, String> properties = new LinkedHashMap<>();
                for (ControllerElement stateElement : sensorElement.getChildren("state")) {
                    properties.put("state-" + stateElement.getAttribute("name"), stateElement.getAttribute("value"));
                }
                ControllerElement minElement = sensorElement.getFirstChild("min");
                if (minElement != null)
                    properties.put("range-min", minElement.getAttribute("value"));
                ControllerElement maxElement = sensorElement.getFirstChild("max");
                if (maxElement != null)
                    properties.put("range-max", maxElement.getAttribute("value"));

                sensorDefinitionList.add(
                    new SensorDefinition(sensorID, sensorName, sensorType, commandDefinition, properties)
                );
            }
            this.sensorDefinitions = sensorDefinitionList.toArray(new SensorDefinition[sensorDefinitionList.size()]);

        } catch (Exception ex) {
            throw new RuntimeException("Error building controller model from XML document", ex);
        }
    }

    @Override
    public String getRootElementNamespace() {
        return NAMESPACE_URI;
    }

    @Override
    public ControllerElement getRoot(XPath xPath) {
        return new ControllerElement(xPath, getW3CDocument().getDocumentElement());
    }

    @Override
    public ControllerDOM copy() {
        return new ControllerDOM((Document) getW3CDocument().cloneNode(true), xpath);
    }

    @Override
    public CommandDefinition[] getCommandDefinitions() {
        return commandDefinitions;
    }

    @Override
    public SensorDefinition[] getSensorDefinitions() {
        return sensorDefinitions;
    }
}