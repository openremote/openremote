/*
 * Copyright 2018, OpenRemote Inc.
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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jeasy.rules.api.Action;
import org.jeasy.rules.api.Condition;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.RuleBuilder;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.openremote.container.Container;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.rules.facade.NotificationsFacade;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.RulesetStatus;
import org.openremote.model.rules.Users;
import org.openremote.model.rules.json.JsonRulesetDefinition;

import javax.script.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.manager.rules.JsonRulesBuilder.executeRuleActions;

public class RulesetDeployment {

    /**
     * An interface that looks like a JavaScript browser console, for simplified logging.
     */
    public static class JsConsole {

        final protected Logger logger;

        public JsConsole(Logger logger) {
            this.logger = logger;
        }

        public void debug(Object o) {
            logger.fine(o != null ? o.toString() : "null");
        }

        public void log(Object o) {
            logger.info(o != null ? o.toString() : "null");
        }

        public void warn(Object o) {
            logger.warning(o != null ? o.toString() : "null");
        }

        public void error(Object o) {
            logger.severe(o != null ? o.toString() : "null");
        }
    }

    // TODO Finish groovy sandbox
    static class GroovyDenyAllFilter extends GroovyValueFilter {
        @Override
        public Object filterReceiver(Object receiver) {
            throw new SecurityException("Not allowed: " + receiver);
        }
    }
    public static final int DEFAULT_RULE_PRIORITY = 1000;
    // Share one JS script engine manager, it's thread-safe
    static final protected ScriptEngineManager scriptEngineManager;

    static final protected GroovyShell groovyShell;

    static {
        scriptEngineManager = new ScriptEngineManager();

        // LOG and console wrapper can be in global scope for all script engines
        // TODO Use a different logger for each RulesEngine and show messages in Manager UI for that engine
        scriptEngineManager.put("LOG", RulesEngine.RULES_LOG);
        scriptEngineManager.put("console", new JsConsole(RulesEngine.RULES_LOG));

        /* TODO Sharing a static GroovyShell doesn't work, redeploying a ruleset which defines classes (e.g. Flight) is broken:

        java.lang.RuntimeException: Error evaluating condition of rule '-Update flight facts when estimated landing time of flight asset is updated':
        No signature of method: org.openremote.manager.setup.database.Script1$_run_closure2$_closure14$_closure17.doCall() is applicable for argument types: (org.openremote.manager.setup.database.Flight) values: [...]
        Possible solutions: doCall(org.openremote.manager.setup.database.Flight), findAll(), findAll(), isCase(java.lang.Object), isCase(java.lang.Object)
        The following classes appear as argument class and as parameter class, but are defined by different class loader:
        org.openremote.manager.setup.database.Flight (defined by 'groovy.lang.GroovyClassLoader$InnerLoader@2cc34cd5' and 'groovy.lang.GroovyClassLoader$InnerLoader@1af957bc')
        If one of the method suggestions matches the method you wanted to call,
        then check your class loader setup.
         */
        groovyShell = new GroovyShell(
                new CompilerConfiguration().addCompilationCustomizers(new SandboxTransformer())
        );
    }

    final protected Ruleset ruleset;
    final protected Rules rules = new Rules();
    final protected AssetStorageService assetStorageService;
    final protected TimerService timerService;
    final protected ManagerExecutorService executorService;
    final protected Assets assetsFacade;
    final protected Users usersFacade;
    final protected NotificationsFacade notificationsFacade;
    final protected List<ScheduledFuture> scheduledRuleActions = new ArrayList<>();
    protected RulesetStatus status;
    protected Throwable error;
    protected JsonRulesBuilder jsonRulesBuilder;
    protected JsonRulesetDefinition jsonRulesetDefinition;

    public RulesetDeployment(Ruleset ruleset, TimerService timerService, AssetStorageService assetStorageService, ManagerExecutorService executorService, Assets assetsFacade, Users usersFacade, NotificationsFacade notificationsFacade) {
        this.ruleset = ruleset;
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.executorService = executorService;
        this.assetsFacade = assetsFacade;
        this.usersFacade = usersFacade;
        this.notificationsFacade = notificationsFacade;

        if (ruleset.getLang() == Ruleset.Lang.JSON) {
            try {
                String rulesStr = ruleset.getRules();
                rulesStr = rulesStr.replace("%RULESET_ID%", Long.toString(ruleset.getId()));
                rulesStr = rulesStr.replace("%RULESET_NAME%", ruleset.getName());
                jsonRulesetDefinition = Container.JSON.readValue(rulesStr, JsonRulesetDefinition.class);
                jsonRulesBuilder = new JsonRulesBuilder(timerService, assetStorageService, executorService, assetsFacade, usersFacade, notificationsFacade, this::scheduleRuleAction);
            } catch (IOException e) {
                RulesEngine.LOG.log(Level.SEVERE, "Error evaluating ruleset: " + ruleset, e);
                setError(e);
            }
        }
    }

    public long getId() {
        return ruleset.getId();
    }

    public String getName() {
        return ruleset.getName();
    }

    public long getVersion() {
        return ruleset.getVersion();
    }

    public Ruleset getRuleset() {
        return ruleset;
    }

    public Rules getRules() {
        return rules;
    }

    /**
     * Called when a ruleset is about to be started (allows for initialisation tasks)
     */
    public void onStart(RulesFacts facts) {
        if (jsonRulesetDefinition != null && jsonRulesetDefinition.rules != null && jsonRulesetDefinition.rules.length > 0) {
            Arrays.stream(jsonRulesetDefinition.rules).forEach(jsonRule ->
                executeRuleActions(jsonRule.onStart, false, facts, null, assetsFacade, usersFacade, notificationsFacade, timerService, assetStorageService, this::scheduleRuleAction));
        }
    }

    public boolean start() {
        RulesEngine.LOG.info("Evaluating ruleset deployment: " + ruleset);
        switch (ruleset.getLang()) {
            case JAVASCRIPT:
                return startRulesJavascript(ruleset, assetsFacade, usersFacade, notificationsFacade);
            case GROOVY:
                return startRulesGroovy(ruleset, assetsFacade, usersFacade, notificationsFacade);
            case JSON:
                return startRulesJson(ruleset);
        }
        return false;
    }

    /**
     * Called when a ruleset is about to be stopped (allows for cleanup tasks)
     */
    public void onStop(RulesFacts facts) {
        if (jsonRulesetDefinition != null && jsonRulesetDefinition.rules != null && jsonRulesetDefinition.rules.length > 0) {
            Arrays.stream(jsonRulesetDefinition.rules).forEach(jsonRule ->
                    executeRuleActions(jsonRule.onStop, false, facts, null, assetsFacade, usersFacade, notificationsFacade, timerService, assetStorageService, this::scheduleRuleAction));
        }
    }

    /**
     * Called when this deployment is stopped, could be the ruleset is being updated, removed or an error has occurred
     * during execution
     */
    public void stop() {
        withLock(toString() + "::stopRulesetDeployment", () ->
                scheduledRuleActions.removeIf(scheduledFuture -> {
                    scheduledFuture.cancel(true);
                    return true;
                }));
    }

    public void onAssetStatesChanged(RulesFacts facts) {
        if (jsonRulesBuilder != null) {
            jsonRulesBuilder.onAssetStatesChanged(facts);
        }
    }

    protected void scheduleRuleAction(Runnable action, long delayMillis) {
        withLock(toString() + "::scheduleRuleAction", () -> {
            ScheduledFuture future = executorService.schedule(() ->
                    withLock(toString() + "::scheduledRuleActionFire", () -> {
                        scheduledRuleActions.removeIf(Future::isDone);
                        action.run();
                    }), delayMillis);
            scheduledRuleActions.add(future);
        });
    }

    protected boolean startRulesJson(Ruleset ruleset) {

        if (jsonRulesetDefinition == null || jsonRulesetDefinition.rules == null || jsonRulesetDefinition.rules.length == 0) {
            RulesEngine.LOG.log(Level.WARNING, "No rules within ruleset so nothing to start: " + ruleset);
            return false;
        }

        Arrays.stream(jsonRulesetDefinition.rules).forEach(r -> jsonRulesBuilder.add(r));

        for (Rule rule : jsonRulesBuilder.build()) {
            RulesEngine.LOG.info("Registering JSON rule: " + rule.getName());
            rules.register(rule);
        }
        RulesEngine.LOG.info("Evaluated ruleset deployment: " + ruleset);

        return true;
    }

    protected boolean startRulesJavascript(Ruleset ruleset, Assets assetsFacade, Users usersFacade, NotificationsFacade consolesFacade) {
        // TODO https://github.com/pfisterer/scripting-sandbox/blob/master/src/main/java/de/farberg/scripting/sandbox/ScriptingSandbox.java
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");
        ScriptContext newContext = new SimpleScriptContext();
        newContext.setBindings(scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        Bindings engineScope = newContext.getBindings(ScriptContext.ENGINE_SCOPE);

        engineScope.put("assets", assetsFacade);
        engineScope.put("users", usersFacade);
        engineScope.put("consoles", consolesFacade);

        String script = ruleset.getRules();

        // Default header/imports for all rules scripts
        script = "load(\"nashorn:mozilla_compat.js\");\n" + // This provides importPackage
                "\n" +
                "importPackage(\n" +
                "    \"java.util.stream\",\n" +
                "    \"org.openremote.model.asset\",\n" +
                "    \"org.openremote.model.attribute\",\n" +
                "    \"org.openremote.model.value\",\n" +
                "    \"org.openremote.model.rules\",\n" +
                "    \"org.openremote.model.query\"\n" +
                ");\n" +
                "var Match = Java.type(\"org.openremote.model.query.BaseAssetQuery$Match\");\n" +
                "var Operator = Java.type(\"org.openremote.model.query.BaseAssetQuery$Operator\");\n" +
                "var NumberType = Java.type(\"org.openremote.model.query.BaseAssetQuery$NumberType\");\n" +
                "var StringPredicate = Java.type(\"org.openremote.model.query.filter.StringPredicate\");\n" +
                "var BooleanPredicate = Java.type(\"org.openremote.model.query.filter.BooleanPredicate\");\n" +
                "var StringArrayPredicate = Java.type(\"org.openremote.model.query.filter.StringArrayPredicate\");\n" +
                "var DateTimePredicate = Java.type(\"org.openremote.model.query.filter.DateTimePredicate\");\n" +
                "var NumberPredicate = Java.type(\"org.openremote.model.query.filter.NumberPredicate\");\n" +
                "var ParentPredicate = Java.type(\"org.openremote.model.query.filter.ParentPredicate\");\n" +
                "var PathPredicate = Java.type(\"org.openremote.model.query.filter.PathPredicate\");\n" +
                "var TenantPredicate = Java.type(\"org.openremote.model.query.filter.TenantPredicate\");\n" +
                "var AttributePredicate = Java.type(\"org.openremote.model.query.filter.AttributePredicate\");\n" +
                "var AttributeExecuteStatus = Java.type(\"org.openremote.model.attribute.AttributeExecuteStatus\");\n" +
                "var EXACT = Match.EXACT;\n" +
                "var BEGIN = Match.BEGIN;\n" +
                "var END = Match.END;\n" +
                "var CONTAINS = Match.CONTAINS;\n" +
                "var EQUALS = Operator.EQUALS;\n" +
                "var GREATER_THAN = Operator.GREATER_THAN;\n" +
                "var GREATER_EQUALS = Operator.GREATER_EQUALS;\n" +
                "var LESS_THAN = Operator.LESS_THAN;\n" +
                "var LESS_EQUALS = Operator.LESS_EQUALS;\n" +
                "var BETWEEN = Operator.BETWEEN;\n" +
                "var REQUEST_START = AttributeExecuteStatus.REQUEST_START;\n" +
                "var REQUEST_REPEATING = AttributeExecuteStatus.REQUEST_REPEATING;\n" +
                "var REQUEST_CANCEL = AttributeExecuteStatus.REQUEST_CANCEL;\n" +
                "var READY = AttributeExecuteStatus.READY;\n" +
                "var COMPLETED = AttributeExecuteStatus.COMPLETED;\n" +
                "var RUNNING = AttributeExecuteStatus.RUNNING;\n" +
                "var CANCELLED = AttributeExecuteStatus.CANCELLED;\n" +
                "var ERROR = AttributeExecuteStatus.ERROR;\n" +
                "var DISABLED = AttributeExecuteStatus.DISABLED;\n" +
                "\n"
                + script;

        try {
            scriptEngine.eval(script, engineScope);

            startRulesJavascript((ScriptObjectMirror) engineScope.get("rules"));

            RulesEngine.LOG.info("Evaluated ruleset deployment: " + ruleset);
            return true;

        } catch (Exception e) {
            RulesEngine.LOG.log(Level.SEVERE, "Error evaluating ruleset: " + ruleset, e);
            setError(e);
            engineScope.clear();
            return false;
        }
    }

    /**
     * Marshal the JavaScript rules array into {@link Rule} instances.
     */
    protected void startRulesJavascript(ScriptObjectMirror scriptRules) {
        if (scriptRules == null || !scriptRules.isArray()) {
            throw new IllegalArgumentException("No 'rules' array defined in ruleset");
        }
        Collection<Object> rulesObjects = scriptRules.values();
        for (Object rulesObject : rulesObjects) {
            ScriptObjectMirror rule = (ScriptObjectMirror) rulesObject;

            String name;
            if (!rule.containsKey("name")) {
                throw new IllegalArgumentException("Missing 'name' in rule definition");
            }
            try {
                name = (String) rule.getMember("name");
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Defined 'name' of rule is not a string");
            }

            String description;
            try {
                description = rule.containsKey("description") ? (String) rule.getMember("description") : null;
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Defined 'description' is not a string in rule: " + name);
            }

            int priority;
            try {
                priority = rule.containsKey("priority") ? (int) rule.getMember("priority") : DEFAULT_RULE_PRIORITY;
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Defined 'priority' is not a number in rule: " + name);
            }

            if (!rule.containsKey("when")) {
                throw new IllegalArgumentException("Missing 'when' function in rule: " + name);
            }

            Condition when;
            try {
                ScriptObjectMirror whenMirror = (ScriptObjectMirror) rule.getMember("when");
                if (!whenMirror.isFunction()) {
                    throw new IllegalArgumentException("Defined 'when' is not a function in rule: " + name);
                }
                when = whenMirror.to(Condition.class);
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Defined 'when' is not a function in rule: " + name);
            }

            Action then;
            try {
                ScriptObjectMirror thenMirror = (ScriptObjectMirror) rule.getMember("then");
                if (!thenMirror.isFunction()) {
                    throw new IllegalArgumentException("Defined 'then' is not a function in rule: " + name);
                }
                then = thenMirror.to(Action.class);
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Defined 'then' is not a function in rule: " + name);
            }

            RulesEngine.LOG.info("Registering rule: " + name);

            rules.register(
                    new RuleBuilder().name(name).description(description).priority(priority).when(when).then(then).build()
            );
        }
    }

    protected boolean startRulesGroovy(Ruleset ruleset, Assets assetsFacade, Users usersFacade, NotificationsFacade notificationFacade) {
        try {
            // TODO Implement sandbox
            // new DenyAll().register();
            Script script = groovyShell.parse(ruleset.getRules());
            Binding binding = new Binding();
            RulesBuilder rulesBuilder = new RulesBuilder();
            binding.setVariable("LOG", RulesEngine.RULES_LOG);
            binding.setVariable("rules", rulesBuilder);
            binding.setVariable("assets", assetsFacade);
            binding.setVariable("users", usersFacade);
            binding.setVariable("notifications", notificationFacade);
            script.setBinding(binding);
            script.run();
            for (Rule rule : rulesBuilder.build()) {
                RulesEngine.LOG.info("Registering rule: " + rule.getName());
                rules.register(rule);
            }
            RulesEngine.LOG.info("Evaluated ruleset deployment: " + ruleset);
            return true;

        } catch (Exception e) {
            RulesEngine.LOG.log(Level.SEVERE, "Error evaluating ruleset: " + ruleset, e);
            setError(e);
            return false;
        }
    }

    public RulesetStatus getStatus() {
        return status;
    }

    public void setStatus(RulesetStatus status) {
        this.status = status;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return getError() != null ? getError().getMessage() : null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", version=" + getVersion() +
                ", status=" + status +
                '}';
    }
}
