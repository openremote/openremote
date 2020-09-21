import {
    AttributePredicate,
    GeofencePredicate,
    JsonRulesetDefinition,
    PushNotificationMessage,
    RuleActionUnion,
    RuleCondition,
    LogicGroup,
    Asset,
    Attribute,
    AttributeEvent,
    MetaItem,
    AssetAttribute,
    AttributeDescriptor,
    MetaItemType,
    MetaItemDescriptor,
    AttributeValueDescriptor
} from "@openremote/model";
import i18next from "i18next";
import Qs from "qs";
import {AssetModelUtil} from "./index";

export class Deferred<T> {

    protected _resolve!: (value?: T | PromiseLike<T>) => void;
    protected _reject!: (reason?: any) => void;
    protected _promise: Promise<T>;

    get resolve() {
        return this._resolve;
    }

    get reject() {
        return this._reject;
    }

    get promise() {
        return this._promise;
    }

    constructor() {
        this._promise = new Promise<T>((resolve1, reject1) => {
            this._resolve = resolve1;
            this._reject = reject1;
        });
        Object.freeze(this);
    }
}

export interface GeoNotification {
    predicate: GeofencePredicate;
    notification?: PushNotificationMessage;
}

export function getQueryParameters(queryStr: string): any {
    const parsed = Qs.parse(queryStr, {ignoreQueryPrefix: true});
    return parsed;
}

export function getQueryParameter(queryStr: string, parameter: string): any | undefined {
    const parsed = getQueryParameters(queryStr);
    return parsed ? parsed[parameter] : undefined;
}

export function getGeoNotificationsFromRulesSet(rulesetDefinition: JsonRulesetDefinition): GeoNotification[] {

    const geoNotifications: GeoNotification[] = [];

    rulesetDefinition.rules!.forEach((rule) => {

        if (rule.when && rule.then && rule.then.length > 0) {
            const geoNotificationMap = new Map<String, GeoNotification[]>();
            addGeofencePredicatesFromRuleConditionCondition(rule.when, 0, geoNotificationMap);

            if (geoNotificationMap.size > 0) {
                rule.then.forEach((ruleAction) => addPushNotificationsFromRuleAction(ruleAction, geoNotificationMap));
            }
            for (const geoNotificationsArr of geoNotificationMap.values()) {
                geoNotificationsArr.forEach((geoNotification) => {
                    if (geoNotification.notification) {
                        geoNotifications.push(geoNotification);
                    }
                });
            }
        }
    });

    return geoNotifications;
}

function addGeofencePredicatesFromRuleConditionCondition(ruleCondition: LogicGroup<RuleCondition> | undefined, index: number, geoNotificationMap: Map<String, GeoNotification[]>) {
    if (!ruleCondition) {
        return;
    }
    const items:any = [];
    if (ruleCondition.groups) {
        ruleCondition.groups.forEach((ruleGroup) => {
            if(ruleGroup.items){
                ruleGroup.items.forEach((ruleTrigger) => {
                    items.push(ruleTrigger)
                });
            }
        });
    }
    
    if (ruleCondition.items) {
        ruleCondition.items.forEach((ruleTrigger) => {
            items.push(ruleTrigger)
        });
        
    }
    if (items) {
        items.forEach((ruleTrigger:any) => {
            if (ruleTrigger.assets && ruleTrigger.assets.attributes) {
                const geoNotifications: GeoNotification[] = [];
                addGeoNotificationsFromAttributePredicateCondition(ruleTrigger.assets.attributes, geoNotifications);
                if (geoNotifications.length > 0) {
                    const tagName = ruleTrigger.tag || index.toString();
                    geoNotificationMap.set(tagName, geoNotifications);
                }
            }
        });
    }
}

function addGeoNotificationsFromAttributePredicateCondition(attributeCondition: LogicGroup<AttributePredicate> | undefined, geoNotifications: GeoNotification[]) {
    if (!attributeCondition) {
        return;
    }

    attributeCondition.items!.forEach((predicate) => {
        if (predicate.value && (predicate.value.predicateType === "radial" || predicate.value!.predicateType === "rect")) {
            geoNotifications.push({
                predicate: predicate.value as GeofencePredicate
            });
        }
    });

    if (attributeCondition.groups) {
        attributeCondition.groups.forEach((condition) => addGeoNotificationsFromAttributePredicateCondition(condition, geoNotifications));
    }
}

function addPushNotificationsFromRuleAction(ruleAction: RuleActionUnion, geoPredicateMap: Map<String, GeoNotification[]>) {
    if (ruleAction && ruleAction.action === "notification") {
        if (ruleAction.notification && ruleAction.notification.message && ruleAction.notification.message.type === "push") {
            // Find applicable targets
            const target = ruleAction.target;
            if (target && target.ruleConditionTag) {
                const geoNotifications = geoPredicateMap.get(target.ruleConditionTag);
                if (geoNotifications) {
                    geoNotifications.forEach((geoNotification) => {
                        geoNotification.notification = ruleAction.notification!.message as PushNotificationMessage;
                    });
                }
            } else {
                // Applies to all LHS rule triggers
                for (const geoNotifications of geoPredicateMap.values()) {
                    geoNotifications.forEach((geoNotification) => {
                        geoNotification.notification = ruleAction.notification!.message as PushNotificationMessage;
                    });
                }
            }
        }
    }
}

