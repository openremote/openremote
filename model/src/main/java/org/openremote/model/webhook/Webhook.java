package org.openremote.model.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.http.HTTPMethod;

import java.util.List;
import java.util.Map;

public class Webhook {

    protected String name;
    protected String url;
    protected Map<String, List<String>> headers;
    protected HTTPMethod httpMethod;
    protected UsernamePassword usernamePassword;
    protected OAuthGrant oAuthGrant;
    protected String payload;

    public Webhook() {
    }

    @JsonCreator
    public Webhook(@JsonProperty("name") String name,
                   @JsonProperty("url") String url,
                   @JsonProperty("headers") Map<String, List<String>> headers,
                   @JsonProperty("httpMethod") HTTPMethod httpMethod,
                   @JsonProperty("usernamePassword") UsernamePassword usernamePassword,
                   @JsonProperty("oAuthGrant") OAuthGrant oAuthGrant,
                   @JsonProperty("payload") String payload) {
        this.name = name;
        this.url = url;
        this.headers = headers;
        this.httpMethod = httpMethod;
        this.usernamePassword = usernamePassword;
        this.oAuthGrant = oAuthGrant;
        this.payload = payload;
    }

    public String getName() {
        return name;
    }

    public Webhook setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HTTPMethod method) {
        this.httpMethod = method;
    }

    public UsernamePassword getUsernamePassword() {
        return usernamePassword;
    }

    public void setAuthMethod(UsernamePassword usernamePassword) {
        this.usernamePassword = usernamePassword;
    }

    public OAuthGrant getOAuthGrant() {
        return oAuthGrant;
    }

    public void setoAuthGrant(OAuthGrant oAuthGrant) {
        this.oAuthGrant = oAuthGrant;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
