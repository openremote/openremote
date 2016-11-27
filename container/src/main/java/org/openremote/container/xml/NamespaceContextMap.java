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

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This should have been part of the JDK.
 * <p>
 * The dumb XPath API needs a map to lookup namespace URIs using prefix keys. Unfortunately,
 * the authors did not know <tt>java.util.Map</tt>.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class NamespaceContextMap extends HashMap<String, String> implements NamespaceContext {

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("No prefix provided!");
        } else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return getDefaultNamespaceURI();
        } else if (get(prefix) != null) {
            return get(prefix);
        } else {
            return XMLConstants.NULL_NS_URI;
        }
    }

    // Whatever, we don't care
    public String getPrefix(String namespaceURI) {
        return null;
    }

    // Whatever, we don't care
    public Iterator getPrefixes(String s) {
        return null;
    }

    protected abstract String getDefaultNamespaceURI();
}
