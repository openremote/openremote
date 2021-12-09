/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh.models;

public class SigModelParser {
    private static final String TAG = SigModelParser.class.getSimpleName();

    public static final short CONFIGURATION_SERVER = 0x0000;
    public static final short CONFIGURATION_CLIENT = 0x0001;
    private static final short HEALTH_SERVER_MODEL = 0x0002;
    private static final short HEALTH_CLIENT_MODEL = 0x0003;

    public static final short GENERIC_ON_OFF_SERVER = 0x1000;
    public static final short GENERIC_ON_OFF_CLIENT = 0x1001;
    public static final short GENERIC_LEVEL_SERVER = 0x1002;
    public static final short GENERIC_LEVEL_CLIENT = 0x1003;

    private static final short GENERIC_DEFAULT_TRANSITION_TIME_SERVER = 0x1004;
    private static final short GENERIC_DEFAULT_TRANSITION_TIME_CLIENT = 0x1005;
    private static final short GENERIC_POWER_ON_OFF_SERVER = 0x1006;
    private static final short GENERIC_POWER_ON_OFF_SETUP_SERVER = 0x1007;
    private static final short GENERIC_POWER_ON_OFF_CLIENT = 0x1008;
    private static final short GENERIC_POWER_LEVEL_SERVER = 0x1009;
    private static final short GENERIC_POWER_LEVEL_SETUP_SERVER = 0x100A;
    private static final short GENERIC_POWER_LEVEL_CLIENT = 0x100B;
    private static final short GENERIC_BATTERY_SERVER = 0x100C;
    private static final short GENERIC_BATTERY_CLIENT = 0x100D;
    private static final short GENERIC_LOCATION_SERVER = 0x100E;
    private static final short GENERIC_LOCATION_SETUP_SERVER = 0x100F;
    private static final short GENERIC_LOCATION_CLIENT = 0x1010;
    private static final short GENERIC_ADMIN_PROPERTY_SERVER = 0x1011;
    private static final short GENERIC_MANUFACTURER_PROPERTY_SERVER = 0x1012;
    private static final short GENERIC_USER_PROPERTY_SERVER = 0x1013;
    private static final short GENERIC_CLIENT_PROPERTY_SERVER = 0x1014;
    private static final short GENERIC_PROPERTY_CLIENT = 0x1015;

    // SIG Sensors, Mesh Model Spec
    private static final short SENSOR_SERVER = 0x1100;
    private static final short SENSOR_SETUP_SERVER = 0x1101;
    private static final short SENSOR_CLIENT = 0x1102;

    //SIG Time and Scene, Mesh Model Spec;
    private static final short TIME_SERVER = 0x1200;
    private static final short TIME_SETUP_SERVER = 0x1201;
    private static final short TIME_CLIENT = 0x1202;
    public static final short SCENE_SERVER = 0x1203;
    public static final short SCENE_SETUP_SERVER = 0x1204;
    private static final short SCENE_CLIENT = 0x1205;
    private static final short SCHEDULER_SERVER = 0x1206;
    private static final short SCHEDULER_SETUP_SERVER = 0x1207;
    private static final short SCHEDULER_CLIENT = 0x1208;

    // SIG Lightning, Mesh Model Spec
    private static final short LIGHT_LIGHTNESS_SERVER = 0x1300;
    private static final short LIGHT_LIGHTNESS_SETUP_SERVER = 0x1301;
    private static final short LIGHT_LIGHTNESS_CLIENT = 0x1302;
    private static final short LIGHT_CTL_SERVER = 0x1303;
    private static final short LIGHT_CTL_SETUP_SERVER = 0x1304;
    private static final short LIGHT_CTL_CLIENT = 0x1305;
    private static final short LIGHT_CTL_TEMPERATURE_SERVER = 0x1306;
    private static final short LIGHT_HSL_SERVER = 0x1307;
    private static final short LIGHT_HSL_SETUP_SERVER = 0x1308;
    private static final short LIGHT_HSL_CLIENT = 0x1309;
    private static final short LIGHT_HSL_HUE_SERVER = 0x130A;
    private static final short LIGHT_HSL_SATURATION_SERVER = 0x130B;
    private static final short LIGHT_XYL_SERVER = 0x130C;
    private static final short LIGHT_XYL_SETUP_SERVER = 0x130D;
    private static final short LIGHT_XYL_CLIENT = 0x130E;
    private static final short LIGHT_LC_SERVER = 0x130F;
    private static final short LIGHT_LC_SETUP_SERVER = 0x1310;
    private static final short LIGHT_LC_CLIENT = 0x1311;

