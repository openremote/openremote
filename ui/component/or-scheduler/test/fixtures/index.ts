import { MwcDialog, MwcInput } from "../../../or-mwc-components/test/fixtures";
import { ct as base, type Page, type Locator, type SharedComponentTestFixtures, withPage } from "@openremote/test";
export { expect } from "@openremote/test";

export class CalendarEvent {
    constructor(private readonly page: Page) {}

    /**
     * Returns the locator for the `getAllDay` checkbox
     */
    getAllDay(option: string, locator?: Locator): Locator {
        return (locator ?? this.page).getByRole("checkbox", { name: "All day" });
    }
}

interface ComponentFixtures extends SharedComponentTestFixtures {
    calendarEvent: CalendarEvent;
    mwcDialog: MwcDialog;
    mwcInput: MwcInput;
}

export const ct = base.extend<ComponentFixtures>({
    // Components
    mwcDialog: withPage(MwcDialog),
    calendarEvent: withPage(CalendarEvent),
    mwcInput: withPage(MwcInput),
});
