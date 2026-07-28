# @openremote/or-rules  \<or-rules\>
[![NPM Version][npm-image]][npm-url]

Web Component for displaying a rules editor. This component requires an OpenRemote Manager to retrieve/save rules.

## Install
```bash
npm i @openremote/or-rules
yarn add @openremote/or-rules
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

The component pairs a rule tree with an editor for the selected ruleset, and loads and saves rulesets itself:

```html
<or-rules></or-rules>
```

`realm` selects which realm's rulesets are listed, defaulting to the current one, and `language` is the ruleset
language used for newly created rules (`JSON`, `FLOW`, `GROOVY` or `JAVASCRIPT`). Set `readonly` to browse without
editing.

### Restricting the editor
`config.controls` narrows what the JSON rule editor offers, which is how an app exposes a simplified subset of the
rules engine:

```typescript
const config: RulesConfig = {
    controls: {
        allowedLanguages: [RulesetLang.JSON],
        allowedConditionTypes: ["assetQuery"],
        hideWhenAddGroup: true,
        multiSelect: false
    }
};
```

### Restricting assets and attributes
`config.descriptors` controls which assets and attributes appear in the pickers, either for the whole editor (`all`)
or separately in the when and then sections. The `assets` map is keyed by asset type, with `*` as the fallback.

```typescript
const config: RulesConfig = {
    descriptors: {
        when: {
            excludeAssets: ["GroupAsset"],
            assets: {
                "*": {excludeAttributes: ["location"]}
            }
        }
    }
};
```

### Templates and handlers
`config.rulesetTemplates` supplies the starting content per language for a new ruleset, and `config.json` supplies
the defaults used when the user adds a rule, condition or action in the JSON editor.

The `rulesetAddHandler`, `rulesetSaveHandler` and `rulesetCopyHandler` callbacks run before the corresponding action;
returning `false` from one cancels it.

```typescript
const config: RulesConfig = {
    rulesetSaveHandler: (ruleset) => !ruleset.meta?.protected
};
```

`config.inputProvider` is a `ValueInputProviderGenerator` that overrides how attribute values are rendered in the
editor.

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-rules.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-rules