const TIME_DURATION_REGEXP = /([+-])?((\d+)[Dd])?\s*((\d+)[Hh])?\s*((\d+)[Mm]$)?\s*((\d+)[Ss])?\s*((\d+)([Mm][Ss]$))?\s*((\d+)[Ww])?\s*((\d+)[Mm][Nn])?\s*((\d+)[Yy])?/;

export function isTimeDuration(time?: string): boolean {
    if (!time) {
        return false;
    }

    time = time.trim();

    return time.length > 0
        && (TIME_DURATION_REGEXP.test(time)
            || isTimeDurationPositiveInfinity(time)
            || isTimeDurationNegativeInfinity(time));
}

export function isTimeDurationPositiveInfinity(time?: string): boolean {
    time = time != null ? time.trim() : undefined;
    return "*" === time || "+*" === time;
}

export function isTimeDurationNegativeInfinity(time?: string): boolean {
    time = time != null ? time.trim() : undefined;
    return "-*" === time;
}

export function isObject(object: any): boolean {
    if (!!object) {
        return typeof object === "object";
    }
    return false;
}

export function objectsEqual(obj1?: any, obj2?: any, deep: boolean = true): boolean {
    if (obj1 === null || obj1 === undefined || obj2 === null || obj2 === undefined) { return obj1 === obj2; }
    // after this just checking type of one would be enough
    if (obj1.constructor !== obj2.constructor) { return false; }
    // if they are functions, they should exactly refer to same one (because of closures)
    if (obj1 instanceof Function) { return obj1 === obj2; }
    // if they are regexps, they should exactly refer to same one (it is hard to better equality check on current ES)
    if (obj1 instanceof RegExp) { return obj1 === obj2; }
    if (obj1 === obj2 || obj1.valueOf() === obj2.valueOf()) { return true; }
    if (Array.isArray(obj1) && obj1.length !== obj2.length) { return false; }

    // if they are dates, they must had equal valueOf
    if (obj1 instanceof Date) { return false; }

    // if they are strictly equal, they both need to be object at least
    if (!(obj1 instanceof Object)) { return false; }
    if (!(obj2 instanceof Object)) { return false; }

    if (deep) {
        // recursive object equality check
        const p = Object.keys(obj1);
        return Object.keys(obj2).every((i) => {
                return p.indexOf(i) !== -1;
            }) &&
            p.every((i) => {
                return objectsEqual(obj1[i], obj2[i]);
            });
    }

    return false;
}

export function arrayRemove<T>(arr: T[], item: T) {
    if (arr.length === 0) {
        return;
    }
    const index = arr.indexOf(item);
    if (index >= 0) {
        arr.splice(index, 1);
    }
}

export function stringMatch(needle: string, haystack: string): boolean {

    if (haystack === needle) {
        return true;
    }

    const startsWith = needle.endsWith("*");
    const endsWith = !startsWith && needle.startsWith("*");
    const regExp = !startsWith && !endsWith && needle.startsWith("^") && needle.endsWith("$")

    if (startsWith && haystack.startsWith(needle.substr(0, needle.length - 1))) {
        return true;
    }

    if (endsWith && haystack.endsWith(needle.substr(1))) {
        return true;
    }

    if (regExp) {
        try {
            const regexp = new RegExp(needle);
            return regexp.test(haystack);
        } catch(e) {
            console.error("Failed to compile needle as a RegExp: " + e);
        }
    }

    return false;
}

export function enumContains(enm: object, val: string): boolean {
    return enm && Object.values(enm).includes(val);
}

export function getEnumKeyAsString(enm: object, val: string): string {
    // @ts-ignore
    const key = Object.keys(enm).find((k) => enm[k] === val);
    return key!;
}

export function getAssetAttribute(asset: Asset, attributeName: string): AssetAttribute | undefined {
    if (asset && asset.attributes && asset.attributes.hasOwnProperty(attributeName)) {
        const attr = {...asset.attributes[attributeName], name: attributeName, assetId: asset.id} as AssetAttribute;
        return attr;
    }
}

export function getAssetAttributes(asset: Asset, exclude?: string[]): AssetAttribute[] {
    if (asset.attributes) {
        return Object.entries(asset.attributes as {[s: string]: AssetAttribute}).filter(([name, attr]) => !exclude || exclude.indexOf(name) >= 0).map(([name, attr]) => {
            attr = {...attr, name: name, assetId: asset.id};
            return attr;
        });
    }

    return [];
}


export function getFirstMetaItem(attribute: Attribute | undefined, name: string): MetaItem | undefined {
    if (!attribute || !attribute.meta) {
        return;
    }

    return attribute.meta.find((metaItem) => metaItem.name === name);
}

export function hasMetaItem(attribute: Attribute, name: string): boolean {
    return !!getFirstMetaItem(attribute, name);
}

