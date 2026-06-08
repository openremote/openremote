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

import org.openremote.container.timer.TimerService;
import org.openremote.model.rules.Ruleset;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroovySandboxReporter {

    public static final String LOGGER_NAME = "org.openremote.rules.GroovySandboxReport";

    protected static final Logger LOG = Logger.getLogger(LOGGER_NAME);

    protected final TimerService timerService;
    protected final int maxSignaturesPerRuleset;
    protected final ConcurrentMap<RulesetKey, RulesetReport> reports = new ConcurrentHashMap<>();

    public GroovySandboxReporter(TimerService timerService, int maxSignaturesPerRuleset) {
        this.timerService = Objects.requireNonNull(timerService, "timerService");
        if (maxSignaturesPerRuleset <= 0) {
            throw new IllegalArgumentException("maxSignaturesPerRuleset must be greater than zero");
        }
        this.maxSignaturesPerRuleset = maxSignaturesPerRuleset;
    }

    public void report(Ruleset ruleset, GroovySandboxSignature signature) {
        Objects.requireNonNull(ruleset, "ruleset");
        Objects.requireNonNull(signature, "signature");

        long now = timerService.getCurrentTimeMillis();
        RulesetReport report = reports.computeIfAbsent(new RulesetKey(ruleset), key -> new RulesetReport(ruleset, now));
        SignatureCounter counter = report.signatureCounters.get(signature);

        if (counter == null) {
            if (report.signatureCounters.size() >= maxSignaturesPerRuleset) {
                report.droppedSignatureCount.increment();
                if (report.capLogged.compareAndSet(false, true)) {
                    LOG.log(Level.WARNING, "Groovy sandbox report signature cap reached: {0}, maxSignatures= {1}",
                            new Object[] { report.summaryPrefix(), maxSignaturesPerRuleset });
                }
                return;
            }

            SignatureCounter newCounter = new SignatureCounter(signature, now);
            counter = report.signatureCounters.putIfAbsent(signature, newCounter);
            if (counter == null) {
                counter = newCounter;
                reportFirstSeenDangerous(report, signature);
            }
        }

        counter.increment(now);
    }

    public void flushAll() {
        reports.values().forEach(this::flush);
    }

    public void flush(Ruleset ruleset) {
        RulesetReport report = reports.get(new RulesetKey(ruleset));
        if (report != null) {
            flush(report);
        }
    }

    public void remove(Ruleset ruleset) {
        RulesetReport report = reports.remove(new RulesetKey(ruleset));
        if (report != null) {
            flush(report);
        }
    }

    protected void flush(RulesetReport report) {
        List<SignatureCounter> counters = new ArrayList<>(report.signatureCounters.values());
        counters.removeIf(counter -> counter.pendingCount() <= 0);
        long droppedSignatureCount = report.droppedSignatureCount.sum();
        long pendingDroppedSignatureCount = droppedSignatureCount - report.lastFlushedDroppedSignatureCount;
        if (counters.isEmpty() && pendingDroppedSignatureCount <= 0) {
            return;
        }

        counters.sort(Comparator
            .comparingInt((SignatureCounter counter) -> classificationPriority(counter.signature.classification()))
            .thenComparing(counter -> counter.signature.phase())
            .thenComparing(counter -> counter.signature.operation())
            .thenComparing(counter -> counter.signature.receiverType())
            .thenComparing(counter -> counter.signature.member()));

        LOG.log(Level.INFO, "Groovy sandbox report summary: {0}"
            + ", pendingSignatures= {1}"
            + ", uniqueSignatures= {2}"
            + ", droppedSignatures= {3}"
            + ", pendingDroppedSignatures= {4}"
            + ", maxSignatures= {5}",
            new Object[] { report.summaryPrefix(),  counters.size(), report.signatureCounters.size(),
                    droppedSignatureCount, pendingDroppedSignatureCount, maxSignaturesPerRuleset });

        report.lastFlushedDroppedSignatureCount = droppedSignatureCount;

        counters.forEach(counter -> {
            long count = counter.count.sum();
            long pendingCount = counter.markFlushed(count);
            LOG.log(Level.INFO, "Groovy sandbox report signature: {0}"
                + ", phase= {1}, operation= {2}, receiver= {3}, member= {4}, argTypes= {5}}, classification= {6}"
                + ", count= {7}, pendingCount= {8}, firstSeen= {9}, lastSeen= {10}",
                new Object[] { report.summaryPrefix(), counter.signature.phase(), counter.signature.operation(),
                        counter.signature.receiverType(), counter.signature.member(), counter.signature.argumentTypes(),
                        counter.signature.classification(), count, pendingCount, counter.firstSeen, counter.lastSeen });
        });
    }

    protected void reportFirstSeenDangerous(RulesetReport report, GroovySandboxSignature signature) {
        if (signature.classification() != GroovySandboxClassification.DANGEROUS) {
            return;
        }

        LOG.warning("Groovy sandbox report dangerous signature first seen: "
            + report.summaryPrefix()
            + ", phase=" + signature.phase()
            + ", operation=" + signature.operation()
            + ", receiver=" + signature.receiverType()
            + ", member=" + signature.member()
            + ", argTypes=" + signature.argumentTypes());
    }

    protected static int classificationPriority(GroovySandboxClassification classification) {
        return switch (classification) {
            case DANGEROUS -> 0;
            case UNKNOWN -> 1;
            case KNOWN -> 2;
        };
    }

    protected static class RulesetReport {
        protected final long rulesetId;
        protected final long rulesetVersion;
        protected final String rulesetName;
        protected final String rulesetType;
        protected final long createdOn;
        protected final ConcurrentMap<GroovySandboxSignature, SignatureCounter> signatureCounters = new ConcurrentHashMap<>();
        protected final AtomicBoolean capLogged = new AtomicBoolean(false);
        protected final LongAdder droppedSignatureCount = new LongAdder();
        protected volatile long lastFlushedDroppedSignatureCount;

        protected RulesetReport(Ruleset ruleset, long createdOn) {
            this.rulesetId = ruleset.getId();
            this.rulesetVersion = ruleset.getVersion();
            this.rulesetName = logToken(ruleset.getName());
            this.rulesetType = ruleset.getClass().getSimpleName();
            this.createdOn = createdOn;
        }

        protected String summaryPrefix() {
            return "rulesetId=" + rulesetId
                + ", rulesetVersion=" + rulesetVersion
                + ", rulesetName=\"" + rulesetName + "\""
                + ", rulesetType=" + rulesetType
                + ", createdOn=" + createdOn;
        }
    }

    protected static class SignatureCounter {
        protected final GroovySandboxSignature signature;
        protected final long firstSeen;
        protected volatile long lastSeen;
        protected volatile long lastFlushedCount;
        protected final LongAdder count = new LongAdder();

        protected SignatureCounter(GroovySandboxSignature signature, long firstSeen) {
            this.signature = signature;
            this.firstSeen = firstSeen;
            this.lastSeen = firstSeen;
        }

        protected void increment(long lastSeen) {
            this.lastSeen = lastSeen;
            count.increment();
        }

        protected long pendingCount() {
            return count.sum() - lastFlushedCount;
        }

        protected long markFlushed(long count) {
            long pendingCount = count - lastFlushedCount;
            lastFlushedCount = count;
            return pendingCount;
        }
    }

    protected record RulesetKey(long rulesetId, long rulesetVersion, String rulesetType) {

        protected RulesetKey(Ruleset ruleset) {
            this(
                ruleset.getId(),
                ruleset.getVersion(),
                ruleset.getClass().getSimpleName()
            );
        }
    }

    protected static String logToken(String value) {
        if (value == null) {
            return GroovySandboxSignature.NULL;
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}
