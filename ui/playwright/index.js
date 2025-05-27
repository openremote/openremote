import { IconSets, OrIconSet, createMdiIconSet, createSvgIconSet } from "@openremote/or-icon";
import { i18next } from "@openremote/or-translate";

IconSets.addIconSet("mdi", createMdiIconSet(""));
IconSets.addIconSet("or", createSvgIconSet(OrIconSet.size, OrIconSet.icons));

await i18next
  .init({
    lng: "en",
    fallbackLng: "en",
    defaultNS: "app",
    fallbackNS: "or",
    ns: ["en", "nl"],
  })
  .then(() => {
    i18next.addResource("en", "or", "thing", "Thing");
    i18next.addResource("nl", "or", "thing", "Ding");
  });
