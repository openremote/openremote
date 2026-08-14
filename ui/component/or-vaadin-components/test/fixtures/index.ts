import type { InputType } from "@openremote/or-vaadin-components/util";
import type { Page, Locator } from "@openremote/test";

export class VaadinInput {
    constructor(private readonly page: Page) {}

    /**
     * Returns the locator for the specified {@link InputType.SELECT|select input} option
     */
    getSelectInputOption(option: string, locator?: Locator): Locator {
        return (locator ?? this.page).locator("or-vaadin-select").getByRole("option").getByText(option, { exact: true });
    }
}

export class VaadinDialog {
    constructor(private readonly page: Page) {}

    /**
     * Returns a locator of the or-vaadin-dialog
     */
    getDialog(): Locator {
        return this.page.locator("or-vaadin-dialog");
    }
}
