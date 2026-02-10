import {getStorybookHelpers} from "@wc-toolkit/storybook-helpers";

export function getORStorybookHelpers(tagName) {
  const config = getStorybookHelpers(tagName);

  // Sort arguments, so that CSS properties end up on the bottom
  if (config.args) {
    const entries = Object.entries(config.args);
    config.args = Object.fromEntries([
      ...entries.filter(([k]) => !k.startsWith("--")),
      ...entries.filter(([k]) => k.startsWith("--"))
    ]);
  }
  return config;
}
