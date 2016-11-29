package org.openremote.controller;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.controller.command.CommandBuilder;
import org.openremote.controller.context.ControllerContext;
import org.openremote.controller.context.InMemorySensorStateStorage;
import org.openremote.controller.deploy.DeploymentDefinition;
import org.openremote.controller.deploy.xml.ControllerDOMParser;
import org.openremote.controller.deploy.Deployment;
import org.openremote.controller.rules.RulesProvider;

import java.io.InputStream;

public class ControllerService implements ContainerService {

    final protected ControllerDOMParser controllerDOMParser = new ControllerDOMParser();
    final protected InputStream deploymentXml;
    final protected CommandBuilder commandBuilder;
    final protected RulesProvider rulesProvider;

    protected ControllerContext controllerContext;

    public ControllerService(InputStream deploymentXml, CommandBuilder commandBuilder, RulesProvider rulesProvider) {
        this.deploymentXml = deploymentXml;
        this.commandBuilder = commandBuilder;
        this.rulesProvider = rulesProvider;
    }

    @Override
    public void init(Container container) throws Exception {
    }

    @Override
    public void configure(Container container) throws Exception {
        // TODO Support booting of multiple controller instances
        Deployment deployment = new Deployment(
            controllerDOMParser.parse(deploymentXml, true),
            commandBuilder,
            new InMemorySensorStateStorage(),
            rulesProvider
        );
        controllerContext = new ControllerContext("OpenRemoteController1", deployment);
    }

    @Override
    public void start(Container container) throws Exception {
        if (controllerContext != null) {
            controllerContext.start();
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (controllerContext != null) {
            controllerContext.stop();
        }
    }

    public ControllerContext getContext() {
        return controllerContext;
    }
}
