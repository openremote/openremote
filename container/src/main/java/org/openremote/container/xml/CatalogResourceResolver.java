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
package org.openremote.container.xml;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Another namespace-URI-to-whatever (namespace, context, resolver, map) magic thingy.
 * <p>
 * Of course it's just a map, like so many others in the JAXP hell. But this time someone
 * really went all out on the API fugliness. The person who designed this probably got promoted
 * and is now designing the signaling system for the airport you are about to land on.
 * Feeling better?
 * </p>
 */
public class CatalogResourceResolver implements LSResourceResolver {

    private static final Logger LOG = Logger.getLogger(CatalogResourceResolver.class.getName());

    private final Map<URI, URL> catalog;

    public CatalogResourceResolver(Map<URI, URL> catalog) {
        this.catalog = catalog;
    }

    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        LOG.fine("Trying to resolve system identifier URI in catalog: " + systemId);
        URL systemURL;
        if ((systemURL = catalog.get(URI.create(systemId))) != null) {
            LOG.finest("Loading catalog resource: " + systemURL);
            try {
                Input i = new Input(systemURL.openStream());
                i.setBaseURI(baseURI);
                i.setSystemId(systemId);
                i.setPublicId(publicId);
                return i;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        LOG.info(
            "System identifier not found in catalog, continuing with default resolution " +
                "(this most likely means remote HTTP request!): " + systemId
        );
        return null;
    }

    // WTF...
    private static final class Input implements LSInput {

        InputStream in;

        public Input(InputStream in) {
            this.in = in;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public InputStream getByteStream() {
            return in;
        }

        public void setByteStream(InputStream byteStream) {
        }

        public String getStringData() {
            return null;
        }

        public void setStringData(String stringData) {
        }

        public String getSystemId() {
            return null;
        }

        public void setSystemId(String systemId) {
        }

        public String getPublicId() {
            return null;
        }

        public void setPublicId(String publicId) {
        }

        public String getBaseURI() {
            return null;
        }

        public void setBaseURI(String baseURI) {
        }

        public String getEncoding() {
            return null;
        }

        public void setEncoding(String encoding) {
        }

        public boolean getCertifiedText() {
            return false;
        }

        public void setCertifiedText(boolean certifiedText) {
        }
    }
}
