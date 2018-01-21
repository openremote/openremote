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
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.Users;

import javax.script.*;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RulesetDeployment {

    public static final int DEFAULT_RULE_PRIORITY = 1000;

    public enum Status {
        /**
         * Ruleset compiled successfully but is not running, due to failure of other rulesets in same scope.
         */
        READY,

        /**
         * Ruleset has been compiled and can be executed.
         */
        DEPLOYED,

        /**
         * Ruleset did not compile successfully and can not be executed.
         */
        COMPILATION_ERROR,

        /**
         * Ruleset was executed but there was a runtime error.
         */
        EXECUTION_ERROR
    }

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

    // Share one JS script engine manager, it's thread-safe
    static final protected ScriptEngineManager scriptEngineManager;

    static final protected GroovyShell groovyShell;

    static {
        scriptEngineManager = new ScriptEngineManager();

        // LOG and console wrapper can be in global scope for all script engines
        scriptEngineManager.put("LOG", RulesEngine.RULES_LOG);
        scriptEngineManager.put("console", new JsConsole(RulesEngine.RULES_LOG));

        groovyShell = new GroovyShell(
            new CompilerConfiguration().addCompilationCustomizers(new SandboxTransformer())
        );
    }

    final protected long id;
    final protected String name;
    final protected long version;
    final protected Rules rules = new Rules();
    protected Status status;
    protected Throwable error;

    public RulesetDeployment(long id, String name, long version) {
        this.id = id;
        this.name = name;
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        return version;
    }

    public Rules getRules() {
        return rules;
    }

    public boolean registerRules(Ruleset ruleset, Assets assetsFacade, Users usersFacade) {
        RulesEngine.LOG.info("Evaluating ruleset deployment: " + ruleset);
        switch (ruleset.getLang()) {
            case JAVASCRIPT:
                return registerRulesJavascript(ruleset, assetsFacade, usersFacade);
            case GROOVY:
                return registerRulesGroovy(ruleset, assetsFacade, usersFacade);
        }
        return false;
    }

    public boolean registerRulesJavascript(Ruleset ruleset, Assets assetsFacade, Users usersFacade) {
        // TODO https://github.com/pfisterer/scripting-sandbox/blob/master/src/main/java/de/farberg/scripting/sandbox/ScriptingSandbox.java
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");
        ScriptContext newContext = new SimpleScriptContext();
        newContext.setBindings(scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        Bindings engineScope = newContext.getBindings(ScriptContext.ENGINE_SCOPE);

        engineScope.put("assets", assetsFacade);
        engineScope.put("users", usersFacade);

        String script = ruleset.getRules();

        // Default header/imports for all rules scripts
        script = "load(\"nashorn:mozilla_compat.js\");\n" + // This provides importPackage
            "\n" +
            "importPackage(\n" +
            "    \"java.util.stream\",\n" +
            "    \"org.openremote.model.asset\",\n" +
            "    \"org.openremote.model.attribute\",\n" +
            "    \"org.openremote.model.value\"\n" +
            ");\n" +
            "var Match = Java.type(\"org.openremote.model.asset.BaseAssetQuery$Match\");\n" +
            "var Operator = Java.type(\"org.openremote.model.asset.BaseAssetQuery$Operator\");\n" +
            "var NumberType = Java.type(\"org.openremote.model.asset.BaseAssetQuery$NumberType\");\n" +
            "var StringPredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$StringPredicate\");\n" +
            "var BooleanPredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$BooleanPredicate\");\n" +
            "var StringArrayPredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$StringArrayPredicate\");\n" +
            "var DateTimePredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$DateTimePredicate\");\n" +
            "var NumberPredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$NumberPredicate\");\n" +
            "var ParentPredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$ParentPredicate\");\n" +
            "var PathPredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$PathPredicate\");\n" +
            "var TenantPredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$TenantPredicate\");\n" +
            "var AttributePredicate = Java.type(\"org.openremote.model.asset.BaseAssetQuery$AttributePredicate\");\n" +
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

            registerRulesJavascript((ScriptObjectMirror) engineScope.get("rules"));

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
    protected void registerRulesJavascript(ScriptObjectMirror scriptRules) {
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

    public boolean registerRulesGroovy(Ruleset ruleset, Assets assetsFacade, Users usersFacade) {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", version=" + version +
            ", status=" + status +
            '}';
    }
}
