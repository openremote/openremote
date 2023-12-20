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
    public boolean UpdateData(List<ValueDatapoint<?>> datapoints) {
        if(datapoints.isEmpty())return false;
        predictedPoints = datapoints;
        return true;
    }

    @Override
    public double[] GetLimits(ValueDatapoint<?> datapoint) {
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
