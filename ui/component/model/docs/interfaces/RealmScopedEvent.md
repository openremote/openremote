[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / RealmScopedEvent

# Interface: RealmScopedEvent

## Extends

- [`SharedEvent`](SharedEvent.md)

## Extended by

- [`GatewayConnectionStatusEvent`](GatewayConnectionStatusEvent.md)
- [`ProtocolDiscoveryImportRequestEvent`](ProtocolDiscoveryImportRequestEvent.md)
- [`ProtocolDiscoveryStartRequestEvent`](ProtocolDiscoveryStartRequestEvent.md)

## Properties

### eventType

> **eventType**: `"gateway-connection-status"` \| `"ProtocolDiscoveryImportRequestEvent"` \| `"ProtocolDiscoveryStartRequestEvent"`

#### Overrides

[`SharedEvent`](SharedEvent.md).[`eventType`](SharedEvent.md#eventtype)

#### Source

model.ts:769

***

### realm?

> `optional` **realm**: `any`

#### Source

model.ts:770

***

### timestamp?

> `optional` **timestamp**: `number`

#### Inherited from

[`SharedEvent`](SharedEvent.md).[`timestamp`](SharedEvent.md#timestamp)

#### Source

model.ts:724
