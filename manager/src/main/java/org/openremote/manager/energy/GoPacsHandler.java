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
import org.lfenergy.shapeshifter.api.FlexRequest;
import org.lfenergy.shapeshifter.api.FlexRequestISPType;
import org.lfenergy.shapeshifter.api.FlexRequestResponse;
import org.lfenergy.shapeshifter.api.USEFRoleType;
import org.lfenergy.shapeshifter.api.model.UftpParticipantInformation;
import org.lfenergy.shapeshifter.core.model.OutgoingUftpMessage;
import org.lfenergy.shapeshifter.core.model.UftpParticipant;
import org.lfenergy.shapeshifter.core.service.UftpParticipantService;
import org.lfenergy.shapeshifter.core.service.handler.UftpPayloadHandler;
import org.lfenergy.shapeshifter.core.service.receiving.UftpReceivedMessageService;
import org.lfenergy.shapeshifter.core.service.validation.UftpValidationService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.energy.gopacs.*;
import org.openremote.model.Container;
import org.openremote.container.web.WebApplication;
import org.openremote.container.web.WebService;
import org.openremote.model.Constants;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.syslog.SyslogCategory;

import java.io.IOException;
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


public class GoPacsHandler implements UftpParticipantService {
    private static final Logger LOG = SyslogCategory.getLogger(API, GoPacsHandler.class);

    protected String contractedEan;
    protected String electricitySupplierAssetId;
    protected String realm;
    protected double powerImportMax;
    protected double powerExportMax;
    protected DeploymentInfo deploymentInfo;
    protected ResteasyClient client;
    protected GopacsClientResource goPacsClient;
    protected GOPACSAddressBookResource goPacsAddressBookResource;
    protected UftpValidationService uftpValidationService;
    protected UftpPayloadHandler uftpPayloadHandler;
    protected UftpReceivedMessageService uftpReceivedMessageService;
    protected GopacsServerResource goPacsServer;
    protected WebService webService;
    protected AssetProcessingService assetProcessingService;
    protected ScheduledExecutorService scheduledExecutorService;
    protected TimerService timerService;

    protected WebService.RequestHandler requestHandler;

    Map<String, UftpParticipantInformation> participants = new HashMap<>();
    List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();

    public static class Factory {
        protected Container container;

        public Factory(Container container) {
            this.container = container;
        }

        public GoPacsHandler createHandler(String contractedEan, String realm, String electricitySupplierAssetId, double powerImportMax, double powerExportMax) {
            return new GoPacsHandler(contractedEan, realm, electricitySupplierAssetId, powerImportMax, powerExportMax, container);
        }
    }

    public static class FlexRequestISPTypeHelper {
        private static final long ISP_DURATION_IN_MINUTES = 15;

        public static LocalTime getISPStart(long ispNumber, int year, int month, int day, String timeZone) {
            ZoneId zoneId = ZoneId.of(timeZone);
            ZonedDateTime date = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, zoneId);

            if (ispNumber == 9 && isLastSundayInMarch(year, month, day)) {
                return date.plusHours(3).toLocalTime();
            } else if (ispNumber == 13 && isLastSundayInOctober(year, month, day)) {
                return date.plusHours(2).toLocalTime();
            } else {
                return date.plusMinutes((ispNumber - 1) * ISP_DURATION_IN_MINUTES).toLocalTime();
            }
        }

        public static LocalTime getISPEnd(int ispNumber, int year, int month, int day, String timeZone) {
            LocalTime end = getISPStart(ispNumber, year, month, day, timeZone).plusMinutes(ISP_DURATION_IN_MINUTES);
            if (ispNumber == 8 && isLastSundayInMarch(year, month, day)) {
                end = end.plusHours(1);
            } else if (ispNumber == 12 && isLastSundayInOctober(year, month, day)) {
                end = end.minusHours(1);
            }
            return end;
        }

        private static boolean isLastSundayInMarch(int year, int month, int day) {
            if (month != 3) { // March is 3 in Java's month numbering
                return false;
            }
            LocalDate date = LocalDate.of(year, month, day);
            int lastDayInMarch = YearMonth.of(year, month).lengthOfMonth();
            LocalDate lastSundayInMarch = LocalDate.of(year, month, lastDayInMarch);
            while (lastSundayInMarch.getDayOfWeek() != DayOfWeek.SUNDAY) {
                lastSundayInMarch = lastSundayInMarch.minusDays(1);
            }
            return date.equals(lastSundayInMarch);
        }

