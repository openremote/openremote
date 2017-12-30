/*
 * Copyright 2015, OpenRemote Inc.
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

package org.openremote.model.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name = "SLOT")
public class Slot extends FlowObject {

    public static final String TYPE_SINK = "urn:openremote:flow:slot:sink";
    public static final String TYPE_SOURCE = "urn:openremote:flow:slot:source";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NODE_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_SLOT_NODE_ID"))
    @org.hibernate.annotations.OnDelete(
        action = org.hibernate.annotations.OnDeleteAction.CASCADE
    )

    @JsonIgnore
    public Node node;

    @Column(name = "CONNECTABLE", nullable = false)
    public boolean connectable = true;

    @Column(name = "PEER_ID")
    public String peerId;

    @Column(name = "PROPERTY_PATH")
    public String propertyPath;


    protected Slot() {
    }


    public Slot(String id, String type) {
        super(null, id, type);
    }


    public Slot(String label, String id, String type) {
        this(label, id, type, null);
    }


    public Slot(String label, String id, String type, String propertyPath) {
        this(label, id, type, true, null, propertyPath);
    }


    public Slot(String id, String type, boolean connectable) {
        this(null, id, type, connectable);
    }


    public Slot(String label, String id, String type, boolean connectable) {
        this(label, id, type, connectable, null, null);
    }


    public Slot(String id, Slot peer, String label) {
        this(label, id, peer.getType(), true, peer.getId(), null);
    }


    public Slot(String label, String id, String type, boolean connectable, String peerId, String propertyPath) {
        super(label, id, type);
        this.connectable = connectable;
        this.peerId = peerId;
        this.propertyPath = propertyPath;
    }

    public boolean isConnectable() {
        return connectable;
    }

    public void setConnectable(boolean connectable) {
        this.connectable = connectable;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(String propertyPath) {
        this.propertyPath = propertyPath;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "label='" + getLabel() + '\'' +
            ", id=" + getId() +
            ", type=" + getType() +
            ", connectable=" + isConnectable() +
            ", peerId=" + getPeerId() +
            ", propertyPath=" + getPropertyPath() +
            '}';
    }
}