export function getMetaValue(metaItemUrn: string | MetaItemDescriptor, attribute: Attribute | undefined, descriptor: AttributeDescriptor | undefined, valueDescriptor?: AttributeValueDescriptor): any {
    const urn = typeof metaItemUrn === "string" ? metaItemUrn : (metaItemUrn as MetaItemDescriptor).urn;

    if (attribute && attribute.meta) {
        const metaItem = attribute.meta.find((mi) => mi.name === urn);
        if (metaItem) {
            return metaItem.value;
        }
    }

    if (descriptor && descriptor.metaItemDescriptors) {
        const metaItemDescriptor = descriptor.metaItemDescriptors.find((mid) => mid.urn === urn);
        if (metaItemDescriptor) {
            return metaItemDescriptor.initialValue;
        }
    }

    if (valueDescriptor && valueDescriptor.metaItemDescriptors) {
        const metaItemDescriptor = valueDescriptor.metaItemDescriptors.find((mid) => mid.urn === urn);
        if (metaItemDescriptor) {
            return metaItemDescriptor.initialValue;
        }
    }
}

export function getAttributeLabel(attribute: Attribute | undefined, descriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, showUnits: boolean, fallback?: string): string {
    if (!attribute && !descriptor) {
        return fallback || "";
    }

    const label = getMetaValue(MetaItemType.LABEL, attribute, descriptor, valueDescriptor) as string;
    let units = showUnits ? getMetaValue(MetaItemType.UNIT_TYPE, attribute, descriptor, valueDescriptor) as string : undefined;
    units = units ? i18next.t(["units." + units, units]) : undefined;
    const name = attribute ? attribute.name : descriptor!.attributeName;
    const keys = [];
    if (name) {
        keys.push("attribute." + name);
        keys.push(name);
    }
    if (label) {
        keys.push(label);
    }
    if (fallback) {
        keys.push(fallback);
    }

    return i18next.t(keys) + (units ? " (" + units + ")" : "");
}

export function getMetaItemLabel(urn: string): string {
    return i18next.t(["metaItemType." + urn, urn], {nsSeparator: "@"});
}

export function getAttributeValueFormat(attribute: Attribute | undefined, descriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined): string | undefined {
    let format = getMetaValue(MetaItemType.FORMAT, attribute, descriptor, valueDescriptor) as string;
    if (!format) {
        let valueType: string | undefined;
        if (attribute) {
            valueType = attribute.type! as string;
        } if (valueDescriptor) {
            valueType = valueDescriptor.valueType;
        } else if (descriptor) {
            valueDescriptor = descriptor.valueDescriptor as any;
            // noinspection SuspiciousTypeOfGuard
            if (typeof valueDescriptor === "string") {
                valueDescriptor = AssetModelUtil.getAttributeValueDescriptor(valueDescriptor);
            }
            if (valueDescriptor) {
                valueType = valueDescriptor.valueType;
            }
        }
        if (valueType) {
            format = i18next.t("attributeValueType." + valueType);
        }
    }
    return format;
}

export function getAttributeValueFormatter(): ((value: any, format: string | undefined) => string) {
    return (value, format) => {
        return value === undefined || value === null ? "" : i18next.t((format ? [format, "%s"] : "%s"), { postProcess: "sprintf", sprintf: [value] });
    };
}

export function getAttributeValueFormatted(attribute: Attribute, descriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, fallback?: string): any {

    if (!attribute) {
        return fallback || "";
    }

    if (attribute.value === undefined || attribute.value === null) {
        return "";
    }

    const format = getAttributeValueFormat(attribute, descriptor, valueDescriptor);
    return getAttributeValueFormatter()(attribute.value, format);
}

/**
 * Immutable update of an asset using the supplied attribute event
 */
export function updateAsset(asset: Asset, event: AttributeEvent): Asset {

    const attributeName = event.attributeState!.attributeRef!.attributeName!;

    if (asset.attributes) {
        if (event.attributeState!.deleted) {
            delete asset.attributes![attributeName];
        } else {
            const attribute = getAssetAttribute(asset, attributeName);
            if (attribute) {
                attribute.value = event.attributeState!.value;
                attribute.valueTimestamp = event.timestamp;
            }
        }
    }

    return Object.assign({}, asset);
}

export function loadJs(url: string) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.type = 'text/javascript';
            script.src = url;
            script.addEventListener('load', (e) => resolve(e), false);
            script.addEventListener('error', (e) => reject(e), false);
            document.body.appendChild(script);
        });
};

export function sortByString<T>(valueExtractor: (item: T) => string): (a: T, b: T) => number {
    return (a,b) => {
        const v1 = valueExtractor(a);
        const v2 = valueExtractor(b);
        if (v1 > v2) {
            return 1;
        }
        if (v1 < v2) {
            return -1;
        }
        return 0;
    };
}

export interface RequestEventDetail<T> {
    allow: boolean;
    detail: T;
}

export function dispatchCancellableEvent<T>(target: EventTarget, event: CustomEvent<RequestEventDetail<T>>, handler: (detail: T) => void) {
    target.dispatchEvent(event);
    window.setTimeout(() => {
        if (event.detail.allow) {
            handler(event.detail.detail);
        }
    });
}
