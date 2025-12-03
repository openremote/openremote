import type { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import type { Page, Locator } from "@openremote/test";

export class MwcInput {
    constructor(private readonly page: Page) {}

    /**
     * Returns the locator for the specified {@link InputType.SELECT|select input} option
     */
    getSelectInputOption(option: string, locator?: Locator): Locator {
        return (locator ?? this.page).locator("or-mwc-input li[role=option]", { hasText: option });
    }

    // getSelect
}

export class MwcDialog {
    constructor(private readonly page: Page) {}

    /**
     * Returns a locator of the or-mwc-dialog
     */
    getDialog(): Locator {
        return this.page.locator("or-mwc-dialog");
    }
}
