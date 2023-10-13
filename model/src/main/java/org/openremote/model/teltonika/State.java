package org.openremote.model.teltonika;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.openremote.model.Constants;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.util.ValueUtil;

import org.openremote.model.value.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openremote.model.value.MetaItemType.*;
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


    //DONE: Add timestamp reported by device
    //TODO: Add description of each attribute
    //TODO: Multiply value by multiplier
    //TODO: Improve storage by using bytes variable?
    public AttributeMap GetAttributes(Map<Integer, TeltonikaParameter> params){
        AttributeMap attributes = new AttributeMap();
        for (Map.Entry<String,Object> entry : reported.entrySet()){
            //Check if parameter is a number value, pointing to a TeltonikaParameter
            String entryId = entry.getKey();
            int parsedEntryId;
            try {
                parsedEntryId = Integer.parseInt(entryId);
            }catch (Exception e){
                continue;
            }
            //If the Teltonika Parameter HashMap contains the requested AVL ID:
            if (params.containsKey(parsedEntryId)){

                //Retrieve the parameter data
                TeltonikaParameter parameter = params.get(Integer.valueOf(entry.getKey()));

                //Create the MetaItem Map
                MetaMap metaMap = new MetaMap();

                // Figure out its attributeType
                ValueDescriptor<?> attributeType = GetAttributeType(parameter);

                //Retrieve its coerced value
                Optional<?> value = ValueUtil.getValueCoerced(entry.getValue().toString(), attributeType.getType());


                //If value was parsed correctly,
                if(value.isPresent()){
                    // Multiply the value with its multiplier if given
                    if(!Objects.equals(parameter.multiplier, "-")){
                        Optional<?> multiplier = ValueUtil.getValueCoerced(parameter.multiplier, attributeType.getType());
                        if(multiplier.isPresent()){
                            long valueNumber = (long) multiplier.get();
                            long multiplierNumber = (long) multiplier.get();

                            value = Optional.of(valueNumber * multiplierNumber);
                        }

                    //TODO: Fix frontend to be able to display even custom units, not just the predefined ones
                    //Add on its units
                    if(!Objects.equals(parameter.units, "-")){
                        MetaItem<String[]> units = new MetaItem<>(MetaItemType.UNITS);
                            units.setValue(Constants.units(parameter.units));
//                            Error when deploying: https://i.imgur.com/4IihWC3.png
//                            metaMap.add(units);
                        }
                    }
                    //Add on its constraints (min, max)
                    if(ValueUtil.isNumber(attributeType.getType())){
                        Optional<?> min = null;
                        Optional<?> max = null;
                        try {
                            min = ValueUtil.getValue(parameter.min, attributeType.getType());
                            max = ValueUtil.getValue(parameter.max, attributeType.getType());
                        } catch (Exception ignored){
                            //ignored
                        }

                        if(min.isPresent() || max.isPresent()){
                            MetaItem<ValueConstraint[]> constraintsMeta = new MetaItem<>(CONSTRAINTS);
                            List<ValueConstraint> constraintValues = new ArrayList<>();
                            min.ifPresent(o -> constraintValues.add(new ValueConstraint.Min((Number) o)));
                            max.ifPresent(o -> constraintValues.add(new ValueConstraint.Max((Number) o)));

                            ValueConstraint[] constraints = constraintValues.toArray(new ValueConstraint[0]);

                            constraintsMeta.setValue(constraints);
                            metaMap.add(constraintsMeta);
                        }
                    }
                    // Add on its label
                    MetaItem<String> label = new MetaItem<>(MetaItemType.LABEL);
                    label.setValue(parameter.propertyName);
                    metaMap.add(label);
                    //Use the MetaMap to create an AttributeDescriptor
                    AttributeDescriptor<?> attributeDescriptor = new AttributeDescriptor<>(parameter.propertyId.toString(), attributeType, metaMap);

                    //Use the AttributeDescriptor and the Value to create a new Attribute
                    Attribute<?> attr = new Attribute(attributeDescriptor, value.get());

                    // Add it to the AttributeMap
                    attributes.add(attr);
                }
            }
        }
        //Special parameter definitions without being defined in the AVL Parameter List, thanks Teltonika
        if (reported.containsKey("latlng")){
            String latlngString = reported.get("latlng").toString();
            GeoJSONPoint point = ParseLatLngToGeoJSONObject(latlngString);
            Attribute<?> attr = new Attribute<>("location", ValueType.GEO_JSON_POINT, point);

            attributes.add(attr);
        }
        //TimeStamp
        if (reported.containsKey("ts")){
            String updateTime = reported.get("ts").toString();
            Date update = new java.util.Date(Long.parseLong(updateTime));
            attributes.add(new Attribute<>("lastContact", ValueType.DATE_AND_TIME, update));

            //Update all affected attribute timestamps
            attributes.forEach(attribute -> {
                attribute.setTimestamp(update.getTime());
            });
        }
        //TODO: Figure out what the parameters: pr, alt, ang, sat, sp, and evt do and implement their functionality

        // Store data points, allow use for rules, and don't allow user parameter transmission, for every attribute parsed
        attributes.forEach(attribute -> attribute.addOrReplaceMeta(
                new MetaItem<>(STORE_DATA_POINTS, true),
                new MetaItem<>(RULE_STATE, true),
                new MetaItem<>(READ_ONLY, true)));

        return attributes;
    }

    private ValueDescriptor<?> GetAttributeType(TeltonikaParameter parameter) {
        return switch (parameter.type) {
            case "Unsigned", "Signed", "unsigned", "UNSIGNED LONG INT" -> ValueType.LONG;
            default -> ValueType.TEXT;
        };
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
