# Asset Attribute Configuration Import/Export Plan

## Requirements

### Purpose

Add a mechanism in OpenRemote Manager to export and import asset attribute configuration through user-editable JSON files. The configuration scope is limited to each attribute's `meta` object and `type`, plus the asset type used for compatibility checks.

The feature is intended to simplify applying the same configuration pattern to similar assets while preserving the existing asset edit workflow.

### Export Format

Exports must use a versioned, human-editable JSON format:

```json
{
  "version": 1,
  "assetType": "ExampleAssetType",
  "attributes": {
    "attributeName": {
      "type": "number",
      "meta": {}
    }
  }
}
```

Rules:

- `version` is required.
- `assetType` is required and comes from the asset `type` attribute.
- `attributes` is required.
- Attribute entries are keyed by attribute name.
- The attribute map key is authoritative; no attribute `name` field is exported.
- Only attributes with non-empty `meta` are exported.
- Attribute `type` is exported.
- Attribute `meta` is exported exactly as stored, including unknown or custom metadata.
- The exported JSON is pretty-printed.
- The downloaded file name is `<asset-name>-attribute-config.json`.

### Export UI

Import and export controls belong in the asset Modify view's `Attributes` section header, before the existing section expand/fullscreen control shown in the Figma reference.

The asset-level header keeps the existing asset actions such as `Save` and `View`; import/export should not be added there in the Figma-aligned design.

Rules:

- Buttons use icon plus text labels.
- Export is disabled when the current Modify view has unsaved changes.
- Export is backed by a backend endpoint.
- Export downloads the file directly after the user confirms the selected attributes.
- The export UI lists all attributes with non-empty `meta`.
- All exportable attributes are selected by default.
- The user can deselect attributes before export.
- Generic parameter selection is part of the target design, but may be implemented after the basic import/export flow.

### Import Format Validation

Import accepts a local JSON file selected through a file picker.

The import file is invalid if:

- JSON parsing fails.
- `version` is missing.
- `version` is unsupported.
- `assetType` is missing.
- `attributes` is missing.
- Attribute entries are malformed.
- No attributes can be imported after validation.

Unsupported future versions must be rejected.

If an imported attribute entry contains an internal `name` field, it is ignored. The attribute map key is authoritative.

### Import Compatibility Rules

Import matching is by exact attribute name.

For each imported attribute:

- If the target asset does not have an attribute with that name, ignore it and record it for reporting.
- If the target asset has the attribute but the `type` differs, ignore it and record it for reporting.
- Type comparison uses exact string equality, including case.
- If the target asset has the attribute and the type matches, the attribute is importable.
- Import never changes the attribute type.

Asset type handling:

- If imported `assetType` differs from the selected asset's `type`, show a separate warning.
- The user may still proceed despite an asset type mismatch.

### Import Apply Behavior

Import uses a backend endpoint for validation and transformation, but must not save the asset server-side.

The backend returns a patched asset or patched attributes payload for the UI to apply to the current Modify view state.

For each importable attribute:

- Replace the entire existing `meta` object with the imported `meta`.
- Existing target meta items not present in the import are removed.
- If the target attribute has no existing `meta`, add the imported `meta`.

After import:

- The Modify view becomes dirty.
- The user uses the existing `Save` or discard/navigation flow to persist or discard the imported changes.
- The asset details update to reflect the edited state.

### Import Confirmation And Reporting

Before applying an import, the user sees a confirmation dialog containing:

- Asset type mismatch warning, if applicable.
- Attributes that will be imported.
- Imported attributes ignored because they are missing on the target asset.
- Imported attributes ignored because their type differs.
- A warning that compatible attribute configuration will overwrite existing metadata in the current edit state.

The user can cancel.

If some attributes are importable and some are ignored, proceeding remains allowed.

After import is applied, report the result to the user in a dialog.

### Generic Parameters

Generic parameters are part of the target design but can be implemented after the basic import/export flow.

A generic parameter means:

- The export preserves that a value is required.
- The concrete value is removed from the exported configuration.
- The user supplies the value during import.

Generic parameter metadata includes:

- Path within the exported configuration.
- Original value type.

Example paths:

```text
attributes.error.meta.agentLink.id
attributes.error.meta.agentLink.unitId
```

Generic behavior:

