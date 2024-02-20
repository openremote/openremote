package org.openremote.agent.protocol.homeassistant;

import com.fasterxml.jackson.core.type.TypeReference;
import org.openremote.agent.protocol.homeassistant.commands.EntityStateCommand;
import org.openremote.agent.protocol.homeassistant.entities.HomeAssistantBaseEntity;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;


public class HomeAssistantApiClient {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, HomeAssistantApiClient.class);
    private final String HomeAssistantApiUrl;
    private final String AccessToken;

    public HomeAssistantApiClient(String homeAssistantUrl, String accessToken) {
        this.HomeAssistantApiUrl = homeAssistantUrl + "/api";
        this.AccessToken = accessToken;
    }

    public boolean isConnected() {
        Optional<String> response = getRequest("");
        return response.isPresent();
    }

    public Optional<List<HomeAssistantBaseEntity>> getEntities() {
        Optional<String> response = getRequest("/states");
        if (response.isPresent()) {
            return ValueUtil.parse(response.get(), new TypeReference<>() {
            });
        }
        return Optional.empty();
    }

    public void updateEntityState(String domain, EntityStateCommand command) {
        LOG.info("Updating entity state: " + domain + "." + command.getService() + " " + command.getEntityId());

        var json = ValueUtil.asJSON(Map.of("entity_id", command.getEntityId()));
        if (command.getAttributeName() != null && !command.getAttributeName().isEmpty()) {
            json = ValueUtil.asJSON(Map.of("entity_id", command.getEntityId(), command.getAttributeName(), command.getAttributeValue()));
        }
        if (json.isEmpty())
            return;

        postRequest("/services/" + domain + "/" + command.getService(), json.get());
    }

    public void postRequest(String path, String json) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HomeAssistantApiUrl + path))
                .header("Authorization", "Bearer " + AccessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            LOG.warning("Error sending request: " + e.getMessage());
        }
    }

    private Optional<String> getRequest(String path) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HomeAssistantApiUrl + path))
                .header("Authorization", "Bearer " + AccessToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return Optional.of(response.body());

        } catch (IOException | InterruptedException e) {
            LOG.warning("Error sending request: " + e.getMessage());
        }
        return Optional.empty();
    }


}
