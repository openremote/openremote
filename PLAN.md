# User-Defined Asset Types Plan

## Goal

Allow super admins to define asset types through the Manager UI without Java code, a rebuild, or a Manager restart.

Phase 1 supports schema-defined asset types only. A custom asset type contributes descriptors to the runtime asset model, and assets of that type are stored as `ThingAsset` instances while preserving the custom `type` string.

## Phase 1 Decisions

- Custom asset type definitions are global to the manager.
- Definitions are stored in the database.
- Definition management is restricted to super admins.
- Custom types do not support inheritance.
- Custom types use `ThingAsset` as their Java backing class.
- Custom types do not inherit from Java asset classes other than using `ThingAsset` as the backing class.
- `ThingAsset` queries do not implicitly include custom asset types.
- `GroupAsset.childAssetType` uses exact stored type matching for custom types.
- Existing unknown-type fallback behavior is preserved.
- The normal add-asset UI only creates assets from built-in types or saved custom definitions.
- Phase 1 exposes a conservative subset of existing value types.
- After assets exist for a custom type, phase 1 only supports adding optional attributes.
- Runtime model refresh is local to the current manager node; cluster-wide propagation is phase 2.

## Non-Goals

- Runtime Java class generation.
- User-defined agent types.
- Custom executable behavior.
- Custom value object definitions.
- Custom type inheritance.
- Parent/child constraint modeling for custom types.
- Cluster-wide live refresh in phase 1.

## Target Behavior

1. A super admin creates `BoilerAsset` with label, icon, colour, description, and attributes.
2. The definition is persisted in the database.
3. The current manager node refreshes its runtime asset model.
4. `/model` exposes a dynamic `AssetTypeInfo` for `BoilerAsset`.
5. The add-asset UI shows `BoilerAsset`.
6. Creating a `BoilerAsset` persists a `ThingAsset` row whose stored `TYPE` is `BoilerAsset`.
7. Reading the asset back preserves `type = "BoilerAsset"`.
8. Descriptor-driven validation and UI rendering use the generated custom type descriptors.
9. Querying for `ThingAsset` does not return `BoilerAsset` assets unless `BoilerAsset` is explicitly requested.
10. A group configured with `childAssetType = "BoilerAsset"` accepts only assets whose stored type is exactly `BoilerAsset`.

Unknown asset types continue to fall back to the existing generic behavior. They are not treated as authored custom types unless a super admin creates a matching definition. If a new definition name already exists on fallback assets, the UI/API should warn and require confirmation because those assets will start resolving against the new schema.

## Data Model

Create a database-backed definition model. Suggested table: `CUSTOM_ASSET_TYPE`.

Suggested columns:

- `NAME`: primary key, globally unique custom type name.
- `DISPLAY_NAME`.
- `ICON`.
- `COLOUR`.
- `DESCRIPTION`.
- `ENABLED`.
- `ATTRIBUTES`: JSON/JSONB array of authored attribute definitions.
- `VERSION`: optimistic locking.
- `CREATED_ON`, `CREATED_BY`, `UPDATED_ON`, `UPDATED_BY`.

Suggested attribute definition fields:

- `name`.
- `type`: existing `ValueDescriptor` name, for example `number`, `text`, `boolean`, or `number[]`.
- `optional`.
- `defaultValue`.
- `units`.
- `format`.
- `constraints`.
- `meta`.
- `position`.

Convenience fields exposed in the UI, such as label, read-only, and datapoint storage, should be stored as existing meta items in `meta`:

- `label` maps to `MetaItemType.LABEL`.
- `readOnly`.
- `storeDataPoints`.

The persisted attribute definition should reuse existing value-model classes where they are stable authored data:

- `ValueConstraint[]` for `constraints`.
- `ValueFormat` for `format`.
- `MetaMap` / `MetaItem` for `meta`.

It should not persist runtime registry objects directly:

- Store a value type as a `String` descriptor name, not as a `ValueDescriptor`.
- Do not store `AssetDescriptor`, `AttributeDescriptor`, or `AssetTypeInfo` as the database representation.

Do not include realm scope, `baseType`, `parentAssetType`, or any inheritance field in phase 1.

The existing filesystem-backed dynamic model loading may remain as an internal or legacy mechanism, but it is not the storage path for user-authored definitions.

### Mapping to Runtime Model

The database row is an authored schema. The runtime model is generated from it.

