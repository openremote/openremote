package org.openremote.manager.ai;

import java.util.HashMap;
import java.util.Map;

public class AiResourceImpl implements AiResource {

    private final AiService aiService;

    public AiResourceImpl(AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public boolean registerAIService(Map<String, Object> map) {
        String ipAddress = String.valueOf(map.get("ip_address"));
        int port = (int) map.get("port");

        aiService.registerAIService(ipAddress, port);
        return true;
    }

    @Override
    public Map<String, Object> getWebsocketInfo() {
        Map<String, Object> map = new HashMap<>();

        AiService.WebsocketInfo info = aiService.getWebsocketInfo();

        map.put("ip_address", info.ipAddress());
        map.put("port", info.port());
        return map;
    }
}
