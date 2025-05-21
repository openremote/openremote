package org.openremote.manager.graphql;
import io.leangen.graphql.metadata.strategy.query.DefaultOperationInfoGenerator;
import io.leangen.graphql.metadata.strategy.query.OperationInfoGenerator;
import io.leangen.graphql.metadata.strategy.query.OperationInfoGeneratorParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Introspector;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static com.google.common.base.CaseFormat.*;

public class SnakeCaseOperationNameGenerator implements OperationInfoGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SnakeCaseOperationNameGenerator.class);
    private final OperationInfoGenerator delegate;

    public SnakeCaseOperationNameGenerator() {
        this.delegate = new DefaultOperationInfoGenerator();
    }

    public SnakeCaseOperationNameGenerator(OperationInfoGenerator delegate) {
        this.delegate = delegate;
    }

    @Override
    public String name(OperationInfoGeneratorParams params) {
        String originalName = delegate.name(params);
        if (originalName == null) {
            return null;
        }

        // Handle JsonSubTypes names that contain hyphens
        if (originalName.contains("-")) {
            // Convert hyphenated names to snake case
            // e.g. "calendar-event" -> "calendar_event"
            return originalName.replace('-', '_');
        }

        // For regular camelCase names, convert to snake_case
        if (originalName.matches(".*[a-z][A-Z].*")) {
            return LOWER_CAMEL.to(LOWER_UNDERSCORE, originalName);
        }

        // If it's already in a valid format, return as is
        return originalName;
    }

    @Override
    public String description(OperationInfoGeneratorParams params) {
        return delegate.description(params);
    }

    @Override
    public String deprecationReason(OperationInfoGeneratorParams params) {
        return delegate.deprecationReason(params);
    }
}
