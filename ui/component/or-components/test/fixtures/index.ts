import { ct as base, ComponentTestFixtures, Page, Locator, withPage } from "@openremote/test";

export class CollapsiblePanel {
  constructor(private readonly page: Page) {}

  getHeader(panel: Locator) {
    return panel.locator("[slot=header]");
  }

  getContent(panel: Locator) {
    return panel.locator("[slot=content]");
  }
}

interface ComponentFixtures extends ComponentTestFixtures {
  collapsiblePanel: CollapsiblePanel;
}

export const ct = base.extend<ComponentFixtures>({
  collapsiblePanel: withPage(CollapsiblePanel),
});