```text
CUSTOM_ASSET_TYPE
  NAME -------------------------------> AssetDescriptor.name
  ICON -------------------------------> AssetDescriptor.icon
  COLOUR -----------------------------> AssetDescriptor.colour
  DISPLAY_NAME -----------------------> attribute/type label metadata or UI label source
  ENABLED ----------------------------> include/exclude from AssetModelProvider

  ATTRIBUTES[n].name -----------------> AttributeDescriptor.name
  ATTRIBUTES[n].type --lookup--------> ValueDescriptor from ValueUtil
  ATTRIBUTES[n].optional ------------> AttributeDescriptor.optional
  ATTRIBUTES[n].constraints ---------> AttributeDescriptor.constraints
  ATTRIBUTES[n].format --------------> AttributeDescriptor.format
  ATTRIBUTES[n].units ---------------> AttributeDescriptor.units
  ATTRIBUTES[n].meta ----------------> AttributeDescriptor.meta
  ATTRIBUTES[n].position ------------> ordering before AssetTypeInfo construction

  ATTRIBUTES[n].defaultValue --------> asset creation / initialization helper only
                                      not part of AttributeDescriptor today
```

Generated runtime object:

```text
AssetTypeInfo
  assetDescriptor:
    new AssetDescriptor(name, icon, colour)
    type == null
    dynamic == true

  attributeDescriptors:
    platform baseline descriptors, if we decide custom assets should expose common Asset/ThingAsset attributes
    + generated AttributeDescriptor[] from CUSTOM_ASSET_TYPE.ATTRIBUTES

  metaItemDescriptors:
    [] in phase 1, because custom definitions reuse existing global meta descriptors

  valueDescriptors:
    [] in phase 1, because custom definitions reuse existing global value descriptors
```

The platform baseline descriptor question should be handled in the converter, not by adding an inheritance field to the database. If custom assets should expose common `Asset` / `ThingAsset` attributes such as `location`, the converter must compose those descriptors into the generated `AssetTypeInfo`; the persisted custom definition should still contain only user-authored attributes.

## Implementation Plan

Each slice should start with automated tests that describe the intended behavior. Do not wait until the end of the feature to add coverage.

### 1. Definition Persistence

Build the database model and persistence service for custom type definitions.

Implementation work:

- Add persistence entity/table for `CUSTOM_ASSET_TYPE`.
- Add API/domain DTOs for `CustomAssetTypeDefinition` and `CustomAssetTypeAttributeDefinition`.
- Store authored attribute definitions as structured JSON for phase 1.
- Store `type` as a `ValueDescriptor` name string.
- Store constraints, format, and meta using `ValueConstraint[]`, `ValueFormat`, and `MetaMap`.
- Do not store `AssetTypeInfo`, `AssetDescriptor`, or `AttributeDescriptor` directly.
- Add service methods for create, read, update, delete, list, and usage count by type name.
- Keep the definition name immutable after creation.

Tests first:

- Persist and read back a definition with multiple attributes.
- Persist and read back constraints, format, units, and meta.
- Verify value types are stored as descriptor names, not serialized `ValueDescriptor` objects.
- Reject duplicate definition names.
- Verify definitions are global and do not carry realm scope.
- Verify optimistic locking/version behavior on update.
- Verify usage count returns assets whose stored `TYPE` equals the definition name.

Likely areas:

- `manager/src/main/java/org/openremote/manager/asset`
- persistence migration/setup code
- `model/src/main/java/org/openremote/model/asset`

### 2. Definition Validation

Create a validator for authored definitions before they are persisted or converted into runtime descriptors.

Implementation work:

- Validate type name format and global uniqueness.
- Reject names that clash with built-in asset descriptors.
- Reject inheritance fields if present in client payloads.
- Reject duplicate attribute names.
- Restrict value types to the phase 1 allowlist.
- Validate default values against the selected value type.
- Validate constraints against the selected value type.
- Reject unsupported meta items.
- When assets already exist for a type, allow only adding optional attributes.
- Block deletion while assets exist.

Tests first:

- Reject collision with a built-in asset type.
- Reject invalid type and attribute names.
- Reject duplicate attribute names.
- Reject unsupported value types.
- Reject default values that do not match the selected value type.
- Reject incompatible updates once assets exist.
- Allow adding an optional attribute once assets exist.
- Reject deleting a type that has assets.

Likely areas:

