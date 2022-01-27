/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.manager.asset;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

// TODO: Implement model client event support
/**
 * A service for abstracting {@link org.openremote.model.util.ValueUtil} and handling local model requests vs
 * {@link org.openremote.model.asset.impl.GatewayAsset} model requests. It also manages the {@link
 * org.openremote.model.asset.AssetModelResource} and provides support for model requests via the client event bus.
 */
public class AssetModelService extends RouteBuilder implements ContainerService {

    protected ManagerIdentityService identityService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;

    @Override
    public void configure() throws Exception {
//        // React if a client wants to read assets and attributes
//        from(CLIENT_EVENT_TOPIC)
//            .routeId("FromClientReadRequests")
//            .filter(
//                or(body().isInstanceOf(ReadAssetsEvent.class), body().isInstanceOf(ReadAssetEvent.class), body().isInstanceOf(ReadAttributeEvent.class)))
//            .choice()
//            .when(body().isInstanceOf(ReadAssetEvent.class))
//            .end();
    }

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {

        identityService = container.getService(ManagerIdentityService.class);
        clientEventService = container.getService(ClientEventService.class);
        gatewayService = container.getService(GatewayService.class);

        container.getService(ManagerWebService.class).addApiSingleton(
            new AssetModelResourceImpl(
                container.getService(TimerService.class),
                identityService,
                this
            )
        );

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public AssetTypeInfo[] getAssetInfos(String parentId, String parentType) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return new AssetTypeInfo[0];
        }

        return ValueUtil.getAssetInfos(parentType);
    }

    public AssetTypeInfo getAssetInfo(String parentId, String assetType) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return null;
        }

        return ValueUtil.getAssetInfo(assetType).orElse(null);
    }

    public AssetDescriptor<?>[] getAssetDescriptors(String parentId, String parentType) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return new AssetDescriptor[0];
        }

        return ValueUtil.getAssetDescriptors(parentType);
    }

    public ValueDescriptor<?>[] getValueDescriptors(String parentId) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return new ValueDescriptor<?>[0];
        }

        return ValueUtil.getValueDescriptors();
    }

    public MetaItemDescriptor<?>[] getMetaItemDescriptors(String parentId) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return new MetaItemDescriptor<?>[0];
        }

        return ValueUtil.getMetaItemDescriptors();
    }
}
