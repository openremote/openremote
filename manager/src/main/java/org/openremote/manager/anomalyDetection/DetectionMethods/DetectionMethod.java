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


    public abstract class DetectionMethod implements IDetectionMethod{
        public AnomalyDetectionConfiguration config;
        public  DetectionMethod(AnomalyDetectionConfiguration config){
            this.config = config;
        }
        public String message;
    }


