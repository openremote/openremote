/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.model.asset;


import jsinterop.annotations.JsType;
import org.openremote.model.attribute.AttributeTypeDescriptor;
import org.openremote.model.attribute.AttributeValueDescriptor;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.http.RequestParams;
import org.openremote.model.http.SuccessStatusCode;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("model")
@JsType(isNative = true)
public interface AssetModelResource {

    /**
     * Retrieve asset descriptors {@link AssetDescriptor} present.
     * <p>
     */
    @GET
    @Path("asset/descriptors")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @SuppressWarnings("unusable-by-js")
    AssetDescriptor[] getAssetDescriptors(@BeanParam RequestParams requestParams);

    /**
     * Retrieve attribute type descriptors {@link AttributeTypeDescriptor} present.
     * <p>
     */
    @GET
    @Path("attribute/typeDescriptors")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @SuppressWarnings("unusable-by-js")
    AttributeTypeDescriptor[] getAttributeTypeDescriptors(@BeanParam RequestParams requestParams);

    /**
     * Retrieve attribute type descriptors {@link AttributeValueDescriptor} present.
     * <p>
     */
    @GET
    @Path("attribute/valueDescriptors")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @SuppressWarnings("unusable-by-js")
    AttributeValueDescriptor[] getAttributeValueDescriptors(@BeanParam RequestParams requestParams);

    /**
     * Retrieve meta type descriptors {@link MetaItemDescriptor} present.
     * <p>
     */
    @GET
    @Path("metaItem/descriptors")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @SuppressWarnings("unusable-by-js")
    MetaItemDescriptor[] getMetaItemDescriptors(@BeanParam RequestParams requestParams);
}
