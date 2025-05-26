import { ct, expect } from "@openremote/test";

import { OrIcon } from "@openremote/or-icon";

for (const icon of ["map", "rhombus-split", "state-machine", "chart-areaspline"]) {
  ct(`Should render "mdi" icon: ${icon}`, async ({ mount }) => {
    const component = await mount(OrIcon, { props: { icon } });
    await expect(component).toHaveScreenshot();
  });
}

for (const icon of ["or:logo", "or:logo-plain", "or:marker"]) {
  ct(`Should render "or" icon: ${icon}`, async ({ mount }) => {
    const component = await mount(OrIcon, { props: { icon } });
    await expect(component).toHaveScreenshot();
  });
}
