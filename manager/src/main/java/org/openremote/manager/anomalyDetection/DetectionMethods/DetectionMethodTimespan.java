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

public class DetectionMethodTimespan extends DetectionMethod{
    private long longestTimespan;
    private long longestTimespanTimestamp;
    private long shortestTimespan;
    private long shortestTimespanTimestamp;
    private long previousValueTimestamp;

    public DetectionMethodTimespan(AnomalyDetectionConfiguration config){
        super(config);
    }

    @Override
    public boolean validateDatapoint(Object value, long timestamp) {
        long timespan = timestamp - previousValueTimestamp;
        boolean valid = true;
        long offset = 0;
        offset = (long)((longestTimespan - shortestTimespan+1)* ((double)config.deviation/100));
        if(offset < 0) offset *= -1;
        if(timespan > longestTimespan + offset){
            valid = false;
        }
        if(valid){
            if(timespan <= shortestTimespan){
                shortestTimespan = timespan;
                shortestTimespanTimestamp = timestamp;
            }
            if(timespan >= longestTimespan){
                longestTimespan = timespan;
                longestTimespanTimestamp = timestamp;
            }
        }
        previousValueTimestamp = timestamp;
        return valid;
    }

    @Override
    public boolean checkRecentDataSaved(long latestTimestamp) {
        boolean needsNewData = false;
        if(longestTimespanTimestamp < latestTimestamp - ((AnomalyDetectionConfiguration.Timespan)config).timespan.toMillis()
                || shortestTimespanTimestamp < latestTimestamp - ((AnomalyDetectionConfiguration.Timespan)config).timespan.toMillis()){
            needsNewData = true;
        }
        return !needsNewData;
    }

    @Override
    public boolean updateData(List<ValueDatapoint<?>> datapoints) {

        if(datapoints.size() <((AnomalyDetectionConfiguration.Timespan)config).minimumDatapoints) return false;
        shortestTimespan = datapoints.get(0).getTimestamp();
        longestTimespan = 0;
        for(int i = 1; i < datapoints.size(); i++){
            long timespan = datapoints.get(i-1).getTimestamp() - datapoints.get(i).getTimestamp();
            long timestamp = datapoints.get(i).getTimestamp();

            if(timespan <= shortestTimespan){
                shortestTimespan = timespan;
                shortestTimespanTimestamp = timestamp;
            }
            if(timespan >= longestTimespan){
                longestTimespan = timespan;
                longestTimespanTimestamp = timestamp;
            }
        }
        previousValueTimestamp = datapoints.get(0).getTimestamp();
        return true;
    }

    @Override
    public double[] getLimits(ValueDatapoint<?> datapoint) {
        return new double[0];
    }
}
