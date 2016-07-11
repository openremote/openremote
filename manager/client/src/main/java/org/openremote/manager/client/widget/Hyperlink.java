package org.openremote.manager.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.util.JsUtil;

public class Hyperlink extends com.google.gwt.user.client.ui.Hyperlink {

    protected String icon;
    protected Label iconLabel = new Label();

    public Hyperlink() {
        super();

        getElement().addClassName("or-Hyperlink");
        Element anchor = getElement().getFirstChildElement();
        // TODO Clickable icon?!
        DOM.insertBefore(getElement(), iconLabel.getElement(), anchor);
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
        iconLabel.getElement().removeClassName("or-HyperlinkIcon");
        if (icon != null) {
            iconLabel.getElement().addClassName("or-HyperlinkIcon fa fa-" + icon);
        }
    }
}
