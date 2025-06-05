package org.openremote.manager.ai;

import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;

public class AiService implements ContainerService {


    public record WebsocketInfo(String ipAddress, int port) {}

    private WebsocketInfo aiServiceWebsocketInfo;

    @Override
    public void init(Container container) throws Exception {
        container.getService(ManagerWebService.class).addApiSingleton(new AiResourceImpl(this));
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }


    public void registerAIService(String ipAddress, int port) {
        aiServiceWebsocketInfo = new WebsocketInfo(ipAddress, port);
    }

    public WebsocketInfo getWebsocketInfo() {
        return aiServiceWebsocketInfo;
    }
}
