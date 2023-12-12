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

import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.RuleListener;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.GeofencePredicate;
import org.openremote.model.rules.Assets;
import org.openremote.model.rules.RulesClock;
import org.openremote.model.rules.TemporaryFact;
import org.openremote.model.util.TimeUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.openremote.model.query.filter.LocationAttributePredicate.getLocationPredicates;

public class RulesFacts extends Facts implements RuleListener {

    // Loop detection
    // TODO Better way than tracking rule trigger count? Max trigger could be a configurable multiple of facts count?
    public static final int MAX_RULES_TRIGGERED_PER_EXECUTION = 100;

    public static final int INITIAL_CAPACITY = 100000;

    public static final String ASSET_STATES = "INTERNAL_ASSET_STATES";
    public static final String ASSET_EVENTS = "INTERNAL_ASSET_EVENTS";
    public static final String EXECUTION_VARS = "INTERNAL_EXECUTION_VAR";
    public static final String ANONYMOUS_FACTS = "ANONYMOUS_FACTS";

    final protected TimerService timerService;
    final protected AssetStorageService assetStorageService;
    final protected Assets assetsFacade;
    final protected Object loggingContext;
    final protected Logger LOG;
    final protected Map<String, Collection<AttributeInfo>> assetIdIndex = new HashMap<>();
    final protected Map<String, Collection<AttributeInfo>> assetTypeIndex = new HashMap<>();
    protected int triggerCount;
    protected boolean trackLocationRules;
    protected Map<String, Set<GeofencePredicate>> assetStateLocationPredicateMap = null;

    public RulesFacts(TimerService timerService, AssetStorageService assetStorageService, Assets assetsFacade, Object loggingContext, Logger logger) {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.assetsFacade = assetsFacade;
        this.loggingContext = loggingContext;
        this.LOG = logger;

        super.put(ASSET_STATES, new ArrayDeque<AttributeInfo>(INITIAL_CAPACITY));
        super.put(ASSET_EVENTS, new ArrayDeque<AssetEvent>(INITIAL_CAPACITY));
        super.put(EXECUTION_VARS, new HashMap<>());
        super.put(ANONYMOUS_FACTS, new ArrayDeque<>(INITIAL_CAPACITY));
    }

    protected void startTrackingLocationRules() {
        LOG.finest("Tracking location predicate rules: started");
        trackLocationRules = true;
    }

    protected List<RulesEngine.AssetLocationPredicates> stopTrackingLocationRules() {
        LOG.finest("Tracking location predicate rules: stopping");
        trackLocationRules = false;
        Map<String, Set<GeofencePredicate>> assetStateLocationPredicateMap = this.assetStateLocationPredicateMap;
        this.assetStateLocationPredicateMap = null;
        return assetStateLocationPredicateMap == null ? null : assetStateLocationPredicateMap.entrySet().stream()
                .map(assetStateSetEntry ->
                        new RulesEngine.AssetLocationPredicates(
                                assetStateSetEntry.getKey(),
                                assetStateSetEntry.getValue())).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Collection<AttributeInfo> getAssetStates() {
        return get(ASSET_STATES);
    }

    @SuppressWarnings("unchecked")
    public Collection<TemporaryFact<AttributeInfo>> getAssetEvents() {
        return get(ASSET_EVENTS);
    }

    @SuppressWarnings("unchecked")
    public Collection<Object> getAnonymousFacts() {
        return get(ANONYMOUS_FACTS);
    }

    public Map<String, Object> getNamedFacts() {
        return asMap().entrySet().stream().filter(entry ->
                !entry.getKey().equals(ASSET_STATES)
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

    public long getFactCount() {
        return getNamedFacts().size() + getAnonymousFacts().size() + getAssetStates().size() + getAssetEvents().size();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getVars() {
        return get(EXECUTION_VARS);
    }

    public RulesFacts bind(String var, Object value) {
        getVars().put(var, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T bound(String var) {
        return (T) getVars().get(var);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String name) {
        Object fact = super.get(name);
        if (fact != null && fact instanceof TemporaryFact<?>) {
            TemporaryFact<?> temporaryFact = (TemporaryFact<?>) fact;
            fact = temporaryFact.getFact();
        }
        return (T) fact;
    }

    public <T> Optional<T> getOptional(String name) {
        return Optional.ofNullable(get(name));
    }

    @Override
    public <T> void put(String name, T fact) {
        switch (name) {
            case ANONYMOUS_FACTS:
            case ASSET_EVENTS:
            case ASSET_STATES:
            case EXECUTION_VARS:
                throw new IllegalArgumentException("Reserved internal fact name: " + name);
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (UPDATE): " + name + " => " + fact + " - on: " + loggingContext);
        }
        super.put(name, fact);
    }

    public RulesFacts put(Object o) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (UPDATE): " + o + " - on: " + loggingContext);
        }
        getAnonymousFacts().remove(o);
        getAnonymousFacts().add(o);
        return this;
    }

    public RulesFacts putAssetState(AttributeInfo assetState) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (UPDATE): " + assetState + " - on: " + loggingContext);
        }
        getAssetStates().remove(assetState);
        getAssetStates().add(assetState);

        // Maintain index of all asset states for this asset by ID
        assetIdIndex.putIfAbsent(assetState.getId(), new ArrayDeque<>());
        assetIdIndex.get(assetState.getId()).remove(assetState);
        assetIdIndex.get(assetState.getId()).add(assetState);

        // Maintain index of all asset states for this asset by type
        assetTypeIndex.putIfAbsent(assetState.getAssetType(), new ArrayDeque<>());
        assetTypeIndex.get(assetState.getAssetType()).remove(assetState);
        assetTypeIndex.get(assetState.getAssetType()).add(assetState);

        return this;
    }

    public RulesFacts removeAssetState(AttributeInfo assetState) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (DELETE): " + assetState + " - on: " + loggingContext);
        }
        getAssetStates().remove(assetState);

        // Maintain index of all asset states for this asset by ID
        Collection<AttributeInfo> assetIdIndexCollection = assetIdIndex.get(assetState.getId());
        if (assetIdIndexCollection != null) {
            assetIdIndexCollection.remove(assetState);
        }

        // Maintain index of all asset states for this asset by type
        Collection<AttributeInfo> assetTypeIndexCollection = assetTypeIndex.get(assetState.getAssetType());
        if (assetTypeIndexCollection != null) {
            assetTypeIndexCollection.remove(assetState);
        }

        return this;
    }

