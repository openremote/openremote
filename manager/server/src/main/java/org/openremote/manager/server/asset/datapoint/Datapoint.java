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
package org.openremote.manager.server.asset.datapoint;

import static org.openremote.model.Constants.PERSISTENCE_JSON_OBJECT_TYPE;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import elemental.json.JsonObject;

/**
 * <p>
 * An datapoint is used to store time series data of an assets attribute.
 * <p>
 */
@Table(name = "DATAPOINT")
@Entity(name = "Datapoint")
public class Datapoint implements Serializable {


    /**
     * 
     */
    private static final long serialVersionUID = 7655761395187723632L;

    @Id
    @Column(name = "ASSET_ID", length = 27, nullable = false)
    protected String assetIdd;

    @Id
    @Column(name = "ATTRIBUTE_NAME", nullable = false)
    protected String attributeName;

    @Id
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_ON", nullable = false)
    protected Date timestamp = new Date();

    @Id
    @Column(name = "VALUE", columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_OBJECT_TYPE)
    protected JsonObject value;

    public Datapoint() {
    }
    
    public Datapoint(String assetIdd, String attributeName, Date timestamp, JsonObject value) {
        super();
        this.assetIdd = assetIdd;
        this.attributeName = attributeName;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getAssetIdd() {
        return assetIdd;
    }

    public void setAssetIdd(String assetIdd) {
        this.assetIdd = assetIdd;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public JsonObject getValue() {
        return value;
    }

    public void setValue(JsonObject value) {
        this.value = value;
    }
    
    

}
