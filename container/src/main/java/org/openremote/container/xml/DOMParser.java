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

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Condensed API for parsing of XML into DOM with (optional) XML schema validation.
 * <p>
 * Provides many XML utility features, like pretty printing, escaping, or node visitor.
 * </p>
 * <p>
 * NOTE: This class is not thread-safe because JAXP factories are not thread-safe!
 * </p>
 */
public abstract class DOMParser<D extends DOM> implements ErrorHandler, EntityResolver {

    private static final Logger LOG = Logger.getLogger(DOMParser.class.getName());

    protected Source[] schemaSources;
    protected Schema schema;

    public DOMParser(Source... schemaSources) {
        this.schemaSources = schemaSources;
    }

    public Schema getSchema() {

        if (schema == null) {
            // Lazy initialization
            // TODO: http://stackoverflow.com/questions/3129934/schemafactory-doesnt-support-w3c-xml-schema-in-platform-level-8
            try {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

                schemaFactory.setResourceResolver(new CatalogResourceResolver(
                    new HashMap<URI, URL>() {{
                        put(DOM.XML_SCHEMA_NAMESPACE, DOM.XML_SCHEMA_RESOURCE);
                    }}
                ));

                if (schemaSources != null) {
                    schema = schemaFactory.newSchema(schemaSources);
                } else {
                    schema = schemaFactory.newSchema();
                }

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return schema;
    }

    // =================================================================================================

    protected abstract D createDOM(Document document);

    public DocumentBuilderFactory createFactory(boolean validating) throws ParserException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {

            // Yes, namespaces would be nice... because they make XML sooooo eXtensible!
            factory.setNamespaceAware(true);

            if (validating) {

                // Well this in theory works without validation but requires namespaces. We don't want
                // namespaces when we are not validating because we get funny xmlns="" in output.
                factory.setXIncludeAware(true);

                // Whatever that does... we want it
                factory.setFeature("http://apache.org/xml/features/xinclude/fixup-base-uris", false);
                factory.setFeature("http://apache.org/xml/features/xinclude/fixup-language", false);

                // Good idea to set a schema when you want to validate! Tell me, how does it work
                // without a schema?!
                factory.setSchema(getSchema());

                // Oh, it's dynamic! Soooo dynamic! This is hilarious:
                //
                // "The parser will validate the document only if a grammar is specified."
                //
                // What, you are surprised that it won't validate without a grammar?!?! Well,
                // I'm going to turn that smart feature on, of course! Seriously, it won't
                // validate without this switch. And I'm so proud having 'apache.org' in my
                // source, a true sign of quality.
                //
                factory.setFeature("http://apache.org/xml/features/validation/dynamic", true);
            }

        } catch (ParserConfigurationException ex) {
            // Lovely, of course it couldn't have been a RuntimeException!
            throw new ParserException(ex);
        }
        return factory;
    }

    public Transformer createTransformer(String method, int indent, boolean standalone) throws ParserException {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();

            if (indent > 0) {
                try {
                    transFactory.setAttribute("indent-number", indent);
                } catch (IllegalArgumentException ex) {
                    // Fuck you, Apache morons.
                }
            }

            Transformer transformer = transFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, standalone ? "no" : "yes");

            // JDK 7 bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7150637
            if (standalone) {
                try {
                    transformer.setOutputProperty("http://www.oracle.com/xml/is-standalone", "yes");
                } catch (IllegalArgumentException e) {
                    // Expected on older versions
                }
            }

            transformer.setOutputProperty(OutputKeys.INDENT, indent > 0 ? "yes" : "no");
            if (indent > 0)
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
            transformer.setOutputProperty(OutputKeys.METHOD, method);

            return transformer;
        } catch (Exception ex) {
            throw new ParserException(ex);
        }
    }

    public D createDocument() {
        try {
            return createDOM(createFactory(false).newDocumentBuilder().newDocument());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // =================================================================================================

    public D parse(URL url) throws ParserException {
        return parse(url, true);
    }

    public D parse(String string) throws ParserException {
        if (string == null)
            throw new IllegalArgumentException("No file available");
        return parse(string, true);
    }

    public D parse(File file) throws ParserException {
        if (file == null)
            throw new IllegalArgumentException("No file available");
        return parse(file, true);
    }

    public D parse(InputStream stream) throws ParserException {
        if (stream == null)
            throw new IllegalArgumentException("No input stream available");
        return parse(stream, true);
    }

    public D parse(URL url, boolean validate) throws ParserException {
        if (url == null)
            throw new IllegalArgumentException("Can't parse null URL");
        try {
            return parse(url.openStream(), validate);
        } catch (Exception ex) {
            throw new ParserException("Parsing URL failed: " + url, ex);
        }
    }

    public D parse(String string, boolean validate) throws ParserException {
        if (string == null)
            throw new IllegalArgumentException("Can't parse null string");
        return parse(new InputSource(new StringReader(string)), validate);
    }

    public D parse(File file, boolean validate) throws ParserException {
        if (file == null)
            throw new IllegalArgumentException("Can't parse null file");
        try {
            return parse(file.toURI().toURL(), validate);
        } catch (Exception ex) {
            throw new ParserException("Parsing file failed: " + file, ex);
        }
    }

    public D parse(InputStream stream, boolean validate) throws ParserException {
        if (stream == null)
            throw new IllegalArgumentException("No file available");
        return parse(new InputSource(stream), validate);
    }

    public D parse(InputSource source, boolean validate) throws ParserException {
        try {

            DocumentBuilder parser = createFactory(validate).newDocumentBuilder();

            parser.setEntityResolver(this);

            parser.setErrorHandler(this);

            Document dom = parser.parse(source);

            dom.normalizeDocument();

            return createDOM(dom);

        } catch (Exception ex) {
            throw unwrapException(ex);
        }
    }

    // =================================================================================================

    public void validate(URL url) throws ParserException {
        if (url == null) throw new IllegalArgumentException("Can't validate null URL");
        LOG.fine("Validating XML of URL: " + url);
        validate(new StreamSource(url.toString()));
    }

    public void validate(String string) throws ParserException {
        if (string == null) throw new IllegalArgumentException("Can't validate null string");
        LOG.fine("Validating XML string characters: " + string.length());
        validate(new SAXSource(new InputSource(new StringReader(string))));
    }

    public void validate(Document document) throws ParserException {
        validate(new DOMSource(document));
    }

    public void validate(DOM dom) throws ParserException {
        validate(new DOMSource(dom.getW3CDocument()));
    }

    public void validate(Source source) throws ParserException {
        try {
            Validator validator = getSchema().newValidator();
            validator.setErrorHandler(this);
            validator.validate(source);
        } catch (Exception ex) {
            throw unwrapException(ex);
        }
    }

    // =================================================================================================

    public XPathFactory createXPathFactory() {
        return XPathFactory.newInstance();
    }

    public XPath createXPath(NamespaceContext nsContext) {
        XPath xpath = createXPathFactory().newXPath();
        xpath.setNamespaceContext(nsContext);
        return xpath;
    }

    public XPath createXPath(XPathFactory factory, NamespaceContext nsContext) {
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(nsContext);
        return xpath;
    }

    public Object getXPathResult(DOM dom, XPath xpath, String expr, QName result) {
        return getXPathResult(dom.getW3CDocument(), xpath, expr, result);
    }

    public Object getXPathResult(DOMElement<?,?> element, XPath xpath, String expr, QName result) {
        return getXPathResult(element.getW3CElement(), xpath, expr, result);
    }

    public Object getXPathResult(Node context, XPath xpath, String expr, QName result) {
        try {
            LOG.fine("Evaluating xpath query: " + expr);
            return xpath.evaluate(expr, context, result);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // =================================================================================================

    public String print(DOM dom) throws ParserException {
        return print(dom, 4, true);
    }

    public String print(DOM dom, int indent) throws ParserException {
        return print(dom, indent, true);
    }

    public String print(DOM dom, boolean standalone) throws ParserException {
        return print(dom, 4, standalone);
    }

    public String print(DOM dom, int indent, boolean standalone) throws ParserException {
        return print(dom.getW3CDocument(), indent, standalone);
    }

    public String print(Document document, int indent, boolean standalone) throws ParserException {
        removeIgnorableWSNodes(document.getDocumentElement());
        return print(new DOMSource(document.getDocumentElement()), indent, standalone);
    }

    public String print(String string, int indent, boolean standalone) throws ParserException {
        return print(new StreamSource(new StringReader(string)), indent, standalone);
    }

    public String print(Source source, int indent, boolean standalone) throws ParserException {
        try {
            Transformer transformer = createTransformer("xml", indent, standalone);
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");

            StringWriter out = new StringWriter();
            transformer.transform(source, new StreamResult(out));
            out.flush();

            return out.toString();

        } catch (Exception e) {
            throw new ParserException(e);
        }
    }

    public String printHTML(Document dom) throws ParserException {
        return printHTML(dom, 4, true, true);
    }

    // This prints XHTML into HTML that IE understands, and that validates with W3C rules

    public String printHTML(Document dom, int indent, boolean standalone, boolean doctype) throws ParserException {

        // Make a copy so we can remove stuff from the DOM that violates W3C when rendered as HTML (go figure!)
        dom = (Document) dom.cloneNode(true);

        // CDATA will be escaped by the transformer for HTML output but we
        // need to copy it into text nodes (yes, I know XML is fantastic...)
        accept(dom.getDocumentElement(), new NodeVisitor(Node.CDATA_SECTION_NODE) {
            @Override
            public void visit(Node node) {
                CDATASection cdata = (CDATASection) node;
                cdata.getParentNode().setTextContent(cdata.getData());
            }
        });

        removeIgnorableWSNodes(dom.getDocumentElement());

        try {
            Transformer transformer = createTransformer("html", indent, standalone);

            if (doctype) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD HTML 4.01 Transitional//EN");
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/html4/loose.dtd");
            }

            StringWriter out = new StringWriter();
            transformer.transform(new DOMSource(dom), new StreamResult(out));
            out.flush();
            String output = out.toString();

            // Rip out the idiotic META http-equiv tag - we have HTTP headers for that!
            String meta = "\\s*<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">";
            output = output.replaceFirst(meta, "");

            // Rip out the even dumber xmlns attribute that magically got added (seems to be a difference between JDK 1.4 and 5)
            String xmlns = "<html xmlns=\"http://www.w3.org/1999/xhtml\">";
            output = output.replaceFirst(xmlns, "<html>");

            return output;

        } catch (Exception ex) {
            throw new ParserException(ex);
        }
    }

    public void removeIgnorableWSNodes(Element element) {
        Node nextNode = element.getFirstChild();
        Node child;
        while (nextNode != null) {
            child = nextNode;
            nextNode = child.getNextSibling();
            if (isIgnorableWSNode(child)) {
                element.removeChild(child);
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                removeIgnorableWSNodes((Element) child);
            }
        }
    }

    public boolean isIgnorableWSNode(Node node) {
        // TODO: What about XML space="preserve"?
        return node.getNodeType() == Node.TEXT_NODE &&
            node.getTextContent().matches("[\\t\\n\\x0B\\f\\r\\s]+");
    }

    // =================================================================================================

    public void warning(SAXParseException e) throws SAXException {
        LOG.warning(e.toString());
    }

    public void error(SAXParseException e) throws SAXException {
        throw new SAXException(new ParserException(e));
    }

    public void fatalError(SAXParseException e) throws SAXException {
        throw new SAXException(new ParserException(e));
    }

    protected ParserException unwrapException(Exception ex) {
        // Another historic moment in Java XML API design!
        if (ex.getCause() != null && ex.getCause() instanceof ParserException) {
            return (ParserException) ex.getCause();
        }
        return new ParserException(ex);
    }

    // =================================================================================================

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        // By default this builds an EntityResolver that _stays offline_.
        // Damn you XML clowns, just because a URI looks like a URL does NOT mean you should fetch it!
        InputSource is;
        if (systemId.startsWith("file://")) {
            is = new InputSource(new FileInputStream(new File(URI.create(systemId))));
        } else {
            is = new InputSource(new ByteArrayInputStream(new byte[0]));
        }
        is.setPublicId(publicId);
        is.setSystemId(systemId);
        return is;

    }

    // ======================================= Utility Methods =============================================

    public static String escape(String string) {
        return escape(string, false, false);
    }

    public static String escape(String string, boolean convertNewlines, boolean convertSpaces) {
        if (string == null) return null;
        StringBuilder sb = new StringBuilder();
        String entity;
        char c;
        for (int i = 0; i < string.length(); ++i) {
            entity = null;
            c = string.charAt(i);
            switch (c) {
                case '<':
                    entity = "&#60;";
                    break;
                case '>':
                    entity = "&#62;";
                    break;
                case '&':
                    entity = "&#38;";
                    break;
                case '"':
                    entity = "&#34;";
                    break;
            }
            if (entity != null) {
                sb.append(entity);
            } else {
                sb.append(c);
            }
        }
        String result = sb.toString();
        if (convertSpaces) {
            // Converts the _beginning_ of line whitespaces into non-breaking spaces
            Matcher matcher = Pattern.compile("(\\n+)(\\s*)(.*)").matcher(result);
            StringBuffer temp = new StringBuffer();
            while (matcher.find()) {
                String group = matcher.group(2);
                StringBuilder spaces = new StringBuilder();
                for (int i = 0; i < group.length(); i++) {
                    spaces.append("&#160;");
                }
                matcher.appendReplacement(temp, "$1" + spaces.toString() + "$3");
            }
            matcher.appendTail(temp);
            result = temp.toString();
        }
        if (convertNewlines) {
            result = result.replaceAll("\n", "<br/>");
        }
        return result;
    }

    public static String stripElements(String xml) {
        if (xml == null) return null;
        return xml.replaceAll("<([a-zA-Z]|/).*?>", "");
    }

    public static void accept(Node node, NodeVisitor visitor) {
        if (node == null) return;
        if (visitor.isHalted()) return;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            boolean cont = true;
            if (child.getNodeType() == visitor.nodeType) {
                visitor.visit(child);
                if (visitor.isHalted()) break;
            }
            accept(child, visitor);
        }
    }

    public static abstract class NodeVisitor {
        private short nodeType;

        protected NodeVisitor(short nodeType) {
            assert nodeType < Node.NOTATION_NODE; // All other node types are below
            this.nodeType = nodeType;
        }

        public boolean isHalted() {
            return false;
        }

        public abstract void visit(Node node);
    }

    public static String wrap(String wrapperName, String fragment) {
        return wrap(wrapperName, null, fragment);
    }

    public static String wrap(String wrapperName, String xmlns, String fragment) {
        StringBuilder wrapper = new StringBuilder();
        wrapper.append("<").append(wrapperName);
        if (xmlns != null) {
            wrapper.append(" xmlns=\"").append(xmlns).append("\"");
        }
        wrapper.append(">");
        wrapper.append(fragment);
        wrapper.append("</").append(wrapperName).append(">");
        return wrapper.toString();
    }

}
