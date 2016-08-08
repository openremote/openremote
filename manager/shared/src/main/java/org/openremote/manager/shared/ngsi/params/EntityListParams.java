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
package org.openremote.manager.shared.ngsi.params;

import org.openremote.manager.shared.ngsi.simplequery.Query;

import javax.ws.rs.QueryParam;

/**
 * A given entity have to match all the criteria to be retrieved (i.e. criteria are combined
 * in a logical AND way). Note that id and idPattern parameters are incompatible in the
 * same request.
 */
public class EntityListParams {

    @QueryParam("id")
    public String[] id;

    @QueryParam("type")
    public String[] type;

    @QueryParam("idPattern")
    public String idPattern;

    @QueryParam("typePattern")
    public String typePattern;

    @QueryParam("q")
    public Query query;

    @QueryParam("georel")
    public String geoRel;

    @QueryParam("geometry")
    public String geometry;

    @QueryParam("coords")
    public String coords;

    @QueryParam("limit")
    public Long limit;

    @QueryParam("offset")
    public Long offset;

    @QueryParam("attrs")
    public String[] attributes;

    @QueryParam("orderBy")
    public String orderBy;

    @QueryParam("options")
    public EntityRepresentation options;

    public EntityListParams() {
    }

    public EntityListParams id(String... id) {
        idPattern = null;
        this.id = id;
        return this;
    }

    public EntityListParams type(String... type) {
        this.type = type;
        return this;
    }

    public EntityListParams idPattern(String idPattern) {
        this.id = null;
        this.idPattern = idPattern;
        return this;
    }

    public EntityListParams typePattern(String typePattern) {
        this.type = null;
        this.typePattern = typePattern;
        return this;
    }

    public EntityListParams query(Query query) {
        this.query = query;
        return this;
    }

    public EntityListParams geoRel(String geoRel) {
        this.geoRel = geoRel;
        return this;
    }

    public EntityListParams geometry(String geometry) {
        this.geometry = geometry;
        return this;
    }

    public EntityListParams coords(String coords) {
        this.coords = coords;
        return this;
    }

    public EntityListParams limit(Long limit) {
        this.limit = limit;
        return this;
    }

    public EntityListParams offset(Long offset) {
        this.offset = offset;
        return this;
    }

    public EntityListParams attributes(String... attributes) {
        this.attributes = attributes;
        return this;
    }

    public EntityListParams orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public EntityListParams options(EntityRepresentation options) {
        this.options = options;
        return this;
    }
}
