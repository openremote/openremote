// test.beforeEach(async ({}) => {});

// test("or-attribute-input", async ({ mount }) => {});

// import { test, expect } from "@playwright/test";
import { OrTranslate } from "@openremote/or-translate";
import { test, expect } from "@sand4rt/experimental-ct-web";
import { OrIcon } from "@openremote/or-icon";
import { OrAttributeInput } from "@openremote/or-attribute-input";
import { OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";

test("or-translate", async ({ mount }) => {
  const component = await mount(OrTranslate, {
    props: {
      value: "test",
    },
  });
  await expect(component).toContainText("test");
});

test("or-icon", async ({ mount }) => {
  const component = await mount(OrIcon, {
    props: {
      icon: "state-machine",
    },
  });
  await expect(component).toContainText("test");
});

test("or-mwc-input", async ({ mount }) => {
  const component = await mount(OrMwcInput, {
    props: {
      type: 'button',
      raised: true,
      label:  "test"
    },
  });
  await expect(component).toContainText("test");
});

test("or-attribute-input", async ({ mount }) => {
  const component = await mount(OrIcon, {
    props: {},
  });
  await expect(component).toContainText("test");
});
