import type { Asset } from "@openremote/model";

export const notes = { name: "notes", type: "text" };
export const location = { name: "location", type: "GEO_JSONPoint" };
export const commonAttrs = { notes, location };
export const validAsset: Asset = {
    id: "validAsset",
    name: "Thing",
    realm: "master",
    type: "ThingAsset",
    attributes: { ...commonAttrs },
};
export const invalidAsset: Asset = {
    id: "invalidAsset",
    name: "Thing",
    realm: "master",
    type: "ThingAsset",
    attributes: { ...commonAttrs, invalid: { name: "invalid", type: "integer" } },
};
