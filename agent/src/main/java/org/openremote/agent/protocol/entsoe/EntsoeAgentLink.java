package org.openremote.agent.protocol.entsoe;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.openremote.model.asset.agent.AgentLink;

public class EntsoeAgentLink extends AgentLink<EntsoeAgentLink> {

    @NotNull
    @JsonPropertyDescription("Energy Identification Code of zone to fetch data for")
    @Pattern(regexp = "^\\d{2}[A-Z][A-Z0-9-]{12}[A-Z0-9]$")
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
