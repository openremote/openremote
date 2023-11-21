
package org.openremote.model.SensorThings;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import org.openremote.model.geo.GeoJSONPoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "@iot.selfLink",
    "@iot.id",
    "description",
    "name",
    "observationType",
    "observedArea",
    "phenomenonTime",
    "resultTime",
    "unitOfMeasurement",
    "Thing",
    "Observations@iot.count",
    "Observations",
    "Observations@iot.nextLink",
    "ObservedProperty@iot.navigationLink",
    "Sensor@iot.navigationLink",
    "Thing@iot.navigationLink",
    "Observations@iot.navigationLink"
})
public class Datastream {

    @JsonProperty("@iot.selfLink")
    private String iotSelfLink;
    @JsonProperty("@iot.id")
    private Integer iotId;
    @JsonProperty("description")
    private String description;
    @JsonProperty("name")
    private String name;
    @JsonProperty("observationType")
    private String observationType;
    @JsonProperty("observedArea")
    @Valid
    private GeoJSONPoint observedArea;
    @JsonProperty("phenomenonTime")
    private String phenomenonTime;
    @JsonProperty("resultTime")
    private String resultTime;
    @JsonProperty("unitOfMeasurement")
    @Valid
    private UnitOfMeasurement unitOfMeasurement;
    @JsonProperty("Thing")
    @Valid
    private Thing thing;
    @JsonProperty("Observations@iot.count")
    private Integer observationsIotCount;
    @JsonProperty("Observations")
    @Valid
    private List<Observation> observations;
    @JsonProperty("Observations@iot.nextLink")
    private String observationsIotNextLink;
    @JsonProperty("ObservedProperty@iot.navigationLink")
    private String observedPropertyIotNavigationLink;
    @JsonProperty("Sensor@iot.navigationLink")
    private String sensorIotNavigationLink;
    @JsonProperty("Thing@iot.navigationLink")
    private String thingIotNavigationLink;
    @JsonProperty("Observations@iot.navigationLink")
    private String observationsIotNavigationLink;
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

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("observationType")
    public String getObservationType() {
        return observationType;
    }

    @JsonProperty("observationType")
    public void setObservationType(String observationType) {
        this.observationType = observationType;
    }
    @JsonProperty("phenomenonTime")
    public String getPhenomenonTime() {
        return phenomenonTime;
    }

    @JsonProperty("phenomenonTime")
    public void setPhenomenonTime(String phenomenonTime) {
        this.phenomenonTime = phenomenonTime;
    }

    @JsonProperty("resultTime")
    public String getResultTime() {
        return resultTime;
    }

    @JsonProperty("resultTime")
    public void setResultTime(String resultTime) {
        this.resultTime = resultTime;
    }

    @JsonProperty("unitOfMeasurement")
    public UnitOfMeasurement getUnitOfMeasurement() {
        return unitOfMeasurement;
    }

    @JsonProperty("unitOfMeasurement")
    public void setUnitOfMeasurement(UnitOfMeasurement unitOfMeasurement) {
        this.unitOfMeasurement = unitOfMeasurement;
    }

    @JsonProperty("Thing")
    public Thing getThing() {
        return thing;
    }

    @JsonProperty("Thing")
    public void setThing(Thing thing) {
        this.thing = thing;
    }

    @JsonProperty("Observations@iot.count")
    public Integer getObservationsIotCount() {
        return observationsIotCount;
    }

    @JsonProperty("Observations@iot.count")
    public void setObservationsIotCount(Integer observationsIotCount) {
        this.observationsIotCount = observationsIotCount;
    }

    @JsonProperty("Observations")
    public List<Observation> getObservations() {
        return observations;
    }

    @JsonProperty("Observations")
    public void setObservations(List<Observation> observations) {
        this.observations = observations;
    }

    @JsonProperty("Observations@iot.nextLink")
    public String getObservationsIotNextLink() {
        return observationsIotNextLink;
    }

    @JsonProperty("Observations@iot.nextLink")
    public void setObservationsIotNextLink(String observationsIotNextLink) {
        this.observationsIotNextLink = observationsIotNextLink;
    }

    @JsonProperty("ObservedProperty@iot.navigationLink")
    public String getObservedPropertyIotNavigationLink() {
        return observedPropertyIotNavigationLink;
    }

    @JsonProperty("ObservedProperty@iot.navigationLink")
    public void setObservedPropertyIotNavigationLink(String observedPropertyIotNavigationLink) {
        this.observedPropertyIotNavigationLink = observedPropertyIotNavigationLink;
    }

    @JsonProperty("Sensor@iot.navigationLink")
    public String getSensorIotNavigationLink() {
        return sensorIotNavigationLink;
    }

    @JsonProperty("Sensor@iot.navigationLink")
    public void setSensorIotNavigationLink(String sensorIotNavigationLink) {
        this.sensorIotNavigationLink = sensorIotNavigationLink;
    }

    @JsonProperty("Thing@iot.navigationLink")
    public String getThingIotNavigationLink() {
        return thingIotNavigationLink;
    }

    @JsonProperty("Thing@iot.navigationLink")
    public void setThingIotNavigationLink(String thingIotNavigationLink) {
        this.thingIotNavigationLink = thingIotNavigationLink;
    }

    @JsonProperty("Observations@iot.navigationLink")
    public String getObservationsIotNavigationLink() {
        return observationsIotNavigationLink;
    }

    @JsonProperty("Observations@iot.navigationLink")
    public void setObservationsIotNavigationLink(String observationsIotNavigationLink) {
        this.observationsIotNavigationLink = observationsIotNavigationLink;
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
