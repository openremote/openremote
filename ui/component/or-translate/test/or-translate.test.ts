import { ct, expect } from "@openremote/test";

import { OrTranslate } from "@openremote/or-translate";

ct.beforeEach(async ({ shared }) => {
  await shared.locales({
    en: {
      test: {
        thing: "Thing",
        "internet.of": { things: "Internet of things" },
      },
    },
    nl: {
      test: {
        thing: "Ding",
        "internet.of": { things: "Internet van dingen" },
      },
    },
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

ct("Should allow mixed key paths", async ({ mount }) => {
  const component = await mount(OrTranslate, {
    props: {
      value: "internet.of.things",
      options: { ns: "test", lng: "en" },
    },
  });
  await expect(component).toContainText("Internet of things");
  await component.update({
    props: {
      value: "internet.of.things",
      options: { ns: "test", lng: "nl" },
    },
  });
  await expect(component).toContainText("Internet van dingen");
});
