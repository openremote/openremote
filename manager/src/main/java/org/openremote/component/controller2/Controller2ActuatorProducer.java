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
