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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@IdClass(Wire.Id.class)
@Entity
@Table(name = "WIRE")
public class Wire {

    public static class Id implements Serializable {

        public String sourceId;
        public String sinkId;
        public String flowId;

        public Id() {
        }

        public Id(String sourceId, String sinkId) {
            this.sourceId = sourceId;
            this.sinkId = sinkId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Id id = (Id) o;

            if (sourceId != null ? !sourceId.equals(id.sourceId) : id.sourceId != null) return false;
            if (sinkId != null ? !sinkId.equals(id.sinkId) : id.sinkId != null) return false;
            return !(flowId != null ? !flowId.equals(id.flowId) : id.flowId != null);

        }

        @Override
        public int hashCode() {
            int result = sourceId != null ? sourceId.hashCode() : 0;
            result = 31 * result + (sinkId != null ? sinkId.hashCode() : 0);
            result = 31 * result + (flowId != null ? flowId.hashCode() : 0);
            return result;
        }
    }

    @javax.persistence.Id
    @NotNull
    @Column(name = "SOURCE_NODE_ID")
    public String sourceId;

    @javax.persistence.Id
    @NotNull
    @Column(name = "SINK_NODE_ID")
    public String sinkId;

    @javax.persistence.Id
    @NotNull
    @Column(name = "FLOW_ID")

    @JsonIgnore
    public String flowId;


    protected Wire() {
    }


    public Wire(Slot source, Slot sink) {
        this(source.getId(), sink.getId());
    }


    public Wire(String sourceId, String sinkId) {
        this.sourceId = sourceId;
        this.sinkId = sinkId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSinkId() {
        return sinkId;
    }

    public boolean equalsSlots(Slot sourceSlot, Slot sinkSlot) {
        return !(sourceSlot == null || sinkSlot == null)
            && equalsSlotIds(sourceSlot.getId(), sinkSlot.getId());
    }

    public boolean equalsSlotIds(String sourceId, String sinkId) {
        return getSourceId().equals(sourceId) && getSinkId().equals(sinkId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Wire wire = (Wire) o;

        if (sourceId != null ? !sourceId.equals(wire.sourceId) : wire.sourceId != null) return false;
        return !(sinkId != null ? !sinkId.equals(wire.sinkId) : wire.sinkId != null);
    }

    @Override
    public int hashCode() {
        int result = sourceId != null ? sourceId.hashCode() : 0;
        result = 31 * result + (sinkId != null ? sinkId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sourceId='" + sourceId + '\'' +
            ", sinkId='" + sinkId + '\'' +
            '}';
    }
}