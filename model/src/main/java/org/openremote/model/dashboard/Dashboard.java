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
package org.openremote.model.dashboard;

import java.util.Date;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.openremote.model.util.HibernateUniqueIdentifierType;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "DASHBOARD")
public class Dashboard {

  @Id
  @HibernateUniqueIdentifierType
  @Column(name = "ID", length = 22, columnDefinition = "char(22)")
  protected String id;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(
      name = "CREATED_ON",
      updatable = false,
      nullable = false,
      columnDefinition = "TIMESTAMP WITH TIME ZONE")
  @org.hibernate.annotations.CreationTimestamp
  protected Date createdOn;

  @NotBlank(message = "{Dashboard.realm.NotBlank}") @Size(min = 1, max = 255, message = "{Asset.realm.Size}") @Column(name = "REALM", nullable = false, updatable = false)
  protected String realm;

  @Version
  @Column(name = "VERSION", nullable = false)
  protected Long version;

  @Column(name = "OWNER_ID", nullable = false)
  protected String ownerId;

  @Column(name = "ACCESS", nullable = false)
  protected DashboardAccess access;

  @NotBlank(message = "{Dashboard.displayName.NotBlank}") @Column(name = "DISPLAY_NAME", nullable = false)
  protected String displayName;

  @NotNull(message = "{Dashboard.template.NotNull}") @Column(name = "TEMPLATE", nullable = false)
  @JdbcTypeCode(SqlTypes.JSON)
  @Valid protected DashboardTemplate template;

  /* ----------------------------- */

  public Dashboard() {}

  public Dashboard(
      String realm, String displayName, DashboardScreenPreset[] screenPresets, String ownerId) {
    this.realm = realm;
    this.template = new DashboardTemplate(screenPresets);
    this.displayName = displayName;
    this.access = DashboardAccess.SHARED;
    this.ownerId = ownerId;
  }

  public Dashboard setId(String id) {
    this.id = id;
    return this;
  }

  public Dashboard setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
    return this;
  }

  public Dashboard setRealm(String realm) {
    this.realm = realm;
    return this;
  }

  public Dashboard setVersion(long version) {
    this.version = version;
    return this;
  }

  public Dashboard setOwnerId(String ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  public Dashboard setAccess(DashboardAccess access) {
    this.access = access;
    return this;
  }

  public Dashboard setDisplayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  public Dashboard setTemplate(@Valid DashboardTemplate template) {
    this.template = template;
    return this;
  }

  public String getId() {
    return this.id;
  }

  public Date getCreatedOn() {
    return this.createdOn;
  }

  public String getRealm() {
    return this.realm;
  }

  public Long getVersion() {
    return this.version;
  }

  public String getOwnerId() {
    return this.ownerId;
  }

  public DashboardAccess getAccess() {
    return this.access;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public DashboardTemplate getTemplate() {
    return this.template;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Dashboard dashboard)) return false;
    return Objects.equals(id, dashboard.id)
        && Objects.equals(createdOn, dashboard.createdOn)
        && Objects.equals(realm, dashboard.realm)
        && Objects.equals(version, dashboard.version)
        && Objects.equals(ownerId, dashboard.ownerId)
        && access == dashboard.access
        && Objects.equals(displayName, dashboard.displayName)
        && Objects.equals(template, dashboard.template);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, createdOn, realm, version, ownerId, access, displayName, template);
  }
}
