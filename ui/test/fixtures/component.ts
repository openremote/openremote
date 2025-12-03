import type { Locator } from "@playwright/experimental-ct-core";
import { Shared } from "./shared";

type ComponentProps<Component extends HTMLElement> = Partial<Component>;
type ComponentSlot = number | string | ComponentSlot[];
type ComponentSlots = Record<string, ComponentSlot> & { default?: ComponentSlot };

type ComponentEvents = Record<string, (detail: any) => void>;

export interface MountOptions<HooksConfig, Component extends HTMLElement> {
    props?: ComponentProps<Component>;
    slots?: ComponentSlots;
    on?: ComponentEvents;
    hooksConfig?: HooksConfig;
}

export interface MountResult<Component extends HTMLElement> extends Locator {
    unmount(): Promise<void>;
    update(options: {
        props?: Partial<ComponentProps<Component>>;
        slots?: Partial<ComponentSlots>;
        on?: Partial<ComponentEvents>;
    }): Promise<void>;
}

export class CtShared extends Shared {
    /**
     * Allows the test to wait for event to be fired
     * @returns A promise that waits for the handler to be called
     */
    promiseEventDispatch<T extends CustomEvent>(): [
        promise: Promise<T["detail"]>,
        handler: (event: T["detail"]) => T["detail"]
    ] {
        let resolver: (value: T | PromiseLike<T>) => void;
        const promise = new Promise<T>((resolve) => (resolver = resolve));
        return [promise, (event: T["detail"]) => resolver(event.value)];
    }
}
