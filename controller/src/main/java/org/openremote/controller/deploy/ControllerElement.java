package org.openremote.controller.deploy;

import org.openremote.container.xml.DOMElement;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

public class ControllerElement extends DOMElement<ControllerElement, ControllerElement> {

    public static final String XPATH_PREFIX = "c";

    public ControllerElement(XPath xpath, Element element) {
        super(xpath, element);
    }

    @Override
    protected String prefix(String localName) {
        return XPATH_PREFIX + ":" + localName;
    }

    @Override
    protected Builder<ControllerElement> createParentBuilder(DOMElement el) {
        return new Builder<ControllerElement>(el) {
            @Override
            public ControllerElement build(Element element) {
                return new ControllerElement(getXpath(), element);
            }
        };
    }

    @Override
    protected ArrayBuilder<ControllerElement> createChildBuilder(DOMElement el) {
        return new ArrayBuilder<ControllerElement>(el) {
            @Override
            public ControllerElement[] newChildrenArray(int length) {
                return new ControllerElement[length];
            }

            @Override
            public ControllerElement build(Element element) {
                return new ControllerElement(getXpath(), element);
            }
        };
    }
}