- `model/src/main/java/org/openremote/model/value`
- `manager/src/main/java/org/openremote/manager/asset`

### 3. Definition to `AssetTypeInfo` Conversion

Convert persisted definitions into the descriptor model already used by backend validation and UI rendering.

Implementation work:

- Add a conversion service, for example `CustomAssetTypeInfoFactory`.
- Generate a dynamic `AssetDescriptor` with:
  - `name = definition.name`
  - `icon = definition.icon`
  - `colour = definition.colour`
  - `type = null`
- Convert each attribute definition into an `AttributeDescriptor`.
- Reuse existing `ValueDescriptor`s from the model registry.
- Sort generated attributes by `position` before constructing `AssetTypeInfo`.
- Add label, units, read-only, constraints, and datapoint storage metadata through existing descriptor/meta mechanisms.
- Keep `defaultValue` out of `AttributeDescriptor`; use it in asset creation/initialization helpers.
- Decide in this converter whether common `Asset` / `ThingAsset` attribute descriptors should be composed into every custom type.
- Do not create custom `ValueDescriptor`s in phase 1.

Tests first:

- Generated descriptor is dynamic and has no Java class.
- Generated `AssetTypeInfo` contains the expected attributes in the expected order.
- Attribute descriptors reference existing value descriptors.
- Generated metadata drives labels, units, read-only flags, constraints, and datapoint storage flags.
- Default values are not encoded into `AttributeDescriptor`.
- Baseline `Asset` / `ThingAsset` descriptors are included or excluded consistently with the converter decision.
- Invalid definitions cannot be converted.

Likely areas:

- `model/src/main/java/org/openremote/model/asset/AssetDescriptor.java`
- `model/src/main/java/org/openremote/model/asset/AssetTypeInfo.java`
- `model/src/main/java/org/openremote/model/value`
- `manager/src/main/java/org/openremote/manager/asset`

### 4. Runtime Model Provider and Local Refresh

Make database-backed definitions part of the effective asset model and refresh the current manager node after definition changes.

Implementation work:

- Extend or add an `AssetModelProvider` that exposes enabled custom definitions as dynamic descriptors.
- Load definitions from the database through the conversion service.
- Add a controlled refresh entry point on `AssetModelService`.
- Refresh the local runtime model after create, update, delete, enable, or disable.
- Guard refresh with synchronization so readers never see partially rebuilt model state.
- Rebuild dependent schema/value caches after refresh.
- Keep cluster-wide refresh propagation out of phase 1.

Tests first:

- Before refresh, a newly persisted definition is not visible in `ValueUtil`.
- After refresh, `ValueUtil.getAssetInfo(customType)` returns the generated info.
- Updating a definition and refreshing replaces the runtime descriptors.
- Disabling or deleting a definition and refreshing removes it from the runtime model.
- Concurrent reads during refresh either see the old complete model or the new complete model.

Likely areas:

- `manager/src/main/java/org/openremote/manager/asset/AssetModelService.java`
- `model/src/main/java/org/openremote/model/util/ValueUtil.java`
- `model/src/main/java/org/openremote/model/AssetModelProvider.java`

### 5. Manager API

Expose definition management through a dedicated API instead of overloading the read-only model resource.

Implementation work:

- Add a resource such as `/custom-asset-types`.
- Operations:
  - `GET /custom-asset-types`
  - `GET /custom-asset-types/{name}`
  - `POST /custom-asset-types`
  - `PUT /custom-asset-types/{name}`
  - `DELETE /custom-asset-types/{name}`
  - `POST /custom-asset-types/{name}/validate`
  - `GET /custom-asset-types/{name}/usage`
- Enforce super-admin authorization on all write operations and management reads.
- Return validation errors without mutating state.
- Return conflict responses for duplicate names and incompatible updates.
- Trigger local model refresh after successful mutations.
- When creating a type name that already exists on fallback assets, require an explicit confirmation flag.

Tests first:

- Super admin can create, update, delete, validate, and list definitions.
- Non-super-admin users receive `403`.
- Invalid create/update requests return validation details.
- Create/update/delete triggers local model refresh.
- Creating a type name already used by fallback assets is blocked without confirmation.
- Confirmed creation for an existing fallback type succeeds and refreshes descriptors.

Likely areas:

- `model/src/main/java/org/openremote/model/asset`
- `manager/src/main/java/org/openremote/manager/asset`
- `manager/src/main/java/org/openremote/manager/web`

