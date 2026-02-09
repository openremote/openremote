import type { InputType } from "@openremote/or-vaadin-components/util";
import type { Page, Locator } from "@openremote/test";

export class VaadinInput {
    constructor(private readonly page: Page) {}

    /**
     * Get the underlying native input element of the or-mwc-input element
     * @param type The {@link InputType} to look for
     * @param locator The locator to start from
     * @returns The locator to the underlying <input> element inside the or-mwc-input
     */
    getInputByType(type: `${InputType}`, locator?: Locator) {
        return (locator ?? this.page).locator(`or-vaadin-input[type=${type}] #component > input`);
    }

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