- Generic values are reusable across attributes.
- If multiple attributes use the same generic path/value concept, the import UI asks once and applies the value everywhere.
- Placeholder names may be auto-derived from paths, for example:
  - `meta.agentLink.id` becomes `agentLinkId`.
  - `meta.agentLink.unitId` becomes `agentLinkUnitId`.
- During export, the UI suggests paths as generic when all selected exported attributes containing that path have identical values.
- During import, supplied values are validated against the schema/type where available, using the stored type information.

## Technical Design

### Data Contract

Introduce an asset attribute configuration document with version `1`.

```ts
interface AttributeConfigurationDocumentV1 {
  version: 1;
  assetType: string;
  attributes: Record<string, AttributeConfigurationEntryV1>;
}

interface AttributeConfigurationEntryV1 {
  type: string;
  meta: Record<string, unknown>;
}
```

The server must treat `attributes` keys as the attribute names. Unknown fields should be ignored for forward tolerance within supported versions, but malformed required fields must fail validation.

The initial implementation does not need to emit generic parameters, but the format should be kept extensible by allowing future top-level fields such as `genericParameters`.

### Backend Export Endpoint

Add a backend endpoint that receives:

- Asset id or full current persisted asset reference.
- Optional list of selected attribute names.

The endpoint:

- Loads the persisted asset.
- Builds the versioned document.
- Filters out attributes with empty or missing `meta`.
- If selected attribute names are provided, includes only selected exportable attributes.
- Includes each exported attribute's `type` and exact `meta`.
- Returns the versioned configuration document as JSON.

The endpoint must enforce the same asset read permissions as existing asset detail reads.

Because export is disabled when the Modify view has unsaved changes, the endpoint can use the persisted asset state without needing a draft payload.

The Manager UI is responsible for pretty-printing the returned document and downloading it as `<asset-name>-attribute-config.json`.

### Backend Import Preview/Patch Endpoint

Add a backend preview endpoint that receives:

- Target asset id.
- Current target asset draft from the Modify view.
- Imported configuration document.

The endpoint:

- Loads the persisted target asset for authorization and identity checks.
- Verifies the submitted draft id, realm, and type still match the persisted target asset.
- Validates JSON structure and supported `version`.
- Validates required fields.
- Compares imported `assetType` with the draft asset `type`.
- Iterates imported attributes by map key.
- Builds compatibility results:
  - importable attributes.
  - missing target attributes.
  - type mismatches.
- Fails if no attributes are importable.
- Produces a patched attributes payload from the submitted draft where each importable target attribute has its `meta` replaced by the imported `meta`.

The endpoint must not persist changes or apply the patch to the UI draft by itself. It only returns validation/report data and the patched payload for the UI to apply after user confirmation.

The endpoint must enforce the same permissions required to modify an asset, because the returned patch is intended for later save.

### Import Preview Response Contract

Use a response shape that directly supports confirmation and application in the UI.

```ts
interface AttributeConfigurationImportPreview {
  valid: true;
  assetTypeMismatch?: {
    expected: string;
    actual: string;
  };
  importableAttributes: Array<{
    name: string;
    type: string;
  }>;
  missingAttributes: Array<{
    name: string;
    type: string;
  }>;
  typeMismatches: Array<{
    name: string;
    importedType: string;
    targetType: string;
  }>;
  patchedAttributes: Record<string, unknown>;
}
```

Validation failures should use the project's existing error response conventions and include a clear error code/message for:

- Invalid JSON.
- Unsupported version.
- Missing required field.
- Malformed attribute entry.
- No importable attributes.

### Frontend Placement

Add `Import` and `Export` controls to the asset Modify view's `Attributes` section header.

The controls are only visible in the single selected asset details flow where the Modify view is available.

Export is disabled when the Modify view is dirty. The disabled state should make it clear that the asset must be saved or reverted before exporting.

Import may be allowed while the Modify view is dirty. The confirmation dialog must explicitly state that compatible imported metadata will overwrite matching metadata in the current draft state.

### Figma To Code Mapping

Figma reference:

- File key: `XqQd05IhDw3aRmeiRLoEiU`.
- Main asset modify frame: `87:11546`, named `Asset page modify preview`.
- Export dialog frame: `199:4166`.
- Import file picker dialog frame: `481:13925`.
- Import preview dialog frame: `223:17824`.
- Import generic parameters dialog frame: `496:5048`.
- Final warning dialog frame: `223:18364`.

