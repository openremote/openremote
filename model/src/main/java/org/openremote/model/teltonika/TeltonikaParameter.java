
package org.openremote.model.teltonika;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
public class TeltonikaParameter {

    @JsonProperty("propertyIdInAvlPacket")
    public Integer propertyId;
    public String propertyName;
    public String bytes;
    public String type;
    public String min;
    public String max;
    public String multiplier;
    public String units;
    public String description;
    public String hwSupport;
    public String parameterGroup;


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
