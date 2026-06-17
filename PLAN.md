# User-Defined Asset Types Plan

## Summary

Today, creating a new asset type in OpenRemote typically means adding a new Java subclass of `Asset`, defining static descriptors on that class, and letting the asset model registry discover it at startup.

The goal of this plan is to allow end users to define custom asset types through the UI, without requiring new Java code or a server restart.

The recommended approach is:

- Treat user-defined asset types as schema-defined asset types backed by `ThingAsset`
- Persist the type definitions separately from asset instances
- Convert those definitions into runtime `AssetTypeInfo` objects
- Reuse the existing descriptor-driven UI and validation flows
- Keep built-in Java asset types unchanged

Phase 1 decisions:

- custom asset type definitions are global to the manager
- definitions are stored in the database
- definition management is restricted to super admins
- custom types do not support inheritance
- custom types cannot inherit from Java asset types other than using `ThingAsset` as the backing class
- `ThingAsset` queries do not implicitly include custom asset types
- existing unknown-type fallback behavior is preserved

This avoids runtime Java class generation and fits the current architecture much better than trying to create JPA/Jackson subtypes dynamically.

## Why This Fits the Current Codebase

The current codebase already contains several pieces that make this feasible:

- The UI mostly renders asset types from `AssetTypeInfo` and `AttributeDescriptor[]`, not from hardcoded Java classes
- The backend already has a dynamic model provider in `AssetModelService`
- Dynamic asset descriptors can already exist without a backing Java type
- Validation is mostly descriptor-driven
- Persistence already contains a workaround for saving custom `type` values on `ThingAsset`

Relevant areas:

- [`model/src/main/java/org/openremote/model/asset/Asset.java`](model/src/main/java/org/openremote/model/asset/Asset.java)
- [`model/src/main/java/org/openremote/model/asset/AssetDescriptor.java`](model/src/main/java/org/openremote/model/asset/AssetDescriptor.java)
- [`model/src/main/java/org/openremote/model/asset/AssetTypeInfo.java`](model/src/main/java/org/openremote/model/asset/AssetTypeInfo.java)
- [`model/src/main/java/org/openremote/model/jackson/AssetTypeIdResolver.java`](model/src/main/java/org/openremote/model/jackson/AssetTypeIdResolver.java)
- [`model/src/main/java/org/openremote/model/util/ValueUtil.java`](model/src/main/java/org/openremote/model/util/ValueUtil.java)
- [`manager/src/main/java/org/openremote/manager/asset/AssetModelService.java`](manager/src/main/java/org/openremote/manager/asset/AssetModelService.java)
- [`manager/src/main/java/org/openremote/manager/asset/AssetStorageService.java`](manager/src/main/java/org/openremote/manager/asset/AssetStorageService.java)
- [`model/src/main/java/org/openremote/model/validation/AssetValidator.java`](model/src/main/java/org/openremote/model/validation/AssetValidator.java)
- [`ui/component/model/src/util.ts`](ui/component/model/src/util.ts)
- [`ui/component/or-asset-tree/src/or-add-asset-dialog.ts`](ui/component/or-asset-tree/src/or-add-asset-dialog.ts)

## Current State

### Asset model registration

The platform builds an asset model registry through `ValueUtil.initialise(...)`, aggregating data from `AssetModelProvider` implementations.

That registry produces:

- `AssetDescriptor`
- `AttributeDescriptor`
- `MetaItemDescriptor`
- `ValueDescriptor`
- `AssetTypeInfo`

This is the source of truth used across the backend and UI.

### Dynamic asset model support already exists

`AssetModelService` already implements `AssetModelProvider` and can load dynamic `AssetTypeInfo` instances from storage. This is an important signal that the architecture already anticipates custom asset types.

### Dynamic descriptors already exist conceptually

`AssetDescriptor` can exist with no backing Java class. In that case:

- `type == null`
- `isDynamic() == true`

That is exactly the shape we want for UI-defined asset types.

### The main gap

The platform still assumes that many asset types correspond to concrete Java classes, especially around:

- Jackson subtype resolution
- JPA inheritance
- startup-time asset model initialization
- runtime refresh of the asset model registry

So the missing work is not inventing a new concept. It is completing the runtime path so that schema-defined asset types are first-class citizens.

## Target Behavior

After this feature is implemented, an administrator should be able to:

1. Open a Manager UI page for custom asset types
2. Define a new asset type such as `BoilerAsset`, `ChargingStation`, or `MeetingRoom`
3. Configure:
   - label
   - icon
   - colour
   - attributes
   - value types
   - constraints
   - default values
   - labels and units
