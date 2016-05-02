/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.controller2;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.logging.Logger;

public class Controller2ReadConsumer extends Controller2Consumer implements Controller2Adapter.SensorListener {

    private static final Logger LOG = Logger.getLogger(Controller2ReadConsumer.class.getName());
    protected String deviceUri;
    protected String resourceUri;

    public Controller2ReadConsumer(Controller2Endpoint endpoint, Processor processor, String deviceUri, String resourceUri) {
        super(endpoint, processor);
        this.deviceUri = deviceUri;
        this.resourceUri = resourceUri;
    }

    @Override
    synchronized protected void doStart() throws Exception {
        super.doStart();
        getEndpoint().getAdapter().addSensorListener(this);
    }

    @Override
    synchronized protected void doStop() throws Exception {
        getEndpoint().getAdapter().removeSensorListener(this);
        super.doStop();
    }

    @Override
    public String getDeviceUri() {
        return deviceUri;
    }

    @Override
    public String getResourceUri() {
        return resourceUri;
    }

    @Override
    public void onUpdate(Object newValue) {
        // Push value into message body and camel type converter can be used to
        // get value into the required type by the next processor in the route
        LOG.fine("Consuming state change from '" + deviceUri + " : " + resourceUri + "'");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(newValue);
        try {
            getProcessor().process(exchange);
        } catch (Exception ex) {
            getExceptionHandler().handleException("Error processing exchange", exchange, ex);
        } finally {
            if (exchange.getException() != null) {
                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
            }
        }
    }

}
