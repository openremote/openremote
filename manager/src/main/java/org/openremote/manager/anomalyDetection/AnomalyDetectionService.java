/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.manager.anomalyDetection;

import com.google.api.client.util.DateTime;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.groovy.runtime.ArrayUtil;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.alarm.AlarmService;
import org.openremote.manager.anomalyDetection.DetectionMethods.*;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetAnomalyDatapointService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.datapoint.AssetPredictedDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.alarm.Alarm;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.*;
import org.openremote.model.datapoint.*;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.value.AnomalyDetectionConfiguration;


import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.model.attribute.Attribute.getAddedOrModifiedAttributes;
import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;
import static org.openremote.model.value.MetaItemType.*;

public class AnomalyDetectionService extends RouteBuilder implements ContainerService, EventListener{

    private static final Logger LOG = Logger.getLogger(AnomalyDetectionService.class.getName());
    private static long STOP_TIMEOUT = Duration.ofSeconds(5).toMillis();
    private List<Asset<?>> anomalyDetectionAssets;
    private HashMap<String, AnomalyAttribute> anomalyDetectionAttributes;


    protected GatewayService gatewayService;
    protected AssetStorageService assetStorageService;
    protected ClientEventService clientEventService;
    protected AssetDatapointService assetDatapointService;
    protected AssetPredictedDatapointService assetPredictedDatapointService;
    protected ScheduledExecutorService executorService;
    protected AlarmService alarmService;
    protected AssetAnomalyDatapointService assetAnomalyDatapointService;

