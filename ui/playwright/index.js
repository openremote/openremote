import { IconSets, OrIconSet, createMdiIconSet, createSvgIconSet } from "@openremote/or-icon";

/// TODO: should not require the backend to be running for the iconset
IconSets.addIconSet("mdi", createMdiIconSet("http://localhost:8080"));
IconSets.addIconSet("or", createSvgIconSet(OrIconSet.size, OrIconSet.icons));
