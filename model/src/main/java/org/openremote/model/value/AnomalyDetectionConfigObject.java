package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class AnomalyDetectionConfigObject implements Serializable {
    public AnomalyDetectionConfiguration[] methods;

    public AnomalyDetectionConfigObject(@JsonProperty("methods") AnomalyDetectionConfiguration[] methods){
        this.methods = methods;
    }

}
