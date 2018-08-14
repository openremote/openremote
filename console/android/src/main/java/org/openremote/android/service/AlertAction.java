package org.openremote.android.service;

import java.io.Serializable;

public class AlertAction implements Serializable {
    private String url;
    private String httpMethod;
    private String data;
    private boolean silent;
    private boolean openInBrowser;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean isOpenInBrowser() {
        return openInBrowser;
    }

    public void setOpenInBrowser(boolean openInBrowser) {
        this.openInBrowser = openInBrowser;
    }

    @Override
    public String toString() {
        return "AlertAction{" +
                "url=" + url +
                ", httpMethod=" + httpMethod +
                ", data=" + data +
                ", silent=" + silent +
                ", openInBrowser=" + openInBrowser +
                '}';
    }
}