[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / ReadAttributeEvent

# Interface: ReadAttributeEvent

## Extends

- [`SharedEvent`](SharedEvent.md).[`HasAssetQuery`](HasAssetQuery.md)

## Properties

### assetQuery?

> `optional` **assetQuery**: [`AssetQuery`](AssetQuery.md)

#### Source

model.ts:119

***

### eventType

> **eventType**: `"read-asset-attribute"`

#### Overrides

[`SharedEvent`](SharedEvent.md).[`eventType`](SharedEvent.md#eventtype)

#### Source

model.ts:117

***

### ref?

> `optional` **ref**: [`AttributeRef`](AttributeRef.md)

#### Source

model.ts:118

***

### timestamp?

> `optional` **timestamp**: `number`

#### Inherited from

[`SharedEvent`](SharedEvent.md).[`timestamp`](SharedEvent.md#timestamp)

#### Source

model.ts:724
