package org.openremote.manager.anomalyDetection.DetectionMethods;

import org.openremote.model.attribute.AttributeAnomaly;
import org.openremote.model.datapoint.AssetAnomalyDatapoint;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.value.AnomalyDetectionConfiguration;

import java.util.List;

public class DetectionMethodChange extends DetectionMethod {
    double biggestIncrease;
    long biggestIncreaseTimestamp;
    double smallestIncrease;
    long smallestIncreaseTimestamp;
    double previousValue;
    long previousValueTimestamp;

    public DetectionMethodChange(AnomalyDetectionConfiguration config){
        super(config);
        anomalyType = AttributeAnomaly.AnomalyType.ContextualOutlier;
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
    public boolean UpdateData(List<AssetAnomalyDatapoint> datapoints) {
        if(datapoints.size() < ((AnomalyDetectionConfiguration.Change)config).minimumDatapoints) return false;
        smallestIncrease = Double.MAX_VALUE;
        biggestIncrease = -100000000;
        for(int i = 1; i < datapoints.size(); i++){
            if(datapoints.get(i).anomalyType == AttributeAnomaly.AnomalyType.Unchecked || datapoints.get(i).anomalyType == AttributeAnomaly.AnomalyType.Valid) {

            }
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
    public double[] GetLimits(ValueDatapoint<?> datapoint) {
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
