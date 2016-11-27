package org.openremote.controller;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.controller.command.CommandFactory;
import org.openremote.controller.deploy.ControllerDOM;
import org.openremote.controller.deploy.ControllerDOMParser;
import org.openremote.controller.event.EventProcessor;
import org.openremote.controller.event.EventProcessorChain;
import org.openremote.controller.event.facade.CommandFacade;
import org.openremote.controller.model.Deployment;
import org.openremote.controller.model.Sensor;
import org.openremote.controller.statuscache.ChangedStatusTable;
import org.openremote.controller.statuscache.StatusCache;

import java.io.InputStream;

public class ControllerService implements ContainerService {

    final protected ControllerDOMParser controllerDOMParser = new ControllerDOMParser();

    final protected InputStream deploymentXml;
    final protected CommandFactory commandFactory;
    final protected EventProcessor[] eventProcessors;

    protected ControllerDOM controllerDOM;
    protected Deployment deployment;
    protected CommandFacade commandFacade;
    protected StatusCache cache;

    public ControllerService(InputStream deploymentXml, CommandFactory commandFactory, EventProcessor[] eventProcessors) {
        this.deploymentXml = deploymentXml;
        this.commandFactory = commandFactory;
        this.eventProcessors = eventProcessors;
    }

    @Override
    public void init(Container container) throws Exception {
        controllerDOM = controllerDOMParser.parse(deploymentXml, true);
    }

    @Override
    public void configure(Container container) throws Exception {
        deployment = new Deployment(controllerDOM, commandFactory);

        commandFacade = new CommandFacade(deployment);
        EventProcessorChain eventProcessorChain = new EventProcessorChain(commandFacade, eventProcessors);

        ChangedStatusTable changeStatusTable = new ChangedStatusTable();
        cache = new StatusCache(changeStatusTable, eventProcessorChain);
    }

    @Override
    public void start(Container container) throws Exception {
        if (cache != null) {
            cache.start();
            for (Sensor sensor : deployment.getSensors()) {
                cache.registerAndStartSensor(sensor);
            }
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (cache != null) {
            cache.shutdown();
        }
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public CommandFacade getCommandFacade() {
        return commandFacade;
    }

    public StatusCache getCache() {
        return cache;
    }
}
