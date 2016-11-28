package org.openremote.controller;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.controller.command.CommandBuilder;
import org.openremote.controller.deploy.DeploymentDefinition;
import org.openremote.controller.deploy.xml.ControllerDOMParser;
import org.openremote.controller.event.EventProcessor;
import org.openremote.controller.event.EventProcessorChain;
import org.openremote.controller.rules.CommandFacade;
import org.openremote.controller.model.Deployment;
import org.openremote.controller.model.Sensor;
import org.openremote.controller.context.DataContext;

import java.io.InputStream;

public class ControllerService implements ContainerService {

    final protected ControllerDOMParser controllerDOMParser = new ControllerDOMParser();
    final protected InputStream deploymentXml;

    final protected CommandBuilder commandBuilder;
    final protected EventProcessor[] eventProcessors;

    protected DeploymentDefinition deploymentDefinition;
    protected Deployment deployment;
    protected CommandFacade commandFacade;
    protected DataContext dataContext;

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
        deployment = new Deployment(deploymentDefinition, commandBuilder);

        commandFacade = new CommandFacade(deployment);

        EventProcessorChain eventProcessorChain = new EventProcessorChain(commandFacade, eventProcessors);

        dataContext = new DataContext(deployment, eventProcessorChain);
    }

    @Override
    public void start(Container container) throws Exception {
        if (dataContext != null) {
            dataContext.start();
            for (Sensor sensor : deployment.getSensors()) {
                dataContext.registerAndStartSensor(sensor);
            }
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (dataContext != null) {
            dataContext.stop();
        }
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public CommandFacade getCommandFacade() {
        return commandFacade;
    }

    public DataContext getDataContext() {
        return dataContext;
    }
}
