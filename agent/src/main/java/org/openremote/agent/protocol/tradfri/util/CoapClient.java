package org.openremote.agent.protocol.tradfri.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * The class that is used to communicate with the IKEA TRÅDFRI gateway using the CoAP protocol
 */
public class CoapClient {

    private final ObjectMapper objectMapper = ValueUtil.JSON;

    /**
     * The credentials used to authenticate the CoAP client to the IKEA TRÅDFRI gateway
     */
    private Credentials credentials;

    /**
     * A DTLS endpoint used to secure the connection between the CoAP client and the IKEA TRÅDFRI gateway
     */
    private Endpoint dtlsEndpoint;

    /**
     * The timeout for connections between the CoAP client and the IKEA TRÅDFRI gateway (in milliseconds)
     * @value 20000
     */
    private long timeout = 20000L;

    /**
     * Get the credentials used to communicate with the IKEA TRÅDFRI gateway
     * @return The credentials that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public Credentials getCredentials() {
        return this.credentials;
    }

    /**
     * Change the credentials used to communicate with the IKEA TRÅDFRI gateway
     * @param credentials The new credentials that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public void setCredentials(Credentials credentials){
        this.credentials = credentials;
        try {
            updateDtlsConnector();
        } catch (IOException ignored) { }
    }

    /**
     * Set up a secure connection between the CoAP client and the IKEA TRÅDFRI gateway
     * @throws IOException Thrown if a failure to open a connection between the CoAP client and the IKEA TRÅDFRI gateway occurs
     */
    private void updateDtlsConnector() throws IOException {
        if(dtlsEndpoint != null) dtlsEndpoint.destroy();
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
        builder.setAddress(new InetSocketAddress(0));
        InMemoryPskStore pskStore = new InMemoryPskStore();
        pskStore.addKnownPeer(new InetSocketAddress(ApiEndpoint.getGatewayIp(), 5684),
                credentials.getIdentity(),
                credentials.getKey().getBytes());
        builder.setPskStore(pskStore);

        DTLSConnector dtlsconnector = new DTLSConnector(builder.build(), null);
        CoapEndpoint.Builder endpointBuilder = new CoapEndpoint.Builder();
        endpointBuilder.setConnector(dtlsconnector);

        dtlsEndpoint = endpointBuilder.build();
        dtlsEndpoint.start();
        EndpointManager.getEndpointManager().setDefaultEndpoint(dtlsEndpoint);
    }

    /**
     * Get timeout for connections between the CoAP client and the IKEA TRÅDFRI gateway (in milliseconds)
     * @return The timeout for connections between the CoAP client and the IKEA TRÅDFRI gateway (in milliseconds)
     */
    public long getTimeout() {
        return this.timeout;
    }

    /**
     * Change the timeout for connections between the CoAP client and the IKEA TRÅDFRI gateway (in milliseconds)
     * @param timeout The new timeout for connections between the CoAP client and the IKEA TRÅDFRI gateway (in milliseconds)
     */
    public void setTimeout(long timeout){
        this.timeout = timeout;
    }

    /**
     * Make a CoAP request to the specified endpoint
     * @param request The Request object
     * @param endpoint The endpoint to make a request to
     * @param responseType The expected type of response
     * @param <T> The expected type of response
     * @return The response from the IKEA TRÅDFRI gateway (converted to the expected response type)
     */
    @SuppressWarnings("unchecked")
    private <T> T request(Request request, String endpoint, Class<T> responseType) {
        try {
            request.setURI(endpoint);
            request.send();
            Response response = request.waitForResponse(timeout);
            if (response == null) return null;
            String responsePayload = response.getPayloadString();
            if (responseType == String.class) return (T) responsePayload;
            return objectMapper.readValue(responsePayload, responseType);
        } catch (InterruptedException | JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Make a CoAP request with a payload to the specified endpoint
     * @param request The Request object
     * @param endpoint The endpoint to make a request to
     * @param payload The payload to send in the request
     * @param responseType The expected type of response
     * @param <T> The expected type of response
     * @return The response from the IKEA TRÅDFRI gateway (converted to the expected response type)
     */
    private <T> T requestWithPayload(Request request, String endpoint, Object payload, Class<T> responseType) {
        try {
            String requestPayload = objectMapper.writeValueAsString(payload);
            request.setPayload(requestPayload);
            request.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_JSON);
            return request(request, endpoint, responseType);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Make a CoAP observe request to the specified endpoint
     * @param endpoint The endpoint to make a request to
     * @param handler The handler to handle the responses from the observe request
     * @return The observe relation that represents the connection to the IKEA TRÅDFRI gateway
     */
    public CoapObserveRelation requestObserve(String endpoint, CoapHandler handler) {
        org.eclipse.californium.core.CoapClient client = new org.eclipse.californium.core.CoapClient();
        client.setTimeout(timeout);
        Request request = Request.newGet();
        request.setURI(endpoint);
        request.setObserve();
        return client.observe(request, handler);
    }

    /**
     * Make a CoAP GET request to the specified endpoint
     * @param endpoint The endpoint to make a request to
     * @param responseType The expected type of response
     * @param <T> The expected type of response
     * @return The response from the IKEA TRÅDFRI gateway (converted to the expected response type)
     */
    public <T> T get(String endpoint, Class<T> responseType) {
        Request request = Request.newGet();
        return request(request, endpoint, responseType);
    }

    /**
     * Make a CoAP POST request with a payload to the specified endpoint
     * @param endpoint The endpoint to make a request to
     * @param payload The payload to send in the request
     * @param responseType The expected type of response
     * @param <T> The expected type of response
     * @return The response from the IKEA TRÅDFRI gateway (converted to the expected response type)
     */
    public <T> T post(String endpoint, Object payload, Class<T> responseType) {
        Request request = Request.newPost();
        return requestWithPayload(request, endpoint, payload, responseType);
    }

    /**
     * Make a CoAP PUT request with a payload to the specified endpoint
     * @param endpoint The endpoint to make a request to
     * @param payload The payload to send in the request
     * @param responseType The expected type of response
     * @param <T> The expected type of response
     * @return The response from the IKEA TRÅDFRI gateway (converted to the expected response type)
     */
    public <T> T put(String endpoint, Object payload, Class<T> responseType) {
        Request request = Request.newPut();
        return requestWithPayload(request, endpoint, payload, responseType);
    }

}