        private static boolean isLastSundayInOctober(int year, int month, int day) {
            if (month != 10) { // October is 10 in Java's month numbering
                return false;
            }
            LocalDate date = LocalDate.of(year, month, day);
            int lastDayInOctober = YearMonth.of(year, month).lengthOfMonth();
            LocalDate lastSundayInOctober = LocalDate.of(year, month, lastDayInOctober);
            while (lastSundayInOctober.getDayOfWeek() != DayOfWeek.SUNDAY) {
                lastSundayInOctober = lastSundayInOctober.minusDays(1);
            }
            return date.equals(lastSundayInOctober);
        }
    }

    protected GoPacsHandler(String contractedEan, String realm, String electricitySupplierAssetId, double powerImportMax, double powerExportMax, Container container) {
        this.contractedEan = contractedEan;
        this.realm = realm;
        this.powerImportMax = powerImportMax;
        this.powerExportMax = powerExportMax;
        this.electricitySupplierAssetId = electricitySupplierAssetId;
        this.client = createClient(org.openremote.container.Container.EXECUTOR);
        this.goPacsClient = client.target("https://clc-message-broker.acc.gopacs-services.eu/shapeshifter/api/v3/").proxy(GopacsClientResource.class);
        this.goPacsAddressBookResource = client.target("https://capacity-limit-contracts.acc.gopacs-services.eu/v2/").proxy(GOPACSAddressBookResource.class);
        this.uftpValidationService = new UftpValidationService(new ArrayList<>());
        this.uftpPayloadHandler = new PayloadHandler(this::handleFlexMessage, this::sendFlexResponse);
        this.uftpReceivedMessageService = new UftpReceivedMessageService(uftpValidationService, uftpPayloadHandler);
        this.scheduledExecutorService = container.getScheduledExecutor();
        this.timerService = container.getService(TimerService.class);
        this.goPacsServer = new GopacsServerResourceImpl(this.uftpReceivedMessageService::process);

        this.webService = container.getService(WebService.class);
        this.assetProcessingService = container.getService(AssetProcessingService.class);
        Application application = createApplication(container);
        ResteasyDeployment resteasyDeployment = createDeployment(application);
        createDeploymentInfo(resteasyDeployment);
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

    private void createDeploymentInfo(ResteasyDeployment resteasyDeployment) {
        String deploymentName = "GoPacs=" + contractedEan;
        String deploymentPath =  "/shapeshifter/api/v3/message";

        ServletInfo resteasyServlet = Servlets.servlet("ResteasyServlet", HttpServlet30Dispatcher.class)
                .setAsyncSupported(true)
                .setLoadOnStartup(1)
                .addMapping("/*");

        deploymentInfo = new DeploymentInfo()
                .setDeploymentName(deploymentName)
                .setContextPath(deploymentPath)
                .addServletContextAttribute(ResteasyDeployment.class.getName(), resteasyDeployment)
                .addServlet(resteasyServlet)
                .setClassLoader(org.openremote.model.Container.class.getClassLoader());
    }

    protected Application createApplication(Container container) {
        List<Object> providers = getStandardProviders();
        providers.add(goPacsServer);
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
            requestHandler = pathStartsWithHandler(deploymentInfo.getDeploymentName(), deploymentInfo.getContextPath(), handlerWrapper);

            LOG.info("Registering GOPACS request handler '"
                    + this.getClass().getSimpleName()
                    + "' for request path: "
                    + deploymentInfo.getContextPath());
            // Add the handler before the greedy deployment handler
            webService.getRequestHandlers().add(0, requestHandler);
        } catch (ServletException e) {
            LOG.severe("Failed to deploy deployment: " + deploymentInfo.getDeploymentName());
        }
    }

    protected void undeploy() {
        for (ScheduledFuture<?> scheduledFuture : scheduledFutureList) {
            scheduledFuture.cancel(true);
        }
        scheduledFutureList.clear();
        if (requestHandler == null) {
            LOG.info("Deployment doesn't exist for instance: " + this);
            return;
        }

        try {
            LOG.info("Un-registering GOPACS request handler '"
                    + this.getClass().getSimpleName()
                    + "' for request path: "
                    + deploymentInfo.getContextPath());
            webService.getRequestHandlers().remove(requestHandler);
            DeploymentManager manager = Servlets.defaultContainer().getDeployment(deploymentInfo.getDeploymentName());
            if (manager != null) {
                manager.stop();
                manager.undeploy();
            }
            Servlets.defaultContainer().removeDeployment(deploymentInfo);
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "An exception occurred whilst un-deploying instance: " + this,
                    ex);
            throw new RuntimeException(ex);
        }
    }

    protected void handleFlexMessage(UftpParticipant participant, FlexRequest flexRequest) {
        LOG.info("Received Flex Request: " + flexRequest);
        int year = flexRequest.getPeriod().getYear();
        int month = flexRequest.getPeriod().getMonthValue();
        int day = flexRequest.getPeriod().getDayOfMonth();

        for (int i = 0; i < flexRequest.getISPS().size(); i++) {
            FlexRequestISPType flexRequestISPType = flexRequest.getISPS().get(i);
            LocalTime start = FlexRequestISPTypeHelper.getISPStart(flexRequestISPType.getStart(), year, month, day, flexRequest.getTimeZone());
            // Watts to Kilo watts
            // Export is a positive value and min power will be negative if not zero
            double importMax = flexRequestISPType.getMaxPower() == 0L ? this.powerImportMax : (double) flexRequestISPType.getMaxPower() / 1000;
            double exportMax = flexRequestISPType.getMinPower() == 0L ? this.powerExportMax : (double) Math.abs(flexRequestISPType.getMinPower()) / 1000;

            schedulePowerUpdates(start, importMax, exportMax);
        }
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

    protected void sendFlexResponse(UftpParticipant participant, FlexRequestResponse flexRequestResponse) {
        OutgoingUftpMessage<FlexRequestResponse> outgoingUftpMessage = OutgoingUftpMessage.create(participant, flexRequestResponse);
        goPacsClient.outMessage(outgoingUftpMessage);
    }


    @Override
    public Optional<UftpParticipantInformation> getParticipantInformation(USEFRoleType usefRoleType, String domain) {
        if (participants.containsKey(domain)) {
            return Optional.of(participants.get(domain));
        } else {
            try (Response response = goPacsAddressBookResource
                    .fetchParticipants(contractedEan)) {
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

