package org.openremote.agent.deploy.xml;

import org.openremote.container.xml.DOMElement;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

public class AgentElement extends DOMElement<AgentElement, AgentElement> {

    public static final String XPATH_PREFIX = "c";

    public AgentElement(XPath xpath, Element element) {
        super(xpath, element);
    }

    @Override
    protected String prefix(String localName) {
        return XPATH_PREFIX + ":" + localName;
    }

    @Override
    protected Builder<AgentElement> createParentBuilder(DOMElement el) {
        return new Builder<AgentElement>(el) {
            @Override
            public AgentElement build(Element element) {
                return new AgentElement(getXpath(), element);
            }
        };
    }

    @Override
    protected ArrayBuilder<AgentElement> createChildBuilder(DOMElement el) {
        return new ArrayBuilder<AgentElement>(el) {
            @Override
            public AgentElement[] newChildrenArray(int length) {
                return new AgentElement[length];
            }

            @Override
            public AgentElement build(Element element) {
                return new AgentElement(getXpath(), element);
            }
        };
    }
}
