package org.openremote.controller.deploy;

import org.openremote.container.xml.DOMParser;
import org.openremote.container.xml.NamespaceContextMap;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;

public class ControllerDOMParser extends DOMParser<ControllerDOM> {

    public static final String SCHEMA_XSD = "/org/openremote/controller/schemas/controller-2.0-M7.xsd";

    public ControllerDOMParser() {
        super(new StreamSource(ControllerDOMParser.class.getResourceAsStream(SCHEMA_XSD)));
    }

    @Override
    protected ControllerDOM createDOM(Document document) {
        return new ControllerDOM(document, createXPath());
    }

    public NamespaceContextMap createDefaultNamespaceContext(String... optionalPrefixes) {
        NamespaceContextMap ctx = new NamespaceContextMap() {
            @Override
            protected String getDefaultNamespaceURI() {
                return ControllerDOM.NAMESPACE_URI;
            }
        };
        for (String optionalPrefix : optionalPrefixes) {
            ctx.put(optionalPrefix, ControllerDOM.NAMESPACE_URI);
        }
        return ctx;
    }

    public XPath createXPath() {
        return super.createXPath(createDefaultNamespaceContext(ControllerElement.XPATH_PREFIX));
    }
}
