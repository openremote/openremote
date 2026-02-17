package org.openremote.agent.protocol.entsoe;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.math.BigDecimal;
import java.util.List;


@XmlRootElement(name = "Publication_MarketDocument", namespace = PublicationMarketDocument.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class PublicationMarketDocument {

    public static final String NS = "urn:iec62325.351:tc57wg16:451-3:publicationdocument:7:3";

    @XmlElement(name = "period.timeInterval", namespace = NS)
    private PeriodTimeInterval periodTimeInterval;

    @XmlElement(name = "TimeSeries", namespace = NS)
    private List<TimeSeries> timeSeries;

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

        @XmlElement(name = "Period", namespace = NS)
        private Period period;

        public Period getPeriod() { return period; }
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

    public List<TimeSeries> getTimeSeries() { return timeSeries; }
    public PeriodTimeInterval getPeriodTimeInterval() { return periodTimeInterval; }
}
