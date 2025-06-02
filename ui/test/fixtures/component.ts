import type { WellknownMetaItems } from "@openremote/model";
import type { Page } from "playwright-core";
import { expect, type Locator } from "@playwright/experimental-ct-core";
import { camelCaseToSentenceCase } from "./shared";

type ComponentProps<Component extends HTMLElement> = Partial<Component>;
type ComponentSlot = number | string | ComponentSlot[];
type ComponentSlots = Record<string, ComponentSlot> & { default?: ComponentSlot };

type ComponentEvents = Record<string, Function>;

export interface MountOptions<HooksConfig, Component extends HTMLElement> {
  props?: ComponentProps<Component>;
  slots?: ComponentSlots;
  on?: ComponentEvents;
  hooksConfig?: HooksConfig;
}

export interface MountResult<Component extends HTMLElement> extends Locator {
  unmount(): Promise<void>;
  update(options: {
    props?: Partial<ComponentProps<Component>>;
    slots?: Partial<ComponentSlots>;
    on?: Partial<ComponentEvents>;
  }): Promise<void>;
}

class CollapsiblePanel {
  constructor(private readonly page: Page) {}

  getHeader(panel: Locator) {
    return panel.locator("[slot=header]");
  }

  getContent(panel: Locator) {
    return panel.locator("[slot=content]");
  }
}

class JsonForms {
  constructor(private readonly page: Page) {}

