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

import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

public class SAXParser {

    private static final Logger LOG = Logger.getLogger(SAXParser.class.getName());

    final private XMLReader xr;

    public SAXParser() {
        this(null);
    }

    public SAXParser(DefaultHandler handler) {
        this.xr = create();
        if (handler != null)
            xr.setContentHandler(handler);
    }

    public void setContentHandler(ContentHandler handler) {
        xr.setContentHandler(handler);
    }

    protected XMLReader create() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();

            if (getSchemaSources() != null) {
                // Jump through all the hoops and create a validating reader
                factory.setNamespaceAware(true);
                factory.setSchema(createSchema(getSchemaSources()));
            }
            XMLReader xmlReader = factory.newSAXParser().getXMLReader();
            xmlReader.setErrorHandler(getErrorHandler());
            return xmlReader;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected Schema createSchema(Source[] schemaSources) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setResourceResolver(new CatalogResourceResolver(
                new HashMap<URI, URL>() {{
                    put(DOM.XML_SCHEMA_NAMESPACE, DOM.XML_SCHEMA_RESOURCE);
                }}
            ));
            return schemaFactory.newSchema(schemaSources);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected Source[] getSchemaSources() {
        return null;
    }

    protected ErrorHandler getErrorHandler() {
        return new SimpleErrorHandler();
    }

    public void parse(InputSource source) throws ParserException {
        try {
            xr.parse(source);
        } catch (Exception ex) {
            throw new ParserException(ex);
        }
    }

    /**
     * Always throws exceptions and stops parsing.
     */
    public class SimpleErrorHandler implements ErrorHandler {
        public void warning(SAXParseException e) throws SAXException {
            throw new SAXException(e);
        }

        public void error(SAXParseException e) throws SAXException {
            throw new SAXException(e);
        }

        public void fatalError(SAXParseException e) throws SAXException {
            throw new SAXException(e);
        }
    }

    public static class Handler<I> extends DefaultHandler {

        protected SAXParser parser;
        protected I instance;
        protected Handler<?> parent;
        protected StringBuilder characters = new StringBuilder();
        protected Attributes attributes;

        public Handler(I instance) {
            this(instance, null, null);
        }

        public Handler(I instance, SAXParser parser) {
            this(instance, parser, null);
        }

        public Handler(I instance, Handler<?> parent) {
            this(instance, parent.getParser(), parent);
        }

        public Handler(I instance, SAXParser parser, Handler<?> parent) {
            this.instance = instance;
            this.parser = parser;
            this.parent = parent;
            if (parser != null) {
                parser.setContentHandler(this);
            }
        }

        public I getInstance() {
            return instance;
        }

        public SAXParser getParser() {
            return parser;
        }

        public Handler<?> getParent() {
            return parent;
        }

        protected void switchToParent() {
            if (parser != null && parent != null) {
                parser.setContentHandler(parent);
                attributes = null;
            }
        }

        public String getCharacters() {
            return characters.toString();
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            this.characters = new StringBuilder();
            this.attributes = new AttributesImpl(attributes); // see http://docstore.mik.ua/orelly/xml/sax2/ch05_01.htm, section 5.1.1
            LOG.fine(getClass().getSimpleName() + " starting: " + localName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            characters.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName,
                               String qName) throws SAXException {

            if (isLastElement(uri, localName, qName)) {
                LOG.fine(getClass().getSimpleName() + ": last element, switching to parent: " + localName);
                switchToParent();
                return;
            }

            LOG.fine(getClass().getSimpleName() + " ending: " + localName);
        }

        protected boolean isLastElement(String uri, String localName, String qName) {
            return false;
        }

        protected Attributes getAttributes() {
            return attributes;
        }
    }

}
