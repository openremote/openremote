package org.openremote.agent.protocol.homeassistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.agent.protocol.homeassistant.commands.EntityStateCommand;
import org.openremote.agent.protocol.homeassistant.entities.HomeAssistantBaseEntity;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class HomeAssistantHttpClient {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, HomeAssistantHttpClient.class);
    private final String HomeAssistantUrl;
    private final String Token;

    public HomeAssistantHttpClient(String homeAssistantUrl, String token) {
        this.HomeAssistantUrl = homeAssistantUrl;
        this.Token = token;
    }

    public Optional<List<HomeAssistantBaseEntity>> getEntities() {
        Optional<String> response = sendGetRequest("/api/states");
        if (response.isPresent()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<HomeAssistantBaseEntity> entities = mapper.readValue(response.get(), new TypeReference<>() {
                });
                return Optional.of(entities);
            } catch (JsonProcessingException e) {
                LOG.warning("Error parsing response: " + e.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }


    public boolean isConnectionSuccessful() {
        Optional<String> response = sendGetRequest("/api");
        return response.isPresent();
    }

    public void setEntityState(String domain, EntityStateCommand command) {
        LOG.info("Setting entity state: " + domain + "." + command.getService() + " " + command.getEntityId());

        // default for setting the entity state
        var json = ValueUtil.asJSON(Map.of("entity_id", command.getEntityId()));

        // if the attribute name is set, we are setting an attribute value.
        if (command.getAttributeName() != null && !command.getAttributeName().isEmpty()) {
            json = ValueUtil.asJSON(Map.of("entity_id", command.getEntityId(), command.getAttributeName(), command.getAttributeValue()));
        }

        if (json.isEmpty())
            return;

        sendPostRequest("/api/services/" + domain + "/" + command.getService(), json.get());

    }

    public void sendPostRequest(String path, String json) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HomeAssistantUrl + path))
                .header("Authorization", "Bearer " + Token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOG.warning("Error sending request: " + e.getMessage());
        }
    }

    private Optional<String> sendGetRequest(String path) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HomeAssistantUrl + path))
                .header("Authorization", "Bearer " + Token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return Optional.of(response.body());

        } catch (Exception e) {
            LOG.warning("Error sending request: " + e.getMessage());
        }
        return Optional.empty();
    }


}
