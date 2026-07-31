# @openremote/or-json-forms  \<or-json-forms\>
[![NPM Version][npm-image]][npm-url]

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

- `{ "type": "string" }`  - Displays a text input field.
- `{ "type": "number" }`  - Displays a number input field.
- `{ "type": "integer" }` - Displays a number input field.
- `{ "type": "array" }`   - Displays a wrapper with a button to add items.
- `{ "type": "object" }`  - Displays a wrapper with a button to add properties.

#### Renderers & Testers

<!--#### Polymorphism-->

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

| type                    | value | formats | Formatted default |
| ----------------------- | ----- | ------- | ------- |
| [...] (array of values) | [...]    |  |       | 
| string                  | `""`    |  date-time, date, time       | `new Date()` |
| integer, number         | `0`     |         |  |
| boolean                 | `false` |         |
| array                   | `[]`    |         |
| object                  | An object with the required properties, otherwise an empty object |         |
| null                    | `null`  |         |

### Example usage

```typescript
import { html, LitElement } from 'lit';
import { customElement } from 'lit/decorators.js';
import { ErrorObject } from "@openremote/or-json-forms";
import "@openremote/or-json-forms";

@customElement("my-json-forms")
export class MyJsonForms extends LitElement {
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
        return html`<or-json-forms .schema="${MyJsonForms.schema}" .uischema="${MyJsonForms.uiSchema}"
                                   .data="${this.data}" .onChange="${(d) => this.onChanged(d)}"></or-json-forms>`
    }

    onChanged(dataAndErrors: { errors: ErrorObject[] | undefined, data: any }) {
        // Do something with the data and errors
    }
};
```

`data` seeds the form and `onChange` reports every edit along with the current validation errors; the component does
not mutate the object it was given. `label` and `required` describe the form itself, and `readonly` renders it
without editors.

### Custom renderers
`renderers` defaults to `StandardRenderers`. Supply your own registry to add or replace a renderer, where each entry
pairs a tester that ranks how well it matches a schema with the renderer to use when it wins.

```typescript
import { rankWith, uiTypeIs } from "@jsonforms/core";
import { StandardRenderers } from "@openremote/or-json-forms";

const renderers = [
    ...StandardRenderers,
    {
        tester: rankWith(10, uiTypeIs("Control")),
        renderer: (state, props) => html`...`
    }
];
```

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

[npm-image]: https://img.shields.io/npm/v/@openremote/or-json-forms.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-json-forms
