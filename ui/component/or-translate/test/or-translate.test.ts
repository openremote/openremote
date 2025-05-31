import { ct, expect } from "@openremote/test";

import { OrTranslate } from "@openremote/or-translate";

ct.beforeEach(async ({ shared }) => {
  await shared.locales({
    en: { test: { thing: "Thing" } },
    nl: { test: { thing: "Ding" } },
  });
});

ct("Should translate text", async ({ mount }) => {
  const component = await mount(OrTranslate, {
    props: {
      value: "thing",
      options: { ns: "test", lng: "en" },
    },
  });
  await expect(component).toContainText("Thing");
  await component.update({
    props: {
      value: "thing",
      options: { ns: "test", lng: "nl" },
    },
  });
  await expect(component).toContainText("Ding");
});
