/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 2.28.785 on 2022-04-14 20:45:29.

export interface ConsoleAppConfig {
    id?: number;
    realm?: string;
    initialUrl?: string;
    url?: string;
    menuEnabled?: boolean;
    menuPosition?: ConsoleAppConfigMenuPosition;
    primaryColor?: string;
    secondaryColor?: string;
    links?: ConsoleAppConfigAppLink[];
}

export interface ConsoleAppConfigAppLink {
    displayText?: string;
    pageLink?: string;
}

export interface Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface AssetDescriptor extends NameHolder {
    descriptorType: "asset" | "agent";
    icon?: string;
    colour?: string;
}

export interface AssetEvent extends SharedEvent, AssetInfo {
    eventType: "asset";
    cause?: AssetEventCause;
    asset?: Asset;
    updatedProperties?: string[];
}

export interface AssetFilter {
    filterType: "asset";
    assetIds?: string[];
    realm?: string;
    parentIds?: string[];
    path?: string[];
    attributeNames?: string[];
    publicEvents?: boolean;
    restrictedEvents?: boolean;
}

export interface AssetTreeNode {
    asset?: Asset;
    children?: AssetTreeNode[];
}

export interface AssetTypeInfo {
    assetDescriptor?: AssetDescriptor;
    attributeDescriptors?: AttributeDescriptor[];
    metaItemDescriptors: string[];
    valueDescriptors: string[];
}

export interface AssetsEvent extends SharedEvent {
    eventType: "assets";
    assets?: Asset[];
}

export interface DeleteAssetsRequestEvent extends SharedEvent {
    eventType: "delete-assets-request";
    assetIds?: string[];
}

export interface DeleteAssetsResponseEvent extends SharedEvent {
    eventType: "delete-assets-response";
    deleted?: boolean;
    assetIds?: string[];
}

export interface ReadAssetEvent extends SharedEvent {
    eventType: "read-asset";
    assetId?: string;
}

export interface ReadAssetsEvent extends SharedEvent {
    eventType: "read-assets";
    assetQuery?: AssetQuery;
}

export interface ReadAttributeEvent extends SharedEvent {
    eventType: "read-asset-attribute";
    ref?: AttributeRef;
}

export interface UserAssetLink {
    id?: UserAssetLinkId;
    createdOn?: DateAsNumber;
    assetName?: string;
    parentAssetName?: string;
    userFullName?: string;
}

export interface UserAssetLinkId {
    realm?: string;
    userId?: string;
    assetId?: string;
}

export interface Agent extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface AgentDescriptor extends AssetDescriptor {
    descriptorType: "agent";
    name?: string;
    icon?: string;
    colour?: string;
    instanceDiscoveryProvider?: boolean;
    assetDiscovery?: boolean;
    assetImport?: boolean;
    agentLinkType?: string;
}

export interface AgentLink {
    type: string;
    id?: string;
    /**
     * Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order
     */
    valueFilters?: ValueFilterUnion[];
    /**
     * Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute
     */
    valueConverter?: { [id: string]: any };
    /**
     * Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion
     */
    writeValueConverter?: { [id: string]: any };
    /**
     * String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol (particularly useful for executable attributes)
     */
    writeValue?: string;
    /**
     * The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.
     */
    messageMatchPredicate?: ValuePredicateUnion;
    /**
     * ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate
     */
    messageMatchFilters?: ValueFilterUnion[];
    /**
     * Don't expect a response from the protocol just update the attribute immediately on write
     */
    updateOnWrite?: boolean;
}

export interface Protocol {
}

export interface BuildingAsset extends CityAsset {
}

export interface CityAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface ConsoleAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface DoorAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface ElectricVehicleAsset extends ElectricityBatteryAsset {
}

export interface ElectricVehicleFleetGroupAsset extends GroupAsset {
}

export interface ElectricityAsset<T> extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface ElectricityBatteryAsset extends ElectricityStorageAsset {
}

export interface ElectricityChargerAsset extends ElectricityStorageAsset {
}

export interface ElectricityConsumerAsset extends ElectricityAsset<ElectricityConsumerAsset> {
}

export interface ElectricityProducerAsset extends ElectricityAsset<ElectricityProducerAsset> {
}

export interface ElectricityProducerSolarAsset extends ElectricityProducerAsset {
}

export interface ElectricityProducerWindAsset extends ElectricityProducerAsset {
}

export interface ElectricityStorageAsset extends ElectricityAsset<ElectricityStorageAsset> {
}

export interface ElectricitySupplierAsset extends ElectricityAsset<ElectricitySupplierAsset> {
}

export interface EnergyOptimisationAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface EnvironmentSensorAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface GatewayAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface GroundwaterSensorAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface GroupAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface LightAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface MicrophoneAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface ParkingAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface PeopleCounterAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface PlugAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface PresenceSensorAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface RoomAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface ShipAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface ThermostatAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface ThingAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface UnknownAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface VentilationAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface WeatherAsset extends Asset {
    id?: string;
    version?: number;
    createdOn?: DateAsNumber;
    name?: string;
    accessPublicRead?: boolean;
    parentId?: string;
    realm?: string;
    parentName?: string;
    parentType?: string;
    type?: string;
    path?: string[];
    attributes?: { [index: string]: Attribute<any> };
}

export interface Attribute<T> extends AbstractNameValueHolder<T>, MetaHolder {
    timestamp?: number;
}

export interface AttributeEvent extends SharedEvent, AssetInfo {
    eventType: "attribute";
    attributeState?: AttributeState;
    realm?: string;
    parentId?: string;
    path?: string[];
}

export interface AttributeLink {
    ref?: AttributeRef;
    converter?: { [id: string]: any };
    filters?: ValueFilterUnion[];
}

export interface AttributeRef {
    id?: string;
    name?: string;
}

export interface AttributeState {
    ref?: AttributeRef;
    value?: any;
    deleted?: boolean;
}

export interface AttributeWriteResult {
    ref?: AttributeRef;
    failure?: AttributeWriteFailure;
}

export interface OAuthClientCredentialsGrant extends OAuthGrant {
    grant_type: "client_credentials" | "password" | "refresh_token";
}

export interface OAuthGrant {
    grant_type: "client_credentials" | "password" | "refresh_token";
    tokenEndpointUri?: string;
    basicAuthHeader?: boolean;
    client_id?: string;
    client_secret?: string;
    scope?: string;
}

export interface OAuthPasswordGrant extends OAuthClientCredentialsGrant {
    grant_type: "password";
    username?: string;
    password?: string;
}

export interface OAuthRefreshTokenGrant extends OAuthClientCredentialsGrant {
    grant_type: "refresh_token";
    refresh_token?: string;
}

export interface UsernamePassword {
    username?: string;
    password?: string;
}

export interface CalendarEvent {
    start?: DateAsNumber;
    end?: DateAsNumber;
    recurrence?: string;
}

export interface ConsoleProvider {
    version?: string;
    requiresPermission?: boolean;
    hasPermission?: boolean;
    success?: boolean;
    enabled?: boolean;
    disabled?: boolean;
    data?: { [id: string]: any };
}

export interface ConsoleRegistration {
    id?: string;
    name?: string;
    version?: string;
    platform?: string;
    providers?: { [index: string]: ConsoleProvider };
    model?: string;
    apps?: string[];
}

export interface AssetDatapoint extends Datapoint {
}

export interface AssetPredictedDatapoint extends Datapoint {
}

export interface Datapoint {
    assetId?: string;
    attributeName?: string;
    timestamp?: DateAsNumber;
    value?: any;
}

export interface DatapointPeriod {
    assetId?: string;
    attributeName?: string;
    oldestTimestamp?: number;
    latestTimestamp?: number;
}

export interface ValueDatapoint<T> {
    x?: number;
    y?: T;
}

export interface Event {
    timestamp?: DateAsNumber;
}

export interface TriggeredEventSubscription<T> {
    events?: T[];
    subscriptionId?: string;
}

export interface EventBus {
    registrations?: EventRegistration<any>[];
}

export interface EventListener<E> {
}

export interface EventRegistration<E> {
    prepare?: boolean;
    eventClass?: string;
    listener?: EventListener<E>;
}

export interface VetoEventException extends RuntimeException {
}

export interface AssetInfo {
}

export interface CancelEventSubscription {
    eventType?: string;
    subscriptionId?: string;
}

export interface EventRequestResponseWrapper<T> {
    messageId?: string;
    event?: T;
}

