/*
 * Copyright 2018, OpenRemote Inc.
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
package org.openremote.container.util;

import org.apache.commons.codec.binary.Base64;

public class CodecUtil {

    protected CodecUtil() {}

    public static byte[] decodeBase64(String base64String) {
        if (base64String == null) {
            return null;
        }

        if (base64String.length() == 0) {
            return new byte[0];
        }

        // Could be data URL encoded so look for comma in first 50 chars
        int searchLength = Math.min(50, base64String.length());
        String str = base64String.substring(0, searchLength-1);
        int commaIndex = str.indexOf(',');
        if (commaIndex >=0) {
            base64String = base64String.substring(commaIndex+1);
        }

        return Base64.decodeBase64(base64String);
    }
}
