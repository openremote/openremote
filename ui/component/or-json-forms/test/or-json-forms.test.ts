import { ct } from "./fixtures/index";

import { OrJSONForms, StandardRenderers } from "@openremote/or-json-forms";
import { schemas } from "./fixtures/schemas";

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
  ct(`Should render form for: ${schema.title}`, async ({ mount, jsonForms }) => {
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
    await jsonForms.walkForm(component, schema);
  });
}
