package org.openremote.manager.energy;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.HttpString;
import jakarta.servlet.ServletException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.lfenergy.shapeshifter.api.*;
import org.lfenergy.shapeshifter.api.model.UftpParticipantInformation;
import org.lfenergy.shapeshifter.core.common.xml.XmlSerializer;
import org.lfenergy.shapeshifter.core.common.xsd.XsdFactory;
import org.lfenergy.shapeshifter.core.common.xsd.XsdSchemaFactoryPool;
import org.lfenergy.shapeshifter.core.common.xsd.XsdSchemaProvider;
import org.lfenergy.shapeshifter.core.common.xsd.XsdValidator;
import org.lfenergy.shapeshifter.core.model.IncomingUftpMessage;
import org.lfenergy.shapeshifter.core.model.OutgoingUftpMessage;
import org.lfenergy.shapeshifter.core.model.UftpParticipant;
import org.lfenergy.shapeshifter.core.service.UftpParticipantService;
import org.lfenergy.shapeshifter.core.service.crypto.LazySodiumBase64Pool;
import org.lfenergy.shapeshifter.core.service.crypto.LazySodiumFactory;
import org.lfenergy.shapeshifter.core.service.crypto.UftpCryptoService;
import org.lfenergy.shapeshifter.core.service.handler.UftpPayloadHandler;
import org.lfenergy.shapeshifter.core.service.participant.ParticipantResolutionService;
import org.lfenergy.shapeshifter.core.service.receiving.UftpReceivedMessageService;
import org.lfenergy.shapeshifter.core.service.serialization.UftpSerializer;
import org.lfenergy.shapeshifter.core.service.validation.UftpValidationService;
import org.openremote.agent.protocol.http.AbstractHTTPServerProtocol;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.energy.gopacs.*;
import org.openremote.model.Container;
import org.openremote.container.web.WebApplication;
import org.openremote.container.web.WebService;
import org.openremote.model.Constants;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.http.AbstractHTTPServerProtocol.configureDeploymentInfo;
import static org.openremote.agent.protocol.http.AbstractHTTPServerProtocol.getStandardProviders;
import static org.openremote.container.web.WebService.pathStartsWithHandler;
import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.model.syslog.SyslogCategory.API;


public class GOPACSHandler implements UftpPayloadHandler, UftpParticipantService {

    private static final Logger LOG = SyslogCategory.getLogger(API, GOPACSHandler.class);
    public static final String OR_GOPACS_PRIVATE_KEY_FILE = "OR_GOPACS_PRIVATE_KEY_FILE";


    protected static final UftpSerializer serializer = new UftpSerializer(new XmlSerializer(), new XsdValidator(new XsdSchemaProvider(new XsdFactory(new XsdSchemaFactoryPool()))));

    protected final boolean devMode;
    protected final String contractedEan;
    protected final String electricitySupplierAssetId;
    protected final String realm;
    protected final double powerImportMax;
    protected final double powerExportMax;
    protected final Map<String, UftpParticipantInformation> participants;

    protected final AssetProcessingService assetProcessingService;
    protected final AssetPredictedDatapointService assetPredictedDatapointService;
    protected final ScheduledExecutorService scheduledExecutorService;
    protected final TimerService timerService;
    protected final WebService webService;

    protected final ResteasyClient client;
    protected final GOPACSClientResource goPacsClientResource;
    protected final GOPACSAddressBookResource goPacsAddressBookResource;
    protected final GOPACSServerResource gopacsServerResource;

    protected final UftpValidationService uftpValidationService;
    protected final UftpReceivedMessageService uftpReceivedMessageService;
    protected final UftpCryptoService cryptoService;
    protected final String privateKey;

    protected AbstractHTTPServerProtocol.DeploymentInstance deployment;

    List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();

    public static class Factory {
        protected Container container;

        public Factory(Container container) {
            this.container = container;
        }

