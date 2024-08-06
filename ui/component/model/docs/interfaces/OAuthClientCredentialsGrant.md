[**@openremote/model**](../README.md) • **Docs**

***

[@openremote/model](../globals.md) / OAuthClientCredentialsGrant

# Interface: OAuthClientCredentialsGrant

## Extends

- [`OAuthGrant`](OAuthGrant.md)

## Extended by

- [`OAuthPasswordGrant`](OAuthPasswordGrant.md)
- [`OAuthRefreshTokenGrant`](OAuthRefreshTokenGrant.md)

## Properties

### additionalValueMap?

> `optional` **additionalValueMap**: `object`

#### Index signature

 \[`index`: `string`\]: `any`[]

#### Inherited from

[`OAuthGrant`](OAuthGrant.md).[`additionalValueMap`](OAuthGrant.md#additionalvaluemap)

#### Source

model.ts:568

***

### basicAuthHeader?

> `optional` **basicAuthHeader**: `boolean`

#### Inherited from

[`OAuthGrant`](OAuthGrant.md).[`basicAuthHeader`](OAuthGrant.md#basicauthheader)

#### Source

model.ts:567

***

### client\_id?

> `optional` **client\_id**: `any`

#### Inherited from

[`OAuthGrant`](OAuthGrant.md).[`client_id`](OAuthGrant.md#client_id)

#### Source

model.ts:569

***

### client\_secret?

> `optional` **client\_secret**: `any`

#### Inherited from

[`OAuthGrant`](OAuthGrant.md).[`client_secret`](OAuthGrant.md#client_secret)

#### Source

model.ts:570

***

### grant\_type

> **grant\_type**: `"client_credentials"` \| `"password"` \| `"refresh_token"`

#### Overrides

[`OAuthGrant`](OAuthGrant.md).[`grant_type`](OAuthGrant.md#grant_type)

#### Source

model.ts:561

***

### scope?

> `optional` **scope**: `any`

#### Inherited from

[`OAuthGrant`](OAuthGrant.md).[`scope`](OAuthGrant.md#scope)

#### Source

model.ts:571

***

### tokenEndpointUri?

> `optional` **tokenEndpointUri**: `any`

#### Inherited from

[`OAuthGrant`](OAuthGrant.md).[`tokenEndpointUri`](OAuthGrant.md#tokenendpointuri)

#### Source

model.ts:566
