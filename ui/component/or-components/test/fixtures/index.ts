import { ct as base, SharedComponentTestFixtures, Page, Locator, withPage } from "@openremote/test";

export class CollapsiblePanel {
  constructor(private readonly page: Page) {}

  getHeader(panel: Locator) {
    return panel.locator("[slot=header]");
  }

  getContent(panel: Locator) {
    return panel.locator("[slot=content]");
  }
}

interface ComponentFixtures extends SharedComponentTestFixtures {
  collapsiblePanel: CollapsiblePanel;
}

export const ct = base.extend<ComponentFixtures>({
  collapsiblePanel: withPage(CollapsiblePanel),
});
