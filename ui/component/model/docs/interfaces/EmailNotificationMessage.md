[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / EmailNotificationMessage

# Interface: EmailNotificationMessage

## Extends

- [`AbstractNotificationMessage`](AbstractNotificationMessage.md)

## Properties

### bcc?

> `optional` **bcc**: [`EmailNotificationMessageRecipient`](EmailNotificationMessageRecipient.md)[]

#### Source

model.ts:922

***

### cc?

> `optional` **cc**: [`EmailNotificationMessageRecipient`](EmailNotificationMessageRecipient.md)[]

#### Source

model.ts:921

***

### from?

> `optional` **from**: [`EmailNotificationMessageRecipient`](EmailNotificationMessageRecipient.md)

#### Source

model.ts:915

***

### html?

> `optional` **html**: `any`

#### Source

model.ts:919

***

### replyTo?

> `optional` **replyTo**: [`EmailNotificationMessageRecipient`](EmailNotificationMessageRecipient.md)

#### Source

model.ts:916

***

### subject?

> `optional` **subject**: `any`

#### Source

model.ts:917

***

### text?

> `optional` **text**: `any`

#### Source

model.ts:918

***

### to?

> `optional` **to**: [`EmailNotificationMessageRecipient`](EmailNotificationMessageRecipient.md)[]

#### Source

model.ts:920

***

### type

> **type**: `"email"`

#### Overrides

[`AbstractNotificationMessage`](AbstractNotificationMessage.md).[`type`](AbstractNotificationMessage.md#type)

#### Source

model.ts:914