4. Save the definition
5. Immediately see the new type in the existing add-asset flows
6. Create assets of that type
7. Have those assets validated and rendered using the same descriptor-based infrastructure as built-in types

The user should not need to:

- write Java code
- rebuild the server
- restart the manager

## Core Design Decision

### Recommended design

Represent user-defined asset types as:

- persisted type definitions
- converted at runtime into dynamic `AssetTypeInfo`
- instantiated as `ThingAsset` at persistence and deserialization time
- carrying the custom `type` string end-to-end

### Why not generate Java subclasses

Generating Java classes at runtime is the wrong fit here because the current system uses:

- JPA single-table inheritance on `Asset`
- Jackson type resolution based on registered classes
- startup-time reflection and descriptor scanning

Runtime Java generation would create major complexity in:

- JPA discriminator handling
- classloading
- packaging
- cluster consistency
- testability
- operational safety

The platform already has a workable fallback model based on descriptors plus `ThingAsset`. That is the path to build on.

## Proposed Architecture

## 1. Persistent definition model

Introduce a dedicated model for user-defined asset type definitions.

Suggested fields:

- `name`
- `displayName`
- `icon`
- `colour`
- `description`
- `version`
- `enabled`
- `attributes[]`

Each attribute definition should include:

- `name`
- `label`
- `valueType`
- `required`
- `defaultValue`
- `units`
- `readOnly`
- `storeDataPoints`
- `constraints[]`
- `meta[]`
- `position` or ordering

Definitions are global in phase 1, so there is no realm or tenant scope field.

Custom type inheritance is not part of phase 1, so the definition model should not include `baseType`, `parentAssetType`, or equivalent inheritance fields.

### Storage choice

User-authored custom asset type definitions are stored in the database.

Pros:

- cluster-safe
- auditable
- permission-aware
- easier concurrency control
- easier API semantics

The existing filesystem-backed dynamic model loading can remain as an internal or legacy mechanism, but it is not the first-class storage path for user-authored definitions.

## 2. Runtime adapter from definition to `AssetTypeInfo`

Add a service that converts a persisted custom type definition into the existing runtime model:

- `AssetDescriptor<?>`
- `AttributeDescriptor<?>[]`
- optional `ValueDescriptor<?>[]`
- `AssetTypeInfo`

This adapter is the translation boundary between the user-authored schema and the current asset model engine.

### Conversion rules

#### Asset descriptor

Generate a dynamic descriptor:

- `name = custom type id`
- `icon = configured icon`
- `colour = configured colour`
- `type = null`

That ensures:

- it is visible in the UI like any other asset type
- it is recognized as dynamic
- it does not pretend to have a Java subclass

#### Attribute descriptors

Each configured attribute becomes an `AttributeDescriptor` with:

- attribute name
- selected `ValueDescriptor`
- optional flag
- units
- constraints
- meta items such as label and read-only

#### Value descriptors

Most custom asset attributes should use existing value types from the model registry.

Custom `ValueDescriptor` support should be treated as optional and deferred unless there is a real use case. In the first version, limit UI-defined attributes to already-supported value types.

This keeps the system simpler and safer.

## 3. Runtime asset model refresh

This is the most important platform change.

Today, the model registry is built through `ValueUtil.initialise(...)` and cached globally. Custom type definitions must become visible immediately after create, update, or delete.

Required capability:

- reload the effective asset model at runtime on the current manager node
- invalidate caches that depend on `AssetTypeInfo`
- keep cluster-wide refresh propagation as phase 2 work

### Design requirement

After saving a custom type definition:

- the backend must rebuild the runtime model safely
- subsequent model API calls must reflect the new type
- existing UI components must receive or request fresh model data
- other manager nodes may require their own refresh until phase 2 cluster propagation is added

### Risk area

The current code strongly assumes model initialization at startup. Adding refresh support must be designed carefully to avoid:

- inconsistent reads during refresh
- partial model state
- stale caches in long-lived services

### Recommended approach

Introduce a model registry service abstraction rather than letting every caller depend on raw static state. If a full refactor is too large, then add a controlled refresh entry point around the existing `ValueUtil` initialization path and guard it with synchronization.

## 4. Dynamic type resolution to `ThingAsset`

User-defined types should be stored and deserialized as `ThingAsset`.

The asset instance still carries its custom `type` string, but the underlying Java class is `ThingAsset`.

### Required rules

- Built-in descriptor with backing Java class:
  - deserialize to that concrete class