export interface EventSubscription<E> {
    eventType?: string;
    filter?: any;
    subscriptionId?: string;
}

export interface SharedEvent extends Event {
    eventType: "asset" | "assets" | "delete-assets-request" | "delete-assets-response" | "read-asset" | "read-assets" | "read-asset-attribute" | "attribute" | "gateway-connection-status" | "ProtocolDiscoveryImportRequestEvent" | "ProtocolDiscoveryStartRequestEvent" | "gateway-disconnect" | "ProtocolDiscoveryAssetFoundEvent" | "ProtocolDiscoveryInstanceFoundEvent" | "ProtocolDiscoveryStartStopResponseEvent" | "ProtocolDiscoveryStopRequestEvent" | "rules-engine-status" | "ruleset-changed" | "request-simulator-state" | "simulator-state" | "syslog";
}

export interface TenantScopedEvent extends SharedEvent {
    eventType: "gateway-connection-status" | "ProtocolDiscoveryImportRequestEvent" | "ProtocolDiscoveryStartRequestEvent";
    realm?: string;
}

export interface UnauthorizedEventSubscription<E> {
    subscription?: EventSubscription<E>;
}

export interface FileInfo {
    name?: string;
    contents?: string;
    binary?: boolean;
}

export interface GatewayConnection {
    localRealm?: string;
    host?: string;
    port?: number;
    realm?: string;
    clientId?: string;
    clientSecret?: string;
    secured?: boolean;
    disabled?: boolean;
}

export interface GatewayConnectionStatusEvent extends TenantScopedEvent {
    eventType: "gateway-connection-status";
    connectionStatus?: ConnectionStatus;
}

export interface GatewayDisconnectEvent extends SharedEvent {
    eventType: "gateway-disconnect";
    reason?: GatewayDisconnectEventReason;
}

export interface GeoJSON {
    type: "Feature" | "FeatureCollection" | "Point";
}

export interface GeoJSONFeature extends GeoJSON {
    type: "Feature";
    geometry?: GeoJSONGeometryUnion;
    properties?: { [index: string]: string };
}

export interface GeoJSONFeatureCollection extends GeoJSON {
    type: "FeatureCollection";
    features?: GeoJSONFeature[];
}

export interface GeoJSONGeometry extends GeoJSON {
    type: "Point";
}

export interface GeoJSONPoint extends GeoJSONGeometry {
    type: "Point";
    coordinates?: number[];
}

export interface RequestParams {
    authorization?: string;
    forwardedProtoHeader?: string;
    forwardedHostHeader?: string;
    uriInfo?: UriInfo;
}

export interface AbstractNotificationMessage {
    type: "email" | "push";
}

export interface EmailNotificationMessage extends AbstractNotificationMessage {
    type: "email";
    from?: EmailNotificationMessageRecipient;
    replyTo?: EmailNotificationMessageRecipient;
    subject?: string;
    text?: string;
    html?: string;
    to?: EmailNotificationMessageRecipient[];
    cc?: EmailNotificationMessageRecipient[];
    bcc?: EmailNotificationMessageRecipient[];
}

export interface EmailNotificationMessageRecipient {
    name?: string;
    address?: string;
}

export interface Notification {
    name?: string;
    message?: AbstractNotificationMessageUnion;
    targets?: NotificationTarget[];
    repeatFrequency?: RepeatFrequency;
    repeatInterval?: string;
}

export interface NotificationTarget {
    type?: NotificationTargetType;
    id?: string;
    data?: any;
}

export interface NotificationSendResult {
    success?: boolean;
    message?: string;
}

export interface PushNotificationAction {
    url?: string;
    data?: any;
    silent?: boolean;
    openInBrowser?: boolean;
    httpMethod?: string;
}

export interface PushNotificationButton {
    title?: string;
    action?: PushNotificationAction;
}

export interface PushNotificationMessage extends AbstractNotificationMessage {
    type: "push";
    title?: string;
    body?: string;
    action?: PushNotificationAction;
    buttons?: PushNotificationButton[];
    data?: { [id: string]: any };
    priority?: PushNotificationMessageMessagePriority;
    targetType?: PushNotificationMessageTargetType;
    target?: string;
    expiration?: number;
}

export interface SentNotification {
    id?: number;
    name?: string;
    type?: string;
    target?: NotificationTargetType;
    targetId?: string;
    source?: NotificationSource;
    sourceId?: string;
    message?: AbstractNotificationMessageUnion;
    error?: string;
    sentOn?: DateAsNumber;
    deliveredOn?: DateAsNumber;
    acknowledgedOn?: DateAsNumber;
    acknowledgement?: string;
}

export interface EpochMillisInstantType extends AbstractSingleColumnStandardBasicType<Date>, VersionType<Date>, LiteralType<Date> {
    javaTypeDescriptor?: JavaTypeDescriptor<DateAsNumber>;
}

export interface ProtocolDiscoveryAssetFoundEvent extends SharedEvent {
    eventType: "ProtocolDiscoveryAssetFoundEvent";
    agentDescriptor?: string;
    assets?: AssetTreeNode[];
}

export interface ProtocolDiscoveryImportRequestEvent extends TenantScopedEvent {
    eventType: "ProtocolDiscoveryImportRequestEvent";
    agentDescriptor?: string;
    assetId?: string;
    assetDiscovery?: boolean;
}

export interface ProtocolDiscoveryInstanceFoundEvent extends SharedEvent {
    eventType: "ProtocolDiscoveryInstanceFoundEvent";
    agentDescriptor?: string;
    instanceName?: string;
    attributes?: Attribute<any>[];
}

export interface ProtocolDiscoveryStartRequestEvent extends TenantScopedEvent {
    eventType: "ProtocolDiscoveryStartRequestEvent";
    agentDescriptor?: string;
    assetId?: string;
    assetDiscovery?: boolean;
}

export interface ProtocolDiscoveryStartStopResponseEvent extends SharedEvent {
    eventType: "ProtocolDiscoveryStartStopResponseEvent";
    agentDescriptor?: string;
    stopped?: boolean;
}

export interface ProtocolDiscoveryStopRequestEvent extends SharedEvent {
    eventType: "ProtocolDiscoveryStopRequestEvent";
}

export interface ErrorResponseMessage extends ProvisioningMessage {
    type: "error";
    error?: ErrorResponseMessageError;
}

export interface ProvisioningConfig<T, U> {
    type: "x509";
    id?: number;
    createdOn?: DateAsNumber;
    lastModified?: DateAsNumber;
    name?: string;
    realm?: string;
    assetTemplate?: string;
    restrictedUser?: boolean;
    userRoles?: ClientRole[];
    disabled?: boolean;
    data?: T;
}

export interface ProvisioningMessage {
    type: "error" | "success" | "x509";
}

export interface ProvisioningUtil {
}

export interface SuccessResponseMessage extends ProvisioningMessage {
    type: "success";
    realm?: string;
    asset?: Asset;
}

export interface X509ProvisioningConfig extends ProvisioningConfig<X509ProvisioningData, X509ProvisioningConfig> {
    type: "x509";
    data?: X509ProvisioningData;
}

export interface X509ProvisioningData {
    CACertPEM?: string;
    ignoreExpiryDate?: boolean;
}

export interface X509ProvisioningMessage extends ProvisioningMessage {
    type: "x509";
    cert?: string;
}

export interface AssetQuery {
    recursive?: boolean;
    select?: AssetQuerySelect;
    access?: AssetQueryAccess;
    ids?: string[];
    names?: StringPredicate[];
    parents?: ParentPredicate[];
    paths?: PathPredicate[];
    tenant?: TenantPredicate;
    userIds?: string[];
    types?: string[];
    attributes?: LogicGroup<AttributePredicate>;
    orderBy?: AssetQueryOrderBy;
    limit?: number;
}

export interface AssetQueryOrderBy {
    property?: AssetQueryOrderBy$Property;
    descending?: boolean;
}

export interface AssetQuerySelect {
    attributes?: string[];
    excludePath?: boolean;
    excludeAttributes?: boolean;
    excludeParentInfo?: boolean;
}

export interface LogicGroup<T> {
    operator?: LogicGroupOperator;
    items?: T[];
    groups?: LogicGroup<T>[];
}

export interface RulesetQuery {
    ids?: number[];
    meta?: NameValuePredicate[];
    limit?: number;
    languages?: RulesetLang[];
    fullyPopulate?: boolean;
    publicOnly?: boolean;
    enabledOnly?: boolean;
    realm?: string;
    assetIds?: string[];
}

