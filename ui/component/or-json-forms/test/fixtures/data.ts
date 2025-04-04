export const translations = {
  en: {
    test: {
      schema: {
        item: {
          ioTDeviceConfiguration: {
            root: {
              label: "IoT Device Configuration",
              description: "Configuration settings for a list of IoT devices",
            },
            label: "Device",
            description: "An individual IoT device",
            deviceId: {
              label: "Device ID",
              description: "A unique identifier for the device",
            },
            deviceName: {
              label: "Device Name",
              description: "The name given to the device",
            },
            online: {
              label: "Online Status",
              description: "Indicates whether the device is currently online",
            },
            sensors: {
              label: "Sensors",
              description: "A list of sensors attached to the device",
              sensor: {
                label: "Sensor",
                description: "An individual sensor within the device",
                type: {
                  label: "Sensor Type",
                  description: "The kind of measurement the sensor performs",
                },
                unit: {
                  label: "Measurement Unit",
                  description: "The unit of measurement used by the sensor",
                },
                enabled: {
                  label: "Enabled",
                  description: "Indicates whether the sensor is active",
                },
              },
            },
          },
        },
      },
    },
  },
  nl: {
    test: {
      schema: {
        item: {
          ioTDeviceConfiguration: {
            root: {
              label: "IoT-apparaatconfiguratie",
              description: "Configuratie-instellingen voor een lijst met IoT-apparaten",
            },
            label: "Apparaat",
            description: "Een individueel IoT-apparaat",
            deviceId: {
              label: "Apparaat-ID",
              description: "Een unieke identificatiecode voor het apparaat",
            },
            deviceName: {
              label: "Apparaatnaam",
              description: "De naam die aan het apparaat is gegeven",
            },
            online: {
              label: "Online status",
              description: "Geeft aan of het apparaat momenteel online is",
            },
            sensors: {
              label: "Sensoren",
              description: "Een lijst met sensoren die aan het apparaat zijn gekoppeld",
              sensor: {
                label: "Sensor",
                description: "Een individuele sensor binnen het apparaat",
                type: {
                  label: "Sensortype",
                  description: "Het soort meting dat de sensor uitvoert",
                },
                unit: {
                  label: "Meeteenheid",
                  description: "De meeteenheid die door de sensor wordt gebruikt",
                },
                enabled: {
                  label: "Ingeschakeld",
                  description: "Geeft aan of de sensor actief is",
                },
              },
            },
          },
        },
      },
    },
  },
};
