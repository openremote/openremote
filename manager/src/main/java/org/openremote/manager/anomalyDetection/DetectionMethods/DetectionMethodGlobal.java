package org.openremote.manager.anomalyDetection.DetectionMethods;

import org.openremote.model.attribute.AttributeAnomaly;
import org.openremote.model.datapoint.AssetAnomalyDatapoint;
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
        anomalyType = AttributeAnomaly.AnomalyType.GlobalOutlier;
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

    public boolean UpdateData(List<AssetAnomalyDatapoint> datapoints) {
        if(datapoints.isEmpty() || datapoints.size() < ((AnomalyDetectionConfiguration.Global)config).minimumDatapoints) return false;
        minValue = Double.MAX_VALUE;
        maxValue = (double)datapoints.get(0).getValue();
        for (AssetAnomalyDatapoint dtapoint : datapoints) {
            if(dtapoint.anomalyType == AttributeAnomaly.AnomalyType.Unchecked || dtapoint.anomalyType == AttributeAnomaly.AnomalyType.Valid){
                if((double)dtapoint.getValue() <= minValue){
                    minValue = (double)dtapoint.getValue();
                    minValueTimestamp = dtapoint.getTimestamp();
                }
                if((double)dtapoint.getValue() >= maxValue){
                    maxValue = (double)dtapoint.getValue();
                    maxValueTimestamp = dtapoint.getTimestamp();
                }
            }
        }
        return true;
    }

    @Override
    public double[] GetLimits(ValueDatapoint<?> datapoint) {
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
