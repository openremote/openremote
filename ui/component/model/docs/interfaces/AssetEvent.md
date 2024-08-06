[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / AssetEvent

# Interface: AssetEvent

## Extends

- [`SharedEvent`](SharedEvent.md).[`AssetInfo`](AssetInfo.md)

## Properties

### asset?

> `optional` **asset**: [`Asset`](Asset.md)

#### Source

model.ts:53

***

### cause?

> `optional` **cause**: [`AssetEventCause`](../enumerations/AssetEventCause.md)

#### Source

model.ts:52

***

### eventType

> **eventType**: `"asset"`

#### Overrides

[`SharedEvent`](SharedEvent.md).[`eventType`](SharedEvent.md#eventtype)

#### Source

model.ts:51

***

### timestamp?

> `optional` **timestamp**: `number`

#### Inherited from

[`SharedEvent`](SharedEvent.md).[`timestamp`](SharedEvent.md#timestamp)

#### Source

model.ts:724

***

### updatedProperties?

> `optional` **updatedProperties**: `any`[]

#### Source

model.ts:54
