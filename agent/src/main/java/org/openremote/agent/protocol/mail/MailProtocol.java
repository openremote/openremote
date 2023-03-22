/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.agent.protocol.mail;

import org.openremote.container.timer.TimerService;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.mail.MailMessage;
import org.openremote.model.query.filter.StringPredicate;

import java.util.Arrays;
import java.util.function.Predicate;

public class MailProtocol extends AbstractMailProtocol<MailAgent, MailProtocol, MailAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "Mail Client";

    public MailProtocol(MailAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "mailClient://" + agent.getHost().orElse("") + ":" + agent.getPort().map(Object::toString).orElse("") + "?username=" + agent.getUsernamePassword().map(UsernamePassword::getUsername).orElse("");
    }

    @Override
    protected String getMailMessageAttributeValue(String assetId, Attribute<?> attribute, MailAgentLink agentLink, MailMessage mailMessage) {
        boolean useSubject = agentLink.getUseSubject() != null && agentLink.getUseSubject();
        return useSubject ? mailMessage.getSubject() : mailMessage.getContent();
    }

    @Override
    protected Predicate<MailMessage> getAttributeMailMessageFilter(String assetId, Attribute<?> attribute, MailAgentLink agentLink) {
        return getAttributeMailMessageFilter(timerService, agentLink);
    }

    public static <T extends MailAgentLink> Predicate<MailMessage> getAttributeMailMessageFilter(TimerService timerService, T agentLink) {
        StringPredicate fromPredicate = agentLink.getFromMatchPredicate();
        StringPredicate subjectPredicate = agentLink.getSubjectMatchPredicate();

        Predicate<MailMessage> messageFilter = null;

        // Do message filtering
        if (fromPredicate != null) {
            Predicate<Object> valuePredicate = fromPredicate.asPredicate(timerService::getCurrentTimeMillis);
            messageFilter = m -> {
                String[] froms = m.getFrom();
                return Arrays.stream(froms).anyMatch(valuePredicate);
            };
        }
        if (subjectPredicate != null) {
            Predicate<Object> valuePredicate = subjectPredicate.asPredicate(timerService::getCurrentTimeMillis);
            Predicate<MailMessage> subjectFilter = m -> {
                String subject = m.getSubject();
                return valuePredicate.test(subject);
            };
            messageFilter = messageFilter != null ? messageFilter.and(subjectFilter) : subjectFilter;
        }

        return messageFilter;
    }
}