- Dynamic descriptor with no backing class:
  - deserialize to `ThingAsset`
- Unknown type:
  - continue using the current fallback behavior

Unknown fallback types must not be treated as user-defined asset types. They remain generic fallback assets unless a super admin explicitly creates a matching custom asset type definition.

Normal UI asset creation should only offer built-in asset types and saved custom asset type definitions. Advanced, import, or API paths that submit an unknown type can keep working through the existing fallback behavior, but the UI should warn users where practical that descriptor-driven validation and generated custom-type UI will not apply.

If a super admin creates a custom type whose name already exists on stored fallback assets, the UI/API should provide a usage warning and require confirmation because those assets will begin resolving against the new schema.

### Why this works

Most of the platform logic that matters for custom types is descriptor-based:

- required attributes
- attribute labels
- attribute types
- value validation
- optional fields shown in the UI

This means the instance class can stay generic while the behavior comes from the dynamic model definition.

### Type matching and inheritance rules

Custom types do not support inheritance in phase 1.

Rules:

- every custom type is an independent schema definition
- every custom type uses `ThingAsset` as its Java backing class
- custom types cannot inherit from another custom type
- custom types cannot inherit from built-in Java asset types other than using `ThingAsset` as the backing class
- queries for `ThingAsset` must not implicitly include custom asset types
- parent/group checks should compare the exact stored asset `type` string for custom types
- `GroupAsset.childAssetType = ThingAsset` must not match all custom types automatically
- `GroupAsset.childAssetType = SomeCustomType` should match only assets whose stored `type` is exactly `SomeCustomType`

## 5. Validation stays descriptor-driven

`AssetValidator` already validates assets primarily against `AssetTypeInfo`.

That should remain the rule:

- custom type definitions define required attributes
- constraints are applied through descriptors and meta
- assets of custom types validate like built-in types

### Additional validation needed

Add validation for the type-definition authoring flow itself:

- type name must be unique
- type name must not clash with built-in asset types
- attribute names must be unique within the type
- only supported value types can be selected
- unsupported meta items must be rejected
- defaults must conform to the selected value type
- constraints must be valid for the chosen value type

## 6. Manager API changes

Do not overload the existing read-only model resource for definition management.

Introduce a dedicated API for custom asset type definitions.

Suggested operations:

- `GET /custom-asset-types`
- `GET /custom-asset-types/{name}`
- `POST /custom-asset-types`
- `PUT /custom-asset-types/{name}`
- `DELETE /custom-asset-types/{name}`
- `POST /custom-asset-types/{name}/validate`

Optional:

- `POST /custom-asset-types/{name}/preview`
- `GET /custom-asset-types/{name}/usage`

### Responsibility split

- Definition API:
  - manages authored custom schemas
- Existing `/model` API:
  - exposes the effective merged runtime model used by the rest of the platform

Definition management must be restricted to super admins in phase 1.

That keeps the architecture clean.

## 7. Manager UI changes

Add a dedicated admin page for custom asset type management.

Suggested capabilities:

- list all custom asset types
- create a new type
- edit an existing type
- delete a type
- preview how the type will appear in the add-asset dialog
- preview generated attributes and validation rules
- warn when creating a type name that already exists on fallback assets

### Type editor form

The first useful version should support:

- display name
- technical name
- icon
- colour
- description
- add/remove/reorder attributes while authoring a new definition
- add optional attributes to a definition after assets exist
- select value type
- required or optional
- label
- units
- read-only
- datapoint storage flag
- simple constraints:
  - min
  - max
  - pattern
  - allowed values

### UI integration points that should then work automatically

Once the runtime model exposes the new `AssetTypeInfo`, these existing consumers should pick it up with limited or no custom logic:

- asset add dialog
- attribute pickers
- asset viewer/editor
- asset tree filters
- dashboard asset-type selectors

The normal add-asset flow should only present built-in asset types and saved custom asset type definitions. It should not silently create new fallback type names.

Some follow-up fixes may still be needed where components assume built-in asset types or concrete Java-backed behavior.

## Scope boundaries

This feature should explicitly cover schema-defined asset types, not executable asset behavior.

### In scope

- new user-defined type name
- icon, colour, label
- attribute schema
- validation
- UI-driven creation and editing
- use in existing asset creation/editing flows

### Out of scope for first version

- runtime Java methods on custom asset types
- protocol-specific behavior implemented through subclass code
- dynamic agent subclasses
- custom type inheritance
- custom types backed by Java asset classes other than `ThingAsset`
- parent/child constraint modeling for custom types
- complex custom value object definitions
- arbitrary business logic injection

