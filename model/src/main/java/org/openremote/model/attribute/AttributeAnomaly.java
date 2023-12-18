/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.attribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.openremote.model.datapoint.AssetPredictedDatapoint;

import java.io.Serializable;
import java.util.Date;

import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;
@Entity
@Table(name = AttributeAnomaly.TABLE_NAME)
@org.hibernate.annotations.Immutable
public class AttributeAnomaly implements Serializable {

    public static final String TABLE_NAME = "asset_anomaly";

    @Id
    @Column(name = "ENTITY_ID", length = 36, nullable = false)
    protected String assetId;

    @Id
    @Column(name = "ATTRIBUTE_NAME", nullable = false)
    protected String attributeName;

    @Id
    @Column(name = "TIMESTAMP", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    protected Date timestamp;

    @Column(name = "ANOMALY_TYPE", updatable = false, nullable = false, columnDefinition = "ANOMALY_TYPE")
    protected AnomalyType anomalyType;

    @Column(name = "ALARM_ID")
    protected Long alarmId;


    public AttributeAnomaly() {
    }
    public AttributeAnomaly(String assetId, String attributeName, Date timestamp, int anomalyType) {

        this.assetId = assetId;
        this.attributeName = attributeName;
        this.timestamp = timestamp;
        if(anomalyType < AnomalyType.values().length && anomalyType >= 0){
            this.anomalyType = AnomalyType.values()[anomalyType];
        }
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public AnomalyType getAnomalyType(){
        return anomalyType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeAnomaly that = (AttributeAnomaly) o;
        return assetId.equals(that.assetId) && attributeName.equals(that.attributeName) && timestamp == that.timestamp && anomalyType.equals(that.anomalyType);
    }

    @Override
    public int hashCode() {
        int result = assetId.hashCode();
        result = 31 * result + attributeName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + assetId + '\'' +
            ", name='" + attributeName + '\'' +
            '}';
    }

    public enum AnomalyType{
        Unchecked,
        Valid,
        Multiple,
        GlobalOutlier,
        ContextualOutlier,
        IrregularInterval

    }
}


