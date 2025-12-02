import { ct, expect } from "@openremote/test";

import { OrCalendarEvent } from "@openremote/or-scheduler";

ct("Button should trigger or-mwc-input-changed event", async ({ mount, page }) => {
  const component = await mount(OrCalendarEvent, {
    props: {},
    on: {},
  });
  await page.waitForTimeout(100000);
  await component.waitFor();
});