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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.query.filter.StringPredicate;

public class MailAgentLink extends AgentLink<MailAgentLink> {

    @JsonPropertyDescription("The predicate to apply to incoming mail message subjects to determine if the message is" +
        " intended for the linked attribute. This must be defined to enable attributes to be updated by the linked" +
        " agent.")
    protected StringPredicate subjectMatchPredicate;
    @JsonPropertyDescription("The predicate to apply to incoming mail message from address(es) to determine if the" +
        " message is intended for the linked attribute. This must be defined to enable attributes to be updated by the" +
        " linked agent.")
    protected StringPredicate fromMatchPredicate;

    @JsonPropertyDescription("Use the subject as value instead of the body")
    protected Boolean useSubject;

    @JsonSerialize
    protected String getType() {
        return getClass().getSimpleName();
    }

    // For Hydrators
    protected MailAgentLink() {}

    public MailAgentLink(String id) {
        super(id);
    }

    public StringPredicate getSubjectMatchPredicate() {
        return subjectMatchPredicate;
    }

    public MailAgentLink setSubjectMatchPredicate(StringPredicate subjectMatchPredicate) {
        this.subjectMatchPredicate = subjectMatchPredicate;
        return this;
    }

    public StringPredicate getFromMatchPredicate() {
        return fromMatchPredicate;
    }

    public MailAgentLink setFromMatchPredicate(StringPredicate fromMatchPredicate) {
        this.fromMatchPredicate = fromMatchPredicate;
        return this;
    }

    public Boolean getUseSubject() {
        return useSubject;
    }

    public MailAgentLink setUseSubject(Boolean useSubject) {
        this.useSubject = useSubject;
        return this;
    }
}
