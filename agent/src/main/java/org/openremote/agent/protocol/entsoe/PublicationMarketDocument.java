package org.openremote.agent.protocol.entsoe;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.math.BigDecimal;
import java.util.List;


@XmlRootElement(name = "Publication_MarketDocument", namespace = PublicationMarketDocument.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class PublicationMarketDocument {

    public static final String NS = "urn:iec62325.351:tc57wg16:451-3:publicationdocument:7:3";

    @XmlElement(name = "mRID", namespace = NS)
    private String mRID;

    @XmlElement(name = "revisionNumber", namespace = NS)
    private Integer revisionNumber;

    @XmlElement(name = "type", namespace = NS)
    private String type;

    @XmlElement(name = "sender_MarketParticipant.mRID", namespace = NS)
    private MarketParticipantMRID senderMarketParticipantMRID;

    @XmlElement(name = "sender_MarketParticipant.marketRole.type", namespace = NS)
    private String senderMarketParticipantMarketRoleType;

    @XmlElement(name = "receiver_MarketParticipant.mRID", namespace = NS)
    private MarketParticipantMRID receiverMarketParticipantMRID;

    @XmlElement(name = "receiver_MarketParticipant.marketRole.type", namespace = NS)
    private String receiverMarketParticipantMarketRoleType;

    @XmlElement(name = "createdDateTime", namespace = NS)
    private String createdDateTime; // keep as String; parse to Instant if you want

    @XmlElement(name = "period.timeInterval", namespace = NS)
    private PeriodTimeInterval periodTimeInterval;

    @XmlElement(name = "TimeSeries", namespace = NS)
    private List<TimeSeries> timeSeries;

    // --- Nested types ---

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MarketParticipantMRID {
        @XmlValue
        private String value;

        @XmlAttribute(name = "codingScheme")
        private String codingScheme;

        public String getValue() { return value; }
        public String getCodingScheme() { return codingScheme; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PeriodTimeInterval {
        @XmlElement(name = "start", namespace = NS)
        private String start;

        @XmlElement(name = "end", namespace = NS)
        private String end;

        public String getStart() { return start; }
        public String getEnd() { return end; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TimeSeries {

        @XmlElement(name = "mRID", namespace = NS)
        private String mRID;

        @XmlElement(name = "auction.type", namespace = NS)
        private String auctionType;

        @XmlElement(name = "businessType", namespace = NS)
        private String businessType;

        @XmlElement(name = "in_Domain.mRID", namespace = NS)
        private DomainMRID inDomainMRID;

        @XmlElement(name = "out_Domain.mRID", namespace = NS)
        private DomainMRID outDomainMRID;

        @XmlElement(name = "contract_MarketAgreement.type", namespace = NS)
        private String contractMarketAgreementType;

        @XmlElement(name = "currency_Unit.name", namespace = NS)
        private String currencyUnitName;

        @XmlElement(name = "price_Measure_Unit.name", namespace = NS)
        private String priceMeasureUnitName;

        @XmlElement(name = "classificationSequence_AttributeInstanceComponent.position", namespace = NS)
        private Integer classificationSequencePosition;

        @XmlElement(name = "curveType", namespace = NS)
        private String curveType;

        @XmlElement(name = "Period", namespace = NS)
        private Period period;

        public Period getPeriod() { return period; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DomainMRID {
        @XmlValue
        private String value;

        @XmlAttribute(name = "codingScheme")
        private String codingScheme;

        public String getValue() { return value; }
        public String getCodingScheme() { return codingScheme; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Period {

        @XmlElement(name = "timeInterval", namespace = NS)
        private PeriodTimeInterval timeInterval;

        @XmlElement(name = "resolution", namespace = NS)
        private String resolution; // e.g. PT15M

        @XmlElement(name = "Point", namespace = NS)
        private List<Point> points;

        public PeriodTimeInterval getTimeInterval() { return timeInterval; }
        public String getResolution() { return resolution; }
        public List<Point> getPoints() { return points; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Point {

        @XmlElement(name = "position", namespace = NS)
        private Integer position;

        @XmlElement(name = "price.amount", namespace = NS)
        private BigDecimal priceAmount;

        public Integer getPosition() { return position; }
        public BigDecimal getPriceAmount() { return priceAmount; }
    }

    // --- getters/setters omitted for brevity ---
    public String getMRID() { return mRID; }
    public List<TimeSeries> getTimeSeries() { return timeSeries; }
    public PeriodTimeInterval getPeriodTimeInterval() { return periodTimeInterval; }
    public String getCreatedDateTime() { return createdDateTime; }
}