Built-in types that require behavior beyond schema should remain Java-defined.

## Data model evolution rules

Schema changes after assets already exist are deliberately restricted in phase 1.

Phase 1 rule:

- after a custom type has asset instances, only adding optional attributes is supported
- incompatible schema edits are blocked

Blocked changes include:

- adding a required attribute
- renaming an attribute
- changing an attribute value type
- removing an attribute
- tightening constraints
- changing default value semantics
- deleting a type that still has asset instances

## Permissions and scope

Custom asset type definitions are global to the manager in phase 1.

Definition management is restricted to super admins.

This means:

- the storage model does not include realm scope
- the API should enforce super-admin authorization
- the Manager UI should expose the custom type management page only to super admins
- the effective runtime model is the same for all realms on a manager node

## Suggested implementation phases

## Phase 1: Foundation

Goal:

- get end-to-end support for user-defined asset types using `ThingAsset`

Deliverables:

- database-backed persistent definition model
- CRUD API for custom asset type definitions
- conversion service from definition to `AssetTypeInfo`
- runtime asset model refresh on the current manager node
- dynamic type resolution to `ThingAsset` for known custom types while preserving unknown-type fallback
- exact stored-type matching for custom types in query and group-related paths
- basic Manager UI for authoring
- visibility in existing add/edit asset flows

Success criteria:

- super admin can define a custom type
- type appears in the UI without restart
- assets of that type can be created and updated
- validation works
- querying for `ThingAsset` does not include custom types
- `GroupAsset.childAssetType` matches custom types only by exact stored type name

## Phase 2: Hardening

Goal:

- make the feature safe for long-term usage

Deliverables:

- broader schema compatibility checks if additional edit operations are introduced
- usage inspection before delete/update
- cluster refresh propagation
- audit trail
- better authorization rules
- import/export of custom type definitions

Success criteria:

- updates are safe
- multi-node behavior is reliable
- operational visibility exists

## Recommended implementation order

1. Define and agree the persistence model for custom type definitions
2. Add backend CRUD API
3. Build definition-to-`AssetTypeInfo` conversion
4. Implement safe runtime model refresh
5. Verify type resolution so known dynamic types deserialize as `ThingAsset` while unknown fallback behavior is preserved
6. Build the Manager UI authoring page
7. Fix integration gaps in existing asset add/edit screens
8. Fix query and `GroupAsset.childAssetType` behavior for exact custom type matching
9. Add phase 1 schema edit checks
10. Add tests

This order reduces risk because the core platform behavior is established before the UI depends on it.

## Affected backend areas

Likely areas to touch:

- asset model provider and registry initialization
- asset model refresh lifecycle
- dynamic descriptor loading
- definition storage and API
- Jackson type resolution
- validation
- asset persistence behavior for custom `type` values

Likely files or nearby areas:

- [`model/src/main/java/org/openremote/model/asset/AssetDescriptor.java`](model/src/main/java/org/openremote/model/asset/AssetDescriptor.java)
- [`model/src/main/java/org/openremote/model/asset/AssetTypeInfo.java`](model/src/main/java/org/openremote/model/asset/AssetTypeInfo.java)
- [`model/src/main/java/org/openremote/model/jackson/AssetTypeIdResolver.java`](model/src/main/java/org/openremote/model/jackson/AssetTypeIdResolver.java)
- [`model/src/main/java/org/openremote/model/util/ValueUtil.java`](model/src/main/java/org/openremote/model/util/ValueUtil.java)
- [`manager/src/main/java/org/openremote/manager/asset/AssetModelService.java`](manager/src/main/java/org/openremote/manager/asset/AssetModelService.java)
- [`manager/src/main/java/org/openremote/manager/asset/AssetModelResourceImpl.java`](manager/src/main/java/org/openremote/manager/asset/AssetModelResourceImpl.java)
- [`manager/src/main/java/org/openremote/manager/asset/AssetStorageService.java`](manager/src/main/java/org/openremote/manager/asset/AssetStorageService.java)
- [`model/src/main/java/org/openremote/model/query/AssetQuery.java`](model/src/main/java/org/openremote/model/query/AssetQuery.java)
- [`model/src/main/java/org/openremote/model/validation/AssetValidator.java`](model/src/main/java/org/openremote/model/validation/AssetValidator.java)

## Affected UI areas

Likely areas to touch:

- new custom asset type management page
- type-definition form components
- model reload behavior in the Manager app
- add-asset flow
- edit-asset flow
- attribute rendering where assumptions about built-in types still exist