    public RulesFacts insertAttributeEvent(long expiresMilliSeconds, AttributeInfo attributeInfo) {
        TemporaryFact<AttributeInfo> fact = new TemporaryFact<>(attributeInfo.getTimestamp(), expiresMilliSeconds, attributeInfo);
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (INSERT): " + fact + " - on: " + loggingContext);
        }
        getAssetEvents().add(fact);
        return this;
    }

    public RulesFacts removeAttributeEvents(AttributeRef attributeRef) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Fact change (DELETE ALL): " + attributeRef + " on: " + loggingContext);
        }
        getAssetEvents().removeIf(fact -> fact.getFact().getRef().equals(attributeRef));
        return this;
    }

    public RulesFacts putTemporary(String expires, Object value) {
        return putTemporary(TimeUtil.parseTimeDuration(expires), value);
    }

    public RulesFacts putTemporary(Duration expires, Object value) {
        return putTemporary(expires.toMillis(), value);
    }

    public RulesFacts putTemporary(double expires, Object value) {
        return putTemporary((long) expires, value);
    }

    public RulesFacts putTemporary(long expires, Object value) {
        getAnonymousFacts().add(new TemporaryFact<>(timerService.getCurrentTimeMillis(), expires, value));
        return this;
    }

    public RulesFacts putTemporary(String name, String expires, Object value) {
        return putTemporary(name, TimeUtil.parseTimeDuration(expires), value);
    }

    public RulesFacts putTemporary(String name, double expires, Object value) {
        return putTemporary(name, (long) expires, value);
    }

    public RulesFacts putTemporary(String name, long expires, Object value) {
        put(name, new TemporaryFact<>(timerService.getCurrentTimeMillis(), expires, value));
        return this;
    }

    public boolean hasTemporaryFacts() {
        return getTemporaryFacts().count() > 0;
    }

    public Stream<TemporaryFact<?>> getTemporaryFacts() {
        return Stream.concat(
                Stream.concat(
                        getAssetEvents().stream().parallel(),
                        getNamedFacts().values().stream().parallel().filter(fact -> fact instanceof TemporaryFact<?>).map(fact -> (TemporaryFact<?>) fact)
                ),
                getAnonymousFacts().stream().parallel().filter(fact -> fact instanceof TemporaryFact<?>).map(fact -> (TemporaryFact<?>) fact)
        ).parallel();
    }

    @Override
    public void remove(String name) {
        super.remove(name);
    }

    public RulesFacts remove(Object fact) {
        getAnonymousFacts().removeIf(anonFact -> {
            if (anonFact instanceof TemporaryFact<?>) {
                anonFact = ((TemporaryFact<?>) anonFact).getFact();
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
                throw new RulesLoopException(MAX_RULES_TRIGGERED_PER_EXECUTION, rule.getName());
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
        if (fact instanceof TemporaryFact<?>) {
            TemporaryFact<?> temporaryFact = (TemporaryFact<?>) fact;
            fact = temporaryFact.getFact();
        }
        return Optional.ofNullable(
                factType.isAssignableFrom(fact.getClass()) && predicate.test((T) fact) ? (T) fact : null
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
                    if (fact instanceof TemporaryFact<?>) {
                        return ((TemporaryFact<?>) fact).getFact();
                    }
                    return fact;
                })
                .map(fact -> (T) fact);
    }

    public Optional<AttributeInfo> matchFirstAssetState(AssetQuery assetQuery) {
        return matchAssetState(assetQuery).findFirst();
    }

    public Stream<AttributeInfo> matchAssetState(AssetQuery assetQuery) {

        if (trackLocationRules && assetQuery.attributes != null) {
            storeLocationPredicates(getLocationPredicates(assetQuery.attributes));
        }

        Predicate<AttributeInfo> p = new AssetQueryPredicate(timerService, assetStorageService, assetQuery);
        return matchAssetState(p);
    }

    public Stream<AttributeInfo> matchAssetState(Predicate<AttributeInfo> p) {
        // Match against all asset states by default
        Stream<AttributeInfo> assetStates = getAssetStates().stream();
        return assetStates.parallel().filter(p);
    }

    public Optional<TemporaryFact<AttributeInfo>> matchFirstAssetEvent(AssetQuery assetQuery) {
        return matchAssetEvent(assetQuery).findFirst();
    }

    public Optional<TemporaryFact<AttributeInfo>> matchLastAssetEvent(AssetQuery assetQuery) {
        return matchAssetEvent(assetQuery).reduce((first, second) -> second);
    }

    @SuppressWarnings("unchecked")
    public Stream<TemporaryFact<AttributeInfo>> matchAssetEvent(AssetQuery assetQuery) {
        Predicate<AttributeInfo> p = new AssetQueryPredicate(timerService, assetStorageService, assetQuery);
        return getAssetEvents().stream().parallel()
                .filter(fact -> this.matchFact(fact, AttributeInfo.class, p).isPresent());
    }

    public RulesFacts updateAssetState(String assetId, String attributeName, Object value) {
        return invalidateAssetStateAndDispatch(assetId, attributeName, value);
    }

    public RulesFacts updateAssetState(String assetId, String attributeName) {
        return invalidateAssetStateAndDispatch(assetId, attributeName, null);
    }

    public void removeExpiredTemporaryFacts() {
        long currentTimestamp = timerService.getCurrentTimeMillis();
        getAssetEvents().removeIf(fact -> {
            boolean result = fact.isExpired(currentTimestamp);
            if (result && LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Fact change (DELETE EXPIRED): " + fact + " - on: " + loggingContext);
            }
            return result;
        });

        List<Fact<?>> expiredFacts = StreamSupport.stream(super.spliterator(), false).filter(fact -> {
            if (fact.getName().equals(ASSET_STATES)
                || fact.getName().equals(ASSET_EVENTS)
                || fact.getName().equals(EXECUTION_VARS)
                || fact.getName().equals(ANONYMOUS_FACTS)
                || !(fact.getValue() instanceof TemporaryFact<?>)) {
                return false;
            }

            boolean result = ((TemporaryFact<?>) fact.getValue()).isExpired(currentTimestamp);
            if (result && LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Fact change (DELETE EXPIRED): " + fact.getValue() + " - on: " + loggingContext);
            }
            return result;
        }).collect(Collectors.toList());

        expiredFacts.forEach(this::remove);

        getAnonymousFacts().removeIf(fact -> {
            boolean result = false;
            if (fact instanceof TemporaryFact<?>) {
                TemporaryFact<?> temporaryFact = (TemporaryFact<?>) fact;
                result = temporaryFact.isExpired(timerService.getCurrentTimeMillis());
            }
            if (result && LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Fact change (DELETE EXPIRED): " + fact + " - on: " + loggingContext);
            }
            return result;
        });
    }

    public boolean logFacts(Logger logger, Level level) {

        if (!logger.isLoggable(level)) {
            return false;
        }

        boolean haveLog = false;
        if (getAllFacts().count() > 0) {
            logger.log(level, "--------------------------------- CLOCK ---------------------------------");
            logger.log(level, Instant.ofEpochMilli(timerService.getCurrentTimeMillis()).toString());
            haveLog = true;
        }

        List<AttributeInfo> sortedAssetStates = new ArrayList<>(getAssetStates());
        sortedAssetStates.sort(Comparator.naturalOrder());
        if (sortedAssetStates.size() > 0) {
            logger.log(level, "--------------------------------- ASSET STATES (" + sortedAssetStates.size() + ") ---------------------------------");
            for (AttributeInfo assetState : sortedAssetStates) {
                logger.log(level, assetState.toString());
            }
            haveLog = true;
        }
        List<TemporaryFact<AttributeInfo>> sortedAssetEvents = new ArrayList<>(getAssetEvents());
        if (sortedAssetEvents.size() > 0) {
            logger.log(level, "--------------------------------- ASSET EVENTS (" + sortedAssetEvents.size() + ") ---------------------------------");
            sortedAssetEvents.sort(Comparator.comparing(TemporaryFact::getTime));
            for (TemporaryFact<AttributeInfo> assetEvent : sortedAssetEvents) {
                logger.log(level, assetEvent.toString());
            }
            haveLog = true;
        }
        Map<String, Object> namedFacts = getNamedFacts();
        List<String> names = new ArrayList<>(namedFacts.keySet());
        names.sort(Comparator.naturalOrder());
        if (names.size() > 0) {
            logger.log(level, "--------------------------------- NAMED FACTS (" + names.size() + ") ---------------------------------");
            for (String name : names) {
                logger.log(level, String.format("%s => %s", name, namedFacts.get(name)));
            }
            haveLog = true;
        }
        if (getAnonymousFacts().size() > 0) {
            logger.log(level, "--------------------------------- ANONYMOUS FACTS (" + getAnonymousFacts().size() + ") ---------------------------------");
            for (Object o : getAnonymousFacts()) {
                logger.log(level, String.format("%s", o));
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

    public RulesClock getClock() {
        return timerService;
    }

    public static Comparator<AttributeInfo> asComparator(AssetQuery.OrderBy orderBy) {

        Function<AttributeInfo, String> keyExtractor = AttributeInfo::getAssetName;
        boolean reverse = orderBy.descending;

        switch (orderBy.property) {

            case CREATED_ON:
                keyExtractor = assetState -> Long.toString(assetState.getCreatedOn().getTime());
                break;
            case ASSET_TYPE:
                keyExtractor = AttributeInfo::getAssetType;
                break;
            case PARENT_ID:
                keyExtractor = AttributeInfo::getParentId;
                break;
            case REALM:
                keyExtractor = AttributeInfo::getRealm;
                break;
        }

        Comparator<AttributeInfo> comparator = Comparator.comparing(keyExtractor);

        if (reverse) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    protected RulesFacts invalidateAssetStateAndDispatch(String assetId, String attributeName, Object value) {
        // Remove the asset state from the facts, it is invalid now
        getAssetStates()
                .removeIf(assetState -> {
                    boolean invalid = assetState.getId().equals(assetId) && assetState.getName().equals(attributeName);
                    if (invalid) {
                        if (LOG.isLoggable(Level.FINEST)) {
                            LOG.finest("Fact change (INTERNAL DELETE): " + assetState + " - on: " + loggingContext);
                        }
                        // Maintain index of all asset states for this asset by ID
                        Collection<AttributeInfo> assetIdIndexCollection = assetIdIndex.get(assetState.getId());
                        if (assetIdIndexCollection != null) {
                            assetIdIndexCollection.remove(assetState);
                        }
                        // Maintain index of all asset states for this asset by type
                        Collection<AttributeInfo> assetTypeIndexCollection = assetTypeIndex.get(assetState.getAssetType());
                        if (assetTypeIndexCollection != null) {
                            assetTypeIndexCollection.remove(assetState);
                        }
                    }
                    return invalid;
                });

        // Dispatch the update to the asset processing service
        AttributeEvent attributeEvent = new AttributeEvent(assetId, attributeName, value);
        LOG.finest("Dispatching " + attributeEvent + " - on: " + loggingContext);
        assetsFacade.dispatch(attributeEvent);

        return this;
    }

    protected void storeLocationPredicates(List<GeofencePredicate> foundLocationPredicates) {

        if (foundLocationPredicates != null && !foundLocationPredicates.isEmpty()) {
            LOG.finest("Location predicate found");
            // Collect asset states only where the attribute is location (location predicates only make sense when the location
            // attribute is exposed to rules - we don't support RULE_EVENT facts just RULE_STATE
            Collection<AttributeInfo> locationAssetStates = getAssetStates().stream().filter(assetState -> assetState.getName().equalsIgnoreCase(Asset.LOCATION.getName())).collect(Collectors.toSet());

            if (assetStateLocationPredicateMap == null) {
                assetStateLocationPredicateMap = new HashMap<>(locationAssetStates.size());
            }

            locationAssetStates.forEach(assetState -> {
                if (!assetStateLocationPredicateMap.containsKey(assetState.getId())) {
                    assetStateLocationPredicateMap.put(assetState.getId(), new HashSet<>());
                }
            });
            assetStateLocationPredicateMap.forEach((assetState, locationPredicates) -> locationPredicates.addAll(foundLocationPredicates));
        }
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
                haveLog = logFacts(LOG, Level.INFO);
            if (logVars)
                haveLog = haveLog || logVars(LOG);
            if (haveLog) {
                LOG.info("------------------------------------------------------------------------------");
            }
        }
    }

}