### 6. Asset Deserialization, Validation, and Persistence

Ensure assets of known custom types behave as descriptor-backed `ThingAsset` instances while unknown fallback behavior remains unchanged.

Implementation work:

- Verify `AssetTypeIdResolver` maps known dynamic descriptors to `ThingAsset`.
- Preserve unknown-type fallback behavior.
- Ensure attribute deserialization can use the JSON `type` id, not only the resolved Java class name, so custom attribute descriptors are available while parsing attributes.
- Ensure `AssetValidator` applies generated custom descriptors to known custom types.
- Ensure required custom attributes are validated.
- Preserve the existing persistence workaround that stores custom `TYPE` values for `ThingAsset`.
- Add helper code if needed so server-side custom asset creation can initialize required attributes from the custom type descriptor.

Tests first:

- JSON asset with `type = customType` deserializes as `ThingAsset` and retains the custom type string.
- Custom attribute values deserialize using the generated custom attribute descriptors.
- Missing required custom attributes fail validation.
- Invalid custom attribute values fail validation.
- Unknown type still falls back to generic behavior.
- Saving and reading a custom asset preserves the custom stored `TYPE`.

Likely areas:

- `model/src/main/java/org/openremote/model/jackson/AssetTypeIdResolver.java`
- `model/src/main/java/org/openremote/model/asset/Asset.java`
- `model/src/main/java/org/openremote/model/attribute/Attribute.java`
- `model/src/main/java/org/openremote/model/validation/AssetValidator.java`
- `manager/src/main/java/org/openremote/manager/asset/AssetStorageService.java`

### 7. Query and Group Type Matching

Make stored custom type names the source of truth for query and group behavior.

Implementation work:

- Verify `AssetQuery.getResolvedAssetTypes(["ThingAsset"])` does not include custom dynamic types.
- If needed, adjust type resolution so dynamic custom types are not treated as Java subclasses of `ThingAsset`.
- Update `GroupAsset.childAssetType` validation:
  - for custom dynamic types, compare `childAssetType` to `asset.getType()`
  - `childAssetType = ThingAsset` should match only stored `type = ThingAsset`, not all custom types
  - preserve existing Java hierarchy behavior for built-in Java-backed types where appropriate
- Ensure rule/query code that uses resolved asset types follows the same semantics.

Tests first:

- Query for `ThingAsset` excludes `BoilerAsset`.
- Query for `BoilerAsset` returns only `BoilerAsset`.
- Query for both returns both.
- Group configured for `BoilerAsset` accepts `BoilerAsset`.
- Group configured for `ThingAsset` rejects `BoilerAsset`.
- Existing built-in group/type behavior remains unchanged.

Likely areas:

- `model/src/main/java/org/openremote/model/query/AssetQuery.java`
- `manager/src/main/java/org/openremote/manager/asset/AssetStorageService.java`
- `manager/src/main/java/org/openremote/manager/rules`

### 8. Manager UI

Add the super-admin UI for definition management and wire custom types into existing asset flows.

Implementation work:

- Add a custom asset type management page visible only to super admins.
- Add list, create, edit, delete, validate, and usage views.
- Add an editor for:
  - display name
  - technical name
  - icon
  - colour
  - description
  - attributes
  - value type
  - optional/required
  - label
  - units
  - read-only
  - datapoint storage
  - simple constraints
- Expose only the phase 1 value type allowlist.
- After save, reload the model data used by `AssetModelUtil`.
- Ensure the normal add-asset dialog only offers built-in types and saved custom definitions.
- Warn or require confirmation when a definition name is already used by fallback assets.

Tests first:

- Super admin sees the management page; non-super-admin does not.
- Creating a valid type through the UI calls the API and refreshes model data.
- The new type appears in the add-asset dialog without restart.
- The add-asset dialog does not allow typing arbitrary unknown type names.
- The editor blocks unsupported value types.
- Existing fallback-type usage produces the expected warning/confirmation flow.

Likely areas:

- `ui/component/model/src/util.ts`
- `ui/component/or-asset-tree/src/or-add-asset-dialog.ts`
- `ui/component/or-asset-tree/src/index.ts`
- `ui/component/or-asset-viewer/src/index.ts`
- `ui/component/or-asset-viewer/src/or-edit-asset-panel.ts`
- `ui/app/manager/src/pages`

