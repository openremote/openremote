package org.openremote.controller;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.controller.command.CommandBuilder;
import org.openremote.controller.context.ControllerContext;
import org.openremote.controller.context.InMemoryStateStorage;
import org.openremote.controller.deploy.DeploymentDefinition;
import org.openremote.controller.deploy.xml.ControllerDOMParser;
import org.openremote.controller.event.EventProcessor;
import org.openremote.controller.deploy.Deployment;

import java.io.InputStream;

public class ControllerService implements ContainerService {

    final protected ControllerDOMParser controllerDOMParser = new ControllerDOMParser();
    final protected InputStream deploymentXml;

    final protected CommandBuilder commandBuilder;
    final protected EventProcessor[] eventProcessors;

    protected DeploymentDefinition deploymentDefinition;
    protected ControllerContext controllerContext;

    public ControllerService(InputStream deploymentXml, CommandBuilder commandBuilder, EventProcessor[] eventProcessors) {
        this.deploymentXml = deploymentXml;
        this.commandBuilder = commandBuilder;
        this.eventProcessors = eventProcessors;
    }

    @Override
    public void init(Container container) throws Exception {
        deploymentDefinition = controllerDOMParser.parse(deploymentXml, true);
    }

    @Override
    public void configure(Container container) throws Exception {
        // TODO Support booting of multiple controller instances
        Deployment deployment = new Deployment(
            deploymentDefinition,
            commandBuilder,
            new InMemoryStateStorage(),
            eventProcessors
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
