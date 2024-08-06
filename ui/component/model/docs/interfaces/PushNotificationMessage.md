[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / PushNotificationMessage

# Interface: PushNotificationMessage

## Extends

- [`AbstractNotificationMessage`](AbstractNotificationMessage.md)

## Properties

### action?

> `optional` **action**: [`PushNotificationAction`](PushNotificationAction.md)

#### Source

model.ts:966

***

### body?

> `optional` **body**: `any`

#### Source

model.ts:965

***

### buttons?

> `optional` **buttons**: [`PushNotificationButton`](PushNotificationButton.md)[]

#### Source

model.ts:967

***

### data?

> `optional` **data**: `object`

#### Index signature

 \[`index`: `string`\]: `any`

#### Source

model.ts:968

***

### expiration?

> `optional` **expiration**: `any`

#### Source

model.ts:972

***

### priority?

> `optional` **priority**: [`PushNotificationMessageMessagePriority`](../enumerations/PushNotificationMessageMessagePriority.md)

#### Source

model.ts:969

***

### target?

> `optional` **target**: `any`

#### Source

model.ts:971

***

### targetType?

> `optional` **targetType**: [`PushNotificationMessageTargetType`](../enumerations/PushNotificationMessageTargetType.md)

#### Source

model.ts:970

***

### title?

> `optional` **title**: `any`

#### Source

model.ts:964

***

### type

> **type**: `"push"`

#### Overrides

[`AbstractNotificationMessage`](AbstractNotificationMessage.md).[`type`](AbstractNotificationMessage.md#type)

#### Source

model.ts:963
