# @openremote/or-json-forms  \<or-json-forms\>
[![NPM Version][npm-image]][npm-url]
[![Linux Build][travis-image]][travis-url]
[![Test Coverage][coveralls-image]][coveralls-url]

Web Component for generating forms based on JSON Schema. This can be useful for creating forms for complex data structures and validating user input.

This component expects the JSON Schemas to be formatted as described in [Usage](#usage).

## Install
```bash
npm i @openremote/or-json-forms
yarn add @openremote/or-json-forms
```

## Usage
<!--For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().-->

The JSON Forms expects [Draft-07](http://json-schema.org/draft-07) schemas.

### Unsupported keywords

The following keywords are not (fully) supported:

- `anyOf`: ???
- `allOf`: ???
- `$ref`: will only resolve the reference if it is in the schema.
- `examples`: planned

### Behavior Specification

The types in a JSON Schema will have the following effects on what is rendered:

- `{ "type": "string" }`  - Displays a text input field.
- `{ "type": "number" }`  - Displays a number input field.
- `{ "type": "integer" }` - Displays a number input field.
- `{ "type": "array" }`   - Displays a wrapper with a button to add items.
- `{ "type": "object" }`  - Displays a wrapper with a button to add properties and decorates each property with a delete button.

The following formats will change type of input which is shown:

- `{ "type": "string", "format": "date-time" }` - Displays a date and time input field.
- `{ "type": "string", "format": "color" }` - Displays a color picker input field and hides the label.
- `{ "type": "string", "format": "date" }` - Displays a date input field.
- `{ "type": "string", "format": "time" }` - Displays a time input field.
- `{ "type": "string", "format": "email" }` - Displays an email input field.
- `{ "type": "string", "format": "tel" }` - Displays a telephone input field.
- `{ "type": "string", "format": "or-multiline" }` - Displays a multiline text area.
- `{ "type": "string", "format": "or-password" }` (or `{ "writeOnly": true }`) - Displays a password input field.
- `{ "type": "string", "format": "timezone" }` - Displays a select dropdown menu.

#### Renderers & Testers

<!--#### Polymorphism -->

<!--'oneOf', 'anyOf', 'allOf'-->

#### Default values

The JSON Forms will resolve default values from the schema based on the `default` property or infer it from the type.

It derives the type from the schema's `type` property, or from properties that are characteristic of the type.

| property                | type               |
| ----------------------- | ------------------ |
| `type`                  | The specified type |
| `properties`            | object             |
| `additionalProperties`  | object             |
| `items`                 | array              |

<!--CombinatorKeyword[] = ['oneOf', 'anyOf', 'allOf']-->

<!-- See `doCreateDefaultValue` in node_modules/@jsonforms/core/src/mappers/renderer.ts -->

Depending on the type, it derives the default value as follows:

| type                    | value   | formats                | Formatted default |
| ----------------------- | ------- | ---------------------- | ----------------- |
| [...] (array of values) | [...]   |                        |                   |
| string                  | `""`    | date, date-time, time | `new Date()`      |
| integer, number         | `0`     |                        |                   |
| boolean                 | `false` |                        |
| array                   | `[]`    |                        |
| object                  | An object with the required properties, otherwise an empty object |         |
| null                    | `null`  |                        |

### Example usage

```typescript
import { html } from 'lit';
import { ErrorObject, StandardRenderers } from "@openremote/or-json-forms";
import "@openremote/or-json-forms";

public class MyJsonForms extends LitElement {
    private static schema = {
        $schema: "http://json-schema.org/draft-07/schema#",
        title: "MyObject",
        type: "object",
        properties: {
            firstname: { type: "string" },
            lastname: { type: "string" },
            birthday: { type: "integer", minimum: 0 },
        },
    };
    // Apply a custom UI schema to remove the outer VerticalLayout
    private static uiSchema: any = { type: "Control", scope: "#" };

    render() {
        return html`<or-json-forms .renderers="${jsonFormsAttributeRenderers}" .schema="${schema}" .uischema="${uiSchema}" .onChange="${onChanged}"></or-json-forms>`
    }

    onChanged(dataAndErrors: { errors: ErrorObject[] | undefined, data: any }) {
        // Do something with the data and errors
    }
};
```

### Custom renderers

### Styling
All styling is done through CSS, the following CSS variables can be used:

```css
--or-app-color3 /* Change text colors */
--or-app-color4 /* Change border colors */
--or-app-color5 /* Change border colors */
--or-icon-fill
```

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.

## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/live-xxx.svg
[npm-url]: https://npmjs.org/package/@openremote/or-json-forms
[travis-image]: https://img.shields.io/travis/live-js/live-xxx/master.svg
[travis-url]: https://travis-ci.org/live-js/live-xxx
[coveralls-image]: https://img.shields.io/coveralls/live-js/live-xxx/master.svg
[coveralls-url]: https://coveralls.io/r/live-js/live-xxx?branch=master
