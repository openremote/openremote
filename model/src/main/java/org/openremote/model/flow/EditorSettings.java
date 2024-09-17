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

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

public class EditorSettings {

    @NotNull
    @Column(name = "EDITOR_TYPE_LABEL", nullable = false)
    public String typeLabel = "Unknown Type";

    @NotNull
    @Column(name = "EDITOR_NODE_COLOR", nullable = false)
    @Enumerated(EnumType.STRING)
    public NodeColor nodeColor = NodeColor.DEFAULT;

    @NotNull
    @Column(name = "EDITOR_POSITION_X", nullable = false)
    public double positionX;

    @NotNull
    @Column(name = "EDITOR_POSITION_Y", nullable = false)
    public double positionY;

    @Column(name = "EDITOR_COMPONENTS")
    public String[] components;


    public EditorSettings() {
    }


    public EditorSettings(String typeLabel) {
        this.typeLabel = typeLabel;
    }


    public EditorSettings(String typeLabel, NodeColor nodeColor) {
        this.typeLabel = typeLabel;
        this.nodeColor = nodeColor;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public NodeColor getNodeColor() {
        return nodeColor;
    }

    public void setNodeColor(NodeColor nodeColor) {
        this.nodeColor = nodeColor;
    }

    public double getPositionX() {
        return positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public String[] getComponents() {
        return components;
    }

    public void setComponents(String[] components) {
        this.components = components;
    }
}
