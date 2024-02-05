package org.openremote.manager.anomalyDetection.DetectionMethods;

import org.openremote.model.attribute.AttributeAnomaly;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.value.AnomalyDetectionConfiguration;

import java.util.List;

public class DetectionMethodTimespan extends DetectionMethod{
    long longestTimespan;
    long longestTimespanTimestamp;
    long shortestTimespan;
    long shortestTimespanTimestamp;
    long previousValueTimestamp;

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
    public boolean UpdateData(List<ValueDatapoint<?>> datapoints) {

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
    public double[] GetLimits(ValueDatapoint<?> datapoint) {
        return new double[0];
    }
}