### 9. End-to-End Regression Coverage

Keep this small and focused. Most coverage should already exist from the earlier slices.

Tests:

- Create custom type, refresh model, create asset, read asset back, validate attributes.
- Restart manager and verify definitions persist and are loaded.
- Update a type by adding an optional attribute and verify new assets use it.
- Verify incompatible updates are blocked.
- Verify unknown fallback assets still work.
- Verify confirmed creation of a custom definition over existing fallback assets changes descriptor behavior as expected.

## Manual Test Plan

Use this section for the phase 1 prototype validation pass. Record the Manager version/commit, browser, realm, user, and any console or network errors for failed cases.

Suggested naming convention for manual data:

- Custom type prefix: `Manual`
- Primary type: `ManualBoilerAsset`
- Unused type: `ManualUnusedAsset`
- Fallback test type: `ManualFallbackAsset`
- Assets: `Manual Boiler 1`, `Manual Boiler 2`, `Manual Generic Thing`

Custom type names must only use word characters (`A-Z`, `a-z`, `0-9`, `_`).

### Manual Test Report Template

```text
Commit / build:
Browser:
Realm:
Super admin user:
Regular user:

Test case:
Result: PASS / FAIL / BLOCKED
Observed:
Expected:
Screenshots / logs:
Notes:
```

### 1. Super Admin Access

Steps:

1. Log in as a super admin.
2. Open the custom asset types page.
3. Verify the page loads and the add button is available.
4. Log in as a regular non-super-admin user.
5. Verify the custom asset types page is not available from navigation.
6. Try opening the page URL directly as the regular user.

Expected:

- Super admins can list and manage custom asset type definitions.
- Non-super-admin users cannot access the management UI.
- Direct URL access as a non-super-admin shows an access-denied or unsupported state, not the editor.

Manual result:

- PASS. Direct access as a non-super-admin displayed `Not supported` in the main page frame.

### 2. Create a Basic Custom Asset Type

Create `ManualBoilerAsset` with:

- Display name: `Manual Boiler`
- Icon: any valid icon, for example `water-boiler`
- Colour: any valid colour, for example `#1976d2`
- Description: `Manual test boiler type`
- Attribute `temperature`
  - Type: `number`
  - Required
  - Label: `Temperature`
  - Units: `C`
  - Default value: `21`
  - Store datapoints: enabled
- Attribute `enabled`
  - Type: `boolean`
  - Optional
  - Label: `Enabled`
  - Default value: `true`
- Attribute `serialNumber`
  - Type: `text`
  - Optional
  - Label: `Serial number`

Steps:

1. Click add custom asset type.
2. Fill in the metadata and attributes above.
3. Save.
4. Stay on the page and inspect the saved row.

Expected:

- Save succeeds.
- A success toast is shown.
- The saved row appears without page reload.
- Usage count is `0`.
- The row shows the expected display name, attribute count, enabled state, icon, colour, and description.

Manual result:

- PASS. The type was created successfully.
- Follow-up: expanded row display had a large gap and attribute information was truncated on the right. Adjusted the read-only details layout so the attributes section uses the full width and wraps long metadata.
- Retest PASS. The expanded row display issue is fixed.

### 3. Add an Asset of the New Custom Type

Steps:

1. Open the normal asset tree/add asset flow.
2. Open the asset type selector.
3. Select `ManualBoilerAsset`.
4. Create asset `Manual Boiler 1`.
5. Open the asset details.

Expected:

- `ManualBoilerAsset` appears in the add-asset selector without restarting Manager.
- The selector does not require typing an arbitrary unknown type name.
- The created asset is saved and appears in the asset tree.
- The asset keeps `type = ManualBoilerAsset`.
- Descriptor-driven fields render for `temperature`, `enabled`, and `serialNumber`.
- Default values are applied on creation where configured.

Verification note:

- Functional check: inspect the asset API response in the browser network panel and verify the asset JSON has `"type": "ManualBoilerAsset"`.
- Stored-value check: query the database with `select id, name, type from asset where realm = '<realm>' and name = 'Manual Boiler 1';` and verify `type = ManualBoilerAsset`.

Manual result:

- PASS. Asset creation, selector behavior, tree visibility, descriptor-driven fields, API `type`, and database `type` were verified successfully.

### 4. Usage Count Refresh

Steps:

