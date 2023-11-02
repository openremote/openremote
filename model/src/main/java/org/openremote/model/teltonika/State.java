package org.openremote.model.teltonika;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.fortuna.ical4j.model.DateTime;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetStateDuration;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.rules.flow.Option;
import org.openremote.model.util.ValueUtil;

import org.openremote.model.value.*;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
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
    //TODO: Add description of each attribute (No idea where to put that yet)
    //DONE: Multiply value by multiplier
    //DEFERRED: Improve storage by using bytes variable? Improvements would be marginal, not to mention type safety.
    //DONE: If location is 0,0, don't overwrite location
    //TODO: Move any function from here; it's a POJO.
    //TODO: Figure out what the parameters: pr, alt, ang, sat, sp, and evt do
    // and implement their functionality (I can guess but I want to know exactly).
    public AttributeMap GetAttributes(Map<Integer, TeltonikaParameter> params, AttributeMap additionalAttributes, Logger logger){
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
                Optional<?> value = Optional.empty();
                try {
                    //Inner method returns Optional.empty, but still throws and prints exception. A lot of clutter, but the exception is handled.
                    value = ValueUtil.getValueCoerced(entry.getValue(), attributeType.getType());
                    if (!value.isPresent()){
                        attributeType = ValueType.TEXT;
                        value = Optional.of(entry.getValue().toString());
                    }
                } catch (Exception ignored){
                    value = Optional.of(entry.getValue().toString());
                    attributeType = ValueType.TEXT;
                    logger.severe("Failed value parse");
                    logger.severe(ignored.toString());
                }


                //If value was parsed correctly,
                if(value.isPresent()){
                    // Multiply the value with its multiplier if given
                    if(!Objects.equals(parameter.multiplier, "-")){
                        Optional<?> multiplier = ValueUtil.parse(parameter.multiplier, attributeType.getType());
                        if(multiplier.isPresent()){
                            try{
                                double valueNumber = (double) value.get();
                                double multiplierNumber = (double) multiplier.get();

                                value = Optional.of(valueNumber * multiplierNumber);
                            }catch (Exception e){
                                logger.severe(parameter.propertyName + "Failed multiplier");
                                logger.severe(e.toString());
                                throw e;
                            }
                        }

                        //TODO: Fix frontend to be able to display even custom units, not just the predefined ones
                       //possibly prepend the unit with the string "custom."? So that it matches the predefined format
                       //Add on its units
                        if(!Objects.equals(parameter.units, "-")){
                            try{
                                MetaItem<String[]> units = new MetaItem<>(MetaItemType.UNITS);
                                units.setValue(Constants.units(parameter.units));
                                //                            Error when deploying: https://i.imgur.com/4IihWC3.png
                                //                            metaMap.add(units);
                            }catch (Exception e){
                                logger.severe(parameter.propertyName + "Failed units");
                                logger.severe(e.toString());
                                throw e;
                            }
                        }
                    }
                    //Add on its constraints (min, max)
                    if(ValueUtil.isNumber(attributeType.getType())){
                        Optional<?> min;
                        Optional<?> max;

                        try{
                            try {
                                //Try to parse Hex in case it is hex
                                max = Optional.of(Double.longBitsToDouble(Long.parseLong(parameter.max.substring(2), 16)));
                                min = Optional.of(Double.longBitsToDouble(Long.parseLong(parameter.min.substring(2), 16)));
                            } catch (Exception ignored) {
                                // ValueUtil.getValue actually logs the exceptions. I can't catch em to then ignore them.
                                min = ValueUtil.getValueCoerced(parameter.min, attributeType.getType());
                                max = ValueUtil.getValueCoerced(parameter.max, attributeType.getType());
                            }
                            if (min.isPresent() || max.isPresent()) {
                                MetaItem<ValueConstraint[]> constraintsMeta = new MetaItem<>(CONSTRAINTS);
                                List<ValueConstraint> constraintValues = new ArrayList<>();
                                min.ifPresent(o -> constraintValues.add(new ValueConstraint.Min((Number) o)));
                                max.ifPresent(o -> constraintValues.add(new ValueConstraint.Max((Number) o)));

                                ValueConstraint[] constraints = constraintValues.toArray(new ValueConstraint[0]);

                                constraintsMeta.setValue(constraints);
                                metaMap.add(constraintsMeta);
                            }
                        }catch (Exception e){
                            logger.severe(parameter.propertyName + "Failed constraints");
                            logger.severe(e.toString());
                            throw e;
                        }
                    }
                    // Add on its label
                    try{
                        MetaItem<String> label = new MetaItem<>(MetaItemType.LABEL);
                        label.setValue(parameter.propertyName);
                        metaMap.add(label);
                    }catch (Exception e){
                        logger.severe(parameter.propertyName + "Failed label");
                        logger.severe(e.toString());
                        throw e;
                    }
                    //Use the MetaMap to create an AttributeDescriptor
                    AttributeDescriptor<?> attributeDescriptor = new AttributeDescriptor<>(parameter.propertyId.toString(), attributeType, metaMap);

                    //Use the AttributeDescriptor and the Value to create a new Attribute
                    // Add it to the AttributeMap
                    attributes.add(new Attribute<>(attributeDescriptor, value.get()));
                }
            }
        }
        //Special parameter definitions without being defined in the AVL Parameter List, thanks Teltonika

        //latlng are the latitude-longitude coordinates, also check if it's 0,0, if it is, don't update.
        if (reported.containsKey("latlng") && !Objects.equals(reported.get("latlng"), "0.000000,0.000000")){
            try{
                String latlngString = reported.get("latlng").toString();
                GeoJSONPoint point = ParseLatLngToGeoJSONObject(latlngString);
                Attribute<?> attr = new Attribute<>("location", ValueType.GEO_JSON_POINT, point);

                attributes.add(attr);
            }catch (Exception e){
                logger.severe("Failed coordinates");
                logger.severe(e.toString());
                throw e;
            }
        }
        //Timestamp grabbed from the device.
        //TODO: It doesn't look like the timestamp is being parsed correctly...
        if (reported.containsKey("ts")){
            try{
                long unixTimestampMillis = Long.parseLong(reported.get("ts").toString());
                Timestamp deviceTimestamp = Timestamp.from(Instant.ofEpochMilli(unixTimestampMillis));
                attributes.add(new Attribute<>("lastContact", ValueType.DATE_AND_TIME, deviceTimestamp));

                //Update all affected attribute timestamps
                attributes.forEach(attribute -> {
                    attribute.setTimestamp(deviceTimestamp.getTime());
                });
            }catch (Exception e){
                logger.severe("Failed timestamps");
                logger.severe(e.toString());
                throw e;
            }

        }

        // Store data points, allow use for rules, and don't allow user parameter modification, for every attribute parsed
        try{
            attributes.forEach(attribute -> attribute.addOrReplaceMeta(
                            new MetaItem<>(STORE_DATA_POINTS, true),
                            new MetaItem<>(RULE_STATE, true),
                            new MetaItem<>(READ_ONLY, true)
                    )
            );
        }catch (Exception e){
            logger.severe("Failed metaItems");
            logger.severe(e.toString());
            throw e;
        }

        return attributes;
    }



    private ValueDescriptor<?> GetAttributeType(TeltonikaParameter parameter) {
        try{
            Double.valueOf(parameter.min);
            Double.valueOf(parameter.max);
            return ValueType.NUMBER;
        }catch (NumberFormatException e){
            return switch (parameter.type) {
                case "Unsigned", "Signed", "unsigned", "UNSIGNED LONG INT" -> ValueType.NUMBER;
                default -> ValueType.TEXT;
            };
        }
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
