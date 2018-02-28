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

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.RuleListener;
import org.openremote.model.asset.AssetQuery;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.rules.AssetQueryPredicate;
import org.openremote.model.rules.AssetState;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.TemporaryFact;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RulesFacts extends Facts implements RuleListener {

    // Loop detection
    // TODO Better way than tracking rule trigger count? Max trigger could be a configurable multiple of facts count?
    public static final int MAX_RULES_TRIGGERED_PER_EXECUTION = 100;

    public static final String CLOCK = "INTERNAL_CLOCK";
    public static final String ASSET_STATES = "INTERNAL_ASSET_STATES";
    public static final String ASSET_EVENTS = "INTERNAL_ASSET_EVENTS";
    public static final String EXECUTION_VARS = "INTERNAL_EXECUTION_VAR";
    public static final String ANONYMOUS_FACTS = "ANONYMOUS_FACTS";

    final protected Assets assetsFacade;
    final protected Object loggingContext;
    final protected Logger LOG;

    public RulesClock clock;

    protected int triggerCount;

    public RulesFacts(Assets assetsFacade, Object loggingContext, Logger logger) {
        this.assetsFacade = assetsFacade;
        this.loggingContext = loggingContext;
        this.LOG = logger;

        asMap().put(ASSET_STATES, new ArrayDeque());
        asMap().put(ASSET_EVENTS, new ArrayDeque());
        asMap().put(EXECUTION_VARS, new HashMap());
        asMap().put(ANONYMOUS_FACTS, new ArrayDeque());
    }

    public void setClock(RulesClock clock) {
        this.clock = clock;
        asMap().put(CLOCK, clock);
    }

    public RulesClock getClock() {
        return clock;
    }

    @SuppressWarnings("unchecked")
    public Collection<AssetState> getAssetStates() {
        return (Collection<AssetState>) get(ASSET_STATES);
    }

    @SuppressWarnings("unchecked")
    public Collection<TemporaryFact<AssetState>> getAssetEvents() {
        return (Collection<TemporaryFact<AssetState>>) get(ASSET_EVENTS);
    }

    @SuppressWarnings("unchecked")
    public Collection<Object> getAnonymousFacts() {
        return (Collection<Object>) get(ANONYMOUS_FACTS);
    }

    public Map<String, Object> getNamedFacts() {
        return asMap().entrySet().stream().filter(entry ->
            !entry.getKey().equals(CLOCK)
                && !entry.getKey().equals(ASSET_STATES)
                && !entry.getKey().equals(ASSET_EVENTS)
                && !entry.getKey().equals(EXECUTION_VARS)
                && !entry.getKey().equals(ANONYMOUS_FACTS)
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Stream<Object> getAllFacts() {
        return
            Stream.concat(
                getNamedFacts().values().stream().parallel(),
                Stream.concat(
                    getAnonymousFacts().stream().parallel(),
                    Stream.concat(
                        getAssetStates().stream().parallel(),
                        getAssetEvents().stream().parallel()
                    )
                )
            ).parallel();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getVars() {
        return (Map<String, Object>) get(EXECUTION_VARS);
    }

    public RulesFacts bind(String var, Object value) {
        getVars().put(var, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T bound(String var) {
        return (T) getVars().get(var);
    }

    @Override
    public RulesFacts put(String name, Object fact) {
        switch (name) {
            case ANONYMOUS_FACTS:
            case ASSET_EVENTS:
            case ASSET_STATES:
            case EXECUTION_VARS:
            case CLOCK:
                throw new IllegalArgumentException("Reserved internal fact name: " + name);
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (UPDATE): " + name + " => " + fact + " - on: " + loggingContext);
        }
        super.put(name, fact);
        return this;
    }

    public RulesFacts put(Object o) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (UPDATE): " + o + " - on: " + loggingContext);
        }
        getAnonymousFacts().remove(o);
        getAnonymousFacts().add(o);
        return this;
    }

    public RulesFacts putAssetState(AssetState assetState) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (UPDATE): " + assetState + " - on: " + loggingContext);
        }
        getAssetStates().remove(assetState);
        getAssetStates().add(assetState);
        return this;
    }

    public RulesFacts removeAssetState(AssetState assetState) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (DELETE): " + assetState + " - on: " + loggingContext);
        }
        getAssetStates().remove(assetState);
        return this;
    }

    public RulesFacts insertAssetEvent(String expires, AssetState assetState) {
        return insertAssetEvent(TimeUtil.parseTimeString(expires), assetState);
    }

    public RulesFacts insertAssetEvent(long expiresMilliSeconds, AssetState assetState) {
        TemporaryFact<AssetState> fact = new TemporaryFact<>(assetState.getTimestamp(), expiresMilliSeconds, assetState);
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (INSERT): " + fact + " - on: " + loggingContext);
        }
        getAssetEvents().add(fact);
        return this;
    }

    public RulesFacts putTemporary(String expires, Object value) {
        return putTemporary(TimeUtil.parseTimeString(expires), value);
    }

    public RulesFacts putTemporary(double expires, Object value) {
        return putTemporary((long) expires, value);
    }

    public RulesFacts putTemporary(long expires, Object value) {
        getAnonymousFacts().add(new TemporaryFact<>((long) getClock().getTimestamp(), expires, value));
        return this;
    }

    public RulesFacts putTemporary(String name, String expires, Object value) {
        return putTemporary(name, TimeUtil.parseTimeString(expires), value);
    }

    public RulesFacts putTemporary(String name, double expires, Object value) {
        return putTemporary(name, (long) expires, value);
    }

    public RulesFacts putTemporary(String name, long expires, Object value) {
        put(name, new TemporaryFact<>((long) getClock().getTimestamp(), expires, value));
        return this;
    }

    public boolean hasTemporaryFacts() {
        return getTemporaryFacts().count() > 0;
    }

    public Stream<TemporaryFact> getTemporaryFacts() {
        return Stream.concat(
            Stream.concat(
                getAssetEvents().stream().parallel(),
                getNamedFacts().values().stream().parallel().filter(fact -> fact instanceof TemporaryFact).map(fact -> (TemporaryFact) fact)
            ),
            getAnonymousFacts().stream().parallel().filter(fact -> fact instanceof TemporaryFact).map(fact -> (TemporaryFact) fact)
        ).parallel();
    }

    @Override
    public RulesFacts remove(String name) {
        super.remove(name);
        return this;
    }

    public RulesFacts remove(Object fact) {
        getAnonymousFacts().removeIf(anonFact -> {
            if (anonFact instanceof TemporaryFact) {
                anonFact = ((TemporaryFact) anonFact).getFact();
            }
            return anonFact.equals(fact);
        });
        return this;
    }

    /**
     * Reset rules triggered counter, used for loop detection.
     */
    public void reset() {
        triggerCount = 0;
    }

    @Override
    public boolean beforeEvaluate(Rule rule, Facts facts) {
        // Clear bound variables so each rule can make its own bindings to
        // transport values between LHS and RHS
        getVars().clear();

        logRule(rule, "Rule candidate", true, false);

        return true;
    }

    @Override
    public void afterEvaluate(Rule rule, Facts facts, boolean evaluationResult) {
        if (evaluationResult) {
            triggerCount++;
            if (triggerCount >= MAX_RULES_TRIGGERED_PER_EXECUTION) {
                throw new IllegalStateException(
                    "Possible rules loop detected, exceeded max trigger count of "
                        + MAX_RULES_TRIGGERED_PER_EXECUTION
                        + " for rule: " + rule.getName()
                );
            }
        }
    }

    @Override
    public void beforeExecute(Rule rule, Facts facts) {
        logRule(rule, "Rule triggered", false, true);
    }

    @Override
    public void onSuccess(Rule rule, Facts facts) {
        logRule(rule, "Rule executed", true, false);
    }

    @Override
    public void onFailure(Rule rule, Facts facts, Exception exception) {
        throw new RuntimeException("Error executing action of rule '" + rule.getName() + "': " + exception.getMessage(), exception);
    }

    @SuppressWarnings("unchecked")
    protected <T> Optional<T> matchFact(Object fact, Class<T> factType, Predicate<T> predicate) {
        if (fact == null)
            return Optional.empty();
        if (fact instanceof TemporaryFact) {
            TemporaryFact temporaryFact = (TemporaryFact) fact;
            fact = temporaryFact.getFact();
        }
        return Optional.ofNullable(
            factType.isAssignableFrom(fact.getClass()) && predicate.test((T) fact) ? (T) fact: null
        );
    }

    public <T> Optional<T> matchFirst(String name) {
        return matchFirst(name, fact -> true);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> matchFirst(String name, Predicate<T> predicate) {
        return matchFirst(name, (Class<T>) Object.class, predicate);
    }

    public <T> Optional<T> matchFirst(String name, Class<T> factType, Predicate<T> predicate) {
        return matchFact(get(name), factType, predicate);
    }

    public <T> Optional<T> matchFirst(Predicate<T> predicate) {
        return match(predicate).findFirst();
    }

    public <T> Optional<T> matchFirst(Class<T> factType) {
        return match(factType).findFirst();
    }

    public <T> Optional<T> matchFirst(Class<T> factType, Predicate<T> predicate) {
        return match(factType, predicate).findFirst();
    }

    @SuppressWarnings("unchecked")
    public <T> Stream<T> match(Predicate<T> predicate) {
        return match((Class<T>) Object.class, predicate);
    }

    public <T> Stream<T> match(Class<T> factType) {
        return match(factType, fact -> true);
    }

    @SuppressWarnings("unchecked")
    public <T> Stream<T> match(Class<T> factType, Predicate<T> predicate) {
        return getAllFacts()
            .filter(fact -> matchFact(fact, factType, predicate).isPresent())
            .map(fact -> {
                if (fact instanceof TemporaryFact) {
                    return ((TemporaryFact) fact).getFact();
                }
                return fact;
            })
            .map(fact -> (T) fact);
    }

    public Optional<AssetState> matchFirstAssetState(AssetQuery assetQuery) {
        return matchAssetState(assetQuery).findFirst();
    }

    public Stream<AssetState> matchAssetState(AssetQuery assetQuery) {
        Predicate<AssetState> p = new AssetQueryPredicate(assetQuery);
        return getAssetStates().stream().parallel().filter(p);
    }

    public Optional<TemporaryFact<AssetState>> matchFirstAssetEvent(AssetQuery assetQuery) {
        return matchAssetEvent(assetQuery).findFirst();
    }

    public Optional<TemporaryFact<AssetState>> matchLastAssetEvent(AssetQuery assetQuery) {
        return matchAssetEvent(assetQuery).reduce((first, second) -> second);
    }

    public Stream<TemporaryFact<AssetState>> matchAssetEvent(AssetQuery assetQuery) {
        Predicate<AssetState> p = new AssetQueryPredicate(assetQuery);
        return getAssetEvents().stream().parallel()
            .filter(fact -> matchFact(fact, AssetState.class, p).isPresent());
    }

    public RulesFacts updateAssetState(String assetId, String attributeName, Value value) {
        return invalidateAssetStateAndDispatch(assetId, attributeName, value);
    }

    public RulesFacts updateAssetState(String assetId, String attributeName) {
        return invalidateAssetStateAndDispatch(assetId, attributeName, null);
    }

    public RulesFacts updateAssetState(String assetId, String attributeName, String value) {
        return updateAssetState(assetId, attributeName, Values.create(value));
    }

    public RulesFacts updateAssetState(String assetId, String attributeName, double value) {
        return updateAssetState(assetId, attributeName, Values.create(value));
    }

    public RulesFacts updateAssetState(String assetId, String attributeName, boolean value) {
        return updateAssetState(assetId, attributeName, Values.create(value));
    }

    public RulesFacts updateAssetState(String assetId, String attributeName, AttributeExecuteStatus status) {
        return updateAssetState(assetId, attributeName, status.asValue());
    }

    protected RulesFacts invalidateAssetStateAndDispatch(String assetId, String attributeName, Value value) {
        // Remove the asset state from the facts, it is invalid now
        getAssetStates()
            .removeIf(assetState -> {
                boolean invalid = assetState.getId().equals(assetId) && assetState.getAttributeName().equals(attributeName);
                if (invalid && LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Fact change (INTERNAL DELETE): " + assetState + " - on: " + loggingContext);
                }
                return invalid;
            });

        // Dispatch the update to the asset processing service
        AttributeEvent attributeEvent = new AttributeEvent(assetId, attributeName, value);
        LOG.finest("Dispatching " + attributeEvent + " - on: " + loggingContext);
        assetsFacade.dispatch(attributeEvent);

        return this;
    }

    public void removeExpiredTemporaryFacts() {
        long currentTimestamp = (long) getClock().getTimestamp();
        getAssetEvents().removeIf(fact -> {
            boolean result = fact.isExpired(currentTimestamp);
            if (result && LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Fact change (DELETE EXPIRED): " + fact + " - on: " + loggingContext);
            }
            return result;
        });
        asMap().entrySet().removeIf(entry -> {
            if (entry.getKey().equals(CLOCK)
                || entry.getKey().equals(ASSET_STATES)
                || entry.getKey().equals(ASSET_EVENTS)
                || entry.getKey().equals(EXECUTION_VARS)
                || entry.getKey().equals(ANONYMOUS_FACTS)
                || !(entry.getValue() instanceof TemporaryFact)) {
                return false;
            }
            boolean result = ((TemporaryFact) entry.getValue()).isExpired(currentTimestamp);
            if (result && LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Fact change (DELETE EXPIRED): " + entry.getValue() + " - on: " + loggingContext);
            }
            return result;
        });
        getAnonymousFacts().removeIf(fact -> {
            boolean result = false;
            if (fact instanceof TemporaryFact) {
                TemporaryFact temporaryFact = (TemporaryFact) fact;
                result = temporaryFact.isExpired((long) getClock().getTimestamp());
            }
            if (result && LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Fact change (DELETE EXPIRED): " + fact + " - on: " + loggingContext);
            }
            return result;
        });
    }

    public boolean logFacts(Logger logger) {
        boolean haveLog = false;
        if (getAllFacts().count() > 0) {
            logger.info("--------------------------------- CLOCK ---------------------------------");
            LOG.info(getClock().toString());
            haveLog = true;
        }

        List<AssetState> sortedAssetStates = new ArrayList<>(getAssetStates());
        sortedAssetStates.sort(Comparator.naturalOrder());
        if (sortedAssetStates.size() > 0) {
            logger.info("--------------------------------- ASSET STATES (" + sortedAssetStates.size() + ") ---------------------------------");
            for (AssetState assetState : sortedAssetStates) {
                logger.info(assetState.toString());
            }
            haveLog = true;
        }
        List<TemporaryFact<AssetState>> sortedAssetEvents = new ArrayList<>(getAssetEvents());
        if (sortedAssetEvents.size() > 0) {
            logger.info("--------------------------------- ASSET EVENTS (" + sortedAssetEvents.size() + ") ---------------------------------");
            sortedAssetEvents.sort(Comparator.comparing(TemporaryFact::getTime));
            for (TemporaryFact<AssetState> assetEvent : sortedAssetEvents) {
                logger.info(assetEvent.toString());
            }
            haveLog = true;
        }
        Map<String, Object> namedFacts = getNamedFacts();
        List<String> names = new ArrayList<>(namedFacts.keySet());
        names.sort(Comparator.naturalOrder());
        if (names.size() > 0) {
            logger.info("--------------------------------- NAMED FACTS (" + names.size() + ") ---------------------------------");
            for (String name : names) {
                logger.info(String.format("%s => %s", name, namedFacts.get(name)));
            }
            haveLog = true;
        }
        if (getAnonymousFacts().size() > 0) {
            logger.info("--------------------------------- ANONYMOUS FACTS (" + getAnonymousFacts().size() + ") ---------------------------------");
            for (Object o : getAnonymousFacts()) {
                logger.info(String.format("%s", o));
            }
            haveLog = true;
        }
        return haveLog;
    }

    public boolean logVars(Logger logger) {
        boolean haveLog = false;
        List<String> sortedVars = new ArrayList<>(getVars().keySet());
        sortedVars.sort(Comparator.naturalOrder());
        if (sortedVars.size() > 0) {
            logger.info("--------------------------------- BOUND VARIABLES (" + sortedVars.size() + ") ---------------------------------");
            for (String var : sortedVars) {
                logger.info(String.format("%s => %s", var, getVars().get(var)));
            }
            haveLog = true;
        }
        return haveLog;
    }

    protected void logRule(Rule rule, String msg, boolean logFacts, boolean logVars) {
        String ruleName = rule.getName();
        if (ruleName.startsWith("-") && LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO,
                String.format("*** %s: %s - on %s", msg, ruleName, loggingContext)
            );
        }
        if (ruleName.startsWith("--") && LOG.isLoggable(Level.INFO)) {
            boolean haveLog = false;
            if (logFacts)
                haveLog = logFacts(LOG);
            if (logVars)
                haveLog = haveLog || logVars(LOG);
            if (haveLog) {
                LOG.info("------------------------------------------------------------------------------");
            }
        }
    }

}