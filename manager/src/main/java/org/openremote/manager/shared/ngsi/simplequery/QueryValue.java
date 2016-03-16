package org.openremote.manager.shared.ngsi.simplequery;

public interface QueryValue {

    String SEPARATOR = ",";
    String SEPARATOR_QUOTE = "'";
    String RANGE = "..";

    @Override
    String toString();

    static QueryValue exact(Object value) {
        return new QueryValue() {
            @Override
            public String toString() {
                return quote(value.toString());
            }
        };
    }

    static QueryValue values(Object[] values) {
        String value = "";
        if (values != null) {
            StringBuilder sb = new StringBuilder();
            for (Object v : values) {
                sb.append(quote(v.toString()));
                sb.append(SEPARATOR);
            }
            if (sb.length() > 0)
                sb.deleteCharAt(sb.length() - 1);
            value = sb.toString();
        }
        final String finalValue = value;
        return new QueryValue() {
            @Override
            public String toString() {
                return finalValue;
            }
        };
    }

    static QueryValue range(Number min, Number max) {
        final String value = min + RANGE + max;
        return new QueryValue() {
            @Override
            public String toString() {
                return value;
            }
        };
    }

    // TODO Java8 Instant not supported on GWT, also no Dateformatter...
    static QueryValue rangeISO8601(String minTimestamp, String maxTimestamp) {
        final String value = minTimestamp+ RANGE + maxTimestamp;
        return new QueryValue() {
            @Override
            public String toString() {
                return value;
            }
        };
    }

    static String quote(String value) {
        if (value.contains(SEPARATOR_QUOTE))
            throw new UnsupportedOperationException(
                "Can't express query values containing " + SEPARATOR_QUOTE + ". No escape character has been specified in NGSIv2."
            );

        if (value.contains(SEPARATOR)) {
            return SEPARATOR_QUOTE + value + SEPARATOR_QUOTE;
        }
        return value;
    }
}
