package org.openremote.model.teltonika;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "reported"
})
public class State {

//    public ReportedState reportedState;
    @JsonProperty("reported")
    private Map<String, Object> reported;



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(State.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("reported");
        sb.append('=');
        sb.append(((this.reported == null) ? "<null>" : this.reported));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    public Attribute<?>[] GetAttributes(Map<Integer, TeltonikaParameter> params){
        ArrayList<Attribute<?>> attributes = new ArrayList<>();
        for (Map.Entry<String,Object> entry : reported.entrySet()){
            String entryId = entry.getKey();
            int parsedEntryId;
            try {
                parsedEntryId = Integer.parseInt(entryId);
            }catch (Exception e){
                continue;
            }

            if (params.containsKey(parsedEntryId)){
                TeltonikaParameter parameter = params.get( Integer.valueOf(entry.getKey()));
                MetaItem<String> meta = new MetaItem<>(MetaItemType.LABEL);
                meta.setValue(parameter.propertyName);
                attributes.add(new Attribute<>(parameter.getPropertyId().toString(),ValueType.TEXT,entry.getValue().toString()).addMeta(meta));

            }
        }
        if (reported.containsKey("latlng")){
            String latlngString = reported.get("latlng").toString();
            GeoJSONPoint point = ParseLatLngToGeoJSONObject(latlngString);
            attributes.add(new Attribute<>("location", ValueType.GEO_JSON_POINT, point));

        }

        return attributes.toArray(new Attribute[0]);
    }

    private GeoJSONPoint ParseLatLngToGeoJSONObject(String latlngString) {
        String regexPattern = "^([-+]?[0-8]?\\d(\\.\\d+)?|90(\\.0+)?),([-+]?(1?[0-7]?[0-9](\\.\\d+)?|180(\\.0+)?))$";

        Pattern r = Pattern.compile(regexPattern);
        Matcher m = r.matcher(latlngString);

        if (m.find()) {
            String latitude = m.group(1);
            String longitude = m.group(4);
            // Since the regex pattern was validated, there is no way for parsing these to throw a NumberFormatException.

            // GeoJSON requires the points in long-lat form, not lat-long
            return new GeoJSONPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));

        } else {
            return null;
        }
    }

}