Likely files or nearby areas:

- [`ui/component/model/src/util.ts`](ui/component/model/src/util.ts)
- [`ui/component/or-asset-tree/src/or-add-asset-dialog.ts`](ui/component/or-asset-tree/src/or-add-asset-dialog.ts)
- [`ui/component/or-asset-tree/src/index.ts`](ui/component/or-asset-tree/src/index.ts)
- [`ui/component/or-asset-viewer/src/index.ts`](ui/component/or-asset-viewer/src/index.ts)
- [`ui/component/or-asset-viewer/src/or-edit-asset-panel.ts`](ui/component/or-asset-viewer/src/or-edit-asset-panel.ts)
- manager page modules under [`ui/app/manager/src/pages`](ui/app/manager/src/pages)

## Testing plan

### Backend tests

- create definition and verify generated `AssetTypeInfo`
- refresh model and verify new type is visible
- create asset of custom type and verify persistence
- read asset back and verify type remains custom
- validate required attributes and invalid values
- reject conflicting type names
- reject invalid schema definitions
- verify delete/update compatibility checks
- verify unknown type fallback is preserved
- verify fallback assets are not treated as authored custom types
- verify `ThingAsset` queries do not include custom asset types
- verify `GroupAsset.childAssetType` exact matching for custom types

### UI tests

- create custom type through the UI
- see type in add-asset dialog
- create asset of that type
- edit asset attributes using generated schema
- verify validation messages
- verify updated type definitions are reflected without restart
- verify normal add-asset UI does not create unknown fallback type names
- verify warning or confirmation when defining a type name already used by fallback assets

### Operational tests

- restart manager and verify definitions persist
- verify model refresh works on the current manager node
- cluster-wide refresh propagation belongs to phase 2 testing

## Main risks

### 1. Runtime model refresh complexity

The existing model infrastructure is startup-oriented. Refreshing it safely is the highest-risk part of the feature.

### 2. Mixed static and dynamic model semantics

Built-in asset types are Java-backed. Custom asset types are schema-backed. The system must keep both working cleanly.

### 3. Hidden assumptions in the UI

Most of the UI is descriptor-driven, but some components may still assume built-in asset types or fixed attribute sets.

### 4. Schema evolution

Allowing admins to change a type after assets already exist can easily break data if compatibility rules are weak.

### 5. Multi-node consistency

If the manager is deployed in a clustered setup, phase 1 only guarantees refresh on the current manager node. Cluster-wide propagation is phase 2 work.

## Recommended constraints for first release

To reduce risk, the first release should deliberately limit what end users can define.

Recommended constraints:

- custom types map to `ThingAsset` only
- use existing value types only
- expose only a conservative subset of existing scalar and common platform value types in the first UI
- no custom type inheritance
- no custom types backed by Java asset classes other than `ThingAsset`
- no parent/child constraint model for custom types
- no implicit inclusion of custom types when querying for `ThingAsset`
- exact stored-type matching for `GroupAsset.childAssetType`
- no user-defined agent types
- no custom executable logic
- no runtime method generation
- no arbitrary custom Java object types
- preserve existing unknown-type fallback behavior
- block incompatible schema edits rather than auto-migrating

These constraints still deliver substantial value while keeping the implementation realistic.

## Resolved decisions

The following phase 1 decisions have been made:

1. Custom asset type definitions are global, not realm-scoped.
2. User-authored definitions are stored in the database.
3. Definition management is restricted to super admins.
4. Custom types do not support inheritance.
5. Custom types use `ThingAsset` as their Java backing class.
6. Existing unknown-type fallback behavior is preserved.
7. The normal UI flow only creates assets from built-in types or saved custom definitions.
8. After a custom type has asset instances, phase 1 only supports adding optional attributes.
9. Cluster-wide live refresh is deferred to phase 2.
10. Phase 1 exposes a conservative subset of existing value types.
11. Parent/child constraint modeling is deferred.
12. `ThingAsset` queries do not include custom asset types.
13. `GroupAsset.childAssetType` uses exact stored type matching for custom types.

## Final recommendation

Proceed with a schema-defined model for user-authored asset types, backed by dynamic `AssetTypeInfo` and instantiated as `ThingAsset`.

That approach:

- aligns with the current descriptor-based architecture
- reuses the existing validation and UI model infrastructure
- avoids runtime Java generation
- keeps built-in asset types intact
- provides a clear phased rollout path

The critical enabling work is runtime model refresh. Once that is in place, the rest of the feature becomes an extension of patterns the codebase already uses.