  /**
   * Exhaust every `or-json-forms` option based on the provided JSONSchema. The following is done:
   * - Adds new items to arrays and parameters for objects
   * - TODO: tests validation of each field
   * - TODO: builds an expected output and compares this with the JSON at the end
   * - TODO: moves specific fields
   * - TODO: removes specific fields
   *
   * @param locator The root element of the `or-json-forms` instance to test.
   * @param schema The same JSONSchema used to generate the form.
   * @param options Options to fill out in the forms without explicitly defining the values.
   * @param path The path to the current node.
   * @param item The item index used to locate array items.
   * @param expected The expected JSON output built up during recursion.
   */
  async walkForm(
    locator: Locator,
    schema: any,
    options: {
      selectAllProps?: boolean;
      itemCount?: number;
    } = {
      selectAllProps: false,
      itemCount: 0,
    },
    path: (string | number)[] = [],
    item = 0,
    expected?: any
  ) {
    const dialog = this.page.locator("or-mwc-dialog");

    switch (schema.type) {
      case "array": {
        locator = locator.locator("or-json-forms-array-control").nth(item);
        path.push("or-json-forms-array-control", item);
        await locator.locator("or-collapsible-panel").click();
        if (schema.items.oneOf) {
          for (const prop of Object.values<any>(schema.items.oneOf)) {
            await locator.getByRole("button", { name: "Add Item" }).click();
            await dialog.locator("li").getByText(prop.title, { exact: true }).click();
            await dialog.getByRole("button", { name: "Add", exact: true }).click();
          }
        } else {
          for (let i = 0; i < schema["or:test:item:count"]; i++) {
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
          await this.page.waitForTimeout(1000);
          await this.page
            .getByRole("alertdialog", { name: " - Add" })
            .locator("label", { hasText: "Key" })
            .pressSequentially("test");
          await dialog.getByRole("button", { name: "Add", exact: true }).click();
          await this.page.waitForTimeout(2000);
        } else {
          for (const key of options?.selectAllProps ? Object.keys(schema.properties) : schema["or:test:props"]) {
            if (key === "type" || key === "id" || schema.required?.includes(key)) continue;
            await locator.getByRole("button", { name: "Add Parameter" }).click();
            await dialog.locator("or-mwc-list li").getByText(camelCaseToSentenceCase(key), { exact: true }).click();
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
        /// TODO: remove condition when or-json-forms-array-control always renders titles
        const options = !path.length ? { name: schema.title } : {};
        locator = locator.getByRole("textbox", options).nth(item);
        await expect(locator).toBeVisible();
        await locator.fill(schema["or:test:value"] ?? "test");
        await expect(locator).toHaveValue(schema["or:test:value"] ?? "test");
        break;
      }
      case "number": {
        const options = !path.length ? { name: schema.title } : {};
        locator = locator.getByRole("spinbutton", options).nth(item);
        await expect(locator).toBeVisible();
        await locator.fill(`${schema["or:test:value"] ?? 0}`);
        await expect(locator).toHaveValue(`${schema["or:test:value"] ?? 0}`);
        break;
      }
      case "integer": {
        const options = !path.length ? { name: schema.title } : {};
        locator = locator.getByRole("spinbutton", options).nth(item);
        await expect(locator).toBeVisible();
        await locator.fill(`${schema["or:test:value"] ?? 0}`);
        await expect(locator).toHaveValue(`${schema["or:test:value"] ?? 0}`);
        break;
      }
      case "boolean": {
        const options = !path.length ? { name: schema.title } : {};
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
      if (schema.items.oneOf) {
        let i = 0;
        for (const prop of Object.values(schema.items.oneOf)) {
          await this.walkForm(locator, prop, options, [...path], i);
          i++;
        }
      } else {
        for (let i = 0; i < schema["or:test:item:count"]; i++) {
          await this.walkForm(locator, schema.items, options, [...path], i);
        }
      }
      // await locator.getByRole("button", { name: "json" }).first().click();
    } else if (schema.type === "object" && !schema.patternProperties) {
      let arrayControls = 0;
      let verticalLayouts = 0;
      for (const [key, prop] of Object.entries<any>(schema.properties)) {
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

    if (schema.type === "array" && schema.type === "object") {
      // await expect(locator.locator("or-ace-editor")).toContainText(JSON.stringify(expected, null, 2));
    }
  }
}

class AssetViewer {
  constructor(private readonly page: Page) {}

  /**
   * Switch between modify mode and view mode
   * @param targetMode view or modify
   */
  async switchMode(targetMode: "modify" | "view") {
    const selector = this.page.getByRole("button", { name: targetMode });
    await selector.waitFor({ state: "visible" });
    await selector.click();
  }

  getAttributeLocator(attribute: string): Locator {
    return this.page.getByRole("row", { name: new RegExp(`\\b${attribute}\\b`, "i") });
  }

  async setAttributeValue(attribute: string, value: string) {
    await this.getAttributeLocator(attribute).locator("input").fill(value);
  }

  /**
   * Add configuration items to an attribute by its name
   * @param attribute  The attribute's name
   * @param items The configuration items to add
   */
  async addConfigurationItems(attribute: string, ...items: `${WellknownMetaItems}`[]) {
    const locator = this.getAttributeLocator(attribute);
    // Assuming items are not collapsed
    await locator.click();
    await locator.locator("+ tr").getByRole("button", { name: "Add configuration items" }).click();
    for (const item of items) {
      await this.page
        .locator("li")
        .filter({ hasText: camelCaseToSentenceCase(item) })
        .check();
    }
    await this.page.getByRole("button", { name: "Add", exact: true }).click();
    // // close attribute menu
    // await this.page.click(`td:has-text("${attr}") >> nth=0`);
  }

  getConfigurationItemLocator(attribute: string, item?: string): Locator {
    return item
      ? // match the sibling row i.e. the configuration items row of the attribute
        this.getAttributeLocator(attribute).locator("+ tr", { hasText: new RegExp(`\\b${item}\\b`, "i") })
      : this.getAttributeLocator(attribute).locator("+ tr");
  }
}

export class Components {
  readonly collapsiblePanel = new CollapsiblePanel(this.page);
  readonly jsonForms = new JsonForms(this.page);
  readonly assetViewer = new AssetViewer(this.page);

  constructor(readonly page: Page) {}
}
