
package org.openremote.model.teltonika;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "RSP"
})
public class TeltonikaResponsePayload {

    @JsonProperty("RSP")
    public String rsp;
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TeltonikaResponsePayload.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("rsp");
        sb.append('=');
        sb.append(((this.rsp == null)?"<null>":this.rsp));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