1. Return to the custom asset types page.
2. Locate `ManualBoilerAsset`.

Expected:

- Usage count is at least `1`.
- Delete action is disabled or otherwise blocked while usage is greater than `0`.

Manual result:

- PASS.

### 5. Compatible Update on an In-Use Type

Steps:

1. Edit `ManualBoilerAsset`.
2. Add attribute `setpoint`.
3. Set type to `number`.
4. Mark it optional.
5. Set label `Setpoint`, units `C`, and default value `19`.
6. Save.
7. Create `Manual Boiler 2` using `ManualBoilerAsset`.
8. Open `Manual Boiler 2`.

Expected:

- Save succeeds because the added attribute is optional.
- The asset model refreshes without restart.
- New assets of this type include or can render the new `setpoint` attribute.
- The configured default value is applied to new assets where the creation flow initializes defaults.

Manual result:

- PASS.

### 6. Blocked Updates on an In-Use Type

Use `ManualBoilerAsset` after at least one asset exists. After each failed attempt, cancel or reload the editor before trying the next one.

Steps and expected results:

1. Remove existing attribute `temperature` and save.
   - Expected: save fails with a message explaining that an attribute cannot be removed while assets use the custom type.
2. Add new attribute `pressure`, type `number`, required, and save.
   - Expected: save fails with a message explaining that new attributes must be optional while assets use the custom type.
3. Change existing attribute `temperature`, for example by changing its type, label, units, default, or metadata, and save.
   - Expected: save fails with a message explaining that an existing attribute cannot be changed while assets use the custom type.

Also verify:

- The toast does not show only a generic `409 Conflict` wrapper.
- The definition remains unchanged after each failed save.

Manual result:

- PASS.
- Follow-up: the delete cross button in the attribute editor was visually truncated. Adjusted the editor action column and icon button sizing so the cross button has stable space.
- Retest FAIL. The delete cross button was still visually truncated. Replaced the Vaadin icon button with a fixed-size native icon button for this attribute-row action.
- Retest PARTIAL. Clipping was fixed, but the cross was not centered and the button was not aligned with the row. Adjusted the native icon button alignment and icon centering.

### 7. Delete Behavior

Steps:

1. Try to delete `ManualBoilerAsset` while `Manual Boiler 1` or `Manual Boiler 2` still exists.
2. Create a separate type `ManualUnusedAsset` with one optional text attribute.
3. Save `ManualUnusedAsset`.
4. Delete `ManualUnusedAsset`.
5. Open the add-asset type selector.

Expected:

- `ManualBoilerAsset` cannot be deleted while assets use it.
- `ManualUnusedAsset` can be deleted after confirmation.
- Deleted custom types disappear from the custom asset type list.
- Deleted custom types disappear from the add-asset selector without restart.

Manual result:

- PASS. The UI disabled deletion while `ManualBoilerAsset` was in use, direct API deletion returned `409 Conflict`, `ManualUnusedAsset` could be deleted, and deleted custom types disappeared from the list and add-asset selector.

### 8. Validation and Editor Guardrails

Steps and expected results:

1. Try a type name containing spaces or dashes, for example `Manual Boiler-Asset`.
   - Expected: save is blocked by the UI.
2. Try duplicate attribute names.
   - Expected: save is blocked by the UI.
3. Open the value type selector.
   - Expected: only the phase 1 allowlist is available.
4. Try a default value that does not match the selected value type, for example `abc` for a `number` attribute.
   - Expected: save fails with a validation message, and the definition is not persisted.
5. Try a built-in type name such as `ThingAsset`.
   - Expected: save fails because custom definitions cannot collide with built-in asset types.

Manual result:

- 8.1 PASS in UI. The create button was disabled for an invalid type name. API verification still required.
- 8.2 PASS in UI. The create button was disabled for duplicate attribute names. API verification still required.
- 8.3 PASS. The value type selector only exposed the expected allowlist.
- 8.4 PASS for validation behavior, but the toast was generic. Added local UI detail for invalid default values.
- 8.5 PASS for validation behavior, but the toast was generic. Added local UI detail for built-in asset type name collisions.
- API verification for 8.1 and 8.2 PASS. Direct create calls returned `400 Bad Request`.
- Retest PASS for 8.4 and 8.5. The generic save-failure toast issue is fixed for invalid default values and built-in type name collisions.

### 9. Existing Fallback Asset Confirmation

