package org.openremote.agent.protocol.velbus.device;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import static org.openremote.agent.protocol.velbus.AbstractVelbusProtocol.LOG;

public enum VelbusDeviceType {

    UNKNOWN(0x00, false),
    VMB4RYNO(0x11, false, RelayProcessor.class),
    VMB4RYLD(0x10, false, RelayProcessor.class),
    VMB1RYNO(0x1B, false, RelayProcessor.class),
    VMB1RYNOS(0x29, false, RelayProcessor.class),
    VMB1RY(0x02, false, RelayProcessor.class),
    VMBGP1(0x1E, true, InputProcessor.class, TemperatureProcessor.class, ThermostatProcessor.class, ProgramsProcessor.class),
    VMBGP2(0x1F, true, InputProcessor.class, TemperatureProcessor.class, ThermostatProcessor.class, ProgramsProcessor.class),
    VMBGP4(0x20, true, InputProcessor.class, TemperatureProcessor.class, ThermostatProcessor.class, ProgramsProcessor.class),
    VMBGPO(0x21, true, InputProcessor.class, TemperatureProcessor.class, ThermostatProcessor.class, ProgramsProcessor.class, OLEDProcessor.class),
    VMBGPOD(0x28, true, InputProcessor.class, TemperatureProcessor.class, ThermostatProcessor.class, ProgramsProcessor.class, OLEDProcessor.class),
    VMB7IN(0x22, false, InputProcessor.class, ProgramsProcessor.class, CounterProcessor.class),
    VMB8PBU(0x16, false, InputProcessor.class, ProgramsProcessor.class),
    VMB6PBN(0x17, false, InputProcessor.class, ProgramsProcessor.class),
    VMBDMI(0x15, false, AnalogOutputProcessor.class),
    VMBDMIR(0x2F, false, AnalogOutputProcessor.class),
    VMBDME(0x14, false, AnalogOutputProcessor.class),
    VMB4DC(0x12, false, AnalogOutputProcessor.class),
    VMB2BLE(0x1D, false, BlindProcessor.class),
    VMB1BL(0x03, false, BlindProcessor.class),
    VMBGP4PIR(0x2D, true, InputProcessor.class, TemperatureProcessor.class, ThermostatProcessor.class, ProgramsProcessor.class),
    VMBPIRM(0x2A, false, InputProcessor.class, ProgramsProcessor.class),
    VMBPIRO(0x2C, false, InputProcessor.class, TemperatureProcessor.class, ProgramsProcessor.class),
    VMBPIRC(0x2B, false, InputProcessor.class, ProgramsProcessor.class),
    VMBMETEO(0x31, false, InputProcessor.class, TemperatureProcessor.class, AnalogInputProcessor.class, ProgramsProcessor.class),
    VMB4AN(0x32, false, InputProcessor.class, ProgramsProcessor.class, AnalogInputProcessor.class, AnalogOutputProcessor.class),
    VMB1TS(0x0C, false, TemperatureProcessor.class, ThermostatProcessor.class);

    private static final Map<Class<? extends FeatureProcessor>, FeatureProcessor> processors = new HashMap<>();
    private final int code;
    private final boolean hasSubAddresses;
    private final Class<? extends FeatureProcessor>[] featureProcessors;

    @SafeVarargs
    VelbusDeviceType(int code, boolean hasSubAddresses, Class<? extends FeatureProcessor>... featureProcessors) {
        this.code = code;
        this.hasSubAddresses = hasSubAddresses;
        this.featureProcessors = featureProcessors;
    }

    public static VelbusDeviceType fromCode(int code) {
        for (VelbusDeviceType type : VelbusDeviceType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }

        return UNKNOWN;
    }

    public int getCode() {
        return this.code;
    }

    public boolean hasSubAddresses() {
        return hasSubAddresses;
    }

    public FeatureProcessor[] getFeatureProcessors() {
        if (featureProcessors == null) {
            return null;
        }

        return Arrays.stream(featureProcessors)
            .map(processorClazz ->
                processors
                    .computeIfAbsent(
                        processorClazz,
                        clazz -> {
                            try {
                                return clazz.getDeclaredConstructor().newInstance();
                            } catch (Exception e) {
                                LOG.log(Level.SEVERE, "Failed to instantiate feature processor: " + clazz.getSimpleName(), e);
                                return null;
                            }
                        }
                    )
            )
            .filter(Objects::nonNull)
            .toArray(FeatureProcessor[]::new);
    }
}
