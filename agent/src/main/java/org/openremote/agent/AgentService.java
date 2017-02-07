package org.openremote.agent;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.agent.command.CommandBuilder;
import org.openremote.agent.context.AgentContext;
import org.openremote.agent.context.InMemorySensorStateHandler;
import org.openremote.agent.deploy.xml.AgentDOMParser;
import org.openremote.agent.deploy.Deployment;
import org.openremote.agent.rules.RulesProvider;

import java.io.InputStream;

public class AgentService implements ContainerService {

    final protected AgentDOMParser agentDOMParser = new AgentDOMParser();
    final protected InputStream deploymentXml;
    final protected CommandBuilder commandBuilder;
    final protected RulesProvider rulesProvider;

    protected AgentContext agentContext;

    public AgentService(InputStream deploymentXml, CommandBuilder commandBuilder, RulesProvider rulesProvider) {
        this.deploymentXml = deploymentXml;
        this.commandBuilder = commandBuilder;
        this.rulesProvider = rulesProvider;
    }

    @Override
    public void init(Container container) throws Exception {
        // TODO Support booting of multiple instances
        Deployment deployment = new Deployment(
            agentDOMParser.parse(deploymentXml, true).getDeploymentDefinition(),
            commandBuilder,
            new InMemorySensorStateHandler(),
            rulesProvider
        );
        agentContext = new AgentContext("OpenRemoteAgent123", deployment);
    }

    @Override
    public void start(Container container) throws Exception {
        if (agentContext != null) {
            agentContext.start();
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (agentContext != null) {
            agentContext.stop();
        }
    }

    public AgentContext getContext() {
        return agentContext;
    }
}
