package org.openremote.manager.anomalyDetection.DetectionMethods;

import org.openremote.model.attribute.AttributeAnomaly;
import org.openremote.model.datapoint.AssetAnomalyDatapoint;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.value.AnomalyDetectionConfiguration;



import java.util.List;

public class DetectionMethodForecast extends DetectionMethod{
    private List<AssetAnomalyDatapoint> predictedPoints;
    public DetectionMethodForecast(AnomalyDetectionConfiguration config){
        super(config);
        anomalyType = AttributeAnomaly.AnomalyType.ContextualOutlier;
    }

    @Override
    public boolean validateDatapoint(Object value, long timestamp) {
        return true;
    }

    @Override
    public boolean checkRecentDataSaved(long latestTimestamp) {
        return false;
    }

    @Override
    public boolean UpdateData(List<AssetAnomalyDatapoint> datapoints) {
        if(!datapoints.isEmpty())return false;
        predictedPoints = datapoints;
        return true;
    }

    @Override
    public double[] GetLimits(ValueDatapoint<?> datapoint) {
        return new double[0];
    }
}
