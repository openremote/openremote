/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.container.web.file;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ResourceImpl implements Resource {

    private static final String ETAG = "W/\"%s-%s\"";

    private final File file;
    private final long length;
    private final long lastModified;
    private final String eTag;
    private final URL url;

    public ResourceImpl(File file) {
        if (file != null && file.isFile()) {
            this.file = file;
            length = file.length();
            lastModified = file.lastModified();
            eTag = createETAG(file.getName(), lastModified);
            try {
                url = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            this.file = null;
            length = 0;
            lastModified = 0;
            eTag = null;
            url = null;
        }
    }

    public ResourceImpl(io.undertow.server.handlers.resource.Resource resource){
        if (resource != null && resource.getUrl() != null && !resource.isDirectory() &&
                // The directory check does not work with JARs so the content length is checked instead
                (!"jar".equals(resource.getUrl().getProtocol()) || resource.getContentLength() > 0)) {
            file = resource.getFile();
            length = resource.getContentLength();
            lastModified = resource.getLastModified().getTime();
            eTag = createETAG(resource.getUrl().getFile(), lastModified);
            url = resource.getUrl();
        }
        else {
            file = null;
            length = 0;
            lastModified = 0;
            eTag = null;
            url = null;
        }
    }

    private String createETAG(String fileName, long lastModified) {
        return format(ETAG, URLEncoder.encode(fileName, UTF_8), lastModified);
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public String getETag() {
        return eTag;
    }

    @Override
    public URL getURL() {
        return url;
    }
}

