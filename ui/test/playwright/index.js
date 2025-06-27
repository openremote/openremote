import i18next from "i18next";
import HttpBackend from "i18next-http-backend";

import { IconSets, OrIconSet, createMdiIconSet, createSvgIconSet } from "@openremote/or-icon";

IconSets.addIconSet("mdi", createMdiIconSet(""));
IconSets.addIconSet("or", createSvgIconSet(OrIconSet.size, OrIconSet.icons));

window._i18next = i18next.use(HttpBackend);
