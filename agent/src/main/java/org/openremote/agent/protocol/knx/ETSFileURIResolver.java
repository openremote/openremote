package org.openremote.agent.protocol.knx;

import org.openremote.model.syslog.SyslogCategory;

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

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

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
            LOG.log(Level.WARNING, "Could not create XML Stream Source for '" + href + "' from ETS project file.");
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
