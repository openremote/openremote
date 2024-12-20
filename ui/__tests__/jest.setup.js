// Mock custom elements API
if (!window.customElements) {
    window.customElements = {
        define: jest.fn(),
        get: jest.fn(),
        whenDefined: jest.fn().mockResolvedValue(true),
        upgrade: jest.fn()
    };
}

// Mock any global browser APIs you use
global.ResizeObserver = jest.fn().mockImplementation(() => ({
    observe: jest.fn(),
    unobserve: jest.fn(),
    disconnect: jest.fn(),
}));

describe('Setup', () => {
    it('should be loaded', () => {
        expect(true).toBe(true);
    });
});