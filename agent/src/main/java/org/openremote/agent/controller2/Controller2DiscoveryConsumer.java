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

import java.util.List;
import java.util.logging.Logger;

public class Controller2DiscoveryConsumer extends Controller2Consumer implements Controller2Adapter.DiscoveryListener{

    private static final Logger LOG = Logger.getLogger(Controller2DiscoveryConsumer.class.getName());

    public Controller2DiscoveryConsumer(Controller2Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    synchronized protected void doStart() throws Exception {
        super.doStart();
        getEndpoint().getAdapter().addDiscoveryListener(this);
    }

    @Override
    synchronized protected void doStop() throws Exception {
        getEndpoint().getAdapter().removeDiscoveryListener(this);
        super.doStop();
    }

    @Override
    public void onDiscovery(List<String> list) {
        if (!isStarted()) {
            LOG.fine("Received discovery event but consumer hasn't been started");
            return;
        }
        LOG.fine("Starting new exchange for discovered devices: " + list.size());
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(list);
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