        public GOPACSHandler createHandler(String contractedEan, String realm, String electricitySupplierAssetId, double powerImportMax, double powerExportMax) {
            return new GOPACSHandler(contractedEan, realm, electricitySupplierAssetId, powerImportMax, powerExportMax, container);
        }
    }

    protected GOPACSHandler(String contractedEan, String realm, String electricitySupplierAssetId, double powerImportMax, double powerExportMax, Container container) {
        this.devMode = container.isDevMode();
        this.contractedEan = contractedEan;
        this.realm = realm;
        this.powerImportMax = powerImportMax;
        this.powerExportMax = powerExportMax;
        this.electricitySupplierAssetId = electricitySupplierAssetId;
        this.participants = new HashMap<>();

        this.assetProcessingService = container.getService(AssetProcessingService.class);
        this.assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class);
        this.scheduledExecutorService = container.getScheduledExecutor();
        this.timerService = container.getService(TimerService.class);
        this.webService = container.getService(WebService.class);

        this.client = createClient(org.openremote.container.Container.EXECUTOR);

        this.goPacsClientResource = client.target("https://clc-message-broker.acc.gopacs-services.eu").proxy(GOPACSClientResource.class);
        this.goPacsAddressBookResource = client.target("https://capacity-limit-contracts.acc.gopacs-services.eu").proxy(GOPACSAddressBookResource.class);
        this.gopacsServerResource = new GOPACSServerResourceImpl(this::processRawMessage);

        this.cryptoService = new UftpCryptoService(new ParticipantResolutionService(this), new LazySodiumFactory(), new LazySodiumBase64Pool());
        this.uftpValidationService = new UftpValidationService(new ArrayList<>());
        this.uftpReceivedMessageService = new UftpReceivedMessageService(new UftpValidationService(new ArrayList<>()), this);


        String goPacsPrivateKeyFile = container.getConfig().get(OR_GOPACS_PRIVATE_KEY_FILE);
        if (TextUtil.isNullOrEmpty(goPacsPrivateKeyFile)) {
            throw new RuntimeException(OR_GOPACS_PRIVATE_KEY_FILE + " not defined, can not send use GOPACS.");
        }
        if (!Files.isReadable(Paths.get(goPacsPrivateKeyFile))) {
            throw new RuntimeException(OR_GOPACS_PRIVATE_KEY_FILE + " invalid path or file not readable: " + goPacsPrivateKeyFile);
        }
        // Read the private key from file
        try {
            this.privateKey = Files.readString(Paths.get(goPacsPrivateKeyFile));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize GOPACSHandler for ean " + contractedEan + ".", ex);
        }

