package org.openremote.manager.energy;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.lfenergy.shapeshifter.api.FlexRequest;
import org.lfenergy.shapeshifter.api.USEFRoleType;
import org.lfenergy.shapeshifter.api.model.UftpParticipantInformation;
import org.lfenergy.shapeshifter.core.model.UftpParticipant;
import org.lfenergy.shapeshifter.core.service.UftpParticipantService;
import org.lfenergy.shapeshifter.core.service.handler.UftpPayloadHandler;
import org.lfenergy.shapeshifter.core.service.receiving.UftpReceivedMessageService;
import org.lfenergy.shapeshifter.core.service.validation.UftpValidationService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.energy.gopacs.GoPacsResource;
import org.openremote.manager.energy.gopacs.GoPacsResourceImpl;
import org.openremote.manager.energy.gopacs.PayloadHandler;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.manager.webhook.WebhookService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.impl.ElectricitySupplierAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.AssetQuery;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.web.WebTargetBuilder.createClient;

public class GoPacsService implements ContainerService {


    protected static class GoPacsHandler implements UftpParticipantService {
        String contractEan;
        ResteasyClient client;
        ResteasyWebTarget goPacsTarget;
        UftpValidationService uftpValidationService;
        UftpPayloadHandler uftpPayloadHandler;
        UftpReceivedMessageService uftpReceivedMessageService;
        GoPacsResource goPacsResource;

        Map<String, UftpParticipantInformation> participants = new HashMap<>();

        public GoPacsHandler(String contractEan) {
            this.contractEan = contractEan;
            this.client = createClient(org.openremote.container.Container.EXECUTOR);
            this.goPacsTarget = client.target("http://localhost:8080/gopacs");
            this.uftpValidationService = new UftpValidationService(new ArrayList<>());
            this.uftpPayloadHandler = new PayloadHandler(this::handleFlexMessage);
            this.uftpReceivedMessageService = new UftpReceivedMessageService(uftpValidationService, uftpPayloadHandler);
            this.goPacsResource = new GoPacsResourceImpl(this.uftpReceivedMessageService::process);
        }

//        ValidationResult validationResult = this.uftpReceivedMessageService.process(message);
//            if (validationResult.valid()) {
//            this.uftpPayloadHandler.notifyNewIncomingMessage(message);
//        } else {
//            LOG.log(Level.SEVERE, "Validation failed for message: " + validationResult.rejectionReason());
//        }


        public void handleFlexMessage(UftpParticipant participant, FlexRequest flexRequest) {

        }


        @Override
        public Optional<UftpParticipantInformation> getParticipantInformation(USEFRoleType usefRoleType, String domain) {
            if (participants.containsKey(domain)) {
                return Optional.of(participants.get(domain));
            } else {
                try (Response response = goPacsTarget
                        .path("participants")
                        .path(usefRoleType.name())
                        .queryParam("contractEan", contractEan)
                        .request()
                        .buildGet()
                        .invoke()) {
                    if (response != null && response.getStatus() == 200) {
                        List<UftpParticipantInformation> participants = response.readEntity(new GenericType<List<UftpParticipantInformation>>() {
                        });
                        for (UftpParticipantInformation participant : participants) {
                            this.participants.put(participant.domain(), participant);
                        }
                        return participants.stream().filter(p -> p.domain().equals(domain)).findFirst();
                    }
                } catch (Exception e) {
                    if (e.getCause() != null && e.getCause() instanceof IOException) {
                        LOG.log(Level.SEVERE, "Exception when requesting participant information", e.getCause());
                    } else {
                        LOG.log(Level.SEVERE, "Exception when requesting participant information", e);
                    }
                }
            }
            return Optional.empty();
        }
    }

    private static final Logger LOG = Logger.getLogger(GoPacsService.class.getName());

    protected WebhookService webhookService;
    protected ManagerWebService webService;
    protected AssetStorageService assetStorageService;

    protected final Map<String, GoPacsHandler> assetGoPacsHandlerMap = new HashMap<>();


    @Override
    public void init(Container container) throws Exception {
        webhookService = container.getService(WebhookService.class);
        webService = container.getService(ManagerWebService.class);
        assetStorageService = container.getService(AssetStorageService.class);
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.fine("Loading optimisation assets...");

        List<ElectricitySupplierAsset> electricitySupplierAssets = assetStorageService.findAll(
                        new AssetQuery()
                                .types(ElectricitySupplierAsset.class)
                )
                .stream()
                .map(asset -> (ElectricitySupplierAsset) asset)
                .filter(energyOptimisationAsset -> energyOptimisationAsset.getContractEan().isPresent())
                .toList();

        electricitySupplierAssets.forEach(this::enableGoPacsHandler);
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    protected synchronized void enableGoPacsHandler(ElectricitySupplierAsset electricitySupplierAsset) {
        electricitySupplierAsset.getAttribute(ElectricitySupplierAsset.CONTRACT_EAN).flatMap(Attribute::getValue).ifPresent(contractEan -> {
            GoPacsHandler goPacsHandler = new GoPacsHandler(contractEan);
            this.webService.addApiSingleton(goPacsHandler.goPacsResource);
            assetGoPacsHandlerMap.put(electricitySupplierAsset.getId(), goPacsHandler);
        });
    }
}
