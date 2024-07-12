package org.openremote.model.gateway;

import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.AssetQuery;

/**
 * A filter for limiting when {@link org.openremote.model.attribute.AttributeEvent}s are sent to the central instance.
 * Consists of an {@link AssetQuery} and various options for limiting the frequency of sending matched events. The
 * options are OR'ed i.e. time
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "matcher=" + matcher +
            ", duration='" + duration + '\'' +
            ", delta=" + delta +
            ", valueChange=" + valueChange +
            '}';
    }
}
