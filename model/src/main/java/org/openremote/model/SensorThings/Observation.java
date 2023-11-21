
package org.openremote.model.SensorThings;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "@iot.selfLink",
    "@iot.id",
    "phenomenonTime",
    "result",
    "resultTime"
})
public class Observation {

    @JsonProperty("@iot.selfLink")
    private String iotSelfLink;
    @JsonProperty("@iot.id")
    private Integer iotId;
    @JsonProperty("phenomenonTime")
    private String phenomenonTime;
    @JsonProperty("result")
    private Double result;
    @JsonProperty("resultTime")
    private String resultTime;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("@iot.selfLink")
    public String getIotSelfLink() {
        return iotSelfLink;
    }

    @JsonProperty("@iot.selfLink")
    public void setIotSelfLink(String iotSelfLink) {
        this.iotSelfLink = iotSelfLink;
    }

    @JsonProperty("@iot.id")
    public Integer getIotId() {
        return iotId;
    }

    @JsonProperty("@iot.id")
    public void setIotId(Integer iotId) {
        this.iotId = iotId;
    }

    @JsonProperty("phenomenonTime")
    public String getPhenomenonTime() {
        return phenomenonTime;
    }

    @JsonProperty("phenomenonTime")
    public void setPhenomenonTime(String phenomenonTime) {
        this.phenomenonTime = phenomenonTime;
    }

    @JsonProperty("result")
    public Double getResult() {
        return result;
    }

    @JsonProperty("result")
    public void setResult(Double result) {
        this.result = result;
    }

    @JsonProperty("resultTime")
    public String getResultTime() {
        return resultTime;
    }

    @JsonProperty("resultTime")
    public void setResultTime(String resultTime) {
        this.resultTime = resultTime;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
