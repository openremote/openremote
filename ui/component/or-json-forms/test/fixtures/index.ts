import { withPage, ct as base, expect, Page, Locator, SharedComponentTestFixtures } from "@openremote/test";
import { JsonSchema7, OrJSONForms } from "@openremote/or-json-forms";
import * as Util from "@openremote/core/lib/util";

interface WalkFormOptions {
    /**
     * Whether all possible object properties should be added.
     * If falsy, then only the specified `or:test:props` are added.
     * @example
     * ```ts
     * "or:test:props": ["prop1", "prop2"]
     * ```
     */
    selectAllProps?: boolean;
}

export interface JsonSchema extends JsonSchema7 {
    /**
     * Describes what properties should be manually selected by {@link walkForm#walkForm}
     */
    "or:test:props"?: string[];
    /**
     * Describes what properties should be manually selected by {@link walkForm#walkForm}
     */
    "or:test:value"?: any;
    /**
     * Describes what properties should be manually selected by {@link JsonForms#walkForm}
     */
    "or:test:item:count"?: number;
    /**
     * The following overwrites the {@link JsonSchema7} types with {@link JsonSchema}
     */
    /***/
    additionalItems?: boolean | JsonSchema;
    items?: JsonSchema | JsonSchema[];
    additionalProperties?: boolean | JsonSchema;
    /**
     * Holds simple JSON Schema definitions for
     * referencing from elsewhere.
     */
    definitions?: {
        [key: string]: JsonSchema;
    };
    /**
     * The keys that can exist on the object with the
     * json schema that should validate their value
     */
    properties?: {
        [property: string]: JsonSchema;
    };
    /**
     * The key of this object is a regex for which
     * properties the schema applies to
     */
    patternProperties?: {
        [pattern: string]: JsonSchema;
    };
    /**
     * If the key is present as a property then the
     * string of properties must also be present.
     * If the value is a JSON Schema then it must
     * also be valid for the object if the key is
     * present.
     */
    dependencies?: {
        [key: string]: JsonSchema | string[];
    };
    allOf?: JsonSchema[];
    anyOf?: JsonSchema[];
    oneOf?: JsonSchema[];
    /**
     * The entity being validated must not match this schema
     */
    not?: JsonSchema;
    contains?: JsonSchema;
    propertyNames?: JsonSchema;
    if?: JsonSchema;
    then?: JsonSchema;
    else?: JsonSchema;
}

export class JsonForms {
    constructor(private readonly page: Page) {}

