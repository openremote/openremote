import type { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import type { Page, Locator } from "@openremote/test";

export class MwcInput {
    constructor(private readonly page: Page) {}

    /**
     * Get the input element of the or-mwc-input element
     * @param type The {@link InputType} to look for
     * @param locator The locator to start from
     * @returns The locator to the or-mwc-input
     */
    getInputByType(type: `${InputType}`, locator?: Locator) {
        return (locator ?? this.page).locator(`or-mwc-input[type=${type}] #component > input`);
    }

    /**
     * Returns the locator for the specified {@link InputType.SELECT|select input} option
     */
    getSelectInputOption(option: string, locator?: Locator): Locator {
        return (locator ?? this.page).locator("or-mwc-input li[role=option]").getByText(option, { exact: true });
    }
}

export class MwcMenu {
    constructor(private readonly page: Page) {}

    /**
     * Returns the locator for the specified menu item
     */
    getMenuItem(option: string, locator?: Locator): Locator {
        return (locator ?? this.page).locator("or-mwc-menu li[role=menuitem]", { hasText: option });
    }
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
