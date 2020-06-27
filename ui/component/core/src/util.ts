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

    if (ruleCondition.items) {
        ruleCondition.items.forEach((ruleTrigger) => {
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
    if (obj1 === obj2) {
        return true;
    }

    if (!obj1 || !obj2) {
        return false;
    }

    if (deep) {
        let iterator1: [string, any][] | undefined;
        let iterator2: [string, any][] | undefined;

        if (Array.isArray(obj1) || typeof obj1 === "object") {
            if (typeof obj2 !== typeof obj1) {
                return false;
            }
            iterator1 = Object.entries(obj1).sort((a, b) => b[0].localeCompare(a[0]));
            iterator2 = Object.entries(obj2).sort((a, b) => b[0].localeCompare(a[0]));
        }

        if (!iterator1 || !iterator2) {
            return false;
        }

        if (iterator1.length !== iterator2.length) {
            return false;
        }

        for (let i = iterator1.length; i--;) {
            if (iterator1[i][0] !== iterator2[i][0] || !objectsEqual(iterator1[i][1], iterator2[i][1])) {
                return false;
            }
        }

        return true;
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
        return metaItem ? metaItem.value : undefined;
    }

    if (descriptor && descriptor.metaItemDescriptors) {
        const metaItemDescriptor = descriptor.metaItemDescriptors.find((mid) => mid.urn === urn);
        return metaItemDescriptor ? metaItemDescriptor.initialValue : undefined;
    }

    if (valueDescriptor && valueDescriptor.metaItemDescriptors) {
        const metaItemDescriptor = valueDescriptor.metaItemDescriptors.find((mid) => mid.urn === urn);
        return metaItemDescriptor ? metaItemDescriptor.initialValue : undefined;
    }
}

export function getAttributeLabel(attribute: Attribute | undefined, descriptor: AttributeDescriptor | undefined, fallback?: string): string {
    if (!attribute && !descriptor) {
        return fallback || "";
    }

    const labelMetaValue = getMetaValue(MetaItemType.LABEL, attribute, descriptor);
    const name = attribute ? attribute.name : descriptor!.attributeName;
    return i18next.t([name, fallback || labelMetaValue || name]);
}

export function getAttributeValueFormatter(attribute: Attribute | undefined, descriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined): ((value: any) => string) {

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

    return (value: any) => {
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

    return getAttributeValueFormatter(attribute, descriptor, valueDescriptor)(attribute.value);
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