export interface UserQuery {
    tenantPredicate?: TenantPredicate;
    assetPredicate?: UserAssetPredicate;
    pathPredicate?: PathPredicate;
    ids?: string[];
    select?: UserQuerySelect;
    usernames?: StringPredicate[];
    limit?: number;
    offset?: number;
    orderBy?: UserQueryOrderBy;
}

export interface UserQueryOrderBy {
    property?: UserQueryOrderBy$Property;
    descending?: boolean;
}

export interface UserQuerySelect {
    basic?: boolean;
    excludeServiceUsers?: boolean;
    excludeRegularUsers?: boolean;
    excludeSystemUsers?: boolean;
}

export interface ArrayPredicate extends ValuePredicate {
    predicateType: "array";
    value?: any;
    index?: number;
    lengthEquals?: number;
    lengthGreaterThan?: number;
    lengthLessThan?: number;
    negated?: boolean;
}

export interface AttributePredicate extends NameValuePredicate {
    meta?: NameValuePredicate[];
    previousValue?: ValuePredicateUnion;
}

export interface BooleanPredicate extends ValuePredicate {
    predicateType: "boolean";
    value?: boolean;
}

export interface CalendarEventPredicate extends ValuePredicate {
    predicateType: "calendar-event";
    timestamp?: DateAsNumber;
}

export interface DateTimePredicate extends ValuePredicate {
    predicateType: "datetime";
    value?: string;
    rangeValue?: string;
    operator?: AssetQueryOperator;
    negate?: boolean;
}

export interface GeofencePredicate extends ValuePredicate {
    predicateType: "radial" | "rect";
    negated?: boolean;
}

export interface LocationAttributePredicate extends AttributePredicate {
}

export interface NameValuePredicate {
    name?: StringPredicate;
    negated?: boolean;
    path?: NameValuePredicatePath;
    value?: ValuePredicateUnion;
}

export interface NameValuePredicatePath {
    paths?: any[];
}

export interface NumberPredicate extends ValuePredicate {
    predicateType: "number";
    value?: number;
    rangeValue?: number;
    operator?: AssetQueryOperator;
    negate?: boolean;
}

export interface ParentPredicate {
    id?: string;
    type?: string;
    name?: string;
    noParent?: boolean;
}

export interface PathPredicate {
    path?: string[];
}

export interface RadialGeofencePredicate extends GeofencePredicate {
    predicateType: "radial";
    radius?: number;
    lat?: number;
    lng?: number;
}

export interface RectangularGeofencePredicate extends GeofencePredicate {
    predicateType: "rect";
    latMin?: number;
    lngMin?: number;
    latMax?: number;
    lngMax?: number;
}

export interface StringPredicate extends ValuePredicate {
    predicateType: "string";
    match?: AssetQueryMatch;
    caseSensitive?: boolean;
    value?: string;
    negate?: boolean;
}

export interface TenantPredicate {
    realm?: string;
}

export interface UserAssetPredicate {
    id?: string;
}

export interface ValueAnyPredicate extends ValuePredicate {
    predicateType: "value-any";
}

export interface ValueEmptyPredicate extends ValuePredicate {
    predicateType: "value-empty";
    negate?: boolean;
}

export interface ValuePredicate {
    predicateType: "array" | "boolean" | "calendar-event" | "datetime" | "radial" | "rect" | "number" | "string" | "value-any" | "value-empty";
}

export interface AssetRuleset extends Ruleset {
    type: "asset";
    assetId?: string;
    accessPublicRead?: boolean;
    realm?: string;
}

export interface AssetState<T> extends NameValueHolder<T>, MetaHolder {
    attributeName?: string;
    timestamp?: number;
    source?: AttributeEventSource;
    oldValue?: T;
    oldValueTimestamp?: number;
    id?: string;
    assetName?: string;
    assetType?: string;
    createdOn?: DateAsNumber;
    path?: string[];
    parentId?: string;
    parentName?: string;
    parentType?: string;
    realm?: string;
    asset?: Asset;
    attribute?: Attribute<T>;
}

export interface GlobalRuleset extends Ruleset {
    type: "global";
}

export interface RulesEngineInfo {
    status?: RulesEngineStatus;
    compilationErrorCount?: number;
    executionErrorCount?: number;
}

export interface RulesEngineStatusEvent extends SharedEvent {
    eventType: "rules-engine-status";
    engineId?: string;
    engineInfo?: RulesEngineInfo;
}

export interface Ruleset {
    type: "asset" | "global" | "tenant";
    id?: number;
    version?: number;
    createdOn?: DateAsNumber;
    lastModified?: DateAsNumber;
    name?: string;
    enabled?: boolean;
    rules?: string;
    lang?: RulesetLang;
    meta?: { [index: string]: any };
    status?: RulesetStatus;
    error?: string;
}

export interface RulesetChangedEvent extends SharedEvent {
    eventType: "ruleset-changed";
    engineId?: string;
    ruleset?: RulesetUnion;
}

export interface TenantRuleset extends Ruleset {
    type: "tenant";
    realm?: string;
    accessPublicRead?: boolean;
}

export interface AttributeInternalValue {
    assetId?: string;
    attributeName?: string;
}

export interface Node {
    id?: string;
    type?: NodeType;
    name?: string;
    position?: NodePosition;
    size?: NodePosition;
    internals?: NodeInternal[];
    inputs?: NodeSocket[];
    outputs?: NodeSocket[];
    displayCharacter?: string;
}

export interface NodeCollection {
    name?: string;
    description?: string;
    nodes?: Node[];
    connections?: NodeConnection[];
}

export interface NodeConnection {
    from?: string;
    to?: string;
}

export interface NodeInternal {
    name?: string;
    picker?: Picker;
    value?: any;
}

export interface NodePosition {
    x?: number;
    y?: number;
}

export interface NodeSocket {
    id?: string;
    name?: string;
    type?: NodeDataType;
    nodeId?: string;
    index?: number;
}

export interface Option {
    name?: string;
    value?: any;
}

export interface Picker {
    type?: PickerType;
    options?: Option[];
}

export interface GeofenceDefinition {
    id?: string;
    lat?: number;
    lng?: number;
    radius?: number;
    httpMethod?: string;
    url?: string;
}

export interface JsonRule {
    name?: string;
    description?: string;
    priority?: number;
    when?: LogicGroup<RuleCondition>;
    then?: RuleActionUnion[];
    otherwise?: RuleActionUnion[];
    recurrence?: RuleRecurrence;
    onStart?: RuleActionUnion[];
    onStop?: RuleActionUnion[];
}

export interface JsonRulesetDefinition {
    rules?: JsonRule[];
    meta?: { [id: string]: any };
}

export interface RuleAction {
    action: "notification" | "update-attribute" | "wait" | "write-attribute";
    target?: RuleActionTarget;
}

export interface RuleActionNotification extends RuleAction {
    action: "notification";
    notification?: Notification;
}

export interface RuleActionTarget {
    conditionAssets?: string;
    matchedAssets?: AssetQuery;
    assets?: AssetQuery;
    users?: UserQuery;
    custom?: string;
}

export interface RuleActionUpdateAttribute extends RuleAction {
    action: "update-attribute";
    attributeName?: string;
    value?: any;
    key?: string;
    index?: number;
    updateAction?: RuleActionUpdateAttributeUpdateAction;
}

export interface RuleActionWait extends RuleAction {
    action: "wait";
    millis?: number;
}

export interface RuleActionWriteAttribute extends RuleAction {
    action: "write-attribute";
    attributeName?: string;
    value?: any;
}

export interface RuleCondition {
    timer?: string;
    assets?: AssetQuery;
    tag?: string;
}

export interface RuleRecurrence {
    scope?: RuleRecurrenceScope;
    mins?: number;
}

export interface RuleTemplate<T> {
    name?: string;
    value?: T;
}

export interface Credential {
    type?: string;
    value?: string;
    temporary?: boolean;
}

export interface RealmRole {
    name?: string;
    description?: string;
}

export interface Role {
    id?: string;
    name?: string;
    description?: string;
    composite?: boolean;
    assigned?: boolean;
    compositeRoleIds?: string[];
}

