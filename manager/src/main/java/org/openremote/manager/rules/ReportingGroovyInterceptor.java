/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.manager.rules;

import org.kohsuke.groovy.sandbox.GroovyInterceptor;
import org.openremote.model.rules.Ruleset;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportingGroovyInterceptor extends GroovyInterceptor {

    private static final Logger LOG = Logger.getLogger(ReportingGroovyInterceptor.class.getName());

    protected final GroovySandboxReporter reporter;
    protected final Ruleset ruleset;
    protected final GroovySandboxPhase phase;

    public ReportingGroovyInterceptor(GroovySandboxReporter reporter, Ruleset ruleset, GroovySandboxPhase phase) {
        this.reporter = Objects.requireNonNull(reporter, "reporter");
        this.ruleset = Objects.requireNonNull(ruleset, "ruleset");
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    @Override
    public Object onMethodCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
        report(GroovySandboxOperation.METHOD, GroovySandboxSignature.typeName(receiver), method, args);
        return invoker.call(receiver, method, args);
    }

    @Override
    public Object onStaticCall(Invoker invoker, Class receiver, String method, Object... args) throws Throwable {
        report(GroovySandboxOperation.STATIC_METHOD, GroovySandboxSignature.typeName(receiver), method, args);
        return invoker.call(receiver, method, args);
    }

    @Override
    public Object onNewInstance(Invoker invoker, Class receiver, Object... args) throws Throwable {
        report(GroovySandboxOperation.CONSTRUCTOR, GroovySandboxSignature.typeName(receiver), "<init>", args);
        return invoker.call(receiver, null, args);
    }

    @Override
    public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
        report(GroovySandboxOperation.GET_PROPERTY, GroovySandboxSignature.typeName(receiver), property);
        return invoker.call(receiver, property);
    }

    @Override
    public Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
        report(GroovySandboxOperation.SET_PROPERTY, GroovySandboxSignature.typeName(receiver), property, value);
        return invoker.call(receiver, property, value);
    }

    @Override
    public Object onGetAttribute(Invoker invoker, Object receiver, String attribute) throws Throwable {
        report(GroovySandboxOperation.GET_ATTRIBUTE, GroovySandboxSignature.typeName(receiver), attribute);
        return invoker.call(receiver, attribute);
    }

    @Override
    public Object onSetAttribute(Invoker invoker, Object receiver, String attribute, Object value) throws Throwable {
        report(GroovySandboxOperation.SET_ATTRIBUTE, GroovySandboxSignature.typeName(receiver), attribute, value);
        return invoker.call(receiver, attribute, value);
    }

    @Override
    public Object onGetArray(Invoker invoker, Object receiver, Object index) throws Throwable {
        report(GroovySandboxOperation.GET_ARRAY, GroovySandboxSignature.typeName(receiver), "-", index);
        return invoker.call(receiver, null, index);
    }

    @Override
    public Object onSetArray(Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
        report(GroovySandboxOperation.SET_ARRAY, GroovySandboxSignature.typeName(receiver), "-", index, value);
        return invoker.call(receiver, null, index, value);
    }

    protected void report(GroovySandboxOperation operation, String receiverType, String member, Object... args) {
        try {
            List<String> argumentTypes = GroovySandboxSignature.typeNames(args);
            GroovySandboxClassification classification = GroovySandboxClassifier.classify(operation, receiverType, member);
            reporter.report(ruleset, new GroovySandboxSignature(
                phase,
                operation,
                receiverType,
                member,
                argumentTypes,
                classification
            ));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to report Groovy sandbox signature", e);
        }
    }
}