        Application application = createApplication(container);
        ResteasyDeployment resteasyDeployment = createDeployment(application);
        DeploymentInfo deploymentInfo = createDeploymentInfo(resteasyDeployment);
        configureDeploymentInfo(deploymentInfo);
        deploy(deploymentInfo);
    }

    private ResteasyDeployment createDeployment(Application application) {
        ResteasyDeployment resteasyDeployment = new ResteasyDeploymentImpl();
        resteasyDeployment.setApplication(application);

        List<String> allowedOrigins = Collections.singletonList("*");

//        if (devMode) {
//            allowedOrigins = Collections.singletonList("*");
//        } else {
//            allowedOrigins = agent.getAllowedOrigins().map(Arrays::asList).orElse(null);
//        }

        if (allowedOrigins != null) {
            String allowedMethods = "OPTIONS, GET, POST";
            CorsFilter corsFilter = new CorsFilter();
            corsFilter.getAllowedOrigins().addAll(allowedOrigins);
            corsFilter.setAllowedMethods(allowedMethods);
            resteasyDeployment.getProviders().add(corsFilter);
        }

        return resteasyDeployment;
    }

    private DeploymentInfo createDeploymentInfo(ResteasyDeployment resteasyDeployment) {
        String deploymentName = "GoPacs=" + contractedEan;
        String deploymentPath = "/gopacs/" + realm;

        ServletInfo resteasyServlet = Servlets.servlet("ResteasyServlet", HttpServlet30Dispatcher.class).setAsyncSupported(true).setLoadOnStartup(1).addMapping("/*");

        return new DeploymentInfo().setDeploymentName(deploymentName).setContextPath(deploymentPath).addServletContextAttribute(ResteasyDeployment.class.getName(), resteasyDeployment).addServlet(resteasyServlet).setClassLoader(org.openremote.model.Container.class.getClassLoader());
    }

    protected Application createApplication(Container container) {
        List<Object> providers = getStandardProviders(this.devMode);
        providers.add(gopacsServerResource);
        return new WebApplication(container, null, providers);
    }

    protected void deploy(DeploymentInfo deploymentInfo) {
        LOG.info("Deploying JAX-RS deployment for instance : " + this);
        DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
        manager.deploy();
        HttpHandler httpHandler;

        try {
            httpHandler = manager.start();

            // Wrap the handler to inject the realm
            HttpHandler handlerWrapper = exchange -> {
                exchange.getRequestHeaders().put(HttpString.tryFromString(Constants.REALM_PARAM_NAME), realm);
                httpHandler.handleRequest(exchange);
            };
            WebService.RequestHandler requestHandler = pathStartsWithHandler(deploymentInfo.getDeploymentName(), deploymentInfo.getContextPath(), handlerWrapper);

            LOG.info("Registering GOPACS request handler '" + this.getClass().getSimpleName() + "' for request path: " + deploymentInfo.getContextPath());
            // Add the handler before the greedy deployment handler
            webService.getRequestHandlers().add(0, requestHandler);

            deployment = new AbstractHTTPServerProtocol.DeploymentInstance(deploymentInfo, requestHandler);
        } catch (ServletException e) {
            LOG.severe("Failed to deploy deployment: " + deploymentInfo.getDeploymentName());
        }
    }

    protected void undeploy() {
        for (ScheduledFuture<?> scheduledFuture : scheduledFutureList) {
            scheduledFuture.cancel(true);
        }
        scheduledFutureList.clear();
        if (deployment == null) {
            LOG.info("Deployment doesn't exist for instance: " + this);
            return;
        }

        try {
            LOG.info("Un-registering GOPACS request handler '" + this.getClass().getSimpleName() + "' for request path: " + deployment.getDeploymentInfo().getContextPath());
            webService.getRequestHandlers().remove(deployment.getRequestHandler());
            DeploymentManager manager = Servlets.defaultContainer().getDeployment(deployment.getDeploymentInfo().getDeploymentName());
            if (manager != null) {
                manager.stop();
                manager.undeploy();
            }
            Servlets.defaultContainer().removeDeployment(deployment.getDeploymentInfo());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "An exception occurred whilst un-deploying instance: " + this, ex);
            throw new RuntimeException(ex);
        }
    }

    protected void handleFlexMessage(UftpParticipant participant, FlexRequest flexRequest) {
        LOG.fine("Received Flex Request: " + flexRequest);
        int year = flexRequest.getPeriod().getYear();
        int month = flexRequest.getPeriod().getMonthValue();
        int day = flexRequest.getPeriod().getDayOfMonth();
        List<ValueDatapoint<?>> powerImportMaxGopacsDatapoints = new ArrayList<>();
        List<ValueDatapoint<?>> powerExportMaxGopacsDatapoints = new ArrayList<>();

        for (int i = 0; i < flexRequest.getISPS().size(); i++) {
            FlexRequestISPType flexRequestISPType = flexRequest.getISPS().get(i);
            LocalTime start = FlexRequestISPTypeHelper.getISPStart(flexRequestISPType.getStart(), year, month, day, flexRequest.getTimeZone());
            double importMax = flexRequestISPType.getMaxPower() == 0L ? this.powerImportMax : (double) flexRequestISPType.getMaxPower() / 1000.0F;
            double exportMax = flexRequestISPType.getMinPower() == 0L ? this.powerExportMax : (double) Math.abs(flexRequestISPType.getMinPower()) / 1000.0F;
            this.schedulePowerUpdates(start, importMax, exportMax);

            // Correct usage of ZoneId.of instead of ZoneOffset.of
            ZoneId zoneId = ZoneId.of(flexRequest.getTimeZone());
            long startEpochMilli = flexRequest.getPeriod().atTime(start).atZone(zoneId).toInstant().toEpochMilli();

            powerImportMaxGopacsDatapoints.add(new ValueDatapoint<>(startEpochMilli, importMax));
            powerExportMaxGopacsDatapoints.add(new ValueDatapoint<>(startEpochMilli, exportMax));
        }

        this.setPredictedDataPoints("powerImportMaxGopacs", powerImportMaxGopacsDatapoints);
        this.setPredictedDataPoints("powerExportMaxGopacs", powerExportMaxGopacsDatapoints);
        LOG.fine("Finished processing Flex Request: " + flexRequest);
    }

    protected void schedulePowerUpdates(LocalTime start, double maxPower, double minPower) {
        long currentTimeMillis = timerService.getCurrentTimeMillis();
        long ispStartMillis = start.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long delay = ispStartMillis - currentTimeMillis;
        scheduledExecutorService.schedule(() -> updatePowerValues(maxPower, minPower), delay, TimeUnit.MILLISECONDS);
    }

    protected void updatePowerValues(double maxPower, double minPower) {
        assetProcessingService.sendAttributeEvent(new AttributeEvent(electricitySupplierAssetId, "powerImportMaxGopacs", maxPower), getClass().getSimpleName());
        assetProcessingService.sendAttributeEvent(new AttributeEvent(electricitySupplierAssetId, "powerExportMaxGopacs", minPower), getClass().getSimpleName());
    }

    protected void sendFlexResponse(String transportXml) {
        goPacsClientResource.outMessage(transportXml);
    }

    protected void setPredictedDataPoints(String attributeName, List<ValueDatapoint<?>> valuesAndTimestamps) {
        assetPredictedDatapointService.updateValues(electricitySupplierAssetId, attributeName, valuesAndTimestamps);
    }

    @Override
    public void notifyNewIncomingMessage(IncomingUftpMessage<? extends PayloadMessageType> message) {
        var messageType = message.payloadMessage().getClass();
        if (!FlexRequest.class.isAssignableFrom(messageType)) {
            return;
        }

        var flexRequest = (FlexRequest) message.payloadMessage();
        this.handleFlexMessage(message.sender(), flexRequest);
    }

    @Override
    public void notifyNewOutgoingMessage(OutgoingUftpMessage<? extends PayloadMessageType> message) {
        var messageType = message.payloadMessage().getClass();
        if (!FlexRequestResponse.class.isAssignableFrom(messageType)) {
            return;
        }

        var flexRequestResponse = (FlexRequestResponse) message.payloadMessage();

        String payloadXml = serializer.toXml(flexRequestResponse);
        SignedMessage signedMessage = cryptoService.signMessage(payloadXml, message.sender(), this.privateKey);
        String transportXml = serializer.toXml(signedMessage);

        goPacsClientResource.outMessage(transportXml);
    }

    protected void processRawMessage(String transportXml) {
        SignedMessage signedMessage = serializer.fromSignedXml(transportXml);
        String payloadXml = cryptoService.verifySignedMessage(signedMessage);
        PayloadMessageType payloadMessage = serializer.fromPayloadXml(payloadXml);
        var incomingUftpMessage = IncomingUftpMessage.create(new UftpParticipant(signedMessage), payloadMessage, transportXml, payloadXml);
        notifyNewIncomingMessage(incomingUftpMessage);
        uftpReceivedMessageService.process(incomingUftpMessage);
    }

    @Override
    public Optional<UftpParticipantInformation> getParticipantInformation(USEFRoleType role, String domain) {
        if (participants.containsKey(domain)) {
            return Optional.of(participants.get(domain));
        } else {
            try (Response response = goPacsAddressBookResource.fetchParticipants(contractedEan)) {
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

