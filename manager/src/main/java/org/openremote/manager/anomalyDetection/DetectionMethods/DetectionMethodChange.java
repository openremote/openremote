/*
 * Copyright 2024, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.anomalyDetection.DetectionMethods;

import org.openremote.model.attribute.AttributeAnomaly;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.value.AnomalyDetectionConfiguration;

import java.util.List;

public class DetectionMethodChange extends DetectionMethod {
    private double biggestIncrease;
    private long biggestIncreaseTimestamp;
    private double smallestIncrease;
    private long smallestIncreaseTimestamp;
    private double previousValue;
    private long previousValueTimestamp; // Is not used since method is now checking change between values instead of change relative to time between points.(Might change later)

    public DetectionMethodChange(AnomalyDetectionConfiguration config){
        super(config);
    }

    public boolean validateDatapoint(Object value, long timestamp) {
        double increase = ((double)value - previousValue);

        boolean valid = true;
        double diff = biggestIncrease - smallestIncrease;

        double offset =  diff * ((double)config.deviation/100);
        if(increase > biggestIncrease + offset){
            valid = false;
        }
        if(increase < smallestIncrease - offset){
            valid = false;
        }
        message = "Value is " + increase + " while limits are " + (biggestIncrease + offset) + " and " + (smallestIncrease- offset);
        if(valid){
            if(increase <= smallestIncrease){
                smallestIncrease = increase;
                smallestIncreaseTimestamp = timestamp;
            }
            if(increase>= biggestIncrease){
                biggestIncrease = increase;
                biggestIncreaseTimestamp = timestamp;
            }
        }
        previousValue = (double)value;
        previousValueTimestamp = timestamp;
        return valid;
    }

    public boolean checkRecentDataSaved(long latestTimestamp) {
        boolean needsNewData = false;
        if(smallestIncreaseTimestamp < latestTimestamp - ((AnomalyDetectionConfiguration.Change)config).timespan.toMillis()
                || biggestIncreaseTimestamp < latestTimestamp - ((AnomalyDetectionConfiguration.Change)config).timespan.toMillis()){
            needsNewData = true;
        }
        return !needsNewData;
    }

    @Override
    public boolean updateData(List<ValueDatapoint<?>> datapoints) {
        if(datapoints.size() < 3) return false;
        smallestIncrease = Double.MAX_VALUE;
        biggestIncrease = -100000000;
        for(int i = 1; i < datapoints.size(); i++){
            double increase = (double)datapoints.get(i-1).getValue() - (double)datapoints.get(i).getValue();
            long timestamp = datapoints.get(i).getTimestamp();

            if(increase <= smallestIncrease){
                smallestIncrease = increase;
                smallestIncreaseTimestamp = timestamp;
            }
            if(increase>= biggestIncrease){
                biggestIncrease = increase;
                biggestIncreaseTimestamp = timestamp;
            }
        }
        previousValue = (double)datapoints.get(0).getValue();
        previousValueTimestamp = datapoints.get(0).getTimestamp();
        return true;
    }

    @Override
    public double[] getLimits(ValueDatapoint<?> datapoint) {
        double increase = ((double)datapoint.getValue() - previousValue);
        double diff = biggestIncrease - smallestIncrease;
        double offset =  diff * ((double)config.deviation/100);
        double[] limits = new double[]{previousValue + smallestIncrease - offset, previousValue + biggestIncrease +offset};

        previousValue = (double)datapoint.getValue();
        if(increase <= smallestIncrease){
            smallestIncrease = increase;
            smallestIncreaseTimestamp = datapoint.getTimestamp();
        }
        if(increase>= biggestIncrease){
            biggestIncrease = increase;
            biggestIncreaseTimestamp = datapoint.getTimestamp();
        }
        return limits;
    }
}
