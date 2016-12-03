package org.openremote.agent.deploy.xml;

import org.openremote.container.xml.DOMParser;
import org.openremote.container.xml.NamespaceContextMap;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;

/**
 * Usage:
 *
 * <pre><code>
 *     AgentDOM dom = new AgentDOMParser().parse(deploymentXml);
 * </code></pre>
 */
public class AgentDOMParser extends DOMParser<AgentDOM> {

    public static final String SCHEMA_XSD = "/org/openremote/agent/schemas/agent-3.xsd";

    public AgentDOMParser() {
        super(new StreamSource(AgentDOMParser.class.getResourceAsStream(SCHEMA_XSD)));
    }

    @Override
    protected AgentDOM createDOM(Document document) {
        return new AgentDOM(document, createXPath());
    }

    public NamespaceContextMap createDefaultNamespaceContext(String... optionalPrefixes) {
        NamespaceContextMap ctx = new NamespaceContextMap() {
            @Override
            protected String getDefaultNamespaceURI() {
                return AgentDOM.NAMESPACE_URI;
            }
        };
        for (String optionalPrefix : optionalPrefixes) {
            ctx.put(optionalPrefix, AgentDOM.NAMESPACE_URI);
        }
        return ctx;
    }

    public XPath createXPath() {
        return super.createXPath(createDefaultNamespaceContext(AgentElement.XPATH_PREFIX));
    }
}
