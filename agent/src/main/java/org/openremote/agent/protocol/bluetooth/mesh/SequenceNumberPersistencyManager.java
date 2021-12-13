/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.model.syslog.SyslogCategory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SequenceNumberPersistencyManager {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, SequenceNumberPersistencyManager.class.getName());

    public static String XML_TAG_SEQUENCE_NUMBER_LIST = "SequenceNumbers";
    public static String XML_TAG_SEQUENCE_NUMBER_ITEM = "SequenceNumberItem";
    public static String XML_TAG_NETWORK_KEY = "NetworkKey";
    public static String XML_TAG_ADDRESS = "UnicastAddress";
    public static String XML_TAG_SEQUENCE_NUMBER = "SequenceNumber";

    private final Map<String, Map<String, Integer>> map = new HashMap<>();
    private final URI filePath = new File("/btmesh/sequencenumber.xml").toURI();

    public synchronized void save(NetworkKey networkKey, int unicastAddress, int sequenceNumber) {
        String key = MeshParserUtils.bytesToHex(networkKey.key, false);
        String address = String.format("%04X", unicastAddress);

        LOG.info("Save sequence number: [sourceAddress=" + address + ", sequenceNumber=" + sequenceNumber + "]");

        if (!map.containsKey(key)) {
            map.put(key, new HashMap<String, Integer>());
        }
        Map<String, Integer> addressToNumberMap = map.get(key);
        addressToNumberMap.put(address, sequenceNumber);

        save();
    }

    public synchronized void save() {
        Document doc = new Document();

        Element root = new Element(XML_TAG_SEQUENCE_NUMBER_LIST);
        doc.addContent(root);

        for (Map.Entry<String, Map<String, Integer>> mapEntry : map.entrySet()) {
            String key = mapEntry.getKey();

            for (Map.Entry<String, Integer> supMapEntry : mapEntry.getValue().entrySet()) {
                String address = supMapEntry.getKey();
                Integer number = supMapEntry.getValue();

                Element itemElement = new Element(XML_TAG_SEQUENCE_NUMBER_ITEM);
                Element keyElement = new Element(XML_TAG_NETWORK_KEY);
                keyElement.setText(key);
                Element addressElement = new Element(XML_TAG_ADDRESS);
                addressElement.setText(address);
                Element numberElement = new Element(XML_TAG_SEQUENCE_NUMBER);
                numberElement.setText(Integer.toString(number));

                itemElement.addContent(keyElement);
                itemElement.addContent(addressElement);
                itemElement.addContent(numberElement);
                root.addContent(itemElement);
            }
        }
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileWriter writer = null;
        File file = null;
        try {
            file = new File(filePath);
            writer = new FileWriter(new File(filePath));
            outputter.output(doc, writer);
            writer.flush();
        } catch (IOException e) {
            LOG.severe("Failed to save sequence number file: '" + file.getPath() + "' because: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.severe("Failed to close sequence number file: '" + file.getPath() + "' because: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void load() {
        if (!fileExists()) {
            return;
        }
        File file = new File(filePath);
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(file);
        } catch (JDOMException e) {
            LOG.severe("Failed to load sequence number file: '" + file.getPath() + "' because: " + e.getMessage());
            e.printStackTrace();
            return;
        } catch (IOException e) {
            LOG.severe("Failed to load sequence number file: '" + file.getPath() + "' because: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        map.clear();
        Element root = doc.getRootElement();
        @SuppressWarnings("unchecked")
        List<Element> itemElements = root.getChildren(XML_TAG_SEQUENCE_NUMBER_ITEM);
        for (Element itemElement : itemElements) {
            Element keyElement = itemElement.getChild(XML_TAG_NETWORK_KEY);
            Element addressElement = itemElement.getChild(XML_TAG_ADDRESS);
            Element numberElement = itemElement.getChild(XML_TAG_SEQUENCE_NUMBER);
            if (keyElement == null || addressElement == null | numberElement == null) {
                continue;
            }
            String key = keyElement.getText();
            String address = addressElement.getText();
            String number = numberElement.getText();

            if (!map.containsKey(key)) {
                map.put(key, new HashMap<String, Integer>());
            }
            try {
                map.get(key).put(address, Integer.valueOf(number));
            } catch (NumberFormatException e) {
                LOG.severe("Error while loading sequence number file: '" + file.getPath() + "' because: " + e.getMessage());
                e.printStackTrace();
                continue;
            }
        }
    }

    public synchronized Integer getSequenceNumber(NetworkKey networkKey, int unicastAddress) {
        Integer number = null;
        String key = MeshParserUtils.bytesToHex(networkKey.key, false);
        String address = String.format("%04X", unicastAddress);
        if (map.containsKey(key)) {
            number = map.get(key).get(address);
        }
        return number;
    }

    // Private Instance Methods -------------------------------------------------------------------

    private boolean fileExists() {
        return new File(filePath).exists();
    }
}
