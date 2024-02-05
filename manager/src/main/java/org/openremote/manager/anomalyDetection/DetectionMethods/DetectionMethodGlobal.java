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

public class DetectionMethodGlobal extends DetectionMethod {
    private double minValue;
    private long minValueTimestamp;
    private double maxValue;
    private long maxValueTimestamp;


    public DetectionMethodGlobal(AnomalyDetectionConfiguration config){
        super(config);
    }


    public boolean validateDatapoint(Object value, long timestamp) {
        double differance = maxValue - minValue + 0.001;
        double deviation = differance * ((double)config.deviation /100);

        double val = (double)value;
        boolean valid = true;
        if(val < minValue - deviation){
            valid = false;
        }
        if(val > maxValue + deviation){
            valid = false;
        }
        if(valid){
            if(val >= maxValue){
                maxValue = val;
                maxValueTimestamp = timestamp;
            }
            if(val <= minValue){
                minValue = val;
                minValueTimestamp =timestamp;
            }
        }
        return valid;
    }


    public boolean checkRecentDataSaved(long latestTimestamp) {
        boolean needsNewData = false;
        long timeMillis = ((AnomalyDetectionConfiguration.Global)config).timespan.toMillis();
        if(minValueTimestamp < latestTimestamp - timeMillis
                || maxValueTimestamp < latestTimestamp - timeMillis){
            needsNewData = true;
        }
        return !needsNewData;
    }

    public boolean updateData(List<ValueDatapoint<?>> datapoints) {
        if(datapoints.isEmpty() || datapoints.size() < ((AnomalyDetectionConfiguration.Global)config).minimumDatapoints) return false;
        minValue = Double.MAX_VALUE;
        maxValue = (double)datapoints.get(0).getValue();
        for (ValueDatapoint dtapoint : datapoints) {
                if((double)dtapoint.getValue() <= minValue){
                    minValue = (double)dtapoint.getValue();
                    minValueTimestamp = dtapoint.getTimestamp();
                }
                if((double)dtapoint.getValue() >= maxValue){
                    maxValue = (double)dtapoint.getValue();
                    maxValueTimestamp = dtapoint.getTimestamp();
                }
        }
        return true;
    }

    @Override
    public double[] getLimits(ValueDatapoint<?> datapoint) {
        double differance = maxValue - minValue + 0.001;
        double deviation = differance * ((double)config.deviation /100);
        double[] limits = new double[]{minValue - deviation, maxValue + deviation};
        double value = (double)datapoint.getValue();
        if(minValue > value){
            minValueTimestamp = datapoint.getTimestamp();
            minValue = value;
        }
        if(maxValue < value){
            maxValueTimestamp = datapoint.getTimestamp();
            maxValue = value;
        }
        return limits;
    }
}
