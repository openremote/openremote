import { ct, expect } from "@openremote/test";

import { OrCollapsiblePanel } from "@openremote/or-components/or-collapsible-panel";

ct.beforeEach(async ({ shared }) => {
  await shared.fonts();
});

ct("Should append header and content to collapsible panel", async ({ mount, components }) => {
  const component = await mount(OrCollapsiblePanel, {
    props: {},
    slots: {
      header: "<div>Header</div>", // slot="" is optional
      content: "<div>Content</div>",
    },
  });

  const header = components.collapsiblePanel.getHeader(component);
  await header.click();
  await expect(header).toContainText("Header");

  const content = components.collapsiblePanel.getContent(component);
  await expect(content).toContainText("Content");
});
