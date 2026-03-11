package org.openremote.agent.protocol.entsoe;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.syslog.SyslogCategory;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class EntsoeProtocol extends AbstractProtocol<EntsoeAgent, EntsoeAgentLink> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, EntsoeProtocol.class);
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneId.of("UTC"));
    private static final DateTimeFormatter ENTSOE_DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .appendOffsetId()
            .toFormatter();
    public static final String PROTOCOL_DISPLAY_NAME = "ENTSO-E";
    private static final AtomicReference<ResteasyClient> client = new AtomicReference<>();
    private static final JAXBContext PUBLICATION_MARKET_DOCUMENT_CONTEXT = createPublicationMarketDocumentContext();
    private static final ThreadLocal<Unmarshaller> PUBLICATION_UNMARSHALLER = ThreadLocal.withInitial(EntsoeProtocol::createPublicationUnmarshaller);
    private static final ThreadLocal<XMLInputFactory> XML_INPUT_FACTORY = ThreadLocal.withInitial(XMLInputFactory::newFactory);

    // Initial delay to allow system to populate agent links
    private static final int INITIAL_POLLING_DELAY_MILLIS = 3000; // 3 seconds
    private static final int DEFAULT_POLLING_MILLIS = 3600000; // 1 hour

    protected ScheduledFuture<?> pollingFuture;

    public EntsoeProtocol(EntsoeAgent agent) {
        super(agent);
        initClient();
    }

    @Override
    protected void doStart(Container container) throws Exception {
        setConnectionStatus(ConnectionStatus.CONNECTING);

        if (agent.getSecurityToken().isEmpty()) {
            setConnectionStatus(ConnectionStatus.ERROR);
            LOG.warning("Security token is not set");
            return;
        }

        if (!healthCheck()) {
            setConnectionStatus(ConnectionStatus.ERROR);
            LOG.warning("Could not reach ENTSO-E API, either API is unavailable or security token is invalid");
            return;
        }

        // Schedule the polling task
        int pollingMillis = agent.getPollingMillis().orElse(DEFAULT_POLLING_MILLIS);
        pollingFuture = scheduledExecutorService.scheduleAtFixedRate(this::updateAllLinkedAttributes, INITIAL_POLLING_DELAY_MILLIS, pollingMillis,
                TimeUnit.MILLISECONDS);

        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        // Cancel the polling task
        if (pollingFuture != null) {
            pollingFuture.cancel(true);
        }
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, EntsoeAgentLink agentLink) throws RuntimeException {

        // TODO: I suppose we still sometimes need to do something, if an attribute is just added, we don't want to wait 1h for it to get data

        // Do nothing
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, EntsoeAgentLink agentLink) {
        // Do nothing
    }

    @Override
    protected void doLinkedAttributeWrite(EntsoeAgentLink agentLink, AttributeEvent event, Object processedValue) {
        // Do nothing
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "https://transparency.entsoe.eu/";
    }

    protected void updateAllLinkedAttributes() {
        LOG.fine("Updating all linked attributes with pricing information from ENTSO-E");

        if (getLinkedAttributes().isEmpty()) {
            LOG.fine("No linked attributes found, skipping pricing data update");
            return;
        }

        getLinkedAttributes().forEach(this::updatePricingInformation);
    }

    protected void updatePricingInformation(AttributeRef attributeRefs, Attribute attribute) {
        /*
        LOG.log(Level.FINE, () -> "Updating pricing information data for asset: " + assetId);

        Asset<?> asset = assetService.findAsset(assetId);
        if (asset == null) {
            LOG.log(Level.WARNING, () -> "Asset not found for asset: " + assetId);
            return;
        }

         */

        EntsoeAgentLink agentLink = agent.getAgentLink(attribute);

        PublicationMarketDocument doc = fetchPricingInformation(buildApiUrl(agentLink.getZone()));
        if (doc == null) {
            LOG.warning(() -> "No ENTSO-E publication document returned for attribute: " + attributeRefs);
            return;
        }

        List<ValueDatapoint<?>> predictedDatapoints = buildPredictedDatapoints(doc);
        if (predictedDatapoints.isEmpty()) {
            LOG.warning(() -> "No datapoints built from ENTSO-E publication document for attribute: " + attributeRefs);
            return;
        }

        predictedDatapointService.updateValues(attributeRefs.getId(), attributeRefs.getName(), predictedDatapoints);

    }

    protected String buildApiUrl(String zone) {
        String securityToken = agent.getSecurityToken().orElseThrow(() -> new IllegalStateException("Security token is not set"));
        String baseUrl = agent.getBaseURL().orElse("https://web-api.tp.entsoe.eu/api");
        Instant start = timerService.getNow();
        Instant end = start.plus(1, ChronoUnit.DAYS);

        return UriBuilder.fromUri(baseUrl)
                .queryParam("documentType", "A44")
                .queryParam("contract_MarketAgreement.type", "A01")
                .queryParam("periodStart", PERIOD_FORMATTER.format(start))
                .queryParam("periodEnd", PERIOD_FORMATTER.format(end))
                .queryParam("in_Domain", zone)
                .queryParam("out_Domain", zone)
                .queryParam("securityToken", securityToken)
                .build()
                .toString();
    }

    /**
     * Perform a health check by sending a request to the ENTSO-E API
     *
     * @return true if the health check is successful, false otherwise
     */
    protected boolean healthCheck() {
        String apiUrl = buildApiUrl("10YBE----------2");
        LOG.info("API URL: " + apiUrl);
        try (Response response = client.get().target(apiUrl).request(javax.ws.rs.core.MediaType.APPLICATION_XML).get()) {
            if (response.getStatus() != 200) {
                LOG.warning("Health check failed with status: " + response.getStatus());
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e, () -> "Failed to perform health check");
            return false;
        }
    }

    /**
     * Fetch the pricing data from the ENTSO-E API for the given API URL
     *
     * @param apiUrl the API URL
     * @return the PublicationMarketDocument from the API
     */
    protected PublicationMarketDocument fetchPricingInformation(String apiUrl) {
        try (Response response = client.get().target(apiUrl).request(javax.ws.rs.core.MediaType.APPLICATION_XML).get()) {
            if (response.getStatus() == 200) {
                String responseXml = response.readEntity(String.class);
                EntsoeXmlMeta xmlMeta = parseEntsoeXmlMeta(responseXml);

                if ("Publication_MarketDocument".equals(xmlMeta.rootElement)) {
                    return (PublicationMarketDocument) PUBLICATION_UNMARSHALLER.get().unmarshal(new StringReader(responseXml));
                }

                if ("Acknowledgement_MarketDocument".equals(xmlMeta.rootElement)) {
                    String reason = xmlMeta.reasonText != null ? xmlMeta.reasonText : "no reason provided";
                    LOG.info("No ENTSO-E pricing data available: " + reason);
                    return null;
                }

                LOG.warning("Unsupported ENTSO-E response XML root element: " + xmlMeta.rootElement);
                return null;
            } else if (response.getStatus() == 401) {
                LOG.warning("API request was unauthorized, either the security token is invalid or does not provide access to the API");
                return null;
            } else {
                LOG.warning("API request failed with status: " + response.getStatus());
                return null;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e, () -> "Failed to fetch pricing data");
            return null;
        }
    }

    protected static JAXBContext createPublicationMarketDocumentContext() {
        try {
            return JAXBContext.newInstance(PublicationMarketDocument.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise PublicationMarketDocument JAXB context", e);
        }
    }

    protected static Unmarshaller createPublicationUnmarshaller() {
        try {
            return PUBLICATION_MARKET_DOCUMENT_CONTEXT.createUnmarshaller();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise PublicationMarketDocument unmarshaller", e);
        }
    }

    protected EntsoeXmlMeta parseEntsoeXmlMeta(String xml) throws Exception {
        XMLStreamReader reader = XML_INPUT_FACTORY.get().createXMLStreamReader(new StringReader(xml));
        String rootElement = null;
        String reasonText = null;
        boolean inReason = false;
        boolean inReasonText = false;

        try {
            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    if (rootElement == null) {
                        rootElement = localName;
                    }
                    if ("Reason".equals(localName)) {
                        inReason = true;
                    } else if (inReason && "text".equals(localName)) {
                        inReasonText = true;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS && inReasonText) {
                    String text = reader.getText();
                    if (text != null && !text.isBlank()) {
                        reasonText = reasonText == null ? text.trim() : reasonText + text.trim();
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();
                    if ("text".equals(localName)) {
                        inReasonText = false;
                    } else if ("Reason".equals(localName)) {
                        inReason = false;
                    }
                }
            }
        } finally {
            reader.close();
        }

        return new EntsoeXmlMeta(rootElement, reasonText);
    }

    protected static class EntsoeXmlMeta {
        protected final String rootElement;
        protected final String reasonText;

        protected EntsoeXmlMeta(String rootElement, String reasonText) {
            this.rootElement = rootElement;
            this.reasonText = reasonText;
        }
    }

    protected List<ValueDatapoint<?>> buildPredictedDatapoints(PublicationMarketDocument document) {
        List<ValueDatapoint<?>> values = new ArrayList<>();
        long nowMillis = timerService.getCurrentTimeMillis();
        if (document.getTimeSeries() == null || document.getTimeSeries().isEmpty()) {
            return values;
        }

        for (PublicationMarketDocument.TimeSeries timeSeries : document.getTimeSeries()) {
            PublicationMarketDocument.Period period = timeSeries.getPeriod();
            if (period == null || period.getPoints() == null || period.getPoints().isEmpty()) {
                continue;
            }

            PublicationMarketDocument.PeriodTimeInterval timeInterval = period.getTimeInterval() != null
                    ? period.getTimeInterval()
                    : document.getPeriodTimeInterval();
            if (timeInterval == null || timeInterval.getStart() == null || period.getResolution() == null) {
                continue;
            }

            final Instant start;
            final Duration resolution;
            try {
                start = parseEntsoeInstant(timeInterval.getStart());
                resolution = Duration.parse(period.getResolution());
            } catch (Exception e) {
                LOG.log(Level.WARNING, e, () -> "Could not parse ENTSO-E timeseries time data");
                continue;
            }

            period.getPoints().stream()
                    .filter(point -> point.getPosition() != null && point.getPosition() > 0 && point.getPriceAmount() != null)
                    .sorted(Comparator.comparingInt(PublicationMarketDocument.Point::getPosition))
                    .forEach(point -> {
                        long timestamp = start
                                .plus(resolution.multipliedBy(point.getPosition() - 1L))
                                .toEpochMilli();
                        if (timestamp >= nowMillis) {
                            values.add(new ValueDatapoint<>(timestamp, point.getPriceAmount()));
                        }
                    });
        }

        values.sort(Comparator.comparingLong(ValueDatapoint::getTimestamp));
        return values;
    }

    protected Instant parseEntsoeInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return OffsetDateTime.parse(value, ENTSOE_DATETIME_FORMATTER).toInstant();
        }
    }

    protected static void initClient() {
        synchronized (client) {
            if (client.get() == null) {
                client.set(createClient(org.openremote.container.Container.SCHEDULED_EXECUTOR));
            }
        }
    }

}
