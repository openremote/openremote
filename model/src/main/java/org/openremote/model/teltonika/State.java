package org.openremote.model.teltonika;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.Constants;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openremote.model.value.MetaItemType.*;

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


    //TODO: Add real timestamp reported by device, check issue #1156
    //TODO: Add description of each attribute (No idea where to put that yet)
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
            //If the Teltonika Parameter HashMap doesn't contain the requested AVL ID, continue:

            if (!params.containsKey(parsedEntryId)) continue;

            //Retrieve the parameter data
            TeltonikaParameter parameter = params.get(Integer.valueOf(entry.getKey()));

            //Create the MetaItem Map
            MetaMap metaMap = new MetaMap();

            // Figure out its attributeType
            ValueDescriptor<?> attributeType = GetAttributeType(parameter);

            //Retrieve its coerced value
            Optional<?> value;
            try {
                //Inner method returns Optional.empty, but still throws and prints exception. A lot of clutter, but the exception is handled.
                value = ValueUtil.getValueCoerced(entry.getValue(), attributeType.getType());
                if (value.isEmpty()){
                    attributeType = ValueType.TEXT;
                    value = Optional.of(entry.getValue().toString());
                }
            } catch (Exception e){
                value = Optional.of(entry.getValue().toString());
                attributeType = ValueType.TEXT;
                logger.severe("Failed value parse");
                logger.severe(e.toString());
            }
            Optional<?> originalValue = value;

            double multiplier = 1L;
            //If value was parsed correctly,
            // Multiply the value with its multiplier if given
            if(!Objects.equals(parameter.multiplier, "-")){
                Optional<?> optionalMultiplier = ValueUtil.parse(parameter.multiplier, attributeType.getType());

                if(optionalMultiplier.isPresent()){

                    if(!ValueUtil.objectsEquals(optionalMultiplier.get(), multiplier)) multiplier = (Double) optionalMultiplier.get();
                    try{
                        double valueNumber = (double) value.get();

                        value = Optional.of(valueNumber * multiplier);
                        //If the original value is unequal to the new (multiplied) value, then we have to also multiply the constraints

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
            //TODO: The constraints need to be the MULTIPLIED values.
            if(ValueUtil.isNumber(attributeType.getType())){
                Optional<?> min;
                Optional<?> max;

                try{
                    //param id 17, 18 and 19, parsed as double, with min = -8000 and max = +8000 is being parsed as 0 and 0?
                    //You cant do this to me Teltonika, why does parameter ID 237 with constraints (0, 1) have value 2 (and description says it can go up to 99)?
                    //TODO: Fix this
                    min = ValueUtil.getValueCoerced(parameter.min, attributeType.getBaseType());
                    max = ValueUtil.getValueCoerced(parameter.max, attributeType.getBaseType());
                    if (min.isPresent() || max.isPresent()) {
                        MetaItem<ValueConstraint[]> constraintsMeta = new MetaItem<>(CONSTRAINTS);
                        List<ValueConstraint> constraintValues = new ArrayList<>();
                        //Do I even have to multiply the constraints?
                        double finalMultiplier = 1;
                        //TODO: Try this with parameter 66 - why is it not properly storing the max constraint?
                        //      It is calculated correctly, check with a debugger, but why is it not storing the data as required?
                        if(multiplier != 1L){
                            finalMultiplier = multiplier;
                        }

                        // Check if the value is correctly within the constraints. If it's not, don't apply the constraint.
                        // The only reason I am doing this is that the constraints are currently not programmatically, thus "seriously", set.
                        // If it was properly defined, then parameter ID 237 wouldn't be inaccurate, let alone to this state.
                        // Not to mention parsing errors (from example from the UDP/Codec 8 to MQTT/Codec JSON converter, look at accelerator axes)
                        // THE AXES VARS OVERFLOW  - if i see that TCT has value -46, if I do 65535 minus the value I am given, then it gives me the real value
                        if (min.isPresent()) {
                            if ( !((Double) value.get() < (Double) min.get())) {
                                constraintValues.add(new ValueConstraint.Min((Double) min.get() * finalMultiplier));
                            }
                        }
                        if(max.isPresent()){
                            if ( !((Double) value.get() > (Double) max.get())) {
                                constraintValues.add(new ValueConstraint.Max((Double) max.get() * finalMultiplier));
                            }
                        }

                        ValueConstraint[] constraints = constraintValues.toArray(new ValueConstraint[0]);

                        constraintsMeta.setValue(constraints);

                        //TODO: Fix this, constraints for some reason are not being applied
//                        metaMap.add(constraintsMeta);
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
            Attribute<?> generatedAttribute = new Attribute(attributeDescriptor, value.get());
            // Add it to the AttributeMap
            attributes.addOrReplace(generatedAttribute);
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
                attributes.add(new Attribute<>("lastContact", ValueType.DATE_AND_TIME, deviceTimestamp).setTimestamp(deviceTimestamp.getTime()));


                //Update all affected attribute timestamps
                attributes.forEach(attribute -> attribute.setTimestamp(deviceTimestamp.getTime()));
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
