package org.openremote.model.gateway;

import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TimeUtil;

import java.util.Optional;

/**
 * A filter for limiting when {@link org.openremote.model.attribute.AttributeEvent}s are sent to the central instance.
 * Consists of an {@link AssetQuery} and various options for limiting the frequency of sending matched events. The
 * options are OR'ed except for {@link #skipAlways}. A null {@link #matcher} indicates a match all. If {@link #delta},
 * {@link #duration}, {@link #valueChange} and {@link #skipAlways} are all null then any event that matches will not be
 * filtered. This facilitates an allow exclusion for one or more attributes provided the allow filter is before any other
 * filter that would also match the event.
 */
public class GatewayAttributeFilter {
    /**
     * The {@link AssetQuery} used to determine if an {@link org.openremote.model.attribute.AttributeEvent} matches
     * this filter
     */
    protected AssetQuery matcher;
    /**
     * ISO8601 duration expression (e.g. PT1H)
     */
    protected String duration;
    /**
     * A delta change required only applicable when {@link Attribute#getTypeClass()} is assignable from {@link Number}
     */
    protected Double delta;
    /**
     * Send whenever the value changes in accordance with {@link Object#equals}
     */
    protected Boolean valueChange;
    /**
     * Do not send any updates for matching attributes
     */
    protected Boolean skipAlways;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected Optional<Long> durationParsedMillis;

    public AssetQuery getMatcher() {
        return matcher;
    }

    public GatewayAttributeFilter setMatcher(AssetQuery matcher) {
        this.matcher = matcher;
        return this;
    }

    public String getDuration() {
        return duration;
    }

    public Optional<Long> getDurationParsed() {
        if (durationParsedMillis == null) {
            durationParsedMillis = Optional.empty();
            if (duration != null) {
                try {
                    durationParsedMillis = Optional.of(TimeUtil.parseTimeDuration(duration));
                } catch (RuntimeException e) {
                    durationParsedMillis = Optional.empty();
                }
            }
        }
        return durationParsedMillis;
    }

    public GatewayAttributeFilter setDuration(String duration) {
        this.duration = duration;
        return this;
    }

    public Double getDelta() {
        return delta;
    }

    public GatewayAttributeFilter setDelta(Double delta) {
        this.delta = delta;
        return this;
    }

    public Boolean getValueChange() {
        return valueChange;
    }

    public GatewayAttributeFilter setValueChange(Boolean valueChange) {
        this.valueChange = valueChange;
        return this;
    }

    public Boolean getSkipAlways() {
        return skipAlways;
    }

    public GatewayAttributeFilter setSkipAlways(Boolean skipAlways) {
        this.skipAlways = skipAlways;
        return this;
    }

    public boolean isAllow() {
        return (skipAlways == null || !skipAlways) && (valueChange == null || !valueChange) && delta == null && TextUtil.isNullOrEmpty(duration);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "matcher=" + matcher +
            ", duration='" + duration + '\'' +
            ", delta=" + delta +
            ", valueChange=" + valueChange +
            ", skipAlways=" + skipAlways +
            '}';
    }
}