export interface Tenant {
    id?: string;
    realm?: string;
    displayName?: string;
    enabled?: boolean;
    notBefore?: number;
    resetPasswordAllowed?: boolean;
    duplicateEmailsAllowed?: boolean;
    rememberMe?: boolean;
    registrationAllowed?: boolean;
    registrationEmailAsUsername?: boolean;
    verifyEmail?: boolean;
    loginWithEmail?: boolean;
    loginTheme?: string;
    accountTheme?: string;
    adminTheme?: string;
    emailTheme?: string;
    accessTokenLifespan?: number;
    realmRoles?: RealmRole[];
}

export interface User {
    realm?: string;
    realmId?: string;
    id?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    enabled?: boolean;
    createdOn?: DateAsNumber;
    secret?: string;
    attributes?: { [index: string]: string[] };
    serviceAccount?: boolean;
    username?: string;
}

export interface UserAttribute {
    name?: string;
    value?: string;
    id?: string;
}

export interface UserPasswordCredentials {
    username?: string;
    password?: string;
}

export interface RequestSimulatorState extends SharedEvent {
    eventType: "request-simulator-state";
    agentId?: string;
}

export interface SimulatorAttributeInfo {
    assetName?: string;
    assetId?: string;
    replay?: boolean;
    attribute?: Attribute<any>;
}

export interface SimulatorReplayDatapoint {
    timestamp?: number;
    value?: any;
}

export interface SimulatorState extends SharedEvent {
    eventType: "simulator-state";
    agentId?: string;
    attributes?: SimulatorAttributeInfo[];
}

export interface SyslogConfig {
    storedLevel?: SyslogLevel;
    storedCategories?: SyslogCategory[];
    storedMaxAgeMinutes?: number;
}

export interface SyslogEvent extends SharedEvent {
    eventType: "syslog";
    id?: number;
    level?: SyslogLevel;
    category?: SyslogCategory;
    subCategory?: string;
    message?: string;
}

export interface SyslogEventLevelCategoryFilter {
    filterType: "level-category-filter";
    level?: SyslogLevel;
    categories?: SyslogCategory[];
}

export interface HealthStatusProvider {
}

export interface AbstractNameValueDescriptorHolder extends ValueDescriptorHolder, NameHolder {
    type?: string;
    constraints?: ValueConstraintUnion[];
    format?: ValueFormat;
    units?: string[];
}

export interface AbstractNameValueHolder<T> extends NameValueHolder<T> {
}

export interface AttributeDescriptor extends AbstractNameValueDescriptorHolder, MetaHolder {
    name?: string;
    type?: string;
    constraints?: ValueConstraintUnion[];
    format?: ValueFormat;
    units?: string[];
    optional?: boolean;
}

export interface JsonPathFilter extends ValueFilter {
    type: "jsonPath";
    path?: string;
    returnFirst?: boolean;
    returnLast?: boolean;
}

export interface MetaHolder {
    meta?: { [index: string]: any };
}

export interface MetaItemDescriptor extends AbstractNameValueDescriptorHolder {
    name?: string;
    type?: string;
    constraints?: ValueConstraintUnion[];
    format?: ValueFormat;
    units?: string[];
}

export interface NameHolder {
    name?: string;
}

export interface NameValueHolder<T> extends ValueHolder<T>, NameHolder {
}

export interface RegexValueFilter extends ValueFilter {
    type: "regex";
    pattern?: Pattern;
    matchGroup?: number;
    matchIndex?: number;
    dotAll?: boolean;
    multiline?: boolean;
}

/**
 * Returns the substring beginning at the specified index (inclusive) and ending at the optional endIndex (exclusive); if endIndex is not supplied then the remainder of the string is returned; negative values can be used to indicate a backwards count from the length of the string e.g. -1 means length-1
 */
export interface SubStringValueFilter extends ValueFilter {
    type: "substring";
    beginIndex?: number;
    endIndex?: number;
}

export interface ValueConstraint {
    type: "allowedValues" | "future" | "futureOrPresent" | "max" | "min" | "notBlank" | "notEmpty" | "notNull" | "past" | "pastOrPresent" | "pattern" | "size";
    message?: string;
}

export interface ValueConstraintAllowedValues extends ValueConstraint {
    type: "allowedValues";
    allowedValueNames?: string[];
    allowedValues?: any[];
}

export interface ValueConstraintFuture extends ValueConstraint {
    type: "future";
}

export interface ValueConstraintFutureOrPresent extends ValueConstraint {
    type: "futureOrPresent";
}

export interface ValueConstraintMax extends ValueConstraint {
    type: "max";
    max?: number;
}

export interface ValueConstraintMin extends ValueConstraint {
    type: "min";
    min?: number;
}

export interface ValueConstraintNotBlank extends ValueConstraint {
    type: "notBlank";
}

export interface ValueConstraintNotEmpty extends ValueConstraint {
    type: "notEmpty";
}

export interface ValueConstraintNotNull extends ValueConstraint {
    type: "notNull";
}

export interface ValueConstraintPast extends ValueConstraint {
    type: "past";
}

export interface ValueConstraintPastOrPresent extends ValueConstraint {
    type: "pastOrPresent";
}

export interface ValueConstraintPattern extends ValueConstraint {
    type: "pattern";
    regexp?: string;
}

export interface ValueConstraintSize extends ValueConstraint {
    type: "size";
    min?: number;
    max?: number;
}

export interface ValueDescriptor extends NameHolder {
    type?: string;
    arrayDimensions?: number;
    constraints?: ValueConstraintUnion[];
    format?: ValueFormat;
    units?: string[];
    jsonType?: string;
}

export interface ValueDescriptorHolder {
    type?: string;
    units?: string[];
    format?: ValueFormat;
    constraints?: ValueConstraintUnion[];
}

export interface ValueFilter {
    type: "jsonPath" | "regex" | "substring";
}

export interface ValueFormat {
    useGrouping?: boolean;
    minimumIntegerDigits?: number;
    minimumFractionDigits?: number;
    maximumFractionDigits?: number;
    minimumSignificantDigits?: number;
    maximumSignificantDigits?: number;
    asBoolean?: boolean;
    asDate?: boolean;
    asSlider?: boolean;
    resolution?: number;
    dateStyle?: ValueFormatStyleRepresentation;
    timeStyle?: ValueFormatStyleRepresentation;
    dayPeriod?: ValueFormatStyleRepresentation;
    hour12?: boolean;
    iso8601?: boolean;
    weekday?: ValueFormatStyleRepresentation;
    era?: ValueFormatStyleRepresentation;
    year?: ValueFormatStyleRepresentation;
    month?: ValueFormatStyleRepresentation;
    week?: ValueFormatStyleRepresentation;
    day?: ValueFormatStyleRepresentation;
    hour?: ValueFormatStyleRepresentation;
    minute?: ValueFormatStyleRepresentation;
    second?: ValueFormatStyleRepresentation;
    fractionalSecondDigits?: number;
    timeZoneName?: ValueFormatStyleRepresentation;
    momentJsFormat?: string;
    asNumber?: boolean;
    asOnOff?: boolean;
    asPressedReleased?: boolean;
    asOpenClosed?: boolean;
    asMomentary?: boolean;
    multiline?: boolean;
}

export interface ValueHolder<T> {
    value?: T;
    type?: string;
}

export interface ColourRGB {
    r?: number;
    g?: number;
    b?: number;
}

export interface PeriodAndDuration {
}

export interface Recur {
    frequency?: string;
    until?: DateAsNumber;
    count?: number;
    interval?: number;
    secondList?: number[];
    minuteList?: number[];
    hourList?: number[];
    dayList?: WeekDay[];
    monthDayList?: number[];
    yearDayList?: number[];
    weekNoList?: number[];
    monthList?: number[];
    setPosList?: number[];
    weekStartDay?: WeekDayDay;
    calendarWeekStartDay?: number;
    experimentalValues?: { [index: string]: string };
    calIncField?: number;
}

export interface Throwable {
    detailMessage?: string;
    cause?: Throwable;
    stackTrace?: StackTraceElement[];
    suppressedExceptions?: Throwable[];
}

export interface StackTraceElement {
    classLoaderName?: string;
    moduleName?: string;
    moduleVersion?: string;
    declaringClass?: string;
    methodName?: string;
    fileName?: string;
    lineNumber?: number;
    format?: number;
}

export interface RuntimeException extends Exception {
}

export interface Coordinate extends Cloneable {
    x?: number;
    y?: number;
    z?: number;
}

export interface UriInfo {
}

export interface Size {
    length?: number;
    precision?: number;
    scale?: number;
    lobMultiplier?: SizeLobMultiplier;
}

