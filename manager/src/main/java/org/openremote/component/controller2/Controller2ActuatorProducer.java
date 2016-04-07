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
package org.openremote.component.controller2;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller2ActuatorProducer extends DefaultProducer {

    private static final Logger LOG = Logger.getLogger(Controller2ActuatorProducer.class.getName());

    public Controller2ActuatorProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public Controller2Endpoint getEndpoint() {
        return (Controller2Endpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // Use either the command from the exchange header or the default endpoint command
        String command = exchange.getIn().getHeader(
            Controller2Component.HEADER_COMMAND,
            null, // TODO: Default command option on producer endpoint URI?
            String.class
        );
        if (command == null || command.length() == 0)
            throw new IllegalArgumentException(
                "No command in exchange header or default command on endpoint URI"
            );

        // As the command argument, use either the exchange body as a string or null
        String arg = exchange.getIn().getBody() != null
            ? exchange.getIn().getBody().toString()
            : null;

        if (LOG.isLoggable(Level.FINE))
            LOG.fine("Producing and sending command '" + command + "' with argument: " + arg);

        getEndpoint().sendCommand(command, arg);
    }
}
