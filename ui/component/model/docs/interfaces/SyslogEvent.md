[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / SyslogEvent

# Interface: SyslogEvent

## Extends

- [`SharedEvent`](SharedEvent.md)

## Properties

### category?

> `optional` **category**: [`SyslogCategory`](../enumerations/SyslogCategory.md)

#### Source

model.ts:1582

***

### eventType

> **eventType**: `"syslog"`

#### Overrides

[`SharedEvent`](SharedEvent.md).[`eventType`](SharedEvent.md#eventtype)

#### Source

model.ts:1579

***

### id?

> `optional` **id**: `any`

#### Source

model.ts:1580

***

### level?

> `optional` **level**: [`SyslogLevel`](../enumerations/SyslogLevel.md)

#### Source

model.ts:1581

***

### message?

> `optional` **message**: `any`

#### Source

model.ts:1584

***

### subCategory?

> `optional` **subCategory**: `any`

#### Source

model.ts:1583

***

### timestamp?

> `optional` **timestamp**: `number`

#### Inherited from

[`SharedEvent`](SharedEvent.md).[`timestamp`](SharedEvent.md#timestamp)

#### Source

model.ts:724
