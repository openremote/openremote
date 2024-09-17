/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol.simulator.storage;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.impl.ElectricityStorageAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.impl.ElectricityStorageAsset.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class StorageSimulatorProtocol extends AbstractProtocol<StorageSimulatorAgent, StorageSimulatorAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "StorageSimulator";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, StorageSimulatorProtocol.class);
    protected final Map<String, Instant> lastUpdateMap = new HashMap<>();
    protected final Map<String, ScheduledFuture<?>> simulationMap = new HashMap<>();
    protected final ReentrantLock lock = new ReentrantLock();

    public StorageSimulatorProtocol(StorageSimulatorAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "storagesimulator://" + agent.getId();

    }

    @Override
    protected void doStart(Container container) throws Exception {
        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        lock.lock();
        try {
            new HashSet<>(simulationMap.keySet()).forEach(assetId -> stopSimulation(assetId));
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, StorageSimulatorAgentLink agentLink) throws RuntimeException {
        lock.lock();
        try {
            if (checkLinkedAttributes(assetId) && !isSimulationStarted(assetId)) {
                startSimulation(assetId);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, StorageSimulatorAgentLink agentLink) {
        lock.lock();
        try {
            if (!checkLinkedAttributes(assetId) && isSimulationStarted(assetId)) {
                stopSimulation(assetId);
                LOG.info("Stopped storage simulation for asset id: " + assetId);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doLinkedAttributeWrite(StorageSimulatorAgentLink agentLink, AttributeEvent event, Object processedValue) {

        // Power attribute is updated only by this protocol not by clients
        if (event.getName().equals(POWER.getName())) {
            return;
        }

        updateLinkedAttribute(event.getRef(), processedValue);
    }

    protected void updateStorageAsset(String assetId) {

        Asset asset = assetService.findAsset(assetId);
        ElectricityStorageAsset storageAsset = asset instanceof ElectricityStorageAsset ? (ElectricityStorageAsset)asset : null;
        if (storageAsset == null) {
            LOG.finest("Storage asset not set so skipping update");
            return;
        }

        Instant now = Instant.now();
        Instant previousTimestamp;
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            previousTimestamp = lastUpdateMap.put(assetId, now);
        } finally {
            lock.unlock();
        }

        double setpoint = storageAsset.getPowerSetpoint().orElse(0d);
        double capacity = storageAsset.getEnergyCapacity().orElse(0d);
        double power = storageAsset.getPower().orElse(0d);
        double level = storageAsset.getEnergyLevel().orElse(0d);
        int minPercentage = storageAsset.getEnergyLevelPercentageMin().orElse(0);
        int maxPercentage = storageAsset.getEnergyLevelPercentageMax().orElse(100);
        capacity = Math.max(0d, capacity);
        level = Math.max(0d, Math.min(capacity, level));
        double maxLevel = (((double) maxPercentage) / 100d) * capacity;
        double minLevel = (((double) minPercentage) / 100d) * capacity;

        if (capacity <= 0d) {
            LOG.info("Storage asset capacity is 0 so not usable: " + assetId);
            level = 0d;
            setpoint = 0d;
        }

        if (capacity > 0 && power != 0d && previousTimestamp != null) {

            // Calculate energy delta since last execution
            Duration duration = Duration.between(previousTimestamp, now);
            long seconds = duration.getSeconds();

            if (seconds > 0) {

                double deltaHours = seconds / 3600d;
                double efficiency = power > 0 ? ((double) storageAsset.getEfficiencyImport().orElse(100)) / 100d : (1d / (((double) storageAsset.getEfficiencyExport().orElse(100)) / 100d)); // Export efficiency < 1 means more energy is consumed to produce requested power
                double energyDelta = power * deltaHours * efficiency;
                double newLevel = Math.max(0d, Math.min(capacity, level + energyDelta));
                energyDelta = newLevel - level;
                level = newLevel;

                if (energyDelta > 0) {
                    updateLinkedAttribute(new AttributeRef(storageAsset.getId(), ElectricityStorageAsset.ENERGY_IMPORT_TOTAL.getName()), storageAsset.getEnergyImportTotal().orElse(0d) + energyDelta);
                } else {
                    updateLinkedAttribute(new AttributeRef(storageAsset.getId(), ElectricityStorageAsset.ENERGY_EXPORT_TOTAL.getName()), storageAsset.getEnergyExportTotal().orElse(0d) - energyDelta);
                }
            }
        }

        power = (setpoint < 0 && level <= minLevel) || (setpoint > 0 && level >= maxLevel) ? 0d : setpoint;

        if (power > 0d && !storageAsset.isSupportsImport().orElse(false)) {
            LOG.fine("Setpoint is requesting power import but asset does not support it: " + storageAsset);
            power = 0d;
        } else if (power < 0d && !storageAsset.isSupportsExport().orElse(false)) {
            LOG.fine("Setpoint is requesting power export but asset does not support it: " + storageAsset);
            power = 0d;
        }

        updateLinkedAttribute(new AttributeRef(assetId, POWER.getName()), power);
        updateLinkedAttribute(new AttributeRef(assetId, ENERGY_LEVEL.getName()), level);
        updateLinkedAttribute(new AttributeRef(assetId, ENERGY_LEVEL_PERCENTAGE.getName()), capacity <= 0d ? 0 : (int) ((level / capacity) * 100));
    }

    protected ScheduledFuture<?> scheduleUpdate(String assetId) {
        return executorService.scheduleAtFixedRate(() -> {
            try {
                updateStorageAsset(assetId);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Exception in " + getProtocolName(), e);
                setConnectionStatus(ConnectionStatus.ERROR);
            }

        }, 0, 1, TimeUnit.MINUTES);
    }

    protected boolean checkLinkedAttributes(String assetId) {
        return getLinkedAttributes().containsKey(new AttributeRef(assetId, POWER_SETPOINT.getName())) &&
               getLinkedAttributes().containsKey(new AttributeRef(assetId, POWER.getName())) &&
               getLinkedAttributes().containsKey(new AttributeRef(assetId, ENERGY_LEVEL.getName()))&&
               getLinkedAttributes().containsKey(new AttributeRef(assetId, ENERGY_LEVEL_PERCENTAGE.getName()));
    }

    protected boolean isSimulationStarted(String assetId) {
        return simulationMap.containsKey(assetId);
    }

    protected void startSimulation(String assetId) {
        LOG.info("Started storage simulation for asset id: " + assetId);
        simulationMap.put(assetId, scheduleUpdate(assetId));
    }

    protected void stopSimulation(String assetId) {
        if (isSimulationStarted(assetId)) {
            lastUpdateMap.remove(assetId);
            ScheduledFuture<?> scheduledFuture = simulationMap.remove(assetId);
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
        }
    }
}