Mapping rules:

- Prefer existing Manager product components for page structure and behavior instead of recreating Figma frames as standalone CSS.
- When the OR design system describes a Figma primitive as a Vaadin component, use the OpenRemote wrapper around that Vaadin component, for example `or-vaadin-button` rather than raw `vaadin-button`.
- Keep the current Manager dialog implementation on `or-mwc-dialog` unless a separate design-system migration explicitly moves this area to `or-vaadin-dialog`.
- Use `or-translate` for user-facing labels added to source code.
- Use `or-icon` inside icon and icon-plus-text buttons.

| Figma mockup component | Design system component | Source code component | Code generation guidance |
| --- | --- | --- | --- |
| `Asset page modify preview` | Product screen composition | `or-asset-viewer` in `ui/component/or-asset-viewer/src/index.ts` | Owns selected asset state, save/view actions, and import/export backend calls. Do not duplicate the full screen layout. |
| `Navigation` | Application navigation | Manager app shell outside this feature | Treat as existing surrounding chrome; import/export work does not generate it. |
| `List component`, `Toolbar list`, `List item structure` | Product list/tree pattern plus list primitives | `or-asset-tree` in `ui/component/or-asset-tree/src/index.ts`, with `or-mwc-list`, `or-vaadin-menu-bar`, `or-vaadin-button`, `or-vaadin-text-field` | Treat as existing asset tree. Do not touch it for this feature unless selection behavior changes. |
| `Asset header - modify` | Product asset header | Asset header template in `or-asset-viewer` | Keep asset-level actions there: asset name, save, view, edit, validation state. Move import/export out of this header for Figma alignment. |
| `Attributes` section header | Product section header | Attribute panel title/actions in `or-edit-asset-panel` | Add Import and Export actions next to the section title and before the expand/fullscreen action. This may require passing callbacks/state from `or-asset-viewer` into `or-edit-asset-panel`. |
| `Asset modify - Attribute` | Product attribute row/card | `_getAttributeTemplate` in `or-edit-asset-panel` | Reuse existing row expansion, value input, delete action, and meta item rendering. Figma export/import previews can use a lighter read-only summary inspired by this component. |
| `Asset modify - Configuration item` | Product meta item row | `_getMetaItemTemplate` in `or-edit-asset-panel`, `or-json-forms`, `or-attribute-input` | Meta item editing remains schema-driven. Do not hand-build forms for generic meta unless a schema/format-specific renderer is needed. |
| `Attribute types` | Value input family | `or-attribute-input`, `or-vaadin-input`, `jsonFormsInputTemplateProvider` | Attribute value rendering should remain type/schema driven. |
| `Button`, `Button (primary)`, `Button (tertiary)`, `Button (icon-only)` | OR design system `Button` variants; underlying Vaadin button | `or-vaadin-button` | Use `theme="primary"` for primary actions, `theme="tertiary"` for tertiary/cancel actions, and `theme="icon"` for icon-only buttons. Put `or-icon slot="prefix"` in icon-plus-text buttons. |
| `Text field` | OR design system `Text field`; described as `<vaadin-text-field>` | `or-vaadin-text-field` | Use for template name, read-only file name display if needed, and plain string generic parameters. |
| `Checkbox with label` | OR design system `Checkbox with label`; described as `<vaadin-checkbox>` | `or-vaadin-checkbox` | Use for export attribute selection and boolean generic parameters. |
| `Combo Box`, select-like fields | OR design system `Combo Box` / select primitives | Prefer `or-vaadin-combo-box` or `or-vaadin-select`; keep `or-mwc-input type=InputType.SELECT` where existing renderers already depend on it | AgentLink generic parameter selection should reuse the AgentLink-style select behavior and options. |
| `Badge`, `counter label` | OR design system `Badge` | `or-vaadin-badge` where a real badge is useful; otherwise a local styled count span | Use for attribute meta counts and generic parameter affected-attribute counts. |
| `Modal header`, `Modal Actions`, `Dialog`, `Confirm dialog` | OR design system dialog components; design-system description references Vaadin dialogs | Current implementation uses `or-mwc-dialog` and `DialogAction` in `or-mwc-components` | Keep import/export modal flows on `or-mwc-dialog` for consistency unless the dialog stack is migrated. |
| `List icons organizer`, chevrons, delete, drag handle, fullscreen | Icon primitives | `or-icon`, usually inside `or-vaadin-button theme="icon"` | Use known icon names already present in Manager where possible: `upload`, `download`, `chevron-right`, `delete`, `content-copy`, `eye`, `pencil`. |

