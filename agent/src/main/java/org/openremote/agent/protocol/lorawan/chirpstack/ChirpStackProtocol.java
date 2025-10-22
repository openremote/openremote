/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.lorawan.chirpstack;

import io.chirpstack.api.DeviceListItem;
import io.chirpstack.api.DeviceProfile;
import io.chirpstack.api.DeviceProfileServiceGrpc;
import io.chirpstack.api.DeviceServiceGrpc;
import io.chirpstack.api.GetDeviceProfileRequest;
import io.chirpstack.api.GetDeviceProfileResponse;
import io.chirpstack.api.ListDevicesRequest;
import io.chirpstack.api.ListDevicesResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.openremote.agent.protocol.lorawan.AbstractLoRaWANProtocol;
import org.openremote.agent.protocol.lorawan.CsvRecord;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.filter.NumberPredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.JsonPathFilter;
import org.openremote.model.value.ValueFilter;

import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.agent.protocol.lorawan.LoRaWANAgent.API_KEY;
import static org.openremote.agent.protocol.lorawan.LoRaWANConstants.ASSET_TYPE_TAG;
import static org.openremote.model.asset.agent.Agent.HOST;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class ChirpStackProtocol extends AbstractLoRaWANProtocol<ChirpStackProtocol, ChirpStackAgent> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ChirpStackProtocol.class);

    public static final String PROTOCOL_DISPLAY_NAME = "ChirpStack";
    public static final String CHIRPSTACK_ASSET_TYPE_TAG = ASSET_TYPE_TAG;
    public static final int GRPC_LIST_DEVICE_LIMIT = 10000;
    public static final int GRPC_TIMEOUT = 10;

    public ChirpStackProtocol(ChirpStackAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "chirpstack://" + getAgent().getHost().orElse("-") + ":"
                               + getAgent().getPort().map(p -> p.toString()).orElse("-");
    }

    @Override
    protected boolean checkAutoDiscoveryPrerequisites() {
        boolean isOk = super.checkAutoDiscoveryPrerequisites();

        List<AbstractMap.SimpleEntry<AttributeDescriptor, Optional<String>>> list = new ArrayList<>(3);
        if (getAgent().getHost().isEmpty()) {
            list.add(new AbstractMap.SimpleEntry<>(HOST, getAgent().getHost()));
        }
        //list.add(new AbstractMap.SimpleEntry<>(PORT, getAgent().getPort().map(p -> p.toString())));
        list.add(new AbstractMap.SimpleEntry<>(API_KEY, getAgent().getApiKey()));

        for (AbstractMap.SimpleEntry<AttributeDescriptor, Optional<String>> item : list) {
            if (!item.getValue().map(attrValue -> !attrValue.trim().isEmpty()).orElse(false)) {
                isOk = false;
                LOG.log(Level.WARNING, "Auto discovery failed because agent attribute '" + item.getKey().getName() + "'  is missing");
            }
        }

        return isOk;
    }

    @Override
    protected AssetTreeNode[]  discoverDevices() {
        Optional<String> host = getAgent().getHost().map(h -> h.trim());
        Optional<Integer> port = getAgent().getPort();
        Optional<String> apiKey = getAgent().getApiKey().map(key -> key.trim());
        Optional<String> applicationId = getAgent().getApplicationId().map(key -> key.trim());
        Boolean isSecureGRPC = getAgent().getSecureGRPC().orElse(false);

        if (host.isEmpty() || apiKey.isEmpty() || applicationId.isEmpty()) {
            return new AssetTreeNode[0];
        }

        ManagedChannel channel = createChannel(host.get(), port.orElse(80), isSecureGRPC);

        Metadata headers = new Metadata();
        Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(AUTHORIZATION_HEADER, "Bearer " + apiKey.get());

        try {
            List<DeviceListItem> deviceList = requestDeviceList(applicationId.get(), channel, headers);

            Map<String, DeviceProfile> profileMap = deviceList.stream()
                .map(DeviceListItem::getDeviceProfileId)
                .filter(id -> !isNullOrEmpty(id))
                .distinct()
                .map(id -> Map.entry(id, requestDeviceProfile(id, channel, headers)))
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().get()
                ));

            AssetTreeNode[] assetTreeNodes = deviceList.stream()
                .filter(deviceListItem -> !isNullOrEmpty(deviceListItem.getDevEui()))
                .filter(deviceListItem -> duplicateAssetCheck(deviceListItem.getDevEui()))
                .filter(deviceListItem -> !isNullOrEmpty(deviceListItem.getDeviceProfileId()))
                .filter(deviceListItem -> profileMap.containsKey(deviceListItem.getDeviceProfileId()))
                .filter(deviceListItem -> profileMap.get(deviceListItem.getDeviceProfileId()).getTagsMap().containsKey(CHIRPSTACK_ASSET_TYPE_TAG))
                .filter(deviceListItem -> !isNullOrEmpty(profileMap.get(deviceListItem.getDeviceProfileId()).getTagsMap().get(CHIRPSTACK_ASSET_TYPE_TAG)))
                .map(deviceListItem -> createAsset(
                    deviceListItem.getDevEui(),
                    profileMap.get(deviceListItem.getDeviceProfileId()).getTagsMap().get(CHIRPSTACK_ASSET_TYPE_TAG),
                    deviceListItem,
                    profileMap.get(deviceListItem.getDeviceProfileId())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(AssetTreeNode::new)
                .toArray(AssetTreeNode[]::new);

            return assetTreeNodes;
        } catch (StatusRuntimeException e) {
            LOG.log(Level.WARNING, "Auto discovery failed because of a gRPC connection error: {code=" + e.getStatus().getCode() + ", description=" + e.getStatus().getDescription() + "}", e);
            return new AssetTreeNode[0];
        } finally {
            if (channel != null) {
                channel.shutdown();
                try {
                    if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                        channel.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    channel.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    protected List<String> createWildcardSubscriptionTopicList() {
        return getAgent().getApplicationId()
            .map(applicationId -> Collections.singletonList("application/" + applicationId + "/device/+/event/up"))
            .orElse(new ArrayList<>());
    }

    @Override
    protected boolean configureMQTTSubscriptionTopic(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        Optional<String> applicationId = getAgent().getApplicationId().map(id -> id.trim());
        Optional<String> devEUI = Optional.ofNullable(csvRecord.getDevEUI()).map(eui -> eui.trim());

        if (applicationId.isPresent() && devEUI.isPresent()) {
            agentLink.setSubscriptionTopic("application/" + applicationId.get() + "/device/" + devEUI.get().toLowerCase() + "/event/up");
        }
        return true;
    }

    @Override
    protected boolean configureMQTTPublishTopic(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        Optional<String> applicationId = getAgent().getApplicationId().map(id -> id.trim());
        Optional<String> devEUI = Optional.ofNullable(csvRecord.getDevEUI()).map(eui -> eui.trim());

        if (applicationId.isPresent() && devEUI.isPresent()) {
            agentLink.setPublishTopic("application/" + applicationId.get() + "/device/" + devEUI.get().toLowerCase() + "/command/down");
        }
        return true;
    }

    @Override
    protected boolean configureMQTTMessageMatchFilterAndPredicate(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }
        getAgentConfigUplinkPort(attribute).ifPresent(port ->
            agentLink.setMessageMatchFilters(new ValueFilter[] {new JsonPathFilter("$.fPort", true, false)})
                     .setMessageMatchPredicate(new NumberPredicate(port))
        );
        return true;
    }

    @Override
    protected boolean configureMQTTWriteValueTemplate(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        Optional<Integer> downlinkPort = getAgentConfigDownlinkPort(attribute);
        Optional<String> devEUI = Optional.ofNullable(csvRecord.getDevEUI()).map(eui -> eui.trim());
        Optional<String> objectTemplate = getAgentConfigWriteObjectValueTemplate(attribute);

        if (downlinkPort.isPresent() && devEUI.isPresent()) {
            String writeValue = "{" +
                "\n  \"devEui\": \"" + devEUI.get() + "\"," +
                "\n  \"confirmed\": true,";
            if (objectTemplate.isPresent()) {
                writeValue += "\n  \"object\": {\n    " + objectTemplate.get() + "\n  },";
            } else {
                writeValue += "\n  \"data\": \"%VALUE%\",";
            }
            writeValue += "\n  \"fPort\": " + downlinkPort.get() + "\n}";

            agentLink.setWriteValue(writeValue);
        }
        return true;
    }

    private ManagedChannel createChannel(String host, int port, boolean isSecure) {
        ManagedChannel channel = null;

        if (isSecure) {
            channel = ManagedChannelBuilder.forAddress(host, port)
                .useTransportSecurity()
                .build();
        } else {
            channel =ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        }

        return channel;
    }

    private Optional<Asset<?>> createAsset(String devEUI, String assetTypeName, DeviceListItem deviceItem, DeviceProfile deviceProfile) {
        if (isNullOrEmpty(devEUI) || isNullOrEmpty(assetTypeName) || deviceItem == null || deviceProfile == null) {
            return Optional.empty();
        }

        CsvRecord record = new CsvRecord();
        record.setDevEUI(devEUI);
        record.setAssetTypeName(assetTypeName);
        Optional.ofNullable(deviceItem.getName())
            .filter(name -> !name.isEmpty())
            .ifPresent(record::setName);

        return Optional.ofNullable(assetTypeName)
            .flatMap(name -> resolveAssetClass(name, deviceItem, deviceProfile))
            .map(clazz -> instantiateAsset(clazz, deviceItem, deviceProfile))
            .flatMap(asset -> configureAsset(asset, record));
    }

    private Optional<Class<? extends Asset<?>>> resolveAssetClass(String simpleClassName, DeviceListItem deviceListItem, DeviceProfile deviceProfile) {
        if (isNullOrEmpty(simpleClassName) || deviceListItem == null || deviceProfile == null ) {
            return null;
        }

        return ValueUtil.getAssetClass(simpleClassName)
            .or(() -> {
                LOG.log(Level.WARNING, "Device auto discovery skipped device because of unknown asset type: '" + simpleClassName + "', " + chirpStackDeviceToString(deviceListItem, deviceProfile));
                return Optional.empty();
            });
    }

    private String createDeviceName(DeviceListItem deviceListItem) {
        return Optional.ofNullable(deviceListItem)
            .map(d -> Optional.ofNullable(d.getName())
                .filter(name -> !isNullOrEmpty(name))
                .orElseGet(() -> Optional.ofNullable(d.getDevEui())
                    .filter(devEui -> !isNullOrEmpty(devEui))
                    .orElse("")))
            .orElse("");
    }

    private Asset<?> instantiateAsset(Class<?> clazz, DeviceListItem deviceListItem, DeviceProfile deviceProfile) {
        if (clazz == null || deviceListItem == null || deviceProfile == null) {
            return null;
        }

        Asset<?> asset = null;
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class);
            asset = (Asset<?>) constructor.newInstance(createDeviceName(deviceListItem));
        } catch (ReflectiveOperationException e) {
            LOG.log(Level.INFO, "Auto discovery failed to create asset: " + chirpStackDeviceToString(deviceListItem, deviceProfile), e);
        }
        return asset;
    }

    private List<DeviceListItem> requestDeviceList(String applicationId, ManagedChannel channel, Metadata headers) {
        if (isNullOrEmpty(applicationId)) {
            return new ArrayList<>();
        }

        DeviceServiceGrpc.DeviceServiceBlockingStub deviceStub = DeviceServiceGrpc.newBlockingStub(channel);
        deviceStub = deviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));

        int offset = 0;
        List<DeviceListItem> allDevices = new ArrayList<>();

        while (true) {
            ListDevicesRequest request = ListDevicesRequest.newBuilder()
                .setApplicationId(applicationId)
                .setLimit(GRPC_LIST_DEVICE_LIMIT)
                .setOffset(offset)
                .build();
            ListDevicesResponse response = deviceStub
                .withDeadlineAfter(GRPC_TIMEOUT, TimeUnit.SECONDS)
                .list(request);

            allDevices.addAll(response.getResultList());

            if (response.getResultList().size() < GRPC_LIST_DEVICE_LIMIT) {
                break;
            }

            offset += GRPC_LIST_DEVICE_LIMIT;
        }

        return allDevices;
    }

    private Optional<DeviceProfile> requestDeviceProfile(String profileId, ManagedChannel channel, Metadata headers) {
        if (isNullOrEmpty(profileId)) {
            return Optional.empty();
        }

        DeviceProfileServiceGrpc.DeviceProfileServiceBlockingStub profileStub = DeviceProfileServiceGrpc.newBlockingStub(channel);
        profileStub = profileStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));

        GetDeviceProfileRequest request = GetDeviceProfileRequest.newBuilder()
            .setId(profileId)
            .build();

        DeviceProfile deviceProfile = null;
        try {
            GetDeviceProfileResponse response = profileStub
                .withDeadlineAfter(GRPC_TIMEOUT, TimeUnit.SECONDS)
                .get(request);
            deviceProfile = response.getDeviceProfile();
        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode() != Status.Code.NOT_FOUND) {
                throw e;
            }
        }

        return Optional.ofNullable(deviceProfile);
    }

    private String chirpStackDeviceToString(DeviceListItem deviceListItem, DeviceProfile deviceProfile) {
        return Optional.ofNullable(deviceListItem)
            .map(d ->
                "ChirpStack device{" +
                "devEUI='" + (d.getDevEui() != null ? d.getDevEui() : "") + '\'' +
                ", name='" + (d.getName() != null ? d.getName() : "") + '\'' +
                ", profile=" + chirpStackProfileToString(deviceProfile) +
                "}"
            )
            .orElse("");
    }

    private String chirpStackProfileToString(DeviceProfile deviceProfile) {
        return Optional.ofNullable(deviceProfile)
            .map(dp ->
                "{" +
                "id='" + (dp.getId() != null ? dp.getId() : "") + '\'' +
                ", name='" + (dp.getName() != null ? dp.getName() : "") + '\'' +
                ", tag["+ CHIRPSTACK_ASSET_TYPE_TAG + "] ='" + Optional.ofNullable(deviceProfile.getTagsMap()).map(tagsMap -> tagsMap.get(CHIRPSTACK_ASSET_TYPE_TAG)).orElse("") + '\'' +
                "}"
            )
            .orElse("");
    }
}
