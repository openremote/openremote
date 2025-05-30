import { ct, expect } from "@openremote/test";

import { OrCollapsiblePanel } from "@openremote/or-components/or-collapsible-panel";

ct.beforeEach(async ({ shared }) => {
  await shared.fonts();
});

ct("Should append header and content to collapsible panel", async ({ mount, page }) => {
  const component = await mount(OrCollapsiblePanel, {
    props: {},
    slots: {
      header: "<div>Header</div>", // slot="" is optional
      content: "<div>Content</div>",
    },
  });

  const header = page.locator("or-collapsible-panel").locator("[slot=header]");
  await header.click();
  await expect(header).toContainText("Header");

  const content = page.locator("or-collapsible-panel").locator("[slot=content]");
  await expect(content).toContainText("Content");
});