export interface SqlTypeDescriptor {
}

export interface JavaTypeDescriptor<T> {
}

export interface Pattern {
}

export interface WeekDay {
    day?: WeekDayDay;
    offset?: number;
}

export interface Exception extends Throwable {
}

export interface Cloneable {
}

export interface AbstractSingleColumnStandardBasicType<T> extends AbstractStandardBasicType<T>, SingleColumnType<T> {
}

export interface VersionType<T> extends Type {
}

export interface LiteralType<T> {
}

export interface Type {
}

export interface AbstractStandardBasicType<T> extends BasicType, StringRepresentableType<T>, ProcedureParameterExtractionAware<T>, ProcedureParameterNamedBinder {
    dictatedSize?: Size;
    sqlTypeDescriptor?: SqlTypeDescriptor;
    javaTypeDescriptor?: JavaTypeDescriptor<T>;
    sqlTypes?: number[];
}

export interface SingleColumnType<T> extends Type {
}

export interface BasicType extends Type {
}

export interface ProcedureParameterNamedBinder {
}

export interface StringRepresentableType<T> {
}

export interface ProcedureParameterExtractionAware<T> {
}

export type DateAsNumber = number;

export type OAuthGrantUnion = OAuthPasswordGrant | OAuthClientCredentialsGrant | OAuthRefreshTokenGrant;

export type SharedEventUnion = SyslogEvent | AttributeEvent | AssetEvent | AssetsEvent | ReadAttributeEvent | ReadAssetEvent | ReadAssetsEvent | SimulatorState | RequestSimulatorState | RulesEngineStatusEvent | RulesetChangedEvent | GatewayDisconnectEvent | GatewayConnectionStatusEvent | DeleteAssetsRequestEvent | DeleteAssetsResponseEvent;

export type GeoJSONUnion = GeoJSONFeatureCollection | GeoJSONFeature | GeoJSONGeometry;

export type GeoJSONGeometryUnion = GeoJSONPoint;

export type AbstractNotificationMessageUnion = PushNotificationMessage | EmailNotificationMessage;

export type ProvisioningConfigUnion<T, U> = X509ProvisioningConfig;

export type ProvisioningMessageUnion = ErrorResponseMessage | SuccessResponseMessage | X509ProvisioningMessage;

export type ValuePredicateUnion = StringPredicate | BooleanPredicate | DateTimePredicate | NumberPredicate | RadialGeofencePredicate | RectangularGeofencePredicate | ArrayPredicate | ValueAnyPredicate | ValueEmptyPredicate | CalendarEventPredicate;

export type RulesetUnion = AssetRuleset | TenantRuleset | GlobalRuleset;

export type RuleActionUnion = RuleActionWait | RuleActionWriteAttribute | RuleActionNotification | RuleActionUpdateAttribute;

export type ValueConstraintUnion = ValueConstraintSize | ValueConstraintPattern | ValueConstraintMin | ValueConstraintMax | ValueConstraintAllowedValues | ValueConstraintPast | ValueConstraintPastOrPresent | ValueConstraintFuture | ValueConstraintFutureOrPresent | ValueConstraintNotEmpty | ValueConstraintNotBlank | ValueConstraintNotNull;

export type ValueFilterUnion = RegexValueFilter | SubStringValueFilter | JsonPathFilter;

export const enum ConsoleAppConfigMenuPosition {
    BOTTOM_LEFT = "BOTTOM_LEFT",
    BOTTOM_RIGHT = "BOTTOM_RIGHT",
    TOP_LEFT = "TOP_LEFT",
    TOP_RIGHT = "TOP_RIGHT",
}

export const enum AssetEventCause {
    CREATE = "CREATE",
    READ = "READ",
    UPDATE = "UPDATE",
    DELETE = "DELETE",
}

export const enum ElectricityConsumerDemandResponseType {
    NONE = "NONE",
    FORECAST = "FORECAST",
    SETPOINT = "SETPOINT",
}

export const enum ConnectionStatus {
    DISCONNECTED = "DISCONNECTED",
    CONNECTING = "CONNECTING",
    DISCONNECTING = "DISCONNECTING",
    CONNECTED = "CONNECTED",
    DISABLED = "DISABLED",
    WAITING = "WAITING",
    ERROR = "ERROR",
    STOPPED = "STOPPED",
}

export const enum ElectricVehicleAssetEnergyType {
    EV = "EV",
    PHEV = "PHEV",
}

export const enum ElectricityChargerAssetConnectorType {
    YAZAKI = "YAZAKI",
    MENNEKES = "MENNEKES",
    LE_GRAND = "LE_GRAND",
    CHADEMO = "CHADEMO",
    COMBO = "COMBO",
    SCHUKO = "SCHUKO",
    ENERGYLOCK = "ENERGYLOCK",
}

export const enum ElectricityProducerSolarAssetPanelOrientation {
    SOUTH = "SOUTH",
    EAST_WEST = "EAST_WEST",
}

export const enum AttributeEventSource {
    CLIENT = "CLIENT",
    INTERNAL = "INTERNAL",
    ATTRIBUTE_LINKING_SERVICE = "ATTRIBUTE_LINKING_SERVICE",
    SENSOR = "SENSOR",
    GATEWAY = "GATEWAY",
}

export const enum AttributeExecuteStatus {
    REQUEST_START = "REQUEST_START",
    REQUEST_REPEATING = "REQUEST_REPEATING",
    REQUEST_CANCEL = "REQUEST_CANCEL",
    READY = "READY",
    COMPLETED = "COMPLETED",
    RUNNING = "RUNNING",
    CANCELLED = "CANCELLED",
}

export const enum AttributeLinkConverterType {
    TOGGLE = "TOGGLE",
    INCREMENT = "INCREMENT",
    DECREMENT = "DECREMENT",
    NEGATE = "NEGATE",
}

export const enum AttributeWriteFailure {
    MISSING_SOURCE = "MISSING_SOURCE",
    ILLEGAL_SOURCE = "ILLEGAL_SOURCE",
    ASSET_NOT_FOUND = "ASSET_NOT_FOUND",
    ATTRIBUTE_NOT_FOUND = "ATTRIBUTE_NOT_FOUND",
    INVALID_AGENT_LINK = "INVALID_AGENT_LINK",
    INVALID_ATTRIBUTE_LINK = "INVALID_ATTRIBUTE_LINK",
    LINKED_ATTRIBUTE_CONVERSION_FAILURE = "LINKED_ATTRIBUTE_CONVERSION_FAILURE",
    ILLEGAL_AGENT_UPDATE = "ILLEGAL_AGENT_UPDATE",
    INVALID_ATTRIBUTE_EXECUTE_STATUS = "INVALID_ATTRIBUTE_EXECUTE_STATUS",
    NO_AUTH_CONTEXT = "NO_AUTH_CONTEXT",
    INSUFFICIENT_ACCESS = "INSUFFICIENT_ACCESS",
    EVENT_IN_FUTURE = "EVENT_IN_FUTURE",
    EVENT_OUTDATED = "EVENT_OUTDATED",
    ATTRIBUTE_VALIDATION_FAILURE = "ATTRIBUTE_VALIDATION_FAILURE",
    PROCESSOR_FAILURE = "PROCESSOR_FAILURE",
    STATE_STORAGE_FAILED = "STATE_STORAGE_FAILED",
    INVALID_VALUE_FOR_WELL_KNOWN_ATTRIBUTE = "INVALID_VALUE_FOR_WELL_KNOWN_ATTRIBUTE",
    GATEWAY_DISCONNECTED = "GATEWAY_DISCONNECTED",
    UNKNOWN = "UNKNOWN",
}

export const enum DatapointInterval {
    MINUTE = "MINUTE",
    HOUR = "HOUR",
    DAY = "DAY",
    WEEK = "WEEK",
    MONTH = "MONTH",
    YEAR = "YEAR",
}

export const enum GatewayDisconnectEventReason {
    TERMINATING = "TERMINATING",
    DISABLED = "DISABLED",
    ALREADY_CONNECTED = "ALREADY_CONNECTED",
    UNRECOGNISED = "UNRECOGNISED",
    PERMANENT_ERROR = "PERMANENT_ERROR",
}

export const enum NotificationSource {
    INTERNAL = "INTERNAL",
    CLIENT = "CLIENT",
    GLOBAL_RULESET = "GLOBAL_RULESET",
    TENANT_RULESET = "TENANT_RULESET",
    ASSET_RULESET = "ASSET_RULESET",
}