    /**
     * Returns the Bluetooth sig model based on the model id.
     *
     * @param modelId bluetooth sig model id
     * @return SigModel
     */
    public static SigModel getSigModel(final int modelId) {
        switch (modelId) {

            case CONFIGURATION_SERVER:
                return new ConfigurationServerModel(modelId);
            case CONFIGURATION_CLIENT:
                return new ConfigurationClientModel(modelId);
            /*
            case HEALTH_SERVER_MODEL:
                return new HealthServerModel(modelId);
            case HEALTH_CLIENT_MODEL:
                return new HealthClientModel(modelId);
            */
            case GENERIC_ON_OFF_SERVER:
                return new GenericOnOffServerModel(modelId);
            case GENERIC_ON_OFF_CLIENT:
                return new GenericOnOffClientModel(modelId);
            /*
            case GENERIC_LEVEL_SERVER:
                return new GenericLevelServerModel(modelId);
            case GENERIC_LEVEL_CLIENT:
                return new GenericLevelClientModel(modelId);
            case GENERIC_DEFAULT_TRANSITION_TIME_SERVER:
                return new GenericDefaultTransitionTimeServer(modelId);
            case GENERIC_DEFAULT_TRANSITION_TIME_CLIENT:
                return new GenericDefaultTransitionTimeClient(modelId);
            case GENERIC_POWER_ON_OFF_SERVER:
                return new GenericPowerOnOffServer(modelId);
            case GENERIC_POWER_ON_OFF_SETUP_SERVER:
                return new GenericPowerOnOffSetupServer(modelId);
            case GENERIC_POWER_ON_OFF_CLIENT:
                return new GenericPowerOnOffClient(modelId);
            case GENERIC_POWER_LEVEL_SERVER:
                return new GenericPowerLevelServer(modelId);
            case GENERIC_POWER_LEVEL_SETUP_SERVER:
                return new GenericPowerLevelSetupServer(modelId);
            case GENERIC_POWER_LEVEL_CLIENT:
                return new GenericPowerLevelClient(modelId);
            case GENERIC_BATTERY_SERVER:
                return new GenericBatteryServer(modelId);
            case GENERIC_BATTERY_CLIENT:
                return new GenericBatteryClient(modelId);
            case GENERIC_LOCATION_SERVER:
                return new GenericLocationServer(modelId);
            case GENERIC_LOCATION_SETUP_SERVER:
                return new GenericLocationSetupServer(modelId);
            case GENERIC_LOCATION_CLIENT:
                return new GenericLocationClient(modelId);
            case GENERIC_ADMIN_PROPERTY_SERVER:
                return new GenericAdminPropertyServer(modelId);
            case GENERIC_MANUFACTURER_PROPERTY_SERVER:
                return new GenericManufacturerPropertyServer(modelId);
            case GENERIC_USER_PROPERTY_SERVER:
                return new GenericUserPropertyServer(modelId);
            case GENERIC_CLIENT_PROPERTY_SERVER:
                return new GenericClientPropertyServer(modelId);
            case GENERIC_PROPERTY_CLIENT:
                return new GenericPropertyClient(modelId);
            case SENSOR_SERVER:
                return new SensorServer(modelId);
            case SENSOR_SETUP_SERVER:
                return new SensorSetupServer(modelId);
            case SENSOR_CLIENT:
                return new SensorClient(modelId);
            case TIME_SERVER:
                return new TimeServer(modelId);
            case TIME_SETUP_SERVER:
                return new TimeSetupServer(modelId);
            case TIME_CLIENT:
                return new TimeClient(modelId);
            case SCENE_SERVER:
                return new SceneServer(modelId);
            case SCENE_SETUP_SERVER:
                return new SceneSetupServer(modelId);
            case SCENE_CLIENT:
                return new SceneClient(modelId);
            case SCHEDULER_SERVER:
                return new SchedulerServer(modelId);
            case SCHEDULER_SETUP_SERVER:
                return new SchedulerSetupServer(modelId);
            case SCHEDULER_CLIENT:
                return new SchedulerClient(modelId);
            case LIGHT_LIGHTNESS_SERVER:
                return new LightLightnessServer(modelId);
            case LIGHT_LIGHTNESS_SETUP_SERVER:
                return new LightLightnessSetupServer(modelId);
            case LIGHT_LIGHTNESS_CLIENT:
                return new LightLightnessClient(modelId);
            case LIGHT_CTL_SERVER:
                return new LightCtlServer(modelId);
            case LIGHT_CTL_SETUP_SERVER:
                return new LightCtlSetupServer(modelId);
            case LIGHT_CTL_CLIENT:
                return new LightCtlClient(modelId);
            case LIGHT_CTL_TEMPERATURE_SERVER:
                return new LightCtlTemperatureServer(modelId);
            case LIGHT_HSL_SERVER:
                return new LightHslServer(modelId);
            case LIGHT_HSL_SETUP_SERVER:
                return new LightHslSetupServer(modelId);
            case LIGHT_HSL_CLIENT:
                return new LightHslClient(modelId);
            case LIGHT_HSL_HUE_SERVER:
                return new LightHslHueServer(modelId);
            case LIGHT_HSL_SATURATION_SERVER:
                return new LightHslSaturationServer(modelId);
            case LIGHT_XYL_SERVER:
                return new LightXylServer(modelId);
            case LIGHT_XYL_SETUP_SERVER:
                return new LightXylSetupServer(modelId);
            case LIGHT_XYL_CLIENT:
                return new LightXylClient(modelId);
            case LIGHT_LC_SERVER:
                return new LightLcServer(modelId);
            case LIGHT_LC_SETUP_SERVER:
                return new LightLcSetupServer(modelId);
            case LIGHT_LC_CLIENT:
                return new LightLcClient(modelId);
             */
            default:
                return new UnknownModel(modelId);
        }
    }
}

