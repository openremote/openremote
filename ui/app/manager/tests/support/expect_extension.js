const { expect } = require("@playwright/test");

expect.extend({
    null(received) {
        if (received === null) {
            return {
                pass: true,
                message: () => `expected null, received ${this.utils.printReceived(received)}`
            }
        }
    }
})