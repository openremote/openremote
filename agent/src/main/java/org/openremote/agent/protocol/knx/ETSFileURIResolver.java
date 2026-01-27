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
package org.openremote.agent.protocol.knx;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.openremote.model.syslog.SyslogCategory;

public class ETSFileURIResolver implements URIResolver {

  private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, KNXProtocol.class);

  private byte[] data;

  public ETSFileURIResolver(byte[] fileData) {
    this.data = fileData;
  }

  @Override
  public Source resolve(String href, String base) throws TransformerException {
    ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(data));
    ZipEntry zipEntry = null;
    try {
      zipEntry = zin.getNextEntry();
      String entryData = null;
      while (zipEntry != null) {
        if (zipEntry.getName().equals(href)) {
          entryData = convertStreamToString(zin);
          break;
        }
        zipEntry = zin.getNextEntry();
      }
      return new StreamSource(new ByteArrayInputStream(entryData.getBytes()));
    } catch (IOException e) {
      LOG.log(
          Level.WARNING,
          "Could not create XML Stream Source for '" + href + "' from ETS project file.");
    }
    return null;
  }

  public static String convertStreamToString(InputStream is) throws IOException {
    if (is != null) {
      Writer writer = new StringWriter();

      char[] buffer = new char[1024];
      try {
        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        int n;
        while ((n = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, n);
        }
      } finally {
        is.close();
      }
      return writer.toString();
    } else {
      return "";
    }
  }
}
