[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / SharedEvent

# Interface: SharedEvent

## Extends

- [`Event`](Event.md)

## Extended by

- [`AssetEvent`](AssetEvent.md)
- [`AssetsEvent`](AssetsEvent.md)
- [`DeleteAssetsRequestEvent`](DeleteAssetsRequestEvent.md)
- [`DeleteAssetsResponseEvent`](DeleteAssetsResponseEvent.md)
- [`ReadAssetEvent`](ReadAssetEvent.md)
- [`ReadAssetsEvent`](ReadAssetsEvent.md)
- [`ReadAttributeEvent`](ReadAttributeEvent.md)
- [`AttributeEvent`](AttributeEvent.md)
- [`RealmScopedEvent`](RealmScopedEvent.md)
- [`GatewayDisconnectEvent`](GatewayDisconnectEvent.md)
- [`ProtocolDiscoveryAssetFoundEvent`](ProtocolDiscoveryAssetFoundEvent.md)
- [`ProtocolDiscoveryInstanceFoundEvent`](ProtocolDiscoveryInstanceFoundEvent.md)
- [`ProtocolDiscoveryStartStopResponseEvent`](ProtocolDiscoveryStartStopResponseEvent.md)
- [`ProtocolDiscoveryStopRequestEvent`](ProtocolDiscoveryStopRequestEvent.md)
- [`RulesEngineStatusEvent`](RulesEngineStatusEvent.md)
- [`RulesetChangedEvent`](RulesetChangedEvent.md)
- [`RequestSimulatorState`](RequestSimulatorState.md)
- [`SimulatorState`](SimulatorState.md)
- [`SyslogEvent`](SyslogEvent.md)

## Properties

### eventType

> **eventType**: `"asset"` \| `"assets"` \| `"delete-assets-request"` \| `"delete-assets-response"` \| `"read-asset"` \| `"read-assets"` \| `"read-asset-attribute"` \| `"attribute"` \| `"gateway-connection-status"` \| `"ProtocolDiscoveryImportRequestEvent"` \| `"ProtocolDiscoveryStartRequestEvent"` \| `"gateway-disconnect"` \| `"ProtocolDiscoveryAssetFoundEvent"` \| `"ProtocolDiscoveryInstanceFoundEvent"` \| `"ProtocolDiscoveryStartStopResponseEvent"` \| `"ProtocolDiscoveryStopRequestEvent"` \| `"rules-engine-status"` \| `"ruleset-changed"` \| `"request-simulator-state"` \| `"simulator-state"` \| `"syslog"`

#### Source

model.ts:774

***

### timestamp?

> `optional` **timestamp**: `number`

#### Inherited from

[`Event`](Event.md).[`timestamp`](Event.md#timestamp)

#### Source

model.ts:724
