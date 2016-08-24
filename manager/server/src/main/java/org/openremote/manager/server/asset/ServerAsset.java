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
package org.openremote.manager.server.asset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.openremote.manager.shared.asset.Asset;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * This can only be used on the server and offers types (such as Hibernate proxying, Geometry)
 * which can not be serialized or compiled on the client.
 */
@Entity(name = "Asset")
public class ServerAsset extends Asset {

    @JoinColumn(name = "PARENT_ID", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    protected ServerAsset parent;

    @NotNull
    @Column(name = "LOCATION", nullable = false)
    @Access(AccessType.PROPERTY)
    @JsonIgnore
    protected Point location;

    public ServerAsset() {
    }

    public ServerAsset(Asset parent) {
        super(parent);
    }

    public ServerAsset getParent() {
        return parent;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
        setCoordinates(
            location.getCoordinate().getOrdinate(Coordinate.X),
            location.getCoordinate().getOrdinate(Coordinate.Y)
        );
    }

}
