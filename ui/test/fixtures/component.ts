import type { Locator } from "@playwright/experimental-ct-core";

type ComponentProps<Component extends HTMLElement> = Partial<Component>;
type ComponentSlot = number | string | ComponentSlot[];
type ComponentSlots = Record<string, ComponentSlot> & { default?: ComponentSlot };

type ComponentEvents = Record<string, Function>;

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
