import { ct, expect } from "@openremote/test";

import { OrTranslate } from "@openremote/or-translate";

ct("Should translate text", async ({ mount }) => {
  const component = await mount(OrTranslate, {
    props: {
      value: "thing",
      options: { ns: "or", lng: "en" },
    },
  });
  await expect(component).toContainText("Thing");
  await component.update({
    props: {
      value: "thing",
      options: { ns: "or", lng: "nl" },
    },
  });
  await expect(component).toContainText("Ding");
});
