package org.openremote.manager.anomalyDetection.DetectionMethods;

import org.openremote.model.datapoint.AssetAnomalyDatapoint;
import org.openremote.model.datapoint.ValueDatapoint;

import java.util.List;

    public interface IDetectionMethod{
        /**Check if value is valid according to the methods rules. */
        boolean validateDatapoint(Object value, long timestamp);
        /**Update needsNewData based on needs method */
        boolean checkRecentDataSaved(long latestTimestamp);
        /**Update saved values used to calculate Limits */
        boolean UpdateData(List<AssetAnomalyDatapoint> datapoints);
        double[] GetLimits(ValueDatapoint<?> datapoint);
    }

