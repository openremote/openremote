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
