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
package org.openremote.manager.asset;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.datapoint.AssetAnomalyDatapointResourceImpl;
import org.openremote.manager.datapoint.AssetAnomalyDatapointService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.*;
import org.openremote.model.datapoint.*;
import org.openremote.model.datapoint.query.AssetDatapointAllAnomaliesQuery;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.value.AnomalyDetectionConfigObject;
import org.openremote.model.value.AnomalyDetectionConfiguration;
import org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage;
import org.openremote.model.value.MetaItemType;
import  org.openremote.model.attribute.AttributeAnomaly.AnomalyType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.model.attribute.Attribute.getAddedOrModifiedAttributes;
import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;
import static org.openremote.model.value.MetaItemType.*;

/**
 * Calculates forecast values for asset attributes with an attached {@link MetaItemType#FORECAST}
 * configuration like {@link ForecastConfigurationWeightedExponentialAverage}.
 */
public class AnomalyDetectionService extends RouteBuilder implements ContainerService, EventListener{

    private static final Logger LOG = Logger.getLogger(AnomalyDetectionService.class.getName());
    private static long STOP_TIMEOUT = Duration.ofSeconds(5).toMillis();
    private List<Asset<?>> anomalyDetectionAssets;
    private HashMap<String, AnomalyAttribute> anomalyDetectionAttributes;


    protected GatewayService gatewayService;
    protected AssetStorageService assetStorageService;
    protected ClientEventService clientEventService;
    protected AssetDatapointService assetDatapointService;
    protected ScheduledExecutorService executorService;
    protected AssetAnomalyDatapointService assetAnomalyDatapointService;

