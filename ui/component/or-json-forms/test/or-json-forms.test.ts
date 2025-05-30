import { ct } from "@openremote/test";

import { OrJSONForms, StandardRenderers } from "@openremote/or-json-forms";

// <or-json-forms .renderers="${jsonFormsAttributeRenderers}" ${ref(jsonForms)}
//                .disabled="${disabled}" .readonly="${readonly}" .label="${label}"
//                .schema="${schema}" label="Agent link" .uischema="${uiSchema}" .onChange="${onAgentLinkChanged}"></or-json-forms>

// cells: ,
// config: ,
// uischemas: ,

const schemas = [
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "String",
    type: "string",
    "or:test:value": "test",
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Boolean",
    type: "boolean",
    "or:test:value": true,
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Integer",
    type: "integer",
    "or:test:value": 1,
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Number",
    type: "number",
    "or:test:value": 1,
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Object",
    type: "object",
    "or:test:props": ["value"],
    properties: {
      value: {
        type: "string",
        "or:test:value": "test",
      },
    },
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Array",
    type: "array",
    "or:test:item:count": 1,
    items: {
      title: "String",
      type: "string",
      "or:test:value": "test",
    },
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "3 Dimensional Array",
    type: "array",
    "or:test:item:count": 2,
    items: {
      title: "2 Dimensional Array",
      type: "array",
      "or:test:item:count": 2,
      items: {
        title: "Array",
        type: "array",
        "or:test:item:count": 2,
        items: {
          title: "String",
          type: "string",
          "or:test:value": "test",
        },
      },
    },
  },
  {
    $schema: "http://json-schema.org/draft-07/schema#",
    title: "Complex",
    type: "array",
    "or:test:item:count": 2,
    items: {
      title: "Object",
      type: "object",
      "or:test:props": ["string", "array", "boolean"],
      properties: {
        string: {
          title: "String",
          type: "string",
          "or:test:value": "test",
        },
        boolean: {
          title: "Boolean",
          type: "boolean",
          // TODO: add support for array count?
          "or:test:value": true,
        },
        array: {
          title: "Array",
          type: "array",
          "or:test:item:count": 2,
          items: {
            title: "String",
            type: "string",
            "or:test:value": "test",
          },
        },
      },
    },
  },
];

const typeValueMap = new Map()
  .set("array", [])
  .set("boolean", false)
  .set("object", {})
  .set("number", 0)
  .set("integer", 0)
  .set("string", "");

ct.beforeEach(async ({ shared }) => {
  await shared.fonts();
  await shared.locales();
});

for (const schema of schemas) {
  ct(`Should render form for: ${schema.title}`, async ({ mount, components }) => {
    const component = await mount(OrJSONForms, {
      props: {
        uischema: { type: "Control", scope: "#" } as any,
        schema,
        data: typeValueMap.get(schema.type),
        renderers: StandardRenderers,
        onChange: () => null,
        readonly: false,
        label: schema.title,
        required: false,
      },
      on: {},
    });
    await components.walkForm(component, schema);
  });
}
