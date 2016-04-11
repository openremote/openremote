package org.openremote.manager.shared.device;

public class ResourceRef {
    protected String uri;
    protected String valueConverter;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getValueConverter() {
        return valueConverter;
    }

    public void setValueConverter(String valueConverter) {
        this.valueConverter = valueConverter;
    }
}