export const enum NotificationTargetType {
    TENANT = "TENANT",
    USER = "USER",
    ASSET = "ASSET",
    CUSTOM = "CUSTOM",
}

export const enum PushNotificationMessageMessagePriority {
    NORMAL = "NORMAL",
    HIGH = "HIGH",
}

export const enum PushNotificationMessageTargetType {
    DEVICE = "DEVICE",
    TOPIC = "TOPIC",
    CONDITION = "CONDITION",
}

export const enum RepeatFrequency {
    ALWAYS = "ALWAYS",
    ONCE = "ONCE",
    HOURLY = "HOURLY",
    DAILY = "DAILY",
    WEEKLY = "WEEKLY",
    MONTHLY = "MONTHLY",
    ANNUALLY = "ANNUALLY",
}

export const enum ErrorResponseMessageError {
    MESSAGE_INVALID = "MESSAGE_INVALID",
    CERTIFICATE_INVALID = "CERTIFICATE_INVALID",
    UNAUTHORIZED = "UNAUTHORIZED",
    FORBIDDEN = "FORBIDDEN",
    UNIQUE_ID_MISMATCH = "UNIQUE_ID_MISMATCH",
    CONFIG_DISABLED = "CONFIG_DISABLED",
    USER_DISABLED = "USER_DISABLED",
    SERVER_ERROR = "SERVER_ERROR",
    ASSET_ERROR = "ASSET_ERROR",
}

export const enum AssetQueryAccess {
    PRIVATE = "PRIVATE",
    PROTECTED = "PROTECTED",
    PUBLIC = "PUBLIC",
}

export const enum AssetQueryMatch {
    EXACT = "EXACT",
    BEGIN = "BEGIN",
    END = "END",
    CONTAINS = "CONTAINS",
}

export const enum AssetQueryOperator {
    EQUALS = "EQUALS",
    GREATER_THAN = "GREATER_THAN",
    GREATER_EQUALS = "GREATER_EQUALS",
    LESS_THAN = "LESS_THAN",
    LESS_EQUALS = "LESS_EQUALS",
    BETWEEN = "BETWEEN",
}

export const enum AssetQueryOrderBy$Property {
    CREATED_ON = "CREATED_ON",
    NAME = "NAME",
    ASSET_TYPE = "ASSET_TYPE",
    PARENT_ID = "PARENT_ID",
    REALM = "REALM",
}

export const enum LogicGroupOperator {
    AND = "AND",
    OR = "OR",
}

export const enum UserQueryOrderBy$Property {
    CREATED_ON = "CREATED_ON",
    FIRST_NAME = "FIRST_NAME",
    LAST_NAME = "LAST_NAME",
    USERNAME = "USERNAME",
    EMAIL = "EMAIL",
}

export const enum RulesEngineStatus {
    STOPPED = "STOPPED",
    RUNNING = "RUNNING",
    ERROR = "ERROR",
}

export const enum RulesetLang {
    JAVASCRIPT = "JAVASCRIPT",
    GROOVY = "GROOVY",
    JSON = "JSON",
    FLOW = "FLOW",
}

export const enum RulesetStatus {
    READY = "READY",
    DEPLOYED = "DEPLOYED",
    COMPILATION_ERROR = "COMPILATION_ERROR",
    EXECUTION_ERROR = "EXECUTION_ERROR",
    LOOP_ERROR = "LOOP_ERROR",
    DISABLED = "DISABLED",
    PAUSED = "PAUSED",
    EXPIRED = "EXPIRED",
    REMOVED = "REMOVED",
    EMPTY = "EMPTY",
}

/**
 * Values:
 * - `NUMBER`
 * - `STRING`
 * - `BOOLEAN`
 * - `TRIGGER` - @deprecated
 * - `COLOR`
 * - `ANY`
 */
export const enum NodeDataType {
    NUMBER = "NUMBER",
    STRING = "STRING",
    BOOLEAN = "BOOLEAN",
    /**
     * @deprecated
     */
    TRIGGER = "TRIGGER",
    COLOR = "COLOR",
    ANY = "ANY",
}

/**
 * Values:
 * - `INPUT`
 * - `PROCESSOR`
 * - `OUTPUT`
 * - `THEN` - @deprecated
 */
export const enum NodeType {
    INPUT = "INPUT",
    PROCESSOR = "PROCESSOR",
    OUTPUT = "OUTPUT",
    /**
     * @deprecated
     */
    THEN = "THEN",
}

export const enum PickerType {
    TEXT = "TEXT",
    MULTILINE = "MULTILINE",
    NUMBER = "NUMBER",
    DROPDOWN = "DROPDOWN",
    DOUBLE_DROPDOWN = "DOUBLE_DROPDOWN",
    CHECKBOX = "CHECKBOX",
    ASSET_ATTRIBUTE = "ASSET_ATTRIBUTE",
    COLOR = "COLOR",
}

export const enum RuleActionUpdateAttributeUpdateAction {
    ADD = "ADD",
    ADD_OR_REPLACE = "ADD_OR_REPLACE",
    REPLACE = "REPLACE",
    DELETE = "DELETE",
    CLEAR = "CLEAR",
}

export const enum RuleRecurrenceScope {
    PER_ASSET = "PER_ASSET",
    GLOBAL = "GLOBAL",
}

export const enum ClientRole {
    READ_ADMIN = "read:admin",
    READ_LOGS = "read:logs",
    READ_USERS = "read:users",
    READ_MAP = "read:map",
    READ_ASSETS = "read:assets",
    READ_RULES = "read:rules",
    WRITE_USER = "write:user",
    WRITE_ADMIN = "write:admin",
    WRITE_LOGS = "write:logs",
    WRITE_ASSETS = "write:assets",
    WRITE_ATTRIBUTES = "write:attributes",
    WRITE_RULES = "write:rules",
    READ = "read",
    WRITE = "write",
}

export const enum SyslogCategory {
    ASSET = "ASSET",
    AGENT = "AGENT",
    NOTIFICATION = "NOTIFICATION",
    RULES = "RULES",
    PROTOCOL = "PROTOCOL",
    GATEWAY = "GATEWAY",
    MODEL_AND_VALUES = "MODEL_AND_VALUES",
    API = "API",
    DATA = "DATA",
}

export const enum SyslogLevel {
    INFO = "INFO",
    WARN = "WARN",
    ERROR = "ERROR",
}

export const enum ValueFormatStyleRepresentation {
    NUMERIC = "numeric",
    DIGIT_2 = "2-digit",
    FULL = "full",
    LONG = "long",
    MEDIUM = "medium",
    SHORT = "short",
    NARROW = "narrow",
}

export const enum WeekDayDay {
    SU = "SU",
    MO = "MO",
    TU = "TU",
    WE = "WE",
    TH = "TH",
    FR = "FR",
    SA = "SA",
}

export const enum SizeLobMultiplier {
    NONE = "NONE",
    K = "K",
    M = "M",
    G = "G",
}