This case requires an existing asset whose stored type is unknown to the runtime model. If the UI cannot create one directly, seed it through an API call, test fixture, or database setup before starting the UI steps.

Setup:

1. Create or seed an asset with stored `type = ManualFallbackAsset`.
2. Verify there is no saved custom definition named `ManualFallbackAsset`.

Steps:

1. Open the custom asset types page as super admin.
2. Create a new definition named `ManualFallbackAsset`.
3. Save.
4. Cancel the confirmation dialog.
5. Verify no definition was created.
6. Create the same definition again.
7. Confirm the warning dialog.
8. Re-open the existing fallback asset.

Expected:

- First save attempt warns that existing fallback assets use this type name.
- Cancelling leaves the system unchanged.
- Confirming creates the definition.
- Usage count reflects the existing fallback asset.
- The existing asset now resolves against the custom type descriptors.

Manual result:

- PASS.

### 10. Unknown Fallback Behavior Remains

This case also requires an asset with an unknown stored type that has no matching custom definition.

Steps:

1. Create or seed an asset with stored `type = ManualStillUnknownAsset`.
2. Do not create a custom definition for that type.
3. Open or read the asset.

Expected:

- The asset remains readable.
- Existing generic fallback behavior is preserved.
- The type is not treated as an authored custom type.
- The type does not appear in the add-asset selector as a saved custom definition.

Manual result:

- PASS. API checks verified the unknown asset remained readable, no custom definition existed, and the type was not exposed through the runtime model.

### 11. Restart Persistence

Steps:

1. Keep `ManualBoilerAsset` and at least one `ManualBoilerAsset` asset.
2. Restart the Manager.
3. Log back in as super admin.
4. Open the custom asset types page.
5. Open the add-asset type selector.
6. Open an existing `ManualBoilerAsset` asset.

Expected:

- The custom definition persists after restart.
- The runtime model loads the custom type at startup.
- The add-asset selector includes the custom type.
- Existing custom assets still read back with `type = ManualBoilerAsset`.
- Descriptor-driven attributes still render correctly.

Manual result:

- PASS.

### 12. Exact Type Matching for Queries and Groups

This case may require API calls or an existing UI path for group asset setup.

Setup:

1. Ensure `ManualBoilerAsset` exists.
2. Create `Manual Boiler 1` with stored type `ManualBoilerAsset`.
3. Create `Manual Generic Thing` with stored type `ThingAsset`.

Query checks:

1. Query assets by type `ThingAsset`.
2. Query assets by type `ManualBoilerAsset`.
3. Query assets by both `ThingAsset` and `ManualBoilerAsset`.

Expected:

- `ThingAsset` query returns `Manual Generic Thing`, not `Manual Boiler 1`.
- `ManualBoilerAsset` query returns `Manual Boiler 1`.
- Combined query returns both assets.

Manual result:

- Query checks PASS. Type-only `asset/query` payloads were used; the attempted `names` filter was not accepted by the API.

Group checks:

1. Create a group with `childAssetType = ManualBoilerAsset`.
2. Try assigning `Manual Boiler 1` as a child.
3. Try assigning `Manual Generic Thing` as a child.
4. Create a group with `childAssetType = ThingAsset`.
5. Try assigning `Manual Boiler 1` as a child.

Expected:

- A group configured for `ManualBoilerAsset` accepts only assets whose stored type is exactly `ManualBoilerAsset`.
- A group configured for `ThingAsset` does not accept assets whose stored type is `ManualBoilerAsset`.
- Existing built-in Java hierarchy behavior for built-in asset types is unchanged.

Manual result:

- PASS. Invalid parent assignments failed as expected when editing the child asset. The UI showed a generic `Failed to save asset` toast, but the system behavior was correct.

### 13. Cleanup

Steps:

1. Delete manual assets created during testing.
2. Delete manual custom asset types after their usage count reaches `0`.
3. Restart Manager if needed and verify deleted custom types do not reappear.

Expected:

- Types in use remain protected from deletion.
- Unused types can be deleted.
- Deleted types are removed from the runtime model and UI after refresh or restart.

Manual result:

- PASS.

## Phase 2 Items

- Cluster-wide model refresh propagation.
- Audit trail for schema changes.
- Import/export of custom type definitions.
- Broader schema compatibility handling if edit operations beyond adding optional attributes are needed.
- More complete operational visibility for definition changes and refresh status.
