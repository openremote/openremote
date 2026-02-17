package org.openremote.agent.protocol.entsoe;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import org.openremote.model.asset.agent.AgentLink;

public class EntsoeAgentLink extends AgentLink<EntsoeAgentLink> {

    @NotNull
    @JsonPropertyDescription("Energy Identification Code of zone to fetch data for")
    private String zone;

    // For Hydrators
    public EntsoeAgentLink() {
    }

    public EntsoeAgentLink(String id) {
        super(id);
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