// Added by 'AssetModelInfoExtension' extension
export const enum WellknownAssets {
    UDPAGENT = "UDPAgent",
    STORAGESIMULATORAGENT = "StorageSimulatorAgent",
    PEOPLECOUNTERASSET = "PeopleCounterAsset",
    ROOMASSET = "RoomAsset",
    CITYASSET = "CityAsset",
    ELECTRICITYCONSUMERASSET = "ElectricityConsumerAsset",
    ELECTRICVEHICLEASSET = "ElectricVehicleAsset",
    VENTILATIONASSET = "VentilationAsset",
    CONSOLEASSET = "ConsoleAsset",
    TRADFRILIGHTASSET = "TradfriLightAsset",
    PRESENCESENSORASSET = "PresenceSensorAsset",
    KNXAGENT = "KNXAgent",
    LIGHTASSET = "LightAsset",
    PLUGASSET = "PlugAsset",
    ELECTRICITYCHARGERASSET = "ElectricityChargerAsset",
    WEATHERASSET = "WeatherAsset",
    THINGASSET = "ThingAsset",
    ELECTRICITYSUPPLIERASSET = "ElectricitySupplierAsset",
    ELECTRICITYBATTERYASSET = "ElectricityBatteryAsset",
    WEBSOCKETAGENT = "WebsocketAgent",
    TCPAGENT = "TCPAgent",
    GATEWAYASSET = "GatewayAsset",
    SERIALAGENT = "SerialAgent",
    PARKINGASSET = "ParkingAsset",
    BLUETOOTHMESHAGENT = "BluetoothMeshAgent",
    TRADFRIPLUGASSET = "TradfriPlugAsset",
    SIMULATORAGENT = "SimulatorAgent",
    SNMPAGENT = "SNMPAgent",
    GROUPASSET = "GroupAsset",
    BUILDINGASSET = "BuildingAsset",
    ELECTRICITYPRODUCERWINDASSET = "ElectricityProducerWindAsset",
    ENVIRONMENTSENSORASSET = "EnvironmentSensorAsset",
    MQTTAGENT = "MQTTAgent",
    ELECTRICVEHICLEFLEETGROUPASSET = "ElectricVehicleFleetGroupAsset",
    VELBUSTCPAGENT = "VelbusTCPAgent",
    ARTNETLIGHTASSET = "ArtnetLightAsset",
    GROUNDWATERSENSORASSET = "GroundwaterSensorAsset",
    VELBUSSERIALAGENT = "VelbusSerialAgent",
    ELECTRICITYPRODUCERSOLARASSET = "ElectricityProducerSolarAsset",
    MICROPHONEASSET = "MicrophoneAsset",
    ENERGYOPTIMISATIONASSET = "EnergyOptimisationAsset",
    ZWAVEAGENT = "ZWaveAgent",
    HTTPAGENT = "HTTPAgent",
    DOORASSET = "DoorAsset",
    THERMOSTATASSET = "ThermostatAsset",
    ELECTRICITYPRODUCERASSET = "ElectricityProducerAsset",
    SHIPASSET = "ShipAsset",
    UNKNOWNASSET = "UnknownAsset"
}

export const enum WellknownAttributes {
    MILEAGEMINIMUM = "mileageMinimum",
    CARBONIMPORT = "carbonImport",
    COLOURRGB = "colourRGB",
    SUPPORTSEXPORT = "supportsExport",
    PRICEHOURLY = "priceHourly",
    WINDSPEEDMAX = "windSpeedMax",
    ENERGYSELFCONSUMPTION = "energySelfConsumption",
    FLOW = "flow",
    LASTACCESS = "lastAccess",
    CHILDASSETTYPE = "childAssetType",
    CHARGERID = "chargerID",
    POSITION = "position",
    NETWORKKEY = "networkKey",
    CARBONSAVING = "carbonSaving",
    ROUTINGMODE = "routingMode",
    UNIVERSE = "universe",
    MSSINUMBER = "MSSINumber",
    SERIALBAUDRATE = "serialBaudrate",
    LEDCOUNT = "lEDCount",
    LOCKED = "locked",
    BASEURL = "baseURL",
    TARIFFIMPORT = "tariffImport",
    ENERGYRENEWABLESHARE = "energyRenewableShare",
    REGION = "region",
    REQUIREDVALUES = "requiredValues",
    FINANCIALSAVING = "financialSaving",
    FLEETCATEGORY = "fleetCategory",
    CONSOLEPLATFORM = "consolePlatform",
    POWEREXPORTMIN = "powerExportMin",
    GATEWAYSTATUS = "gatewayStatus",
    NO2LEVEL = "NO2Level",
    EFFICIENCYEXPORT = "efficiencyExport",
    CHARGECYCLES = "chargeCycles",
    SERIALPORT = "serialPort",
    UNLOCK = "unlock",
    TAGS = "tags",
    MILEAGEMIN = "mileageMin",
    PANELORIENTATION = "panelOrientation",
    LOCATION = "location",
    CLIENTSECRET = "clientSecret",
    WINDDIRECTION = "windDirection",
    WINDSPEED = "windSpeed",
    PARTICLESPM10 = "particlesPM10",
    APPLICATIONKEY = "applicationKey",
    POWEREXPORTMAX = "powerExportMax",
    LENGTH = "length",
    USERNAMEPASSWORD = "usernamePassword",
    SETWINDACTUALVALUEWITHFORECAST = "setWindActualValueWithForecast",
    POWERFORECAST = "powerForecast",
    SETACTUALSOLARVALUEWITHFORECAST = "setActualSolarValueWithForecast",
    WATERLEVEL = "waterLevel",
    CONSOLENAME = "consoleName",
    CONNECTHEADERS = "connectHeaders",
    WEBSOCKETQUERY = "websocketQuery",
    COOLING = "cooling",
    POLLINGMILLIS = "pollingMillis",
    SOILTEMPERATURE = "soilTemperature",
    SUNIRRADIANCE = "sunIrradiance",
    ODOMETER = "odometer",
    SUPPORTSIMPORT = "supportsImport",
    MESSAGEMAXLENGTH = "messageMaxLength",
    ENERGYLEVELPERCENTAGEMIN = "energyLevelPercentageMin",
    LIGHTID = "lightId",
    REQUESTQUERYPARAMETERS = "requestQueryParameters",
    PROXYADDRESS = "proxyAddress",
    ENERGYLOCAL = "energyLocal",
    POWER = "power",
    ENERGYAUTARKY = "energyAutarky",
    MODEL = "model",
    STREET = "street",
    MANUFACTURER = "manufacturer",
    ONOFF = "onOff",
    RESUMESESSION = "resumeSession",
    TARIFFEXPORT = "tariffExport",
    WINDSPEEDMIN = "windSpeedMin",
    CITY = "city",
    POWERSETPOINT = "powerSetpoint",
    CONNECTORTYPE = "connectorType",
    PRESENCE = "presence",
    ENERGYEXPORTTOTAL = "energyExportTotal",
    CARBONEXPORT = "carbonExport",
    SPACESOCCUPIED = "spacesOccupied",
    REQUESTTIMEOUTMILLIS = "requestTimeoutMillis",
    ENERGYLEVEL = "energyLevel",
    OPTIMISATIONDISABLED = "optimisationDisabled",
    AGENTSTATUS = "agentStatus",
    WEBSOCKETPATH = "websocketPath",
    WINDSPEEDREFERENCE = "windSpeedReference",
    PRICEDAILY = "priceDaily",
    COUNTGROWTHMINUTE = "countGrowthMinute",
    IMONUMBER = "IMONumber",
    INCLUDEFORECASTSOLARSERVICE = "includeForecastSolarService",
    REQUESTHEADERS = "requestHeaders",
    HOST = "host",
    COUNTOUTMINUTE = "countOutMinute",
    ENERGYTYPE = "energyType",
    MESSAGECHARSET = "messageCharset",
    PANELPITCH = "panelPitch",
    EMAIL = "email",
    BINDPORT = "bindPort",
    CONSOLEVERSION = "consoleVersion",
    PARTICLESPM1 = "particlesPM1",
    MTU = "mtu",
    SPEED = "speed",
    ENERGYLEVELPERCENTAGEMAX = "energyLevelPercentageMax",
    MESSAGESTRIPDELIMITER = "messageStripDelimiter",
    AREA = "area",
    COUNTRY = "country",
    SHIPTYPE = "shipType",
    CONNECTSUBSCRIPTIONS = "connectSubscriptions",
    RELATIVEHUMIDITY = "relativeHumidity",
    INCLUDEFORECASTWINDSERVICE = "includeForecastWindService",
    SOURCEADDRESS = "sourceAddress",
    PANELAZIMUTH = "panelAzimuth",
    RAINFALL = "rainfall",
    ENERGYIMPORTTOTAL = "energyImportTotal",
    OAUTHGRANT = "oAuthGrant",
    OZONELEVEL = "ozoneLevel",
    SEQUENCENUMBER = "sequenceNumber",
    COUNTINMINUTE = "countInMinute",
    PORT = "port",
    SUNALTITUDE = "sunAltitude",
    COUNTOUT = "countOut",
    CHARGERCONNECTED = "chargerConnected",
    ROOMNUMBER = "roomNumber",
    ENINUMBER = "ENINumber",
    PARTICLESPM2_5 = "particlesPM2_5",
    AGENTDISABLED = "agentDisabled",
    DISABLED = "disabled",
    MESSAGESOURCEADDRESS = "messageSourceAddress",
    COUNTIN = "countIn",
    MILEAGECHARGED = "mileageCharged",
    FORCECHARGE = "forceCharge",
    TEMPERATURESETPOINT = "temperatureSetpoint",
    TIMEINJECTIONINTERVAL = "timeInjectionInterval",
    FINANCIALWEIGHTING = "financialWeighting",
    CONSOLEPROVIDERS = "consoleProviders",
    ENERGYLEVELPERCENTAGE = "energyLevelPercentage",
    POWERIMPORTMAX = "powerImportMax",
    SPACESBUFFER = "spacesBuffer",
    COUNTTOTAL = "countTotal",
    SPACESTOTAL = "spacesTotal",
    BINDHOST = "bindHost",
    VEHICLECATEGORY = "vehicleCategory",
    FANSPEED = "fanSpeed",
    MESSAGECONVERTBINARY = "messageConvertBinary",
    FINANCIALCOST = "financialCost",
    NOTES = "notes",
    EFFICIENCYIMPORT = "efficiencyImport",
    MESSAGEDELIMITERS = "messageDelimiters",
    TEMPERATURE = "temperature",
    ENERGYCAPACITY = "energyCapacity",
    AVAILABLECHARGINGSPACES = "availableChargingSpaces",
    SUNZENITH = "sunZenith",
    POWERIMPORTMIN = "powerImportMin",
    SNMPVERSIONVALUE = "SNMPVersionValue",
    INTERVALSIZE = "intervalSize",
    BRIGHTNESS = "brightness",
    POSTALCODE = "postalCode",
    SPACESOPEN = "spacesOpen",
    HUMIDITY = "humidity",
    SOUNDLEVEL = "soundLevel",
    SECUREMODE = "secureMode",
    MILEAGECAPACITY = "mileageCapacity",
    DIRECTION = "direction",
    ENERGYLEVELSCHEDULE = "energyLevelSchedule",
    UVINDEX = "uVIndex",
    CONNECTURL = "connectURL",
    CLIENTID = "clientId",
    NATMODE = "NATMode",
    UPDATEONWRITE = "updateOnWrite",
    GROUPID = "groupId",
    VEHICLEID = "vehicleID",
    FOLLOWREDIRECTS = "followRedirects",
    CARBONCOST = "carbonCost",
    SUNAZIMUTH = "sunAzimuth",
    MESSAGECONVERTHEX = "messageConvertHex",
    WEBSOCKETMODE = "websocketMode",
    AVAILABLEDISCHARGINGSPACES = "availableDischargingSpaces",
    COLOURTEMPERATURE = "colourTemperature",
    VEHICLECONNECTED = "vehicleConnected"
}

