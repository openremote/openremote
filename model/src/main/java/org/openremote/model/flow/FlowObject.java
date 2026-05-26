/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.flow;

import org.openremote.model.IdentifiableEntity;
import org.openremote.model.util.HibernateUniqueIdentifierType;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@MappedSuperclass
public class FlowObject implements IdentifiableEntity<FlowObject> {

  @Id
  @HibernateUniqueIdentifierType
  @Column(name = "ID")
  public String id;

  @NotNull @Size(min = 3, max = 255) @Column(name = "MODEL_TYPE")
  public String type;

  @Column(name = "LABEL")
  public String label;

  protected FlowObject() {}

  public FlowObject(String id, String type, String label) {
    this.id = id;
    this.type = type;
    this.label = label;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public FlowObject setId(String id) {
    this.id = id;
    return this;
  }

  public String getType() {
    return this.type;
  }

  public boolean isOfType(String type) {
    return this.getType().equals(type);
  }

  public String toTypeIdString() {
    return this.type + ':' + this.id;
  }

  public String getLabel() {
    return this.label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isLabelEmpty() {
    return this.getLabel() == null || this.getLabel().length() == 0;
  }

  public String getDefaultedLabel() {
    return this.isLabelEmpty() ? "Unnamed " + this.getClass().getSimpleName() : this.getLabel();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o != null && this.getClass() == o.getClass()) {
      FlowObject that = (FlowObject) o;
      return this.getId().equals(that.getId());
    } else {
      return false;
    }
  }

  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "id='"
        + id
        + '\''
        + ", type='"
        + type
        + '\''
        + ", label='"
        + label
        + '\''
        + '}';
  }
}
