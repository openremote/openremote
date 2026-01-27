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
package org.openremote.model.query;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;

public class UserQuery {

  public static class AttributeValuePredicate {
    public boolean negated;
    public StringPredicate name;
    public StringPredicate value;

    public AttributeValuePredicate(boolean negated, StringPredicate name) {
      this.negated = negated;
      this.name = name;
    }

    @JsonCreator
    public AttributeValuePredicate(boolean negated, StringPredicate name, StringPredicate value) {
      this.negated = negated;
      this.name = name;
      this.value = value;
    }

    public boolean isNegated() {
      return negated;
    }

    public AttributeValuePredicate setNegated(boolean negated) {
      this.negated = negated;
      return this;
    }

    public StringPredicate getName() {
      return name;
    }

    public AttributeValuePredicate setName(StringPredicate name) {
      this.name = name;
      return this;
    }

    public StringPredicate getValue() {
      return value;
    }

    public AttributeValuePredicate setValue(StringPredicate value) {
      this.value = value;
      return this;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName()
          + "{"
          + "negated="
          + negated
          + ", name="
          + name
          + ", value="
          + value
          + '}';
    }
  }

  public static class Select {
    public boolean basic;

    public Select basic(boolean basic) {
      this.basic = basic;
      return this;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" + ", basic=" + basic + '}';
    }
  }

  public static class OrderBy {

    public enum Property {
      CREATED_ON,
      FIRST_NAME,
      LAST_NAME,
      USERNAME,
      EMAIL
    }

    public Property property;
    public boolean descending;

    public OrderBy() {}

    public OrderBy(Property property) {
      this.property = property;
    }

    public OrderBy(Property property, boolean descending) {
      this.property = property;
      this.descending = descending;
    }

    public OrderBy property(Property property) {
      this.property = property;
      return this;
    }

    public OrderBy descending(boolean descending) {
      this.descending = descending;
      return this;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName()
          + "{"
          + "property="
          + property
          + ", descending="
          + descending
          + '}';
    }
  }

  // Restriction predicates
  public RealmPredicate realmPredicate;
  public String[] assets;
  public PathPredicate pathPredicate;
  public String[] ids;
  public Select select;
  public StringPredicate[] usernames;

  /** AND condition is assumed between values */
  public AttributeValuePredicate[] attributes;

  /**
   * OR condition is assumed between values (AND filtering can be applied by the caller on the
   * results)
   */
  public StringPredicate[] clientRoles;

  public StringPredicate[] realmRoles;
  public Boolean serviceUsers;
  public Integer limit;
  public Integer offset;
  public OrderBy orderBy;

  public UserQuery() {}

  public UserQuery realm(RealmPredicate realmPredicate) {
    this.realmPredicate = realmPredicate;
    return this;
  }

  public UserQuery assets(String... assetIds) {
    this.assets = assetIds;
    return this;
  }

  public UserQuery assetPath(PathPredicate pathPredicate) {
    this.pathPredicate = pathPredicate;
    return this;
  }

  public UserQuery ids(String... ids) {
    this.ids = ids;
    return this;
  }

  public UserQuery usernames(StringPredicate... usernames) {
    this.usernames = usernames;
    return this;
  }

  public UserQuery attributes(AttributeValuePredicate... attributes) {
    this.attributes = attributes;
    return this;
  }

  public UserQuery clientRoles(StringPredicate... clientRoles) {
    this.clientRoles = clientRoles;
    return this;
  }

  public UserQuery realmRoles(StringPredicate... realmRoles) {
    this.realmRoles = realmRoles;
    return this;
  }

  public UserQuery serviceUsers(Boolean serviceUsers) {
    this.serviceUsers = serviceUsers;
    return this;
  }

  public UserQuery limit(int limit) {
    this.limit = limit;
    return this;
  }

  public UserQuery offset(int offset) {
    this.offset = offset;
    return this;
  }

  public UserQuery orderBy(OrderBy orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  public UserQuery select(Select select) {
    this.select = select;
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "realmPredicate="
        + realmPredicate
        + ", assets="
        + assets
        + ", pathPredicate="
        + pathPredicate
        + ", ids="
        + (ids != null ? Arrays.toString(ids) : "null")
        + ", usernames="
        + (usernames != null ? Arrays.toString(usernames) : "null")
        + ", serviceUsers="
        + serviceUsers
        + ", attributes="
        + (attributes != null ? Arrays.toString(attributes) : "null")
        + ", clientRoles="
        + (clientRoles != null ? Arrays.toString(clientRoles) : "null")
        + ", realmRoles="
        + (realmRoles != null ? Arrays.toString(realmRoles) : "null")
        + ", limit="
        + limit
        + ", offset="
        + offset
        + ", orderBy="
        + orderBy
        + ", select="
        + select
        + '}';
  }
}
