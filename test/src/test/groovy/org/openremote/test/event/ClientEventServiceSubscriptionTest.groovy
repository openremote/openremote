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
package org.openremote.test.event

import io.undertow.websockets.core.WebSocketChannel
import org.apache.camel.Exchange
import org.apache.camel.component.undertow.UndertowConstants
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.openremote.container.security.AuthContext
import org.openremote.manager.event.ClientEventService
import org.openremote.model.event.Event
import org.openremote.model.event.shared.CancelEventSubscription
import org.openremote.model.event.shared.EventSubscription
import org.openremote.model.event.shared.UnauthorizedEventSubscription
import org.openremote.model.alarm.AlarmEvent
import org.openremote.model.syslog.SyslogEvent
import spock.lang.Specification
import spock.lang.Unroll

import static org.openremote.model.Constants.AUTH_CONTEXT
import static org.openremote.model.Constants.REALM_PARAM_NAME

class ClientEventServiceSubscriptionTest extends Specification {

    def "subscribes and unsubscribes by event type and subscription id"() {
        given:
        def service = new TestClientEventService()
        def channel = dummyChannel()
        String sessionKey = "session-1"
        service.sessionChannels.put(sessionKey, channel)

        and:
        def subscription = new EventSubscription<>(SyslogEvent.class, null, "sub-1")

        when: "subscribing"
        service.procesInboundEvent(exchangeFor(sessionKey, channel, subscription))

        then:
        service.websocketSessionSubscriptionConsumers[sessionKey].size() == 1
        service.eventSubscriptions.size() == 1
        service.sentPayloads.last().is(subscription)

        when: "unsubscribing with event type + subscription id"
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new CancelEventSubscription(SyslogEvent.class, "sub-1")))

        then:
        !service.websocketSessionSubscriptionConsumers.containsKey(sessionKey)
        service.eventSubscriptions.empty
    }

    def "unsubscribes all matching subscriptions when only event type is provided"() {
        given:
        def service = new TestClientEventService()
        def channel = dummyChannel()
        String sessionKey = "session-2"
        service.sessionChannels.put(sessionKey, channel)

        and:
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new EventSubscription<>(SyslogEvent.class, null, "sub-a")))
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new EventSubscription<>(SyslogEvent.class, null, "sub-b")))
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new EventSubscription<>(AlarmEvent.class)))

        expect:
        service.websocketSessionSubscriptionConsumers[sessionKey].size() == 3

        when:
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new CancelEventSubscription(Event.getEventType(SyslogEvent.class), null)))

        then:
        service.websocketSessionSubscriptionConsumers[sessionKey].size() == 1
        service.websocketSessionSubscriptionConsumers[sessionKey].keySet() == ["alarm::"] as Set
        service.eventSubscriptions.size() == 1
    }

    def "unsubscribes all matching subscriptions when only subscription id is provided"() {
        given:
        def service = new TestClientEventService()
        def channel = dummyChannel()
        String sessionKey = "session-3"
        service.sessionChannels.put(sessionKey, channel)

        and:
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new EventSubscription<>(SyslogEvent.class, null, "shared-id")))
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new EventSubscription<>(AlarmEvent.class, null, "shared-id")))
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new EventSubscription<>(AlarmEvent.class)))

        expect:
        service.websocketSessionSubscriptionConsumers[sessionKey].size() == 3

        when:
        service.procesInboundEvent(exchangeFor(sessionKey, channel, new CancelEventSubscription("shared-id")))

        then:
        service.websocketSessionSubscriptionConsumers[sessionKey].size() == 1
        service.websocketSessionSubscriptionConsumers[sessionKey].keySet() == ["alarm::"] as Set
        service.eventSubscriptions.size() == 1
    }

    @Unroll
    def "rejects subscription when delimiter is present in #fieldName"() {
        given:
        def service = new TestClientEventService()
        def channel = dummyChannel()
        String sessionKey = "session-4"
        service.sessionChannels.put(sessionKey, channel)

        when:
        def exchange = exchangeFor(sessionKey, channel, subscription)
        service.procesInboundEvent(exchange)

        then:
        exchange.routeStop
        !service.websocketSessionSubscriptionConsumers.containsKey(sessionKey)
        service.eventSubscriptions.empty
        service.sentPayloads.size() == 1
        service.sentPayloads[0] instanceof UnauthorizedEventSubscription

        where:
        fieldName         | subscription
        "eventType"      | new EventSubscription<>("bad::event")
        "subscriptionId" | new EventSubscription<>(SyslogEvent.class, null, "bad::id")
    }

    private Exchange exchangeFor(String sessionKey, WebSocketChannel channel, Object body) {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext())
        exchange.in.body = body
        exchange.in.setHeader(UndertowConstants.CONNECTION_KEY, sessionKey)
        exchange.in.setHeader(UndertowConstants.CHANNEL, channel)
        exchange.in.setHeader(AUTH_CONTEXT, Stub(AuthContext))
        exchange.in.setHeader(REALM_PARAM_NAME, "master")
        exchange
    }

    private WebSocketChannel dummyChannel() {
        Stub(WebSocketChannel)
    }

    private static class TestClientEventService extends ClientEventService {
        final List<Object> sentPayloads = []

        @Override
        boolean authorizeEventSubscription(String realm, AuthContext authContext, EventSubscription<?> subscription) {
            return true
        }

        @Override
        void sendToWebsocketSession(String sessionKey, Object data) {
            sentPayloads.add(data)
        }
    }
}
