package org.openremote.extension.energy

import static org.openremote.model.value.MetaItemType.*

import org.openremote.extension.energy.model.ElectricityAsset
import org.openremote.extension.energy.model.ElectricityBatteryAsset
import org.openremote.extension.energy.model.ElectricityConsumerAsset
import org.openremote.extension.energy.model.ElectricityProducerSolarAsset
import org.openremote.extension.energy.model.ElectricityProducerWindAsset
import org.openremote.extension.energy.model.ElectricitySupplierAsset
import org.openremote.extension.energy.model.EnergyOptimisationAsset
import org.openremote.manager.setup.ManagerSetup
import org.openremote.model.Container
import org.openremote.model.attribute.MetaItem
import org.openremote.model.geo.GeoJSONPoint

class ManagerTestSetup extends ManagerSetup {

    String realmEnergyName
    String electricityOptimisationAssetId
    String electricityConsumerAssetId
    String electricitySolarAssetId
    String electricityWindAssetId
    String electricitySupplierAssetId
    String electricityBatteryAssetId

    ManagerTestSetup(Container container) {
        super(container)
    }

    @Override
    void onStart() throws Exception {
        super.onStart()

        KeycloakTestSetup keycloakTestSetup = setupService.getTaskOfType(KeycloakTestSetup.class)
        realmEnergyName = keycloakTestSetup.realmEnergy.getName()

        EnergyOptimisationAsset electricityOptimisationAsset = new EnergyOptimisationAsset("Optimisation")
        electricityOptimisationAsset.setIntervalSize(3d)
        electricityOptimisationAsset.setRealm(keycloakTestSetup.realmEnergy.getName())
        electricityOptimisationAsset.setFinancialWeighting(100)
        electricityOptimisationAsset = assetStorageService.merge(electricityOptimisationAsset)
        electricityOptimisationAssetId = electricityOptimisationAsset.getId()

        ElectricityConsumerAsset electricityConsumerAsset = new ElectricityConsumerAsset("Consumer")
        electricityConsumerAsset.setParent(electricityOptimisationAsset)
        electricityConsumerAsset.getAttribute(ElectricityAsset.POWER).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        )
        electricityConsumerAsset = assetStorageService.merge(electricityConsumerAsset)
        electricityConsumerAssetId = electricityConsumerAsset.getId()

        ElectricityProducerSolarAsset electricitySolarAsset = new ElectricityProducerSolarAsset("Producer")
        electricitySolarAsset.setParent(electricityOptimisationAsset)
        electricitySolarAsset.getAttribute(ElectricityAsset.POWER).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        )
        electricitySolarAsset.setPanelOrientation(ElectricityProducerSolarAsset.PanelOrientation.SOUTH)
        electricitySolarAsset.setPanelAzimuth(0)
        electricitySolarAsset.setPanelPitch(30)
        electricitySolarAsset.setEfficiencyExport(100)
        electricitySolarAsset.setPowerExportMax(2.5)
        electricitySolarAsset.setLocation(new GeoJSONPoint(9.195285, 48.787418))
        electricitySolarAsset.setSetActualSolarValueWithForecast(true)
        electricitySolarAsset.setIncludeForecastSolarService(true)
        electricitySolarAsset = assetStorageService.merge(electricitySolarAsset)
        electricitySolarAssetId = electricitySolarAsset.getId()

        ElectricityProducerWindAsset electricityWindAsset = new ElectricityProducerWindAsset("Wind Turbine")
        electricityWindAsset.setParent(electricityOptimisationAsset)
        electricityWindAsset.getAttribute(ElectricityAsset.POWER).ifPresent(attr ->
                attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        )
        electricityWindAsset.setWindSpeedMax(18d)
        electricityWindAsset.setWindSpeedMin(2d)
        electricityWindAsset.setWindSpeedReference(12d)
        electricityWindAsset.setPowerExportMax(9000d)
        electricityWindAsset.setEfficiencyExport(100)
        electricityWindAsset.setPowerExportMax(2.5)
        electricityWindAsset.setLocation(new GeoJSONPoint(9.195285, 48.787418))
        electricityWindAsset.setSetActualWindValueWithForecast(true)
        electricityWindAsset.setIncludeForecastWindService(true)
        electricityWindAsset = assetStorageService.merge(electricityWindAsset)
        electricityWindAssetId = electricityWindAsset.getId()

        ElectricityBatteryAsset electricityBatteryAsset = new ElectricityBatteryAsset("Battery")
        electricityBatteryAsset.setParent(electricityOptimisationAsset)
        electricityBatteryAsset.setEnergyCapacity(200d)
        electricityBatteryAsset.setEnergyLevelPercentageMin(20)
        electricityBatteryAsset.setEnergyLevelPercentageMax(80)
        electricityBatteryAsset.setEnergyLevel(100d)
        electricityBatteryAsset.setPowerImportMax(7d)
        electricityBatteryAsset.setPowerExportMax(20d)
        electricityBatteryAsset.setPowerSetpoint(0d)
        electricityBatteryAsset.setEfficiencyImport(95)
        electricityBatteryAsset.setEfficiencyExport(98)
        electricityBatteryAsset.setSupportsExport(true)
        electricityBatteryAsset.setSupportsImport(true)
        electricityBatteryAsset.getAttribute(ElectricityAsset.POWER_SETPOINT).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        )
        electricityBatteryAsset = assetStorageService.merge(electricityBatteryAsset)
        electricityBatteryAssetId = electricityBatteryAsset.getId()

        ElectricitySupplierAsset electricitySupplierAsset = new ElectricitySupplierAsset("Supplier")
        electricitySupplierAsset.setParent(electricityOptimisationAsset)
        electricitySupplierAsset.setTariffExport(-0.05)
        electricitySupplierAsset.setTariffImport(0.08)
        electricitySupplierAsset.getAttribute(ElectricityAsset.TARIFF_IMPORT).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        )
        electricitySupplierAsset.getAttribute(ElectricityAsset.TARIFF_EXPORT).ifPresent(attr ->
            attr.addMeta(new MetaItem<>(HAS_PREDICTED_DATA_POINTS))
        )
        electricitySupplierAsset = assetStorageService.merge(electricitySupplierAsset)
        electricitySupplierAssetId = electricitySupplierAsset.getId()
    }
}