    @Override
    public void init(Container container) throws Exception {
        anomalyDetectionAssets = new ArrayList<>();
        anomalyDetectionAttributes = new HashMap<>();

        gatewayService = container.getService(GatewayService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        clientEventService = container.getService(ClientEventService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class);
        alarmService = container.getService(AlarmService.class);
        assetAnomalyDatapointService = container.getService(AssetAnomalyDatapointService.class);
        executorService = container.getExecutorService();
        container.getService(ManagerWebService.class).addApiSingleton(
                new AnomalyDectectionResourceImpl(
                        container.getService(TimerService.class),
                        container.getService(ManagerIdentityService.class),
                        container.getService(AssetStorageService.class),
                        this
                )
        );
    }


    @Override
    public void start(Container container) throws Exception {
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
        clientEventService.addInternalSubscription(AttributeEvent.class,null, this::onAttributeChange );

        LOG.fine("Loading anomaly detection asset attributes...");

        anomalyDetectionAssets = getAnomalyDetectionAssets();

        anomalyDetectionAssets.forEach(asset -> asset.getAttributes().stream().filter(attr -> attr.hasMeta(ANOMALYDETECTION) && attr.hasMeta(STORE_DATA_POINTS)).forEach(
                attribute -> {
                    anomalyDetectionAttributes.put(asset.getId() + "$" + attribute.getName(),new AnomalyAttribute(asset,attribute));
                }
        ));
        LOG.fine("Found anomaly detection asset attributes count  = " + anomalyDetectionAttributes.size());
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
                .routeId("Persistence-AnomalyDetectionConfiguration")
                .filter(isPersistenceEventForEntityType(Asset.class))
                .filter(isNotForGateway(gatewayService))
                .process(exchange -> {
                    PersistenceEvent<Asset<?>> persistenceEvent = (PersistenceEvent<Asset<?>>)exchange.getIn().getBody(PersistenceEvent.class);
                    processAssetChange(persistenceEvent);
                });
    }

    protected void processAssetChange(PersistenceEvent<Asset<?>> persistenceEvent) {

        LOG.finest("Processing asset persistence event: " + persistenceEvent.getCause());
        Asset<?> asset = persistenceEvent.getEntity();

        //updates hashmap with the attributes to contain only attributes with the anomaly detection meta item
        switch (persistenceEvent.getCause()) {
            case CREATE:
                // loop through all attributes with an anomaly detection meta item and add them to the watch list.
                (asset.getAttributes().stream().filter(attr -> attr.hasMeta(ANOMALYDETECTION) && attr.hasMeta(STORE_DATA_POINTS))).forEach(
                        attribute -> {
                            anomalyDetectionAttributes.put(asset.getId() + "$" + attribute.getName(),new AnomalyAttribute(asset,attribute));
                        }
                );
                break;
            case UPDATE:
                (((AttributeMap) persistenceEvent.getPreviousState("attributes")).stream().filter(attr -> attr.hasMeta(ANOMALYDETECTION))).forEach(
                        attribute -> {
                            anomalyDetectionAttributes.remove(asset.getId() + "$" + attribute.getName());
                        }
                );
                (asset.getAttributes().stream().filter(attr -> attr.hasMeta(ANOMALYDETECTION) && attr.hasMeta(STORE_DATA_POINTS))).forEach(
                        attribute -> {
                            if(attribute.hasMeta(STORE_DATA_POINTS)){
                                anomalyDetectionAttributes.put(asset.getId() + "$" + attribute.getName(),new AnomalyAttribute(asset,attribute));
                            }
                        }
                );
                break;
            case DELETE: {
                (((AttributeMap) persistenceEvent.getCurrentState("attributes")).stream().filter(attr -> attr.hasMeta(ANOMALYDETECTION))).forEach(
                        attribute -> {
                            anomalyDetectionAttributes.remove(asset.getId() + "$" + attribute.getName());
                        }
                );
                break;
            }
        }
    }

    protected List<Asset<?>> getAnomalyDetectionAssets() {
        return assetStorageService.findAll(
                new AssetQuery().attributes(
                        new AttributePredicate().meta(
                                new NameValuePredicate(
                                        ANOMALYDETECTION,
                                        new StringPredicate(AssetQuery.Match.CONTAINS, true, "type")
                                )
                        )
                )
        );
    }

    /**
     * Gets a list of datapoints over a timespan 5 times longer as the timespan in the anomalyDetectionConfiguration, calculates the limits for these points
     * and returns these values in a list and makes a last list with the original values which fall outside these limits.<p><p/>
     * Returns null if anomalyDetectionConfiguration is invalid<p>
     * Returns empty list if no datapoints are saved yet<p>
     * Returns just original datapoints if not enough datapoints are available to calculate limits<p>
     */
    public ValueDatapoint<?>[][] getAnomalyDatapointLimits( String assetId, String attributeName, AnomalyDetectionConfiguration anomalyDetectionConfiguration) {
        ValueDatapoint<?>[][] vdaa = new ValueDatapoint<?>[0][0];
        DetectionMethod detectionMethod;
        long timespan = 0;
        int minimumDatapoints = 0;
        ValueDatapoint<?>[] datapoints;
        List<ValueDatapoint<?>> assetAnomalyDatapoints = new ArrayList<>();
        if(anomalyDetectionConfiguration == null) return null;
        String type = anomalyDetectionConfiguration.getClass().getSimpleName();
        DatapointPeriod period = assetDatapointService.getDatapointPeriod(assetId, attributeName);
        switch (type) {
            case "Global" ->{
                detectionMethod = new DetectionMethodGlobal(anomalyDetectionConfiguration);
                timespan = ((AnomalyDetectionConfiguration.Global)detectionMethod.config).timespan.toMillis();
                minimumDatapoints = ((AnomalyDetectionConfiguration.Global)detectionMethod.config).minimumDatapoints;

                if(period.getLatest() == null)return vdaa;
                if(period.getLatest() - period.getOldest() < timespan) return new ValueDatapoint<?>[1][0];
                datapoints = assetDatapointService.queryDatapoints(assetId, attributeName,new AssetDatapointAllQuery(period.getLatest() - timespan*5, period.getLatest()));
            }
            case "Change" -> {
                detectionMethod = new DetectionMethodChange(anomalyDetectionConfiguration);
                timespan = ((AnomalyDetectionConfiguration.Change)detectionMethod.config).timespan.toMillis();
                minimumDatapoints = ((AnomalyDetectionConfiguration.Change)detectionMethod.config).minimumDatapoints;

                if(period.getLatest() == null)return vdaa;
                if(period.getLatest() - period.getOldest() < timespan) return new ValueDatapoint<?>[1][0];
                datapoints = assetDatapointService.queryDatapoints(assetId, attributeName,new AssetDatapointAllQuery(period.getLatest() - timespan*5, period.getLatest()));
            }
            case "Forecast" -> {
                detectionMethod = new DetectionMethodForecast(anomalyDetectionConfiguration);
                timespan = 1000*60*20;
                minimumDatapoints = 1;

                if(period.getLatest() == null)return vdaa;
                if(period.getLatest() - period.getOldest() < timespan) return new ValueDatapoint<?>[1][0];
                datapoints = ArrayUtils.addAll( assetPredictedDatapointService.queryDatapoints(assetId, attributeName,new AssetDatapointAllQuery(period.getLatest(), period.getLatest() + timespan*5)),
                                                assetDatapointService.queryDatapoints(assetId, attributeName,new AssetDatapointAllQuery(period.getLatest() - timespan*5, period.getLatest())));
            }
            default -> {
                return null;
            }
        }

        //check if there are enough datapoints to draw at least 2 limits
        if(datapoints.length < minimumDatapoints+ 2) return new ValueDatapoint<?>[1][datapoints.length];
        ValueDatapoint<?>[][] valueDatapoints = new ValueDatapoint[4][datapoints.length - minimumDatapoints+1];
        List<ValueDatapoint<?>> anomalyDatapoints = new ArrayList<>();


        int index =0;
        long finalTimespan = timespan;
        for(int i = datapoints.length - minimumDatapoints; i >= 0; i--){
            ValueDatapoint<?> dp = datapoints[i];
            if(detectionMethod.checkRecentDataSaved(dp.getTimestamp())){
                double[] values = detectionMethod.GetLimits(datapoints[i]);
                valueDatapoints[0][index] = new ValueDatapoint<>(datapoints[i].getTimestamp(), values[0]);
                valueDatapoints[1][index] = new ValueDatapoint<>(datapoints[i].getTimestamp(), values[1]);
                if((double)datapoints[i].getValue() < values[0] || (double)datapoints[i].getValue() > values[1]){
                    anomalyDatapoints.add(datapoints[i]);
                }
            }else if(type.equals("Global") || type.equals("Change")){
                if(detectionMethod.UpdateData(Arrays.stream(datapoints).filter(p -> p.getTimestamp() > dp.getTimestamp() - finalTimespan && p.getTimestamp() < dp.getTimestamp()).toList())) {
                    double[] values = detectionMethod.GetLimits(datapoints[i]);
                    valueDatapoints[0][index] = new ValueDatapoint<>(datapoints[i].getTimestamp(), values[0]);
                    valueDatapoints[1][index] = new ValueDatapoint<>(datapoints[i].getTimestamp(), values[1]);
                    if ((double) datapoints[i].getValue() < values[0] || (double) datapoints[i].getValue() > values[1]) {
                        anomalyDatapoints.add(datapoints[i]);
                    }
                }
            }else if(type.equals("Forecast")){
                detectionMethod.UpdateData(Arrays.stream(datapoints).filter(p -> p.getTimestamp() > period.getLatest()).toList());
            }
            index++;

        }
        valueDatapoints[3] = new ValueDatapoint[anomalyDatapoints.size()];
        for(int i = 0; i < valueDatapoints[3].length; i++){
            valueDatapoints[3][i] = anomalyDatapoints.get(i);
        }
        valueDatapoints[2] = datapoints;
        return valueDatapoints;
    }

    protected void onAttributeChange(AttributeEvent event) {
        long startMillis = System.currentTimeMillis();
        //only handle events coming from attributes in the with anomaly detection
        if(anomalyDetectionAttributes.containsKey(event.getAssetId() + "$" + event.getAttributeName())){
            AnomalyAttribute anomalyAttribute = anomalyDetectionAttributes.get(event.getAssetId() + "$" + event.getAttributeName());
            anomalyAttribute.validateDatapoint(event.getValue(), event.getTimestamp());
        }
        if (LOG.isLoggable(FINE)) {
            LOG.fine("Attribute Anomaly detection took " + (System.currentTimeMillis() - startMillis) + "ms");
        }
    }

    public class AnomalyAttribute {
        private AttributeRef attributeRef;
        private List<DetectionMethod> detectionMethods;
        public AnomalyAttribute(Asset<?> asset, Attribute<?> attribute) {
            this(asset.getId(), attribute);
        }

        public AnomalyAttribute(String assetId, Attribute<?> attribute) {
            requireNonNullAndNonEmpty(assetId);
            if (attribute == null) {
                throw new IllegalArgumentException("Attribute cannot be null");
            }
            this.attributeRef = new AttributeRef(assetId, attribute.getName());
            this.detectionMethods = new ArrayList<>();
            if(attribute.getMetaItem(ANOMALYDETECTION).isPresent()){
                for (AnomalyDetectionConfiguration con: attribute.getMetaValue(ANOMALYDETECTION).get().methods){
                    switch (con.getClass().getSimpleName()) {
                        case "Global" -> detectionMethods.add(new DetectionMethodGlobal(con));
                        case "Change" -> detectionMethods.add(new DetectionMethodChange(con));
                        case "Timespan" -> detectionMethods.add(new DetectionMethodTimespan(con));
                        case "Forecast" -> detectionMethods.add(new DetectionMethodForecast(con));
                    }
                }
            }
            for (DetectionMethod method: detectionMethods) {
                method.UpdateData(GetDatapoints(method));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnomalyDetectionService.AnomalyAttribute that = (AnomalyDetectionService.AnomalyAttribute) o;
            return attributeRef.getId().equals(that.attributeRef.getId()) && attributeRef.getName().equals(that.attributeRef.getName());
        }

        public String getId() {
            return attributeRef.getId();
        }

        public String getName() {
            return attributeRef.getName();
        }

        public AttributeRef getAttributeRef() {
            return attributeRef;
        }

        public boolean validateDatapoint(Optional<Object> value, long timestamp){
            boolean valid = true;

            for (DetectionMethod method: detectionMethods) {
                if(value.isPresent() && method.config.onOff) {
                    List<ValueDatapoint<?>> datapoints = new ArrayList<>();
                    boolean enoughData = true;
                    if (!method.checkRecentDataSaved(timestamp)) {
                        datapoints = GetDatapoints(method);
                        enoughData = method.UpdateData(datapoints);
                    }
                    if (enoughData)
                        if (!method.validateDatapoint(value.get(), timestamp)) {
                            valid = false;
                            if (method.config.alarmOnOff && method.config.alarm.getSeverity() != null) {
                                String message = method.config.alarm.getContent();

                                message = message.replace("%ASSET_ID%", attributeRef.getId());
                                message = message.replace("%ATTRIBUTE_NAME%", attributeRef.getName());
                                Optional<SentAlarm> existingAlarm = alarmService.getAlarmsByAssetId(attributeRef.getId()).stream().filter(a -> a.getStatus() == Alarm.Status.OPEN
                                        && Objects.equals(a.getSourceId(), attributeRef.getName() + "$" + method.config.name)).findFirst();
                                long alarmId = 0;
                                if (existingAlarm.isEmpty()) {
                                    //create new alarm
                                    Alarm alarm = new Alarm(method.config.name + " Detected 1 anomaly", message + "\n1: " + new Date(timestamp), method.config.alarm.getSeverity(), method.config.alarm.getAssigneeId(), method.config.alarm.getRealm());
                                    SentAlarm sentAlarm = alarmService.sendAlarm(alarm, Alarm.Source.INTERNAL, attributeRef.getName() + "$" + method.config.name);
                                    alarmService.assignUser(sentAlarm.getId(), method.config.alarm.getAssigneeId());
                                    alarmService.linkAssets(new ArrayList<>(Collections.singletonList(attributeRef.getId())), alarm.getRealm(), sentAlarm.getId());
                                    alarmId = sentAlarm.getId();
                                } else {
                                    //update alarm
                                    SentAlarm alarm = existingAlarm.get();
                                    long count = (assetAnomalyDatapointService.countAnomaliesInAlarm(alarm.getId()) + 1);
                                    alarm.setContent(alarm.getContent() + "\n" + count + ": " + new Date(timestamp));
                                    alarm.setLastModified(new Date(timestamp));
                                    alarm.setTitle(method.config.name + " Detected " + count + " anomalies");
                                    alarmService.updateAlarm(alarm.getId(), alarm);
                                    alarmId = alarm.getId();
                                }
                                assetAnomalyDatapointService.updateValue(attributeRef.getId(), attributeRef.getName(), method.config.name, new Date(timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), alarmId);
                            }
                        }
                }
            }
            return  valid;
        }

        public List<ValueDatapoint<?>> GetDatapoints(DetectionMethod detectionMethod){
            DatapointPeriod period = assetDatapointService.getDatapointPeriod(attributeRef.getId(), attributeRef.getName());
            List<ValueDatapoint<?>> datapoints = new ArrayList<>();
            if(period.getLatest() == null) return datapoints;

            AttributeAnomaly[] anomalies = new AttributeAnomaly[0];
            ValueDatapoint<?>[] valueDatapoints = new ValueDatapoint[0];
            if(detectionMethod.config.getClass().getSimpleName().equals("Global")||detectionMethod.config.getClass().getSimpleName().equals("Change")){
                long maxTimespan = 0;
                int maxMinimumDatapoints = 0;
                if(detectionMethod.config.getClass().getSimpleName().equals("Global")){
                    if(((AnomalyDetectionConfiguration.Global)detectionMethod.config).timespan == null) return  datapoints;
                    if( ((AnomalyDetectionConfiguration.Global)detectionMethod.config).timespan.toMillis() > maxTimespan) maxTimespan =  ((AnomalyDetectionConfiguration.Global)detectionMethod.config).timespan.toMillis();
                }else if(detectionMethod.config.getClass().getSimpleName().equals("Change")){
                    if(((AnomalyDetectionConfiguration.Change)detectionMethod.config).timespan == null) return  datapoints;
                    if( ((AnomalyDetectionConfiguration.Change)detectionMethod.config).timespan.toMillis() > maxTimespan) maxTimespan =  ((AnomalyDetectionConfiguration.Change)detectionMethod.config).timespan.toMillis();
                }
                if(period.getLatest() - period.getOldest() < maxTimespan)return datapoints;
                anomalies = assetAnomalyDatapointService.getAnommalies(attributeRef.getId(), attributeRef.getName(),new AssetDatapointAllQuery(period.getLatest()- maxTimespan, period.getLatest()));
                valueDatapoints = assetDatapointService.queryDatapoints(attributeRef.getId(), attributeRef.getName(),new AssetDatapointAllQuery(period.getLatest()- maxTimespan, period.getLatest()));

            }else if(detectionMethod.config.getClass().getSimpleName().equals("Forecast")){
                DatapointPeriod predictedPeriod = assetPredictedDatapointService.getDatapointPeriod(attributeRef.getId(), attributeRef.getName());
                valueDatapoints = assetPredictedDatapointService.queryDatapoints(attributeRef.getId(),attributeRef.getName(), new AssetDatapointAllQuery(period.getOldest(), period.getLatest()+ (period.getLatest()-period.getOldest())));
            }
            return Arrays.stream(valueDatapoints).toList();
        }
    }
}

