package org.openremote.agent.command;

import org.openremote.agent.sensor.Sensor;

/**
 * Push commands are intended for collecting updates from "active" devices which broadcast
 * them over various transport mechanisms -- examples include bus installations such as
 * KNX bus or IP-network broadcasts.
 *
 * Push commands are expected to create their own threads (if needed) which implement the
 * push functionality and also push updates to the context of the agent using the
 * sensor callback API provided.
 *
 * Push command implementations use the sensor {@link Sensor#update} method to push state
 * changes into the agents's context, as shown in the example below:
 *
 * <pre><code>
 *
 * public class SamplePushCommand implements PushCommand, Runnable {
 *
 *    // This implementation assumes a command instance per sensor, so only
 *    // deals with a single sensor reference
 *    Sensor sensor;
 *
 *    &#64;Override
 *    public void setSensor(Sensor sensor) {
 *      this.sensor = sensor;
 *
 *      // Initialize the sensor with a default value. The sensor implementation may provide
 *      // additional validation or translations to these values...
 *
 *      sensor.update("0");
 *
 *      // This implementation starts a listening thread per sensor. If you want multiple
 *      // push commands / sensors to share same resources or threads, this can be managed in
 *      // the command builder implementation...
 *
 *      Thread t = new Thread(this);
 *      t.start();
 *    }
 *
 *    &#64;Overide
 *    public void run() {
 *      String sensorValue = implementYourListenerLogic()
 *      sensor.update(sensorValue);
 *    }
 * }
 *</code></pre>
 *
 *
 *
 * <b>NOTE:</b> The agent at this point does not implement any flow control on its side --
 *              therefore a very busy listener can overflow it with too many updates. Control
 *              flow must be implemented by the listener implementation ensuring that not too
 *              many updates are created.
 */
public interface PushCommand extends SensorUpdateCommand {

  /**
   * This method may be invoked multiple times if the command instance is associated
   * with several sensors. How many instances of the command exist and how multiple
   * associations are handled is the responsibility of the command/factory implementation.
   */
  void start(Sensor sensor);

    /**
     * This method may be invoked multiple times if the command instance is associated
     * with several sensors. How many instances of the command exist and how multiple
     * associations are handled is the responsibility of the command/factory implementation.
     */
  void stop(Sensor sensor);
}
