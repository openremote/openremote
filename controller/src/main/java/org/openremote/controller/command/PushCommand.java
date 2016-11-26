package org.openremote.controller.command;

import org.openremote.controller.model.Sensor;

/**
 * Push commands are intended for collecting events from "active" devices which broadcast
 * them over various transport mechanisms -- examples include bus installations such as
 * KNX bus or IP-network broadcasts. In some cases it is also possible to use event listener
 * interface to implement polling mechanism to "passive" devices -- normally a default
 * polling implementation is provided by {@link PullCommand}
 * implementation but push commands can be used to override this default implementation.
 *
 * A push command can be used with the same 'sensor' abstraction (seen for example in
 * the controller's <tt>controller.xml</tt) configuration file) as
 * {@link PullCommand}s.
 *
 * Push commands are expected to create their own threads (if needed) which implement the
 * push functionality and also directly push received events to the
 * (global) state cache of the controller using the sensor callback API provided.
 *
 * Push command implementations can use the sensor
 * {@link org.openremote.controller.model.Sensor#update} method to push state changes
 * into the controller, as shown in the example below:
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
 * <b>NOTE:</b> The controller at this point does not implement any flow control on its side --
 *              therefore a very busy listener can overflow it with too many events. Control
 *              flow must be implemented co-operatively by the listener implementation ensuring
 *              that not too many events are created.
 *
 */
public interface PushCommand extends EventProducerCommand {

  /**
   * Each event listener is initialized with one or more sensor references the listener is bound to.
   * If the listener is used as an input for multiple sensors, this callback is invoked multiple
   * times, once for each associated sensor.
   * 
   * @param sensor    sensor this event listener is bound to
   */
  void setSensor(Sensor sensor);

  void stop(Sensor sensor);
}