    /**
     * Exhaust every `or-json-forms` option based on the provided JSONSchema. The following is done:
     * - Adds new items to arrays and parameters for objects
     * @todo builds an expected output and compares this with the JSON at the end
     * @todo moves specific fields
     * @todo removes specific fields
     *
     * @param locator The root element of the `or-json-forms` instance to test.
     * @param schema The same JSONSchema used to generate the form.
     * @param options Options to fill out in the forms without explicitly defining the values.
     * @param path The path to the current node.
     * @param item The item index used to locate array items.
     * @param expected not implemented - The expected JSON output built up during recursion.
     */
    async walkForm(
        locator: Locator,
        schema: JsonSchema,
        options?: WalkFormOptions,
        path: (string | number)[] = [],
        item = 0,
        parent?: "array" | "object", // TODO: remove first condition when or-json-forms-array-control always renders titles
        expected?: any
    ) {
        const dialog = this.page.locator("or-mwc-dialog");

        switch (schema.type) {
            case "array": {
                locator = locator.locator("or-json-forms-array-control").nth(item);
                path.push("or-json-forms-array-control", item);
                await locator.locator("or-collapsible-panel").click();
                if (!Array.isArray(schema?.items) && schema.items?.oneOf) {
                    for (const prop of Object.values<any>(schema.items.oneOf)) {
                        await locator.getByRole("button", { name: "Add Item" }).click();
                        await dialog.locator("li").getByText(prop.title, { exact: true }).click();
                        await dialog.getByRole("button", { name: "Add", exact: true }).click();
                    }
                } else {
                    for (let i = 0; i < (schema?.["or:test:item:count"] ?? 0); i++) {
                        await locator.getByRole("button", { name: "Add Item" }).click();
                    }
                }
                break;
            }
            case "object": {
                locator = locator.locator("or-json-forms-vertical-layout").nth(item);
                path.push("or-json-forms-vertical-layout", item);
                await locator.locator("or-collapsible-panel").click();
                if (schema.patternProperties) {
                    await locator.getByRole("button", { name: "Add Parameter" }).click();
                    await this.page
                        .getByRole("alertdialog", { name: " - Add" })
                        .locator("label", { hasText: "Key" })
                        .pressSequentially("test");
                    await dialog.getByRole("button", { name: "Add", exact: true }).click();
                } else {
                    const properties = options?.selectAllProps
                        ? Object.keys(schema.properties ?? {})
                        : schema?.["or:test:props"] ?? [];
                    for (const key of properties) {
                        if (key === "type" || key === "id" || schema.required?.includes(key)) continue;
                        await locator.getByRole("button", { name: "Add Parameter" }).click();

                        const name = Util.camelCaseToSentenceCase(key);
                        await dialog.locator("or-mwc-list li").getByText(name, { exact: true }).click();
                        const anyOfPicker = dialog.locator("#schema-picker or-mwc-input");
                        if (await anyOfPicker.isVisible()) {
                            await anyOfPicker.click();
                            await anyOfPicker.locator("li").first().click();
                        }
                        await dialog.getByRole("button", { name: "Add", exact: true }).click();
                    }
                }
                break;
            }
            case "string": {
                /// TODO: remove first condition when or-json-forms-array-control always renders titles
                const options = parent !== "array" && schema.title ? { name: schema.title, exact: true } : {};
                locator = locator.getByRole("textbox", options).nth(item);
                await expect(locator).toBeVisible();
                await locator.fill(schema["or:test:value"] ?? "test");
                await expect(locator).toHaveValue(schema["or:test:value"] ?? "test");
                break;
            }
            case "number": {
                const options = parent !== "array" && schema.title ? { name: schema.title, exact: true } : {};
                locator = locator.getByRole("spinbutton", options).nth(item);
                await expect(locator).toBeVisible();
                await locator.fill(`${schema["or:test:value"] ?? 0}`);
                await expect(locator).toHaveValue(`${schema["or:test:value"] ?? 0}`);
                break;
            }
            case "integer": {
                const options = parent !== "array" && schema.title ? { name: schema.title, exact: true } : {};
                locator = locator.getByRole("spinbutton", options).nth(item);
                await expect(locator).toBeVisible();
                await locator.fill(`${schema["or:test:value"] ?? 0}`);
                await expect(locator).toHaveValue(`${schema["or:test:value"] ?? 0}`);
                break;
            }
            case "boolean": {
                const options = parent !== "array" && schema.title ? { name: schema.title, exact: true } : {};
                locator = locator.getByRole("checkbox", options).nth(item);
                await expect(locator).toBeVisible();
                await expect(locator).not.toBeChecked();
                if (schema["or:test:value"]) {
                    await locator.check();
                    await expect(locator).toBeChecked();
                }
                break;
            }
        }

        if (schema.type === "array") {
            if (!Array.isArray(schema?.items) && schema.items?.oneOf) {
                let i = 0;
                for (const prop of Object.values(schema.items.oneOf)) {
                    await this.walkForm(locator, prop as JsonSchema, options, [...path], i, "array");
                    i++;
                }
            } else {
                for (let i = 0; i < (schema?.["or:test:item:count"] ?? 0); i++) {
                    await this.walkForm(locator, schema.items as JsonSchema, options, [...path], i, "array");
                }
            }
            // await locator.getByRole("button", { name: "json" }).first().click();
        } else if (schema.type === "object" && !schema.patternProperties) {
            let arrayControls = 0;
            let verticalLayouts = 0;
            for (const [key, prop] of Object.entries<any>(schema.properties ?? {})) {
                if (schema["or:test:props"] && !schema["or:test:props"].includes(key)) continue;
                if (["type", "id"].includes(key)) continue;
                if (prop.type === "array") {
                    await this.walkForm(locator, prop, options, [...path], arrayControls);
                    arrayControls++;
                } else if (prop.type === "object") {
                    await this.walkForm(locator, prop, options, [...path], verticalLayouts);
                    verticalLayouts++;
                } else {
                    await this.walkForm(locator, prop, options, [...path]);
                }
            }
            // await locator.getByRole("button", { name: "json" }).first().click();
        }

        if (schema?.type === "array" || schema?.type === "object") {
            // await expect(locator.locator("or-ace-editor")).toContainText(JSON.stringify(expected, null, 2));
        }
    }

    public async getValidity(form: Locator) {
        return form.evaluate((el: OrJSONForms) => el.checkValidity());
    }
}

interface ComponentFixtures extends SharedComponentTestFixtures {
    jsonForms: JsonForms;
}

export const ct = base.extend<ComponentFixtures>({
    jsonForms: withPage(JsonForms),
});
