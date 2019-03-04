import {JsonRulesetDefinition, PushNotificationMessage, RadialGeofencePredicate} from "@openremote/model";

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
    predicate: RadialGeofencePredicate
    notification: PushNotificationMessage
}

export function getGeoNotificationsFromRulesSet(rulesetDefinition: JsonRulesetDefinition): GeoNotification[] {

    let geoPredicates: GeoNotification[] = [];
    let geoNotification: GeoNotification;

    rulesetDefinition.rules!.forEach((rule) => {
        geoNotification = {} as GeoNotification;
        if (rule.when && rule.when.asset) {
            rule.when.asset.attributes!.predicates!.forEach((predicate) => {
                if (predicate.value!.predicateType == "radial" || predicate.value!.predicateType == "rect") {
                    geoNotification.predicate = predicate.value as RadialGeofencePredicate
                }
            });
        }

        if (rule.then) {
            rule.then.forEach((action) => {
                if (action.action == "notification") {
                    if (action.notification && action.notification.message) {
                        geoNotification.notification = action.notification.message as PushNotificationMessage;
                    }
                }
            });
        }
        if (geoNotification.predicate && geoNotification.notification) {
            geoPredicates.push(geoNotification)
        }
    });

    return geoPredicates;
}
