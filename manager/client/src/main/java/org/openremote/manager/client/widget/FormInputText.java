package org.openremote.manager.client.widget;

import com.google.gwt.user.client.ui.TextBox;

public class FormInputText extends TextBox {

    String placeholder = "";

    boolean autofocus;

    public FormInputText() {
        super();
        setStyleName("or-FormControl or-FormInputText");
    }

    public String getPlaceholder() {
        return placeholder;
    }
    public void setPlaceholder(String text) {
        placeholder = (text != null ? text : "");
        getElement().setPropertyString("placeholder", placeholder);
    }

    public boolean isAutofocus() {
        return autofocus;
    }

    public void setAutofocus(boolean autofocus) {
        getElement().setPropertyBoolean("autofocus", autofocus);
    }
}