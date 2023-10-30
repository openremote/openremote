
package org.openremote.model.teltonika;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "propertyIdInAvlPacket",
        "propertyName",
        "bytes",
        "type",
        "min",
        "max",
        "multiplier",
        "units",
        "description",
        "hwSupport",
        "parameterGroup"
})
public class TeltonikaParameter {

    @JsonProperty("propertyIdInAvlPacket")
    public Integer propertyId;
    @JsonProperty("propertyName")
    public String propertyName;
    @JsonProperty("bytes")
    public String bytes;
    @JsonProperty("type")
    public String type;
    @JsonProperty("min")
    public String min;
    @JsonProperty("max")
    public String max;
    @JsonProperty("multiplier")
    public String multiplier;
    @JsonProperty("units")
    public String units;
    @JsonProperty("description")
    public String description;
    @JsonProperty("hwSupport")
    public String hwSupport;
    @JsonProperty("parameterGroup")
    public String parameterGroup;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();


    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TeltonikaParameter.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("propertyIdInAvlPacket");
        sb.append('=');
        sb.append(((this.propertyId == null)?"<null>":this.propertyId));
        sb.append(',');
        sb.append("propertyName");
        sb.append('=');
        sb.append(((this.propertyName == null)?"<null>":this.propertyName));
        sb.append(',');
        sb.append("bytes");
        sb.append('=');
        sb.append(((this.bytes == null)?"<null>":this.bytes));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("min");
        sb.append('=');
        sb.append(((this.min == null)?"<null>":this.min));
        sb.append(',');
        sb.append("max");
        sb.append('=');
        sb.append(((this.max == null)?"<null>":this.max));
        sb.append(',');
        sb.append("multiplier");
        sb.append('=');
        sb.append(((this.multiplier == null)?"<null>":this.multiplier));
        sb.append(',');
        sb.append("units");
        sb.append('=');
        sb.append(((this.units == null)?"<null>":this.units));
        sb.append(',');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
        sb.append(',');
        sb.append("hwSupport");
        sb.append('=');
        sb.append(((this.hwSupport == null)?"<null>":this.hwSupport));
        sb.append(',');
        sb.append("parameterGroup");
        sb.append('=');
        sb.append(((this.parameterGroup == null)?"<null>":this.parameterGroup));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    public Integer getPropertyId(){
        return this.propertyId;
    }

    @Override
    public boolean equals(Object param) {

        if(param.getClass() == this.getClass()){
            return Objects.equals(((TeltonikaParameter)param).getPropertyId(), this.getPropertyId());
        } return false;
    }
}
