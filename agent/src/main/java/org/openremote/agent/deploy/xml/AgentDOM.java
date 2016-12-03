package org.openremote.agent.deploy.xml;

import org.openremote.container.xml.DOM;
import org.openremote.agent.deploy.CommandDefinition;
import org.openremote.agent.deploy.DeploymentDefinition;
import org.openremote.agent.deploy.SensorDefinition;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import java.util.*;
import java.util.logging.Logger;

/**
 * Usage:
 * <p>
 * <pre><code>
 *     AgentDOM dom = new AgentDOMParser().parse(deploymentXml);
 *     Deployment deployment = new Deployment(dom.getDeploymentDefinition(), ...);
 * </code></pre>
 */
public class AgentDOM extends DOM {

    private static final Logger LOG = Logger.getLogger(AgentDOM.class.getName());

    public static final String NAMESPACE_URI = "http://www.openremote.org/schemas/agent/3";

    final protected XPath xpath;
    final protected DeploymentDefinition deploymentDefinition;

    public AgentDOM(Document dom, XPath xpath) {
        super(dom);
        this.xpath = xpath;

        this.deploymentDefinition = new DeploymentDefinition();

        try {
            List<CommandDefinition> commandDefinitionList = new ArrayList<>();
            AgentElement[] commandElements = getRoot(xpath).getFirstChild("commands").getChildren();
            for (AgentElement commandElement : commandElements) {
                Integer commandID = Integer.valueOf(commandElement.getAttribute("id"));
                String protocolType = commandElement.getAttribute("protocol");
                Map<String, String> properties = new LinkedHashMap<>();
                for (AgentElement propertyElement : commandElement.getChildren("property")) {
                    properties.put(propertyElement.getAttribute("name"), propertyElement.getAttribute("value"));
                }
                commandDefinitionList.add(
                    new CommandDefinition(commandID, protocolType, properties)
                );
            }
            deploymentDefinition.setCommandDefinitions(
                commandDefinitionList.toArray(new CommandDefinition[commandDefinitionList.size()]
                )
            );

            List<SensorDefinition> sensorDefinitionList = new ArrayList<>();
            AgentElement[] sensorElements = getRoot(xpath).getFirstChild("sensors").getChildren();
            for (AgentElement sensorElement : sensorElements) {
                Integer sensorID = Integer.valueOf(sensorElement.getAttribute("id"));
                String sensorName = sensorElement.getAttribute("name");
                String sensorType = sensorElement.getAttribute("type").toLowerCase(Locale.ROOT);

                Integer commandRefID = null;
                AgentElement includeElement = sensorElement.getFirstChild("include");
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
                for (AgentElement stateElement : sensorElement.getChildren("state")) {
                    properties.put("state-" + stateElement.getAttribute("name"), stateElement.getAttribute("value"));
                }
                AgentElement minElement = sensorElement.getFirstChild("min");
                if (minElement != null)
                    properties.put("range-min", minElement.getAttribute("value"));
                AgentElement maxElement = sensorElement.getFirstChild("max");
                if (maxElement != null)
                    properties.put("range-max", maxElement.getAttribute("value"));

                sensorDefinitionList.add(
                    new SensorDefinition(sensorID, sensorName, sensorType, commandDefinition, properties)
                );
            }
            deploymentDefinition.setSensorDefinitions(
                sensorDefinitionList.toArray(new SensorDefinition[sensorDefinitionList.size()])
            );

            Map<String, String> config = new HashMap<>();
            AgentElement[] configPropertyElements = getRoot(xpath).getFirstChild("config").getChildren();
            for (AgentElement configPropertyElement : configPropertyElements) {
                String name = configPropertyElement.getAttribute("name");
                String value = configPropertyElement.getAttribute("value");
                if (name != null) {
                    if (value != null) {
                        config.put(name, value);
                    } else {
                        LOG.info("Ignoring config property without value: " + name);
                    }
                }
            }
            deploymentDefinition.setConfig(config);

        } catch (Exception ex) {
            throw new RuntimeException("Error parsing agent deployment from XML document", ex);
        }
    }

    @Override
    public String getRootElementNamespace() {
        return NAMESPACE_URI;
    }

    @Override
    public AgentElement getRoot(XPath xPath) {
        return new AgentElement(xPath, getW3CDocument().getDocumentElement());
    }

    @Override
    public AgentDOM copy() {
        return new AgentDOM((Document) getW3CDocument().cloneNode(true), xpath);
    }

    public DeploymentDefinition getDeploymentDefinition() {
        return deploymentDefinition;
    }
}