/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonValue;
import org.openremote.model.attribute.Attribute;

import java.io.Serializable;

/**
 * Represents formatting rules to apply to date and number values when converting to {@link String} representation; based on
 * HTML Intl API, see: <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl">here</a>.
 * {@link ValueFormat}s should be merged for UI consumption in the following priority order:
 * <ol>
 * <li>{@link Attribute} {@link MetaItemType#FORMAT}</li>
 * <li>{@link AttributeDescriptor#getFormat}/{@link MetaItemDescriptor#getFormat}</li>
 * <li>{@link ValueDescriptor#getFormat}</li>
 * </ol>
 */
public class ValueFormat implements Serializable {

    public enum StyleRepresentation {
        NUMERIC("numeric"),
        DIGIT_2("2-digit"),
        FULL("full"),
        LONG("long"),
        MEDIUM("medium"),
        SHORT("short"),
        NARROW("narrow");

        protected final String value;

        StyleRepresentation(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public static ValueFormat NUMBER_0_DP() { return new ValueFormat().setMaximumFractionDigits(0); }
    public static ValueFormat NUMBER_1_DP() { return new ValueFormat().setMinimumFractionDigits(1).setMaximumFractionDigits(1); }
    public static ValueFormat NUMBER_2_DP() { return new ValueFormat().setMinimumFractionDigits(2).setMaximumFractionDigits(2); }
    public static ValueFormat NUMBER_3_DP() { return new ValueFormat().setMinimumFractionDigits(3).setMaximumFractionDigits(3); }
    public static ValueFormat NUMBER_1_DP_MAX() { return new ValueFormat().setMaximumFractionDigits(1); }
    public static ValueFormat NUMBER_2_DP_MAX() { return new ValueFormat().setMaximumFractionDigits(2); }
    public static ValueFormat NUMBER_3_DP_MAX() { return new ValueFormat().setMaximumFractionDigits(3); }
    public static ValueFormat DATE_DAY_MONTH_YEAR() { return new ValueFormat(); }
    public static ValueFormat DATE_WEEK_NUMBER() { return new ValueFormat().setWeek(StyleRepresentation.NUMERIC); }
    public static ValueFormat DATE_DAY_MONTH_YEAR_TIME_WITHOUT_SECOND() { return new ValueFormat(); }
    public static ValueFormat DATE_DAY_MONTH_YEAR_TIME_WITH_SECONDS() { return new ValueFormat(); }
    public static ValueFormat DATE_DATE_TIME_WITHOUT_SECONDS() { return new ValueFormat(); }
    public static ValueFormat DATE_DATE_TIME_WITH_SECONDS() { return new ValueFormat(); }
    public static ValueFormat DATE_DATE_ISO8601() { return new ValueFormat().setIso8601(true); }
    public static ValueFormat BOOLEAN_ON_OFF() { return new ValueFormat().setAsOnOff(true); }
    public static ValueFormat BOOLEAN_AS_0_1() { return new ValueFormat().setAsNumber(true); }
    public static ValueFormat BOOLEAN_AS_OPEN_CLOSED() { return new ValueFormat().setAsOpenClosed(true); }
    public static ValueFormat BOOLEAN_AS_PRESSED_RELEASED() { return new ValueFormat().setAsPressedReleased(true); }
    public static ValueFormat TEXT_MULTILINE() { return new ValueFormat().setMultiline(true); }

    /* NUMBER FORMATS */
    protected Boolean useGrouping;
    protected Integer minimumIntegerDigits;
    protected Integer minimumFractionDigits;
    protected Integer maximumFractionDigits;
    protected Integer minimumSignificantDigits;
    protected Integer maximumSignificantDigits;
    protected Boolean asBoolean;
    protected Boolean asDate;
    protected Boolean asSlider;
    protected Number resolution;

    /* DATE FORMATS */
    protected StyleRepresentation dateStyle;
    protected StyleRepresentation timeStyle;
    protected StyleRepresentation dayPeriod;
    protected Boolean hour12;
    protected Boolean iso8601;
    protected StyleRepresentation weekday;
    protected StyleRepresentation era;
    protected StyleRepresentation year;
    protected StyleRepresentation month;
    protected StyleRepresentation week;
    protected StyleRepresentation day;
    protected StyleRepresentation hour;
    protected StyleRepresentation minute;
    protected StyleRepresentation second;
    protected Integer fractionalSecondDigits;
    protected StyleRepresentation timeZoneName;
    protected String momentJsFormat;

    /* BOOLEAN FORMATS */
    protected Boolean asNumber;
    protected Boolean asOnOff;
    protected Boolean asPressedReleased;
    protected Boolean asOpenClosed;
    protected Boolean asMomentary;

    /* TEXT FORMATS */
    protected Boolean multiline;

    public Boolean getUseGrouping() {
        return useGrouping;
    }

    public ValueFormat setUseGrouping(Boolean useGrouping) {
        this.useGrouping = useGrouping;
        return this;
    }

    public Integer getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    public ValueFormat setMinimumIntegerDigits(Integer minimumIntegerDigits) {
        this.minimumIntegerDigits = minimumIntegerDigits;
        return this;
    }

    public Integer getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    public ValueFormat setMinimumFractionDigits(Integer minimumFractionDigits) {
        this.minimumFractionDigits = minimumFractionDigits;
        return this;
    }

    public Integer getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    public ValueFormat setMaximumFractionDigits(Integer maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
        return this;
    }

    public Integer getMinimumSignificantDigits() {
        return minimumSignificantDigits;
    }

    public ValueFormat setMinimumSignificantDigits(Integer minimumSignificantDigits) {
        this.minimumSignificantDigits = minimumSignificantDigits;
        return this;
    }

    public Integer getMaximumSignificantDigits() {
        return maximumSignificantDigits;
    }

    public ValueFormat setMaximumSignificantDigits(Integer maximumSignificantDigits) {
        this.maximumSignificantDigits = maximumSignificantDigits;
        return this;
    }

    public StyleRepresentation getDateStyle() {
        return dateStyle;
    }

    public ValueFormat setDateStyle(StyleRepresentation dateStyle) {
        this.dateStyle = dateStyle;
        return this;
    }

    public StyleRepresentation getTimeStyle() {
        return timeStyle;
    }

    public ValueFormat setTimeStyle(StyleRepresentation timeStyle) {
        this.timeStyle = timeStyle;
        return this;
    }

    public StyleRepresentation getDayPeriod() {
        return dayPeriod;
    }

    public ValueFormat setDayPeriod(StyleRepresentation dayPeriod) {
        this.dayPeriod = dayPeriod;
        return this;
    }

    public Boolean getHour12() {
        return hour12;
    }

    public ValueFormat setHour12(Boolean hour12) {
        this.hour12 = hour12;
        return this;
    }

    public StyleRepresentation getWeekday() {
        return weekday;
    }

    public ValueFormat setWeekday(StyleRepresentation weekday) {
        this.weekday = weekday;
        return this;
    }

    public StyleRepresentation getEra() {
        return era;
    }

    public ValueFormat setEra(StyleRepresentation era) {
        this.era = era;
        return this;
    }

    public StyleRepresentation getYear() {
        return year;
    }

    public ValueFormat setYear(StyleRepresentation year) {
        this.year = year;
        return this;
    }

    public StyleRepresentation getMonth() {
        return month;
    }

    public ValueFormat setMonth(StyleRepresentation month) {
        this.month = month;
        return this;
    }

    public StyleRepresentation getWeek() {
        return week;
    }

    public ValueFormat setWeek(StyleRepresentation week) {
        this.week = week;
        return this;
    }

    public StyleRepresentation getDay() {
        return day;
    }

    public ValueFormat setDay(StyleRepresentation day) {
        this.day = day;
        return this;
    }

    public StyleRepresentation getHour() {
        return hour;
    }

    public ValueFormat setHour(StyleRepresentation hour) {
        this.hour = hour;
        return this;
    }

    public StyleRepresentation getMinute() {
        return minute;
    }

    public ValueFormat setMinute(StyleRepresentation minute) {
        this.minute = minute;
        return this;
    }

    public StyleRepresentation getSecond() {
        return second;
    }

    public ValueFormat setSecond(StyleRepresentation second) {
        this.second = second;
        return this;
    }

    public Integer getFractionalSecondDigits() {
        return fractionalSecondDigits;
    }

    public ValueFormat setFractionalSecondDigits(Integer fractionalSecondDigits) {
        this.fractionalSecondDigits = fractionalSecondDigits;
        return this;
    }

    public StyleRepresentation getTimeZoneName() {
        return timeZoneName;
    }

    public ValueFormat setTimeZoneName(StyleRepresentation timeZoneName) {
        this.timeZoneName = timeZoneName;
        return this;
    }

    public Boolean getIso8601() {
        return iso8601;
    }

    public ValueFormat setIso8601(Boolean iso8601) {
        this.iso8601 = iso8601;
        return this;
    }

    public Boolean getAsBoolean() {
        return asBoolean;
    }

    public ValueFormat setAsBoolean(Boolean asBoolean) {
        this.asBoolean = asBoolean;
        return this;
    }

    public Boolean getAsNumber() {
        return asNumber;
    }

    public ValueFormat setAsNumber(Boolean asNumber) {
        this.asNumber = asNumber;
        return this;
    }

    public Boolean getAsOnOff() {
        return asOnOff;
    }

    public ValueFormat setAsOnOff(Boolean asOnOff) {
        this.asOnOff = asOnOff;
        return this;
    }

    public Boolean getAsPressedReleased() {
        return asPressedReleased;
    }

    public ValueFormat setAsPressedReleased(Boolean asPressedReleased) {
        this.asPressedReleased = asPressedReleased;
        return this;
    }

    public Boolean getAsOpenClosed() {
        return asOpenClosed;
    }

    public ValueFormat setAsOpenClosed(Boolean asOpenClosed) {
        this.asOpenClosed = asOpenClosed;
        return this;
    }

    public Boolean getAsDate() {
        return asDate;
    }

    public ValueFormat setAsDate(Boolean asDate) {
        this.asDate = asDate;
        return this;
    }

    public String getMomentJsFormat() {
        return momentJsFormat;
    }

    public ValueFormat setMomentJsFormat(String momentJsFormat) {
        this.momentJsFormat = momentJsFormat;
        return this;
    }

    public Boolean getAsMomentary() {
        return asMomentary;
    }

    public ValueFormat setAsMomentary(Boolean asMomentary) {
        this.asMomentary = asMomentary;
        return this;
    }

    public Number getResolution() {
        return resolution;
    }

    public ValueFormat setResolution(Number resolution) {
        this.resolution = resolution;
        return this;
    }

    public Boolean getAsSlider() {
        return asSlider;
    }

    public ValueFormat setAsSlider(Boolean asSlider) {
        this.asSlider = asSlider;
        return this;
    }

    public Boolean getMultiine() {
        return multiline;
    }

    public ValueFormat setMultiline(Boolean multiline) {
        this.multiline = multiline;
        return this;
    }
}
