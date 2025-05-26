import { IconSets, OrIconSet, createMdiIconSet, createSvgIconSet } from "@openremote/or-icon";
import { i18next } from "@openremote/or-translate";

/// TODO: should not require the backend to be running for the iconset
IconSets.addIconSet("mdi", createMdiIconSet("http://localhost:8080"));
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
