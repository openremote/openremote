/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.server.asset;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.openremote.agent.protocol.Protocol;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.security.AuthContext;
import org.openremote.manager.server.agent.AgentService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.asset.AssetProcessingException;
import org.openremote.manager.shared.asset.AssetProcessingException.Reason;
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.value.Values;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getAgentLink;

public final class AssetRoute {

    public static final String HEADER_ASSET = AssetRoute.class.getName() + ".ASSET";
    public static final String HEADER_ATTRIBUTE = AssetRoute.class.getName() + ".ATTRIBUTE";

    public static Predicate isPersistenceEventForEntityType(Class<?> type) {
        return exchange -> {
            Class<?> entityType = exchange.getIn().getHeader(PersistenceEvent.HEADER_ENTITY_TYPE, Class.class);
            return type.isAssignableFrom(entityType);
        };
    }

    public static Predicate isPersistenceEventForAssetType(AssetType assetType) {
        return exchange -> {
            if (!(exchange.getIn().getBody() instanceof PersistenceEvent))
                return false;
            PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
            Asset asset = (Asset) persistenceEvent.getEntity();
            return asset.getWellKnownType() == assetType;
        };
    }

    public static Processor extractAttributeEventDetails(AssetStorageService assetStorageService) {
        return exchange -> {
            AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
            if (event.getEntityId() == null || event.getEntityId().isEmpty())
                return;
            if (event.getAttributeName() == null || event.getAttributeName().isEmpty())
                return;

            ServerAsset asset;
            if (exchange.getIn().getHeader(HEADER_ASSET) == null) {
                asset = assetStorageService.find(event.getEntityId(), true);
                if (asset == null)
                    return;
                exchange.getIn().setHeader(HEADER_ASSET, asset);
            } else {
                asset = exchange.getIn().getHeader(HEADER_ASSET, ServerAsset.class);
            }

            AssetAttribute attribute;
            if (exchange.getIn().getHeader(HEADER_ATTRIBUTE) == null) {
                attribute = asset.getAttribute(event.getAttributeName()).orElse(null);
                if (attribute == null)
                    return;
                exchange.getIn().setHeader(HEADER_ATTRIBUTE, attribute);
            }
        };
    }

    public static Processor validateAttributeEvent() {
        return exchange -> {

            AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);

            Source source = exchange.getIn().getHeader(AttributeEvent.HEADER_SOURCE, () -> null, Source.class);
            if (source == null) {
                throw new AssetProcessingException(Reason.MISSING_SOURCE);
            }

            ServerAsset asset = exchange.getIn().getHeader(HEADER_ASSET, ServerAsset.class);
            if (asset == null) {
                throw new AssetProcessingException(Reason.ASSET_NOT_FOUND);
            }

            AssetAttribute attribute = exchange.getIn().getHeader(HEADER_ATTRIBUTE, AssetAttribute.class);
            if (attribute == null) {
                throw new AssetProcessingException(Reason.ATTRIBUTE_NOT_FOUND);
            }

            if (asset.getWellKnownType() == AssetType.AGENT) {
                throw new AssetProcessingException(Reason.ILLEGAL_AGENT_UPDATE);
            }

            // For executable attributes, non-sensor sources can set a writable attribute execute status
            if (attribute.isExecutable() && source != Source.SENSOR) {
                Optional<AttributeExecuteStatus> status = event.getValue()
                    .flatMap(Values::getString)
                    .flatMap(AttributeExecuteStatus::fromString);

                if (!status.isPresent() && !status.get().isWrite()) {
                    throw new AssetProcessingException(Reason.INVALID_ATTRIBUTE_EXECUTE_STATUS);
                }
            }
        };
    }

    public static Processor validateAttributeEventFromClient(AssetStorageService assetStorageService,
                                                             ManagerIdentityService identityService) {
        return exchange -> {

            AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);
            if (authContext == null) {
                throw new AssetProcessingException(Reason.NO_AUTH_CONTEXT);
            }

            AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
            ServerAsset asset = exchange.getIn().getHeader(HEADER_ASSET, ServerAsset.class);
            AssetAttribute attribute = exchange.getIn().getHeader(HEADER_ATTRIBUTE, AssetAttribute.class);
            Source source = exchange.getIn().getHeader(AttributeEvent.HEADER_SOURCE, () -> null, Source.class);

            if (source != Source.CLIENT) {
                throw new AssetProcessingException(Reason.ILLEGAL_SOURCE);
            }

            // Check realm, must be accessible
            if (!identityService.isTenantActiveAndAccessible(authContext, asset)) {
                throw new AssetProcessingException(Reason.INSUFFICIENT_ACCESS);
            }

            // Check read-only
            if (attribute.isReadOnly() && !authContext.isSuperUser()) {
                throw new AssetProcessingException(Reason.INSUFFICIENT_ACCESS);
            }

            // Regular user must have write assets role
            if (!authContext.hasResourceRoleOrIsSuperUser(ClientRole.WRITE_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                throw new AssetProcessingException(Reason.INSUFFICIENT_ACCESS);
            }

            // Check restricted user
            if (identityService.isRestrictedUser(authContext.getUserId())) {
                // Must be asset linked to user
                if (!assetStorageService.isUserAsset(authContext.getUserId(), event.getEntityId())) {
                    throw new AssetProcessingException(Reason.INSUFFICIENT_ACCESS);
                }
                // Must be protected attributes
                if (!attribute.isProtected()) {
                    throw new AssetProcessingException(Reason.INSUFFICIENT_ACCESS);
                }
            }
        };
    }

    public static Processor validateAttributeEventFromSensor(AgentService agentService) {
        return exchange -> {

            AssetAttribute attribute = exchange.getIn().getHeader(HEADER_ATTRIBUTE, AssetAttribute.class);
            Source source = exchange.getIn().getHeader(AttributeEvent.HEADER_SOURCE, () -> null, Source.class);

            if (source != Source.SENSOR) {
                throw new AssetProcessingException(Reason.ILLEGAL_SOURCE);
            }

            Optional<AssetAttribute> protocolConfiguration =
                getAgentLink(attribute).flatMap(agentService::getProtocolConfiguration);

            if (!protocolConfiguration.isPresent()) {
                throw new AssetProcessingException(Reason.INVALID_AGENT_LINK);
            }

        };
    }

    public static Processor handleAssetProcessingException(Logger logger) {
        return exchange -> {
            AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
            Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);

            StringBuilder error = new StringBuilder();

            Source source = exchange.getIn().getHeader(AttributeEvent.HEADER_SOURCE, "unknown source", Source.class);
            if (source != null) {
                error.append("Error processing from ").append(source);
            }

            String protocolName = exchange.getIn().getHeader(Protocol.SENSOR_QUEUE_SOURCE_PROTOCOL, String.class);
            if (protocolName != null) {
                error.append(" (protocol: ").append(protocolName).append(")");
            }

            if (exception instanceof AssetProcessingException) {
                AssetProcessingException processingException = (AssetProcessingException) exception;
                error.append(" - ").append(processingException.getReason());
                error.append(": ").append(event.toString());
                logger.warning(error.toString());
            } else {
                error.append(": ").append(event.toString());
                logger.log(Level.WARNING, error.toString(), exception);
            }

            // Make the exception available if MEP is InOut
            exchange.getOut().setBody(exception);
        };
    }
}