    @Override
    public void init(Container container) throws Exception {
        anomalyDetectionAssets = new ArrayList<>();
        anomalyDetectionAttributes = new HashMap<>();

        gatewayService = container.getService(GatewayService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        clientEventService = container.getService(ClientEventService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
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

    public ValueDatapoint<?>[][] getAnomalyDatapointLimits( String assetId, String attributeName, AnomalyDetectionConfiguration anomalyDetectionConfiguration) {
        ValueDatapoint<?> vd = new ValueDatapoint<>(1699448760,2);
        ValueDatapoint<?>[] vda = new ValueDatapoint<?>[]{vd,vd,vd};
        ValueDatapoint<?>[][] vdaa = new ValueDatapoint<?>[][]{vda,vda};

        DetectionMethod detectionMethod;
        if(anomalyDetectionConfiguration != null) {
            switch (anomalyDetectionConfiguration.getClass().getSimpleName()) {
                case "Global" -> detectionMethod = new DetectionMethodGlobal(anomalyDetectionConfiguration);
                case "Change" -> detectionMethod = new DetectionMethodChange(anomalyDetectionConfiguration);
                case "Timespan" -> detectionMethod = new DetectionMethodTimespan(anomalyDetectionConfiguration);
                default -> {
                    return vdaa;
                }
            }
        }else{
            return vdaa;
        }
        if(anomalyDetectionAttributes.containsKey(assetId + "$" + attributeName)){
            AnomalyAttribute anomalyAttribute = anomalyDetectionAttributes.get(assetId + "$" + attributeName);
            DatapointPeriod period = assetDatapointService.getDatapointPeriod(assetId, attributeName);
            if(period.getLatest() - period.getOldest() > detectionMethod.config.timespan.toMillis()){
                List<AssetAnomalyDatapoint> datapoints = anomalyAttribute.GetDatapoints(period.getLatest() - detectionMethod.config.timespan.toMillis()*5,period.getLatest());
                ValueDatapoint<?>[][] valueDatapoints = new ValueDatapoint[2][datapoints.size()-detectionMethod.config.minimumDatapoints+1];
                int index =0;
                for(int i = datapoints.size() - detectionMethod.config.minimumDatapoints; i >= 0; i--){
                    AssetAnomalyDatapoint dp = datapoints.get(i);
                    if(!detectionMethod.checkRecentDataSaved(dp.getTimestamp())){
                       detectionMethod.UpdateData(datapoints.stream().filter(p -> p.getTimestamp() > dp.getTimestamp() - detectionMethod.config.timespan.toMillis() && p.getTimestamp() < dp.getTimestamp()).toList());
                    }
                    double[] values = detectionMethod.GetLimits(datapoints.get(i));
                    valueDatapoints[0][index] = new ValueDatapoint<>(datapoints.get(i).getTimestamp(), values[0]);
                    valueDatapoints[1][index] = new ValueDatapoint<>(datapoints.get(i).getTimestamp(), values[1]);
                    index++;
                }
                return valueDatapoints;

            }
        }
        return vdaa;
    }

    protected void onAttributeChange(AttributeEvent event) {
        //only handle events coming from attributes in the with anomaly detection
        AnomalyType anomalyType = AnomalyType.Unchecked;
        if(anomalyDetectionAttributes.containsKey(event.getAssetId() + "$" + event.getAttributeName())){
            AnomalyAttribute anomalyAttribute = anomalyDetectionAttributes.get(event.getAssetId() + "$" + event.getAttributeName());
            anomalyType = anomalyAttribute.validateDatapoint(event.getValue(), event.getTimestamp());
            assetAnomalyDatapointService.updateValue(anomalyAttribute.getId(), anomalyAttribute.getName(),anomalyType, event.timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), anomalyAttribute);
        }
    }

    public class AnomalyAttribute {
        private AttributeRef attributeRef;
        private List<DetectionMethod> detectionMethods;
        public boolean hasEnoughDatapoints;
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
            hasEnoughDatapoints = false;
            List<AssetAnomalyDatapoint> dtapoints = GetDatapoints();
            if(dtapoints.size() >1) hasEnoughDatapoints = true;
            if(attribute.getMetaItem(ANOMALYDETECTION).isPresent()){
                for (AnomalyDetectionConfiguration con: attribute.getMetaValue(ANOMALYDETECTION).get().methods){
                    switch (con.getClass().getSimpleName()) {
                        case "Global" -> detectionMethods.add(new DetectionMethodGlobal(con));
                        case "Change" -> detectionMethods.add(new DetectionMethodChange(con));
                        case "Timespan" -> detectionMethods.add(new DetectionMethodTimespan(con));
                    }
                }
            }

            for (DetectionMethod method: detectionMethods) {
                if(hasEnoughDatapoints) method.UpdateData(dtapoints);
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

        public AnomalyType validateDatapoint(Optional<Object> value, long timestamp){
            AnomalyType anomalyType = AnomalyType.Unchecked;
            int anomalyCount = 0;

            for (DetectionMethod method: detectionMethods) {
                if(value.isPresent() && method.config.onOff){
                    if(hasEnoughDatapoints){
                        if (!method.validateDatapoint(value.get(),timestamp)) {
                            anomalyCount++;
                            anomalyType = method.anomalyType;
                        }
                    }else{
                        anomalyCount = -1;
                        List<AssetAnomalyDatapoint> datapoints = GetDatapoints();
                        if(datapoints.size() > 1){
                            method.UpdateData(datapoints);
                            hasEnoughDatapoints = true;
                        }
                    }
                    if(!method.checkRecentDataSaved(timestamp)){
                        List<AssetAnomalyDatapoint> datapoints = GetDatapoints();
                        if(datapoints.size() > 1){
                            method.UpdateData(datapoints);
                            hasEnoughDatapoints = true;
                        }else{
                            hasEnoughDatapoints = false;
                        }
                    }
                }
            }
            if(anomalyCount == 0) anomalyType = AnomalyType.Valid;
            if(anomalyCount > 1) anomalyType = AnomalyType.Multiple;

            return  anomalyType;
        }

        public List<AssetAnomalyDatapoint> GetDatapoints(){
            DatapointPeriod period = assetDatapointService.getDatapointPeriod(attributeRef.getId(), attributeRef.getName());
            List<AssetAnomalyDatapoint> datapoints = new ArrayList<>();
            if(period.getLatest() == null) return datapoints;
            long maxTimespan = 0;
            int maxMinimumDatapoints = 0;
            for (DetectionMethod detectionMethod: detectionMethods){
                if(detectionMethod.config.timespan.toMillis() > maxTimespan) maxTimespan = detectionMethod.config.timespan.toMillis();
                if(detectionMethod.config.minimumDatapoints > maxMinimumDatapoints) maxMinimumDatapoints = detectionMethod.config.minimumDatapoints;
            }
            //test if there are enough datapoints for all detection methods
            if(period.getLatest() - period.getOldest() < maxTimespan)return datapoints;
            AttributeAnomaly[] anomalies = assetAnomalyDatapointService.getAnommalies(attributeRef.getId(), attributeRef.getName(),new AssetDatapointAllQuery(period.getLatest()- maxTimespan, period.getLatest()));
            for (ValueDatapoint<?> datapoint: assetDatapointService.queryDatapoints(attributeRef.getId(), attributeRef.getName(),new AssetDatapointAllQuery(period.getLatest()- maxTimespan, period.getLatest() ))) {
                Optional<AttributeAnomaly> anomaly = Arrays.stream(anomalies).filter(a -> a.getTimestamp().getTime() == datapoint.getTimestamp()).findFirst();
                if(anomaly.isPresent()){
                    datapoints.add(new AssetAnomalyDatapoint(attributeRef,datapoint.getValue(),datapoint.getTimestamp(),anomaly.get().getAnomalyType()));
                }else{
                    datapoints.add(new AssetAnomalyDatapoint(attributeRef,datapoint.getValue(),datapoint.getTimestamp(),AnomalyType.Unchecked));
                }
            }
            if(datapoints.size() < maxMinimumDatapoints) return new ArrayList<>();
            return datapoints;
        }
        public List<AssetAnomalyDatapoint> GetDatapoints(long start, long end){
            List<AssetAnomalyDatapoint> datapoints = new ArrayList<>();
            AttributeAnomaly[] anomalies = assetAnomalyDatapointService.getAnommalies(attributeRef.getId(), attributeRef.getName(),new AssetDatapointAllQuery(start, end));
            for (ValueDatapoint<?> datapoint: assetDatapointService.queryDatapoints(attributeRef.getId(), attributeRef.getName(),new AssetDatapointAllQuery(start, end))) {
                Optional<AttributeAnomaly> anomaly = Arrays.stream(anomalies).filter(a -> a.getTimestamp().getTime() == datapoint.getTimestamp()).findFirst();
                if(anomaly.isPresent()){
                    datapoints.add(new AssetAnomalyDatapoint(attributeRef,datapoint.getValue(),datapoint.getTimestamp(),anomaly.get().getAnomalyType()));
                }else{
                    datapoints.add(new AssetAnomalyDatapoint(attributeRef,datapoint.getValue(),datapoint.getTimestamp(),AnomalyType.Unchecked));
                }
            }
            return datapoints;
        }
        public void UpdateAnomalyDatapoint(String assetId, String attributeName, AnomalyType type, LocalDateTime timestamp){
            assetAnomalyDatapointService.updateValue(assetId, attributeName, type, timestamp, null);
        }
    }

    private abstract class DetectionMethod implements IDetectionMethod{
        public AnomalyDetectionConfiguration config;
        public AnomalyType anomalyType;
        public  DetectionMethod(AnomalyDetectionConfiguration config){
            this.config = config;
        }
    }
    private interface IDetectionMethod{
        /**Check if value is valid according to the methods rules. */
        boolean validateDatapoint(Object value, long timestamp);
        /**Update needsNewData based on needs method */
        boolean checkRecentDataSaved(long latestTimestamp);
        /**Update saved values used to calculate Limits */
        void UpdateData(List<AssetAnomalyDatapoint> datapoints);
        double[] GetLimits(AssetAnomalyDatapoint datapoint);
    }


    private class DetectionMethodGlobal extends DetectionMethod{
        private double minValue;
        private long minValueTimestamp;
        private double maxValue;
        private long maxValueTimestamp;


        public DetectionMethodGlobal(AnomalyDetectionConfiguration config){
            super(config);
            anomalyType = AnomalyType.GlobalOutlier;
        }


        public boolean validateDatapoint(Object value, long timestamp) {
            double differance = maxValue - minValue + 0.001;
            double deviation = differance * ((double)config.deviation /100);

            double val = (double)value;
            boolean valid = true;
            if(val < minValue - deviation){
                valid = false;
            }
            if(val > maxValue + deviation){
                valid = false;
            }
            if(valid){
                if(val >= maxValue){
                    maxValue = val;
                    maxValueTimestamp = timestamp;
                }
                if(val <= minValue){
                    minValue = val;
                    minValueTimestamp =timestamp;
                }
            }
            return valid;
        }


        public boolean checkRecentDataSaved(long latestTimestamp) {
            boolean needsNewData = false;
            long timeMillis =config.timespan.toMillis();
            if(minValueTimestamp < latestTimestamp - timeMillis
            || maxValueTimestamp < latestTimestamp - timeMillis){
                needsNewData = true;
            }
            return !needsNewData;
        }

        public void UpdateData(List<AssetAnomalyDatapoint> datapoints) {
            minValue = Double.MAX_VALUE;
            maxValue = (double)datapoints.get(0).getValue();
            for (AssetAnomalyDatapoint dtapoint : datapoints) {
                if(dtapoint.anomalyType == AnomalyType.Unchecked || dtapoint.anomalyType == AnomalyType.Valid){
                    if((double)dtapoint.getValue() <= minValue){
                        minValue = (double)dtapoint.getValue();
                        minValueTimestamp = dtapoint.getTimestamp();
                    }
                    if((double)dtapoint.getValue() >= maxValue){
                        maxValue = (double)dtapoint.getValue();
                        maxValueTimestamp = dtapoint.getTimestamp();
                    }
                }
            }
        }

        @Override
        public double[] GetLimits(AssetAnomalyDatapoint datapoint) {
            double differance = maxValue - minValue + 0.001;;
            double deviation = differance * ((double)config.deviation /100);
            double[] limits = new double[]{minValue - deviation, maxValue + deviation};
            double value = (double)datapoint.getValue();
            if(minValue > value) minValue = value;
            if(maxValue < value) maxValue = value;
            return limits;
        }
    }
    private class DetectionMethodChange extends DetectionMethod{

        double biggestIncrease;
        long biggestIncreaseTimestamp;
        double smallestIncrease;
        long smallestIncreaseTimestamp;
        double previousValue;
        long previousValueTimestamp;

        public DetectionMethodChange(AnomalyDetectionConfiguration config){
            super(config);
            anomalyType = AnomalyType.ContextualOutlier;
        }

        public boolean validateDatapoint(Object value, long timestamp) {
            double increase = ((double)value - previousValue);

            boolean valid = true;
            double offset = 0;
            offset = biggestIncrease* ((double)config.deviation/100);
            if(increase > biggestIncrease + offset){
                valid = false;
            }
            offset = smallestIncrease* ((double)config.deviation/100);
            if(increase < smallestIncrease - offset){
                valid = false;
            }
            if(valid){
                if(increase <= smallestIncrease){
                    smallestIncrease = increase;
                    smallestIncreaseTimestamp = timestamp;
                }
                if(increase>= biggestIncrease){
                    biggestIncrease = increase;
                    biggestIncreaseTimestamp = timestamp;
                }
            }
            previousValue = (double)value;
            previousValueTimestamp = timestamp;
            return valid;
        }

        public boolean checkRecentDataSaved(long latestTimestamp) {
            boolean needsNewData = false;
            if(smallestIncreaseTimestamp < latestTimestamp - config.timespan.toMillis()
                    || biggestIncreaseTimestamp < latestTimestamp - config.timespan.toMillis()){
                needsNewData = true;
            }
            return !needsNewData;
        }

        @Override
        public void UpdateData(List<AssetAnomalyDatapoint> datapoints) {

            if(datapoints.size() <2) return;
            smallestIncrease = Double.MAX_VALUE;
            biggestIncrease = (double)datapoints.get(0).getValue() - (double)datapoints.get(1).getValue();
            for(int i = 1; i < datapoints.size(); i++){
                if(datapoints.get(i).anomalyType == AnomalyType.Unchecked || datapoints.get(i).anomalyType == AnomalyType.Valid) {

                }
               double increase = (double)datapoints.get(i-1).getValue() - (double)datapoints.get(i).getValue();
               long timestamp = datapoints.get(i).getTimestamp();

                if(increase <= smallestIncrease){
                    smallestIncrease = increase;
                    smallestIncreaseTimestamp = timestamp;
                }
                if(increase>= biggestIncrease){
                    biggestIncrease = increase;
                    biggestIncreaseTimestamp = timestamp;
                }
            }
            previousValue = (double)datapoints.get(0).getValue();
            previousValueTimestamp = datapoints.get(0).getTimestamp();

        }

        @Override
        public double[] GetLimits(AssetAnomalyDatapoint datapoint) {
            double increase = ((double)datapoint.getValue() - previousValue);
            double bigoffset =  biggestIncrease * ((double)config.deviation/100);
            double smalloffset = smallestIncrease * ((double)config.deviation/100);

            double[] limits = new double[]{previousValue + smallestIncrease+ smalloffset, previousValue + biggestIncrease +bigoffset};

            previousValue = (double)datapoint.getValue();
            if(increase <= smallestIncrease){
                smallestIncrease = increase;
                smallestIncreaseTimestamp = datapoint.getTimestamp();
            }
            if(increase>= biggestIncrease){
                biggestIncrease = increase;
                biggestIncreaseTimestamp = datapoint.getTimestamp();
            }
            return limits;
        }
    }

    private class DetectionMethodTimespan extends DetectionMethod{
        long longestTimespan;
        long longestTimespanTimestamp;
        long shortestTimespan;
        long shortestTimespanTimestamp;
        long previousValueTimestamp;

        public DetectionMethodTimespan(AnomalyDetectionConfiguration config){
            super(config);
            anomalyType = AnomalyType.IrregularInterval;
        }

        @Override
        public boolean validateDatapoint(Object value, long timestamp) {
            long timespan = timestamp - previousValueTimestamp;
            boolean valid = true;
            long offset = 0;
            offset = (long)((longestTimespan - shortestTimespan+1)* ((double)config.deviation/100));
            if(offset < 0) offset *= -1;
            if(timespan > longestTimespan + offset){
                valid = false;
            }
            if(valid){
                if(timespan <= shortestTimespan){
                    shortestTimespan = timespan;
                    shortestTimespanTimestamp = timestamp;
                }
                if(timespan >= longestTimespan){
                    longestTimespan = timespan;
                    longestTimespanTimestamp = timestamp;
                }
            }
            previousValueTimestamp = timestamp;
            return valid;
        }

        @Override
        public boolean checkRecentDataSaved(long latestTimestamp) {
            boolean needsNewData = false;
            if(longestTimespanTimestamp < latestTimestamp - config.timespan.toMillis()
                    || shortestTimespanTimestamp < latestTimestamp - config.timespan.toMillis()){
                needsNewData = true;
            }
            return !needsNewData;
        }

        @Override
        public void UpdateData(List<AssetAnomalyDatapoint> datapoints) {

            if(datapoints.size() <2) return;
            shortestTimespan = datapoints.get(0).getTimestamp();
            longestTimespan = 0;
            for(int i = 1; i < datapoints.size(); i++){
                long timespan = datapoints.get(i-1).getTimestamp() - datapoints.get(i).getTimestamp();
                long timestamp = datapoints.get(i).getTimestamp();

                if(timespan <= shortestTimespan){
                    shortestTimespan = timespan;
                    shortestTimespanTimestamp = timestamp;
                }
                if(timespan >= longestTimespan){
                    longestTimespan = timespan;
                    longestTimespanTimestamp = timestamp;
                }
            }
            previousValueTimestamp = datapoints.get(0).getTimestamp();
        }

        @Override
        public double[] GetLimits(AssetAnomalyDatapoint datapoint) {
            return new double[0];
        }
    }
}

