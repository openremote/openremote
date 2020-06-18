package org.openremote.agent.protocol.tradfri.device;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.openremote.agent.protocol.tradfri.util.CoapClient;

/**
 * The class that observes a device to automagically detect changes
 */
public abstract class Observer implements CoapHandler {

    /**
     * The endpoint to observe
     */
    private String endpoint;

    /**
     * A CoAP client that can be used to communicate with the IKEA TRÅDFRI gateway
     */
    private CoapClient coapClient;

    /**
     * The observe relation used by CoAP to keep track of the connection to the IKEA TRÅDFRI gateway
     */
    private CoapObserveRelation coapObserveRelation;

    /**
     * Construct the Observer class
     * @param endpoint The endpoint to observe
     * @param coapClient A CoAP client that can be used to communicate with the device using the IKEA TRÅDFRI gateway
     */
    public Observer(String endpoint, CoapClient coapClient) {
        this.endpoint = endpoint;
        this.coapClient = coapClient;
    }

    /**
     * Start observing the endpoint to automagically detect changes
     * @return True if successfully started observing, false if not
     */
    public boolean start(){
        if(coapObserveRelation == null || coapObserveRelation.isCanceled()){
            coapObserveRelation = coapClient.requestObserve(endpoint, this);
            return true;
        }
        return false;
    }

    /**
     * Stop observing the device
     * @return True if successfully stopped observing, false if not
     */
    public boolean stop(){
        if(coapObserveRelation != null && !coapObserveRelation.isCanceled()){
            coapObserveRelation.proactiveCancel();
            return true;
        }
        return false;
    }

    /**
     * Check if there is a difference between the old value and the new value
     * @param oldValue The old value
     * @param newValue The new value
     * @return True if there is a difference between the old value and the new value, false if they are the same
     */
    protected boolean checkChanges(Object oldValue, Object newValue){
        return ((oldValue == null && newValue != null) || (newValue == null && oldValue != null) || (oldValue != null && !oldValue.equals(newValue)));
    }

    /**
     * Handles a new response from the CoAP client
     * @param coapResponse The response to the CoAP request
     */
    @Override
    public void onLoad(CoapResponse coapResponse) {
        if(!coapResponse.isSuccess()) return;
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                callEventHandlers(coapResponse.getResponseText());
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    /**
     * Handles an error from the CoAP client
     */
    @Override
    public void onError() {
    }

    /**
     * Call the appropriate event handlers
     * @param payload The payload text of the CoAP response
     */
    public abstract void callEventHandlers(String payload);

}
