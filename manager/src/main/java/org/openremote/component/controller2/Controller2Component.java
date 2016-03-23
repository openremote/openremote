package org.openremote.component.controller2;

import com.ning.http.client.uri.Uri;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.util.Map;

public class Controller2Component extends UriEndpointComponent {

    public static final String DISCOVERY = "discovery";
    public static final String HEADER_COMMAND = Controller2Component.class.getCanonicalName() + ".HEADER_COMMAND";

    protected final Controller2Adapter.Manager adapterManager;

    public Controller2Component(Controller2Adapter.Manager adapterManager) {
        super(Controller2Endpoint.class);
        this.adapterManager = adapterManager;
    }

    public Controller2Component() {
        this(Controller2Adapter.DEFAULT_MANAGER);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        if (remaining == null || remaining.length() == 0) {
            throw new IllegalArgumentException("Required '<IP or host name>[:<port>]' in URI");
        }
        try {
            URI endpointUri = URI.create(uri);
            String host = endpointUri.getHost();
            int port = endpointUri.getPort() > 0 ? endpointUri.getPort() : 8080; // TODO default port?
            String path = endpointUri.getPath();
            boolean discoveryOnly = endpointUri.getPath().equals("/discovery");
            return new Controller2Endpoint(uri, this, adapterManager, host, port, discoveryOnly);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
    }

}