export const enum WellknownMetaItems {
    FORMAT = "format",
    DATAPOINTSMAXAGEDAYS = "dataPointsMaxAgeDays",
    ACCESSRESTRICTEDWRITE = "accessRestrictedWrite",
    SECRET = "secret",
    RULEEVENT = "ruleEvent",
    ATTRIBUTELINKS = "attributeLinks",
    RULERESETIMMEDIATE = "ruleResetImmediate",
    RULESTATE = "ruleState",
    HASPREDICTEDDATAPOINTS = "hasPredictedDataPoints",
    STOREDATAPOINTS = "storeDataPoints",
    LABEL = "label",
    CONSTRAINTS = "constraints",
    UNITS = "units",
    SHOWONDASHBOARD = "showOnDashboard",
    READONLY = "readOnly",
    MULTILINE = "multiline",
    ACCESSPUBLICWRITE = "accessPublicWrite",
    MOMENTARY = "momentary",
    AGENTLINK = "agentLink",
    RULEEVENTEXPIRES = "ruleEventExpires",
    ACCESSPUBLICREAD = "accessPublicRead",
    ACCESSRESTRICTEDREAD = "accessRestrictedRead"
}

export const enum WellknownValueTypes {
    ASSETQUERY = "assetQuery",
    WSURL = "WS_URL",
    COLOURRGB = "colourRGB",
    VALUEFORMAT = "valueFormat",
    TEXT = "text",
    BYTE = "byte",
    USERNAMEANDPASSWORD = "usernameAndPassword",
    HTTPURL = "HTTP_URL",
    ATTRIBUTELINK = "attributeLink",
    ASSETID = "assetID",
    NUMBERMAP = "numberMap",
    HTTPMETHOD = "HTTPMethod",
    PANELORIENTATION = "panelOrientation",
    ATTRIBUTESTATE = "attributeState",
    POSITIVENUMBER = "positiveNumber",
    POSITIVEINTEGER = "positiveInteger",
    EXECUTIONSTATUS = "executionStatus",
    BIGINTEGER = "bigInteger",
    PERIODDURATIONISO8601 = "periodDurationISO8601",
    LONG = "long",
    DATEANDTIME = "dateAndTime",
    BOOLEANMAP = "booleanMap",
    CONNECTORTYPE = "connectorType",
    NEGATIVEINTEGER = "negativeInteger",
    TCPIPPORTNUMBER = "TCP_IPPortNumber",
    GEOJSONPOINT = "GEO_JSONPoint",
    SNMPVERSION = "SNMPVersion",
    JSONOBJECT = "JSONObject",
    ASSETTYPE = "assetType",
    INTEGERMAP = "integerMap",
    BIGNUMBER = "bigNumber",
    IPADDRESS = "IPAddress",
    ENERGYTYPE = "energyType",
    EMAIL = "email",
    TIMESTAMP = "timestamp",
    ATTRIBUTEREFERENCE = "attributeReference",
    KNXMESSAGESOURCEADDRESS = "kNXMessageSourceAddress",
    UUID = "UUID",
    INTEGER = "integer",
    OAUTHGRANT = "oAuthGrant",
    NUMBER = "number",
    TEXTMAP = "textMap",
    WEBSOCKETSUBSCRIPTION = "websocketSubscription",
    BOOLEAN = "boolean",
    TIMEANDPERIODDURATIONISO8601 = "timeAndPeriodDurationISO8601",
    TIMEDURATIONISO8601 = "timeDurationISO8601",
    TIMESTAMPISO8601 = "timestampISO8601",
    CONSOLEPROVIDERS = "consoleProviders",
    MULTIVALUEDTEXTMAP = "multivaluedTextMap",
    NEGATIVENUMBER = "negativeNumber",
    JSON = "JSON",
    CRONEXPRESSION = "CRONExpression",
    INTEGERBYTE = "integerByte",
    AGENTLINK = "agentLink",
    DIRECTION = "direction",
    HOSTORIPADDRESS = "hostOrIPAddress",
    CALENDAREVENT = "calendarEvent",
    CONNECTIONSTATUS = "connectionStatus",
    VALUECONSTRAINT = "valueConstraint"
}

export const enum WellknownUnitTypes {
    MONTH = "month",
    MILE_SCANDINAVIAN = "mile_scandinavian",
    OUNCE = "ounce",
    HECTARE = "hectare",
    YARD = "yard",
    YEAR = "year",
    JOULE = "joule",
    VOLT = "volt",
    HERTZ = "hertz",
    MILE = "mile",
    DEGREE = "degree",
    KILO = "kilo",
    BTU = "btu",
    FOOT = "foot",
    MEGA = "mega",
    KELVIN = "kelvin",
    CARBON = "carbon",
    DECIBEL_ATTENUATED = "decibel_attenuated",
    INCH = "inch",
    SQUARED = "squared",
    PERCENTAGE = "percentage",
    IN_HG = "inch_mercury",
    CENTI = "centi",
    GRAM = "gram",
    CUBED = "cubed",
    DAY = "day",
    RADIAN = "radian",
    OHM = "ohm",
    MINUTE = "minute",
    PART_PER_MILLION = "ppm",
    AMP = "amp",
    MASS_POUND = "pound",
    KNOT = "knot",
    METRE = "metre",
    LITRE = "litre",
    FLUID_OUNCE = "fluid_ounce",
    HOUR = "hour",
    ACRE = "acre",
    CELSIUS = "celsius",
    RPM = "rpm",
    PASCAL = "pascal",
    DECIBEL = "decibel",
    LUMEN = "lumen",
    WEEK = "week",
    FAHRENHEIT = "fahrenheit",
    WATT = "watt",
    GALLON = "gallon",
    LUX = "lux",
    MILLI = "milli",
    BAR = "bar",
    PEAK = "peak",
    SECOND = "second",
    MICRO = "micro",
    PER = "per",
    STONE = "stone"
}

export const enum WellknownRulesetMetaItems {
    SHOWONLIST = "showOnList",
    TRIGGERONPREDICTEDDATA = "triggerOnPredictedData",
    CONTINUEONERROR = "continueOnError",
    VALIDITY = "validity"
}
