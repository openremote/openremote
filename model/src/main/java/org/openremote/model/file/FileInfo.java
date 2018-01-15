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
package org.openremote.model.file;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileInfo {
    protected final String name;
    protected final String contents;
    protected final boolean binary;

    @JsonCreator
    public FileInfo(@JsonProperty("name") String name, @JsonProperty("contents") String contents, @JsonProperty("binary") boolean binary) {
        this.name = name;
        this.contents = contents;
        this.binary = binary;
    }

    public String getName() {
        return name;
    }

    public String getContents() {
        return contents;
    }

    public boolean isBinary() {
        return binary;
    }
}
