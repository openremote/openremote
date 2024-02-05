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


import java.util.ArrayList;
import java.util.List;

public class DetectionMethodForecast extends DetectionMethod{
    private List<ValueDatapoint<?>> predictedPoints;
    public DetectionMethodForecast(AnomalyDetectionConfiguration config){
        super(config);
        predictedPoints = new ArrayList<>();
    }

    @Override
    public boolean validateDatapoint(Object value, long timestamp) {
        for(int i = 1; i < predictedPoints.size(); i++){
            if((double)predictedPoints.get(i).getTimestamp() < timestamp){
                double predictedValue =
                        ((double)predictedPoints.get(i-1).getValue() - (double)predictedPoints.get(i).getValue())
                        /
                        (predictedPoints.get(i-1).getTimestamp() - predictedPoints.get(i).getTimestamp())
                        *
                        (timestamp - predictedPoints.get(i).getTimestamp())
                        +
                        (double)predictedPoints.get(i).getValue();
                return !(predictedValue -  config.deviation > (double)value) && !(predictedValue + config.deviation < (double) value);
            }
        }
        return true;
    }

    @Override
    public boolean checkRecentDataSaved(long latestTimestamp) {
        if(predictedPoints.isEmpty())return false;
        if(latestTimestamp >= predictedPoints.get(predictedPoints.size() -1).getTimestamp()  && latestTimestamp <= predictedPoints.get(0).getTimestamp()){
            return true;
        }
        return false;
    }

    @Override
    public boolean updateData(List<ValueDatapoint<?>> datapoints) {
        if(datapoints.isEmpty())return false;
        predictedPoints = datapoints;
        return true;
    }

    @Override
    public double[] getLimits(ValueDatapoint<?> datapoint) {
        for(int i = 1; i < predictedPoints.size(); i++){
            if((double)predictedPoints.get(i).getTimestamp() <= datapoint.getTimestamp()){
                double predictedValue =
                        ((double)predictedPoints.get(i-1).getValue() - (double)predictedPoints.get(i).getValue())
                                /
                                (predictedPoints.get(i-1).getTimestamp() - predictedPoints.get(i).getTimestamp())
                                *
                                (datapoint.getTimestamp() - predictedPoints.get(i).getTimestamp())
                                +
                                (double)predictedPoints.get(i).getValue();
                return new double[]{predictedValue - config.deviation, predictedValue + config.deviation};
            }
        }
        return new double[2];
    }
}