Code Connect note:

- The Figma MCP cannot currently read or write formal Code Connect mappings for this account because it requires a Dev or Full seat on an Organization or Enterprise plan.
- Until Code Connect is available, this table is the working mapping used for Figma-to-code generation.

### Export Flow

1. User opens an asset in Modify view.
2. If the draft is clean, `Export` is enabled.
3. User clicks `Export`.
4. UI requests exportable attribute metadata or derives it from the current clean asset state.
5. Dialog lists attributes with non-empty `meta`, all selected by default.
6. User confirms.
7. UI calls the backend export endpoint with selected attribute names.
8. UI pretty-prints the returned document.
9. Browser downloads `<asset-name>-attribute-config.json`.

If there are no attributes with non-empty `meta`, the export UI reports that there is nothing to export.

### Import Flow

1. User opens an asset in Modify view.
2. User clicks `Import`.
3. UI opens a file picker for `.json`.
4. UI uploads/parses the file content and calls the backend import preview/patch endpoint with the current asset draft.
5. Backend returns compatibility report and patched attributes payload, or a validation error.
6. UI shows confirmation dialog with:
   - asset type mismatch warning if present.
   - importable attributes.
   - skipped missing attributes.
   - skipped type mismatches.
   - overwrite warning.
7. If the user cancels, no draft state changes.
8. If the user confirms, UI applies the patched attributes payload to the Modify view state.
9. Modify view becomes dirty.
10. UI shows an import result dialog.
11. User saves or discards through the existing Modify view flow.

### Generic Parameter Extension

Add generic parameter support after the base flow is working.

Possible future document extension:

```json
{
  "version": 1,
  "assetType": "ExampleAssetType",
  "genericParameters": {
    "agentLinkId": {
      "type": "text",
      "paths": [
        "attributes.error.meta.agentLink.id",
        "attributes.pumpDown.meta.agentLink.id"
      ]
    }
  },
  "attributes": {
    "error": {
      "type": "boolean",
      "meta": {
        "agentLink": {
          "type": "ModbusAgentLink",
          "unitId": 1
        }
      }
    }
  }
}
```

The exact placeholder representation should be finalized during generic-parameter implementation. The main requirement is that the exported file records all required values, their types, and where they must be applied.

Generic export design:

- Analyze selected attributes' `meta` trees.
- Identify paths present in more than one selected attribute.
- Suggest paths where all present values are identical.
- Let the user choose which suggested paths become generic.
- Remove the concrete value from the exported location or replace it with a placeholder marker.
- Store the placeholder type and all target paths.

Generic import design:

- Detect required generic parameters.
- Ask the user once for each reusable parameter.
- Validate entered values against the stored type and available schema.
- Apply values to all recorded paths before producing the patched attributes payload.

### Implementation Phases

#### Phase 1: Base Backend Contract

- Add DTOs/models for version `1` documents.
- Add export endpoint.
- Add import validation/patch endpoint.
- Add validation tests for accepted and rejected documents.
- Add compatibility tests for missing attributes, type mismatches, asset type mismatch, and successful meta replacement.

#### Phase 2: Base Frontend Flow

- Add Modify view action buttons.
- Disable export when dirty.
- Add export attribute selection dialog.
- Add JSON file picker import flow.
- Add import confirmation dialog.
- Apply patched attributes to draft state after confirmation.
- Add import result dialog.
- Verify existing save/discard behavior persists or discards imported changes.

#### Phase 3: Generic Parameters

- Extend document format with generic parameter metadata.
- Add export UI for suggested generic paths.
- Add import UI for required generic values.
- Add server-side type/schema validation for supplied generic values.
- Add tests for reusable placeholders across multiple attributes.

### Open Technical Decisions

- Final backend URL paths and method names should follow the existing Manager API conventions.
- The exact patched attributes payload should match the frontend's existing asset edit state shape to avoid client-side conversion logic.
- The generic parameter placeholder representation should be finalized when that phase starts.
