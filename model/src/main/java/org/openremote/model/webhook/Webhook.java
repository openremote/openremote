package org.openremote.model.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Webhook {

    public enum Method {
        GET, POST, PUT, DELETE
    }

    public enum AuthMethod {
        NONE, HTTP_BASIC, API_KEY, OAUTH2
    }

    public static class Header {
        public String header;
        public String value;

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /* --------- */

    protected String name;
    protected String url;
    protected Header[] headers;
    protected Method method;
    protected AuthMethod authMethod;
    protected String payload;

    public Webhook() {
    }

    @JsonCreator
    public Webhook(@JsonProperty("name") String name, @JsonProperty("url") String url, @JsonProperty("headers") Header[] headers, @JsonProperty("method") Method method, @JsonProperty("authMethod") AuthMethod authMethod, @JsonProperty("payload") String payload) {
        this.name = name;
        this.url = url;
        this.headers = headers;
        this.method = method;
        this.authMethod = authMethod;
        this.payload = payload;
    }

    public String getName() {
        return name;
    }
    public Webhook setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) {
        this.url = url;
    }

    public Header[] getHeaders() { return headers; }
    public void setHeaders(Header[] headers) {
        this.headers = headers;
    }

    public Method getMethod() { return method; }
    public void setMethod(Method method) {
        this.method = method;
    }

    public AuthMethod getAuthMethod() { return authMethod; }
    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    public String getPayload() { return payload; }
    public void setPayload(String payload) {
        this.payload = payload;
    }
}
