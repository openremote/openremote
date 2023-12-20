package org.openremote.manager.anomalyDetection.DetectionMethods;

import org.openremote.model.attribute.AttributeAnomaly;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.value.AnomalyDetectionConfiguration;

import java.util.List;


    public abstract class DetectionMethod implements IDetectionMethod{
        public AnomalyDetectionConfiguration config;
        public  DetectionMethod(AnomalyDetectionConfiguration config){
            this.config = config;
        }
        public String message;
    }


