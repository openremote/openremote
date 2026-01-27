/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.model.security;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.Subselect;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Subselect(
    "select * from PUBLIC.KEYCLOAK_ROLE where NOT CLIENT_ROLE") // Map this immutable to an SQL
// view, don't use/create table
public class RealmRole {

  @JsonIgnore @Id protected String id;

  @Column(name = "name")
  protected String name;

  @Column(name = "description")
  protected String description;

  protected RealmRole() {}

  @JsonCreator
  public RealmRole(String id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public RealmRole(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RealmRole realmRole = (RealmRole) o;
    return name.equals(realmRole.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
