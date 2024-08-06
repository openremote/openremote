export interface PersistenceEvent<T> {
    cause?: PersistenceEventCause;
    entity?: T;
    propertyNames?: any[];
    currentState?: any[];
    previousState?: any[];
}
export interface ConsoleAppConfig {
    id?: any;
    realm?: any;
    initialUrl?: any;
    url?: any;
    menuEnabled?: any;
    menuPosition?: ConsoleAppConfigMenuPosition;
    primaryColor?: any;
    secondaryColor?: any;
    links?: ConsoleAppConfigAppLink[];
}
export interface ConsoleAppConfigAppLink {
    displayText?: any;
    pageLink?: any;
}
export interface Asset extends AssetInfo {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface AssetDescriptor extends NameHolder {
    descriptorType: "asset" | "agent";
    icon?: any;
    colour?: any;
    dynamic?: any;
}
export interface AssetEvent extends SharedEvent, AssetInfo {
    eventType: "asset";
    cause?: AssetEventCause;
    asset?: Asset;
    updatedProperties?: any[];
}
export interface AssetFilter {
    filterType: "asset";
    assetIds?: any[];
    assetTypes?: any[];
    assetClasses?: any[];
    realm?: any;
    parentIds?: any[];
    path?: any[];
    attributeNames?: any[];
    publicEvents?: boolean;
    restrictedEvents?: boolean;
    internal?: boolean;
}
export interface AssetInfo {
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
    assetIds?: any[];
}
export interface DeleteAssetsResponseEvent extends SharedEvent {
    eventType: "delete-assets-response";
    deleted?: boolean;
    assetIds?: any[];
}
export interface HasAssetQuery {
}
export interface ReadAssetEvent extends SharedEvent, HasAssetQuery {
    eventType: "read-asset";
    assetId?: any;
    assetQuery?: AssetQuery;
}
export interface ReadAssetsEvent extends SharedEvent, HasAssetQuery {
    eventType: "read-assets";
    assetQuery?: AssetQuery;
}
export interface ReadAttributeEvent extends SharedEvent, HasAssetQuery {
    eventType: "read-asset-attribute";
    ref?: AttributeRef;
    assetQuery?: AssetQuery;
}
export interface UserAssetLink {
    id?: UserAssetLinkId;
    createdOn?: DateAsNumber;
    assetName?: any;
    parentAssetName?: any;
    userFullName?: any;
}
export interface UserAssetLinkId {
    realm?: any;
    userId?: any;
    assetId?: any;
}
export interface Agent extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface AgentDescriptor extends AssetDescriptor {
    descriptorType: "agent";
    name?: any;
    icon?: any;
    colour?: any;
    instanceDiscoveryProvider?: any;
    agentLinkType?: any;
    assetDiscovery?: boolean;
    assetImport?: boolean;
    dynamic?: any;
}
export interface AgentLink {
    type: string;
    id?: any;
    /**
     * Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order
     */
    valueFilters?: ValueFilterUnion[];
    /**
     * Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute
     */
    valueConverter?: {
        [index: string]: any;
    };
    /**
     * Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion
     */
    writeValueConverter?: {
        [index: string]: any;
    };
    /**
     * String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol (particularly useful for executable attributes)
     */
    writeValue?: any;
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
    updateOnWrite?: any;
}
export interface Protocol {
}
export interface BuildingAsset extends CityAsset {
}
export interface CityAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface ConsoleAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface DoorAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface ElectricVehicleAsset extends ElectricityBatteryAsset {
}
export interface ElectricVehicleFleetGroupAsset extends GroupAsset {
}
export interface ElectricityAsset<T> extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
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
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface EnvironmentSensorAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface GatewayAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface GroundwaterSensorAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface GroupAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface LightAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface MicrophoneAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface ParkingAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface PeopleCounterAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface PlugAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface PresenceSensorAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface RoomAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface ShipAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface ThermostatAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface ThingAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface UnknownAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface VentilationAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface WeatherAsset extends Asset {
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    name?: any;
    accessPublicRead?: boolean;
    parentId?: any;
    realm?: any;
    type?: any;
    path?: any[];
    attributes?: {
        [index: string]: Attribute<any>;
    };
}
export interface Attribute<T> extends AbstractNameValueHolder<T>, MetaHolder {
    timestamp?: number;
}
export interface AttributeEvent extends SharedEvent, AttributeInfo {
    eventType: "attribute";
    deleted?: boolean;
    realm?: any;
    parentId?: any;
    path?: any[];
    assetName?: any;
    assetType?: any;
    createdOn?: DateAsNumber;
}
export interface AttributeInfo extends AssetInfo, NameValueHolder<any>, MetaHolder {
    value?: any;
    ref?: AttributeRef;
    timestamp?: number;
    oldValueTimestamp?: number;
    oldValue?: any;
}
export interface AttributeLink {
    ref?: AttributeRef;
    converter?: {
        [index: string]: any;
    };
    filters?: ValueFilterUnion[];
}
export interface AttributeRef {
    id?: any;
    name?: any;
}
export interface AttributeState {
    ref?: AttributeRef;
    value?: any;
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
    tokenEndpointUri?: any;
    basicAuthHeader?: boolean;
    additionalValueMap?: {
        [index: string]: any[];
    };
    client_id?: any;
    client_secret?: any;
    scope?: any;
}
export interface OAuthPasswordGrant extends OAuthClientCredentialsGrant {
    grant_type: "password";
    username?: any;
    password?: any;
}
export interface OAuthRefreshTokenGrant extends OAuthClientCredentialsGrant {
    grant_type: "refresh_token";
    refresh_token?: any;
}
export interface UsernamePassword {
    username?: any;
    password?: any;
}
export interface CalendarEvent {
    start?: DateAsNumber;
    end?: DateAsNumber;
    recurrence?: any;
}
export interface ConsoleProvider {
    version?: any;
    requiresPermission?: boolean;
    hasPermission?: boolean;
    success?: boolean;
    enabled?: boolean;
    disabled?: boolean;
    data?: {
        [index: string]: any;
    };
}
export interface ConsoleRegistration {
    id?: any;
    name?: any;
    version?: any;
    platform?: any;
    providers?: {
        [index: string]: ConsoleProvider;
    };
    model?: any;
    apps?: any[];
}
export interface Dashboard {
    id?: any;
    createdOn?: DateAsNumber;
    realm?: any;
    version?: number;
    ownerId?: any;
    viewAccess?: DashboardAccess;
    editAccess?: DashboardAccess;
    displayName?: any;
    template?: DashboardTemplate;
}
export interface DashboardGridItem {
    id?: any;
    x?: number;
    y?: number;
    w?: number;
    h?: number;
    minH?: number;
    minW?: number;
    minPixelH?: number;
    minPixelW?: number;
    noResize?: boolean;
    noMove?: boolean;
    locked?: boolean;
}
export interface DashboardScreenPreset {
    id?: any;
    displayName?: any;
    breakpoint?: number;
    scalingPreset?: DashboardScalingPreset;
    redirectDashboardId?: any;
}
export interface DashboardTemplate {
    id?: any;
    columns?: number;
    maxScreenWidth?: number;
    refreshInterval?: DashboardRefreshInterval;
    screenPresets?: DashboardScreenPreset[];
    widgets?: DashboardWidget[];
}
export interface DashboardWidget {
    id?: any;
    displayName?: any;
    gridItem?: DashboardGridItem;
    widgetTypeId?: any;
    widgetConfig?: any;
}
export interface AssetDatapoint extends Datapoint {
}
export interface AssetPredictedDatapoint extends Datapoint {
}
export interface Datapoint {
    assetId?: any;
    attributeName?: any;
    timestamp?: DateAsNumber;
    value?: any;
}
export interface DatapointID {
    assetId?: any;
    attributeName?: any;
    timestamp?: DateAsNumber;
}
export interface DatapointPeriod {
    assetId?: any;
    attributeName?: any;
    oldestTimestamp?: any;
    latestTimestamp?: any;
}
export interface ValueDatapoint<T> {
    x?: number;
    y?: T;
}
export interface AssetDatapointAllQuery extends AssetDatapointQuery {
    type: "all";
}
export interface AssetDatapointIntervalQuery extends AssetDatapointQuery {
    type: "interval";
    interval?: any;
    gapFill?: boolean;
    formula?: AssetDatapointIntervalQueryFormula;
}
export interface AssetDatapointLTTBQuery extends AssetDatapointQuery {
    type: "lttb";
    amountOfPoints?: number;
}
export interface AssetDatapointQuery {
    type: "all" | "interval" | "lttb";
    fromTimestamp?: number;
    toTimestamp?: number;
    fromTime?: DateAsNumber;
    toTime?: DateAsNumber;
}
export interface Event {
    timestamp?: number;
}
export interface TriggeredEventSubscription<T> {
    events?: T[];
    subscriptionId?: any;
}
export interface EventBus {
    registrations?: EventRegistration<any>[];
}
export interface EventListener<E> {
}
export interface EventRegistration<E> {
    prepare?: boolean;
    eventClass?: any;
    listener?: EventListener<E>;
}
export interface VetoEventException {
    detailMessage?: any;
    cause?: any;
    stackTrace?: any[];
    suppressedExceptions?: any[];
}
export interface CancelEventSubscription {
    eventType?: any;
    subscriptionId?: any;
}
export interface EventRequestResponseWrapper<T> {
    messageId?: any;
    event?: T;
}
export interface EventSubscription<E> {
    eventType?: any;
    filter?: any;
    subscriptionId?: any;
}
export interface RealmScopedEvent extends SharedEvent {
    eventType: "gateway-connection-status" | "ProtocolDiscoveryImportRequestEvent" | "ProtocolDiscoveryStartRequestEvent";
    realm?: any;
}
export interface SharedEvent extends Event {
    eventType: "asset" | "assets" | "delete-assets-request" | "delete-assets-response" | "read-asset" | "read-assets" | "read-asset-attribute" | "attribute" | "gateway-connection-status" | "ProtocolDiscoveryImportRequestEvent" | "ProtocolDiscoveryStartRequestEvent" | "gateway-capabilities-request" | "gateway-capabilities-response" | "gateway-disconnect" | "gateway-tunnel-start-request" | "gateway-tunnel-start-response" | "gateway-tunnel-stop-request" | "gateway-tunnel-stop-response" | "ProtocolDiscoveryAssetFoundEvent" | "ProtocolDiscoveryInstanceFoundEvent" | "ProtocolDiscoveryStartStopResponseEvent" | "ProtocolDiscoveryStopRequestEvent" | "rules-engine-status" | "ruleset-changed" | "request-simulator-state" | "simulator-state" | "syslog";
}
export interface UnauthorizedEventSubscription<E> {
    subscription?: EventSubscription<E>;
}
export interface FileInfo {
    name?: any;
    contents?: any;
    binary?: boolean;
}
export interface GatewayCapabilitiesRequestEvent extends SharedEvent {
    eventType: "gateway-capabilities-request";
}
export interface GatewayCapabilitiesResponseEvent extends SharedEvent {
    eventType: "gateway-capabilities-response";
    tunnelingSupported?: boolean;
}
export interface GatewayConnection {
    localRealm?: any;
    host?: any;
    port?: any;
    realm?: any;
    clientId?: any;
    clientSecret?: any;
    secured?: any;
    disabled?: boolean;
}
export interface GatewayConnectionStatusEvent extends RealmScopedEvent {
    eventType: "gateway-connection-status";
    connectionStatus?: ConnectionStatus;
}
export interface GatewayDisconnectEvent extends SharedEvent {
    eventType: "gateway-disconnect";
    reason?: GatewayDisconnectEventReason;
}
export interface GatewayTunnelInfo {
    realm?: any;
    gatewayId?: any;
    targetPort?: number;
    target?: any;
    assignedPort?: any;
    type?: GatewayTunnelInfoType;
    id?: any;
}
export interface GatewayTunnelStartRequestEvent extends SharedEvent {
    eventType: "gateway-tunnel-start-request";
    sshHostname?: any;
    sshPort?: number;
    info?: GatewayTunnelInfo;
}
export interface GatewayTunnelStartResponseEvent extends SharedEvent {
    eventType: "gateway-tunnel-start-response";
    error?: any;
}
export interface GatewayTunnelStopRequestEvent extends SharedEvent {
    eventType: "gateway-tunnel-stop-request";
    info?: GatewayTunnelInfo;
}
export interface GatewayTunnelStopResponseEvent extends SharedEvent {
    eventType: "gateway-tunnel-stop-response";
    error?: any;
}
export interface GeoJSON {
    type: "Feature" | "FeatureCollection" | "Point";
}
export interface GeoJSONFeature extends GeoJSON {
    type: "Feature";
    geometry?: GeoJSONGeometryUnion;
    properties?: {
        [index: string]: any;
    };
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
    headers?: any;
    authorization?: any;
    forwardedProtoHeader?: any;
    forwardedHostHeader?: any;
    uriInfo?: any;
}
export interface MailMessage {
    content?: any;
    contentType?: any;
    headers?: {
        [index: string]: any[];
    };
    subject?: any;
    sentDate?: DateAsNumber;
    from?: any[];
}
export interface ManagerAppConfig {
    loadLocales?: boolean;
    languages?: {
        [index: string]: any;
    };
    realms?: {
        [index: string]: ManagerAppRealmConfig;
    };
    pages?: {
        [index: string]: any;
    };
    manager?: ManagerConfig;
}
export interface ManagerAppRealmConfig {
    appTitle?: any;
    styles?: any;
    logo?: any;
    logoMobile?: any;
    favicon?: any;
    language?: any;
    headers?: any[];
}
export interface ManagerConfig {
    managerUrl?: any;
    keycloakUrl?: any;
    appVersion?: any;
    realm?: any;
    clientId?: any;
    autoLogin?: boolean;
    consoleAutoEnable?: boolean;
    loadIcons?: boolean;
    pollingIntervalMillis?: number;
    loadTranslations?: any[];
    loadDescriptors?: boolean;
    translationsLoadPath?: any;
    skipFallbackToBasicAuth?: boolean;
    applyConfigToAdmin?: boolean;
    auth?: Auth;
    credentials?: UsernamePassword;
    eventProviderType?: EventProviderType;
    mapType?: MapType;
    configureTranslationsOptions?: any;
    basicLoginProvider?: any;
    defaultLanguage?: any;
}
export interface MapConfig {
    options?: {
        [index: string]: MapRealmConfig;
    };
}
export interface MapRealmConfig {
    center?: number[];
    bounds?: number[];
    zoom?: number;
    minZoom?: number;
    maxZoom?: number;
    boxZoom?: boolean;
    geocodeUrl?: any;
    geoJson?: GeoJsonConfig;
}
export interface GeoJsonConfig {
    source?: any;
    layers?: any[];
}
export interface AbstractNotificationMessage {
    type: "email" | "push";
}
export interface EmailNotificationMessage extends AbstractNotificationMessage {
    type: "email";
    from?: EmailNotificationMessageRecipient;
    replyTo?: EmailNotificationMessageRecipient;
    subject?: any;
    text?: any;
    html?: any;
    to?: EmailNotificationMessageRecipient[];
    cc?: EmailNotificationMessageRecipient[];
    bcc?: EmailNotificationMessageRecipient[];
}
export interface EmailNotificationMessageRecipient {
    name?: any;
    address?: any;
}
export interface Notification {
    name?: any;
    message?: AbstractNotificationMessageUnion;
    targets?: NotificationTarget[];
    repeatFrequency?: RepeatFrequency;
    repeatInterval?: any;
}
export interface NotificationTarget {
    type?: NotificationTargetType;
    id?: any;
    data?: any;
}
export interface NotificationSendResult {
    success?: boolean;
    message?: any;
}
export interface PushNotificationAction {
    url?: any;
    data?: any;
    silent?: boolean;
    openInBrowser?: boolean;
    httpMethod?: any;
}
export interface PushNotificationButton {
    title?: any;
    action?: PushNotificationAction;
}
export interface PushNotificationMessage extends AbstractNotificationMessage {
    type: "push";
    title?: any;
    body?: any;
    action?: PushNotificationAction;
    buttons?: PushNotificationButton[];
    data?: {
        [index: string]: any;
    };
    priority?: PushNotificationMessageMessagePriority;
    targetType?: PushNotificationMessageTargetType;
    target?: any;
    expiration?: any;
}
export interface SentNotification {
    id?: any;
    name?: any;
    type?: any;
    target?: NotificationTargetType;
    targetId?: any;
    source?: NotificationSource;
    sourceId?: any;
    message?: AbstractNotificationMessageUnion;
    error?: any;
    sentOn?: DateAsNumber;
    deliveredOn?: DateAsNumber;
    acknowledgedOn?: DateAsNumber;
    acknowledgement?: any;
}
export interface InstantEpochConverter {
}
export interface LTreeType {
}
export interface ProtocolAssetService {
}
export interface ProtocolDiscoveryAssetFoundEvent extends SharedEvent {
    eventType: "ProtocolDiscoveryAssetFoundEvent";
    agentDescriptor?: any;
    assets?: AssetTreeNode[];
}
export interface ProtocolDiscoveryImportRequestEvent extends RealmScopedEvent {
    eventType: "ProtocolDiscoveryImportRequestEvent";
    agentDescriptor?: any;
    assetId?: any;
    assetDiscovery?: boolean;
}
export interface ProtocolDiscoveryInstanceFoundEvent extends SharedEvent {
    eventType: "ProtocolDiscoveryInstanceFoundEvent";
    agentDescriptor?: any;
    instanceName?: any;
    attributes?: Attribute<any>[];
}
export interface ProtocolDiscoveryStartRequestEvent extends RealmScopedEvent {
    eventType: "ProtocolDiscoveryStartRequestEvent";
    agentDescriptor?: any;
    assetId?: any;
    assetDiscovery?: boolean;
}
export interface ProtocolDiscoveryStartStopResponseEvent extends SharedEvent {
    eventType: "ProtocolDiscoveryStartStopResponseEvent";
    agentDescriptor?: any;
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
    id?: any;
    createdOn?: DateAsNumber;
    lastModified?: DateAsNumber;
    name?: any;
    realm?: any;
    assetTemplate?: any;
    restrictedUser?: boolean;
    userRoles?: ClientRole[];
    disabled?: boolean;
    data?: T;
}
export interface ProvisioningMessage {
    type: "error" | "success" | "x509";
}
export interface SuccessResponseMessage extends ProvisioningMessage {
    type: "success";
    realm?: any;
    asset?: Asset;
}
export interface X509ProvisioningConfig extends ProvisioningConfig<X509ProvisioningData, X509ProvisioningConfig> {
    type: "x509";
    data?: X509ProvisioningData;
}
export interface X509ProvisioningData {
    CACertPEM?: any;
    ignoreExpiryDate?: boolean;
}
export interface X509ProvisioningMessage extends ProvisioningMessage {
    type: "x509";
    cert?: any;
}
export interface AssetQuery {
    recursive?: boolean;
    select?: AssetQuerySelect;
    access?: AssetQueryAccess;
    ids?: any[];
    names?: StringPredicate[];
    parents?: ParentPredicate[];
    paths?: PathPredicate[];
    realm?: RealmPredicate;
    userIds?: any[];
    types?: any[];
    attributes?: LogicGroup<AttributePredicate>;
    orderBy?: AssetQueryOrderBy;
    limit?: number;
}
export interface AssetQueryOrderBy {
    property?: AssetQueryOrderBy$Property;
    descending?: boolean;
}
export interface AssetQuerySelect {
    attributes?: any[];
}
export interface DashboardQuery {
    select?: DashboardQuerySelect;
    conditions?: DashboardQueryConditions;
    ids?: any[];
    names?: StringPredicate[];
    userIds?: any[];
    realm?: RealmPredicate;
    start?: any;
    limit?: any;
}
export interface DashboardQueryAssetConditions {
    access?: DashboardQueryAssetAccess[];
    minAmount?: DashboardQueryConditionMinAmount;
    parents?: ParentPredicate[];
}
export interface DashboardQueryConditions {
    dashboard?: DashboardQueryDashboardConditions;
    asset?: DashboardQueryAssetConditions;
}
export interface DashboardQueryDashboardConditions {
    viewAccess?: DashboardAccess[];
    editAccess?: DashboardAccess[];
    minWidgets?: any;
}
export interface DashboardQuerySelect {
    metadata?: boolean;
    template?: boolean;
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
    realm?: any;
    assetIds?: any[];
}
export interface UserQuery {
    realmPredicate?: RealmPredicate;
    assets?: any[];
    pathPredicate?: PathPredicate;
    ids?: any[];
    select?: UserQuerySelect;
    usernames?: StringPredicate[];
    attributes?: UserQueryAttributeValuePredicate[];
    realmRoles?: StringPredicate[];
    serviceUsers?: any;
    limit?: any;
    offset?: any;
    orderBy?: UserQueryOrderBy;
}
export interface UserQueryAttributeValuePredicate {
    negated?: boolean;
    name?: StringPredicate;
    value?: StringPredicate;
}
export interface UserQueryOrderBy {
    property?: UserQueryOrderBy$Property;
    descending?: boolean;
}
export interface UserQuerySelect {
    basic?: boolean;
}
export interface ArrayPredicate extends ValuePredicate {
    predicateType: "array";
    value?: any;
    index?: any;
    lengthEquals?: any;
    lengthGreaterThan?: any;
    lengthLessThan?: any;
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
    value?: any;
    rangeValue?: any;
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
    value?: any;
    rangeValue?: any;
    operator?: AssetQueryOperator;
    negate?: boolean;
}
export interface ParentPredicate {
    id?: any;
}
export interface PathPredicate {
    path?: any[];
}
export interface RadialGeofencePredicate extends GeofencePredicate {
    predicateType: "radial";
    radius?: number;
    lat?: number;
    lng?: number;
}
export interface RealmPredicate {
    name?: any;
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
    value?: any;
    negate?: boolean;
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
    assetId?: any;
    accessPublicRead?: boolean;
    realm?: any;
}
export interface GlobalRuleset extends Ruleset {
    type: "global";
}
export interface RealmRuleset extends Ruleset {
    type: "realm";
    realm?: any;
    accessPublicRead?: boolean;
}
export interface RulesClock {
}
export interface RulesEngineInfo {
    status?: RulesEngineStatus;
    compilationErrorCount?: number;
    executionErrorCount?: number;
}
export interface RulesEngineStatusEvent extends SharedEvent {
    eventType: "rules-engine-status";
    engineId?: any;
    engineInfo?: RulesEngineInfo;
}
export interface Ruleset {
    type: "asset" | "global" | "realm";
    id?: any;
    version?: number;
    createdOn?: DateAsNumber;
    lastModified?: DateAsNumber;
    name?: any;
    enabled?: boolean;
    rules?: any;
    lang?: RulesetLang;
    meta?: {
        [index: string]: any;
    };
    status?: RulesetStatus;
    error?: any;
}
export interface RulesetChangedEvent extends SharedEvent {
    eventType: "ruleset-changed";
    engineId?: any;
    ruleset?: RulesetUnion;
}
export interface SunPositionTrigger {
    position?: SunPositionTriggerPosition;
    location?: GeoJSONPoint;
    offsetMins?: any;
}
export interface AttributeInternalValue {
    assetId?: any;
    attributeName?: any;
}
export interface Node {
    id?: any;
    type?: NodeType;
    name?: any;
    position?: NodePosition;
    size?: NodePosition;
    internals?: NodeInternal[];
    inputs?: NodeSocket[];
    outputs?: NodeSocket[];
    displayCharacter?: any;
}
export interface NodeCollection {
    name?: any;
    description?: any;
    nodes?: Node[];
    connections?: NodeConnection[];
}
export interface NodeConnection {
    from?: any;
    to?: any;
}
export interface NodeInternal {
    name?: any;
    picker?: Picker;
    value?: any;
}
export interface NodePosition {
    x?: number;
    y?: number;
}
export interface NodeSocket {
    id?: any;
    name?: any;
    type?: NodeDataType;
    nodeId?: any;
    index?: number;
}
export interface Option {
    name?: any;
    value?: any;
}
export interface Picker {
    type?: PickerType;
    options?: Option[];
}
export interface GeofenceDefinition {
    id?: any;
    lat?: number;
    lng?: number;
    radius?: number;
    httpMethod?: any;
    url?: any;
}
export interface JsonRule {
    name?: any;
    description?: any;
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
    meta?: {
        [id: string]: any;
    };
}
export interface RuleAction {
    action: "notification" | "update-attribute" | "wait" | "webhook" | "write-attribute";
    target?: RuleActionTarget;
}
export interface RuleActionNotification extends RuleAction {
    action: "notification";
    notification?: Notification;
}
export interface RuleActionTarget {
    conditionAssets?: any;
    matchedAssets?: AssetQuery;
    assets?: AssetQuery;
    users?: UserQuery;
    linkedUsers?: any;
    custom?: any;
}
export interface RuleActionUpdateAttribute extends RuleAction {
    action: "update-attribute";
    attributeName?: any;
    value?: any;
    key?: any;
    index?: any;
    updateAction?: RuleActionUpdateAttributeUpdateAction;
}
export interface RuleActionWait extends RuleAction {
    action: "wait";
    millis?: number;
}
export interface RuleActionWebhook extends RuleAction {
    action: "webhook";
    target?: any;
    webhook?: Webhook;
    mediaType?: any;
}
export interface RuleActionWriteAttribute extends RuleAction {
    action: "write-attribute";
    attributeName?: any;
    value?: any;
}
export interface RuleCondition {
    duration?: any;
    cron?: any;
    sun?: SunPositionTrigger;
    assets?: AssetQuery;
    tag?: any;
}
export interface RuleRecurrence {
    scope?: RuleRecurrenceScope;
    mins?: any;
}
export interface RuleTemplate<T> {
    name?: any;
    value?: T;
}
export interface Credential {
    type?: any;
    value?: any;
    temporary?: any;
}
export interface Realm {
    id?: any;
    name?: any;
    displayName?: any;
    enabled?: any;
    notBefore?: any;
    resetPasswordAllowed?: any;
    duplicateEmailsAllowed?: any;
    rememberMe?: any;
    registrationAllowed?: any;
    registrationEmailAsUsername?: any;
    verifyEmail?: any;
    loginWithEmail?: any;
    loginTheme?: any;
    accountTheme?: any;
    adminTheme?: any;
    emailTheme?: any;
    accessTokenLifespan?: any;
    realmRoles?: RealmRole[];
}
export interface RealmRole {
    name?: any;
    description?: any;
}
export interface Role {
    id?: any;
    name?: any;
    description?: any;
    composite?: boolean;
    assigned?: any;
    compositeRoleIds?: any[];
}
export interface User {
    realm?: any;
    realmId?: any;
    id?: any;
    firstName?: any;
    lastName?: any;
    email?: any;
    enabled?: any;
    createdOn?: DateAsNumber;
    secret?: any;
    attributes?: {
        [index: string]: any[];
    };
    serviceAccount?: boolean;
    username?: any;
}
export interface UserAttribute {
    name?: any;
    value?: any;
    id?: any;
}
export interface UserPasswordCredentials {
    username?: any;
    password?: any;
}
export interface UserSession {
    ID?: any;
    username?: any;
    startTimeMillis?: number;
    remoteAddress?: any;
}
export interface Setup {
}
export interface SetupTasks {
}
export interface RequestSimulatorState extends SharedEvent {
    eventType: "request-simulator-state";
    agentId?: any;
}
export interface SimulatorAttributeInfo {
    assetName?: any;
    assetId?: any;
    replay?: boolean;
    attribute?: Attribute<any>;
}
export interface SimulatorReplayDatapoint {
    timestamp?: number;
    value?: any;
}
export interface SimulatorState extends SharedEvent {
    eventType: "simulator-state";
    agentId?: any;
    attributes?: SimulatorAttributeInfo[];
}
export interface SyslogConfig {
    storedLevel?: SyslogLevel;
    storedCategories?: SyslogCategory[];
    storedMaxAgeMinutes?: number;
}
export interface SyslogEvent extends SharedEvent {
    eventType: "syslog";
    id?: any;
    level?: SyslogLevel;
    category?: SyslogCategory;
    subCategory?: any;
    message?: any;
}
export interface SyslogEventLevelCategoryFilter {
    filterType: "level-category-filter";
    level?: SyslogLevel;
    categories?: SyslogCategory[];
}
export interface HealthStatusProvider {
}
export interface AbstractNameValueDescriptorHolder extends ValueDescriptorHolder, NameHolder {
    type?: any;
    constraints?: ValueConstraintUnion[];
    format?: ValueFormat;
    units?: any[];
}
export interface AbstractNameValueHolder<T> extends NameValueHolder<T> {
}
export interface AttributeDescriptor extends AbstractNameValueDescriptorHolder, MetaHolder {
    name?: any;
    type?: any;
    constraints?: ValueConstraintUnion[];
    format?: ValueFormat;
    units?: any[];
    optional?: any;
}
export interface ForecastConfiguration {
    type: "wea";
}
export interface ForecastConfigurationWeightedExponentialAverage extends ForecastConfiguration {
    type: "wea";
    pastPeriod?: ForecastConfigurationWeightedExponentialAverageExtendedPeriodAndDuration;
    pastCount?: any;
    forecastPeriod?: ForecastConfigurationWeightedExponentialAverageExtendedPeriodAndDuration;
    forecastCount?: any;
}
export interface ForecastConfigurationWeightedExponentialAverageExtendedPeriodAndDuration extends PeriodAndDuration {
}
export interface JsonPathFilter extends ValueFilter {
    type: "jsonPath";
    path?: any;
    returnFirst?: boolean;
    returnLast?: boolean;
}
export interface MetaHolder {
    meta?: {
        [index: string]: any;
    };
}
export interface MetaItemDescriptor extends AbstractNameValueDescriptorHolder {
    name?: any;
    type?: any;
    constraints?: ValueConstraintUnion[];
    format?: ValueFormat;
    units?: any[];
}
export interface NameHolder {
    name?: any;
}
export interface NameValueHolder<T> extends ValueHolder<T>, NameHolder {
}
export interface RegexValueFilter extends ValueFilter {
    type: "regex";
    pattern?: Pattern;
    matchGroup?: any;
    matchIndex?: any;
    dotAll?: any;
    multiline?: any;
}
/**
 * Returns the substring beginning at the specified index (inclusive) and ending at the optional endIndex (exclusive); if endIndex is not supplied then the remainder of the string is returned; negative values can be used to indicate a backwards count from the length of the string e.g. -1 means length-1
 */
export interface SubStringValueFilter extends ValueFilter {
    type: "substring";
    beginIndex?: number;
    endIndex?: any;
}
export interface ValueConstraint {
    type: "allowedValues" | "future" | "futureOrPresent" | "max" | "min" | "notBlank" | "notEmpty" | "notNull" | "past" | "pastOrPresent" | "pattern" | "size";
    message?: any;
}
export interface ValueConstraintAllowedValues extends ValueConstraint {
    type: "allowedValues";
    allowedValueNames?: any[];
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
    max?: any;
}
export interface ValueConstraintMin extends ValueConstraint {
    type: "min";
    min?: any;
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
    regexp?: any;
    flags?: any[];
}
export interface ValueConstraintSize extends ValueConstraint {
    type: "size";
    min?: any;
    max?: any;
}
export interface ValueDescriptor extends NameHolder {
    type?: any;
    constraints?: ValueConstraintUnion[];
    format?: ValueFormat;
    units?: any[];
    arrayDimensions?: any;
    metaUseOnly?: any;
    jsonType?: any;
}
export interface ValueDescriptorHolder {
    type?: any;
    units?: any[];
    format?: ValueFormat;
    constraints?: ValueConstraintUnion[];
}
export interface ValueFilter {
    type: "jsonPath" | "regex" | "substring";
}
export interface ValueFormat {
    useGrouping?: any;
    minimumIntegerDigits?: any;
    minimumFractionDigits?: any;
    maximumFractionDigits?: any;
    minimumSignificantDigits?: any;
    maximumSignificantDigits?: any;
    asBoolean?: any;
    asDate?: any;
    asSlider?: any;
    resolution?: any;
    dateStyle?: ValueFormatStyleRepresentation;
    timeStyle?: ValueFormatStyleRepresentation;
    dayPeriod?: ValueFormatStyleRepresentation;
    hour12?: any;
    iso8601?: any;
    weekday?: ValueFormatStyleRepresentation;
    era?: ValueFormatStyleRepresentation;
    year?: ValueFormatStyleRepresentation;
    month?: ValueFormatStyleRepresentation;
    week?: ValueFormatStyleRepresentation;
    day?: ValueFormatStyleRepresentation;
    hour?: ValueFormatStyleRepresentation;
    minute?: ValueFormatStyleRepresentation;
    second?: ValueFormatStyleRepresentation;
    fractionalSecondDigits?: any;
    timeZoneName?: ValueFormatStyleRepresentation;
    momentJsFormat?: any;
    asNumber?: any;
    asOnOff?: any;
    asPressedReleased?: any;
    asOpenClosed?: any;
    asMomentary?: any;
    multiline?: any;
}
export interface ValueHolder<T> {
    value?: T;
    type?: any;
}
export interface ColourRGB {
    r?: number;
    g?: number;
    b?: number;
}
export interface PeriodAndDuration {
}
export interface Webhook {
    name?: any;
    url?: any;
    headers?: {
        [index: string]: any[];
    };
    httpMethod?: HTTPMethod;
    usernamePassword?: UsernamePassword;
    oAuthGrant?: OAuthGrantUnion;
    payload?: any;
}
export interface Recur {
    frequency?: RecurFrequency;
    skip?: RecurSkip;
    until?: DateAsNumber;
    rscale?: RecurRScale;
    count?: any;
    interval?: any;
    secondList?: any[];
    minuteList?: any[];
    hourList?: any[];
    dayList?: WeekDay[];
    monthDayList?: any[];
    yearDayList?: any[];
    weekNoList?: any[];
    monthList?: Month[];
    setPosList?: any[];
    transformers?: {
        [index: string]: Transformer<DateAsNumber[]>;
    };
    weekStartDay?: WeekDayDay;
    calendarWeekStartDay?: number;
    experimentalValues?: {
        [index: string]: any;
    };
    calIncField?: number;
}
export interface Coordinate {
    x?: number;
    y?: number;
    z?: number;
}
export interface Pattern {
}
export interface WeekDay {
    day?: WeekDayDay;
    offset?: number;
}
export interface Month {
    monthOfYear?: number;
    leapMonth?: boolean;
}
export interface Transformer<T> {
}
export type DateAsNumber = number;
export type OAuthGrantUnion = OAuthPasswordGrant | OAuthClientCredentialsGrant | OAuthRefreshTokenGrant;
export type AssetDatapointQueryUnion = AssetDatapointAllQuery | AssetDatapointLTTBQuery | AssetDatapointIntervalQuery;
export type SharedEventUnion = SyslogEvent | AttributeEvent | AssetEvent | AssetsEvent | ReadAttributeEvent | ReadAssetEvent | ReadAssetsEvent | SimulatorState | RequestSimulatorState | RulesEngineStatusEvent | RulesetChangedEvent | GatewayDisconnectEvent | GatewayConnectionStatusEvent | GatewayCapabilitiesRequestEvent | GatewayCapabilitiesResponseEvent | GatewayTunnelStartRequestEvent | GatewayTunnelStartResponseEvent | GatewayTunnelStopRequestEvent | GatewayTunnelStopResponseEvent | DeleteAssetsRequestEvent | DeleteAssetsResponseEvent;
export type GeoJSONUnion = GeoJSONFeatureCollection | GeoJSONFeature | GeoJSONGeometry;
export type GeoJSONGeometryUnion = GeoJSONPoint;
export type AbstractNotificationMessageUnion = PushNotificationMessage | EmailNotificationMessage;
export type ProvisioningConfigUnion<T, U> = X509ProvisioningConfig;
export type ProvisioningMessageUnion = ErrorResponseMessage | SuccessResponseMessage | X509ProvisioningMessage;
export type ValuePredicateUnion = StringPredicate | BooleanPredicate | DateTimePredicate | NumberPredicate | RadialGeofencePredicate | RectangularGeofencePredicate | ArrayPredicate | ValueAnyPredicate | ValueEmptyPredicate | CalendarEventPredicate;
export type RulesetUnion = AssetRuleset | RealmRuleset | GlobalRuleset;
export type RuleActionUnion = RuleActionWait | RuleActionWriteAttribute | RuleActionNotification | RuleActionUpdateAttribute | RuleActionWebhook;
export type ForecastConfigurationUnion = ForecastConfigurationWeightedExponentialAverage;
export type ValueConstraintUnion = ValueConstraintSize | ValueConstraintPattern | ValueConstraintMin | ValueConstraintMax | ValueConstraintAllowedValues | ValueConstraintPast | ValueConstraintPastOrPresent | ValueConstraintFuture | ValueConstraintFutureOrPresent | ValueConstraintNotEmpty | ValueConstraintNotBlank | ValueConstraintNotNull;
export type ValueFilterUnion = RegexValueFilter | SubStringValueFilter | JsonPathFilter;
export declare const enum PersistenceEventCause {
    CREATE = "CREATE",
    UPDATE = "UPDATE",
    DELETE = "DELETE"
}
export declare const enum ConsoleAppConfigMenuPosition {
    BOTTOM_LEFT = "BOTTOM_LEFT",
    BOTTOM_RIGHT = "BOTTOM_RIGHT",
    TOP_LEFT = "TOP_LEFT",
    TOP_RIGHT = "TOP_RIGHT"
}
export declare const enum AssetEventCause {
    CREATE = "CREATE",
    READ = "READ",
    UPDATE = "UPDATE",
    DELETE = "DELETE"
}
export declare const enum ElectricityConsumerDemandResponseType {
    NONE = "NONE",
    FORECAST = "FORECAST",
    SETPOINT = "SETPOINT"
}
export declare const enum ConnectionStatus {
    DISCONNECTED = "DISCONNECTED",
    CONNECTING = "CONNECTING",
    DISCONNECTING = "DISCONNECTING",
    CONNECTED = "CONNECTED",
    DISABLED = "DISABLED",
    WAITING = "WAITING",
    ERROR = "ERROR",
    STOPPED = "STOPPED"
}
export declare const enum ElectricVehicleAssetEnergyType {
    EV = "EV",
    PHEV = "PHEV"
}
export declare const enum ElectricityChargerAssetConnectorType {
    YAZAKI = "YAZAKI",
    MENNEKES = "MENNEKES",
    LE_GRAND = "LE_GRAND",
    CHADEMO = "CHADEMO",
    COMBO = "COMBO",
    SCHUKO = "SCHUKO",
    ENERGYLOCK = "ENERGYLOCK"
}
export declare const enum ElectricityProducerSolarAssetPanelOrientation {
    SOUTH = "SOUTH",
    EAST_WEST = "EAST_WEST"
}
export declare const enum AttributeExecuteStatus {
    REQUEST_START = "REQUEST_START",
    REQUEST_REPEATING = "REQUEST_REPEATING",
    REQUEST_CANCEL = "REQUEST_CANCEL",
    READY = "READY",
    COMPLETED = "COMPLETED",
    RUNNING = "RUNNING",
    CANCELLED = "CANCELLED"
}
export declare const enum AttributeLinkConverterType {
    TOGGLE = "TOGGLE",
    INCREMENT = "INCREMENT",
    DECREMENT = "DECREMENT",
    NEGATE = "NEGATE"
}
export declare const enum AttributeWriteFailure {
    ASSET_NOT_FOUND = "ASSET_NOT_FOUND",
    ATTRIBUTE_NOT_FOUND = "ATTRIBUTE_NOT_FOUND",
    INSUFFICIENT_ACCESS = "INSUFFICIENT_ACCESS",
    INVALID_VALUE = "INVALID_VALUE",
    INTERCEPTOR_FAILURE = "INTERCEPTOR_FAILURE",
    STATE_STORAGE_FAILED = "STATE_STORAGE_FAILED",
    CANNOT_PROCESS = "CANNOT_PROCESS",
    QUEUE_FULL = "QUEUE_FULL",
    UNKNOWN = "UNKNOWN"
}
export declare const enum DashboardAccess {
    PUBLIC = "PUBLIC",
    SHARED = "SHARED",
    PRIVATE = "PRIVATE"
}
export declare const enum DashboardRefreshInterval {
    OFF = "OFF",
    ONE_MIN = "ONE_MIN",
    FIVE_MIN = "FIVE_MIN",
    QUARTER = "QUARTER",
    ONE_HOUR = "ONE_HOUR"
}
export declare const enum DashboardScalingPreset {
    WRAP_TO_SINGLE_COLUMN = "WRAP_TO_SINGLE_COLUMN",
    KEEP_LAYOUT = "KEEP_LAYOUT",
    REDIRECT = "REDIRECT",
    BLOCK_DEVICE = "BLOCK_DEVICE"
}
export declare const enum DatapointInterval {
    MINUTE = "MINUTE",
    HOUR = "HOUR",
    DAY = "DAY",
    WEEK = "WEEK",
    MONTH = "MONTH",
    YEAR = "YEAR"
}
export declare const enum AssetDatapointIntervalQueryFormula {
    MIN = "MIN",
    AVG = "AVG",
    MAX = "MAX"
}
export declare const enum GatewayDisconnectEventReason {
    TERMINATING = "TERMINATING",
    DISABLED = "DISABLED",
    ALREADY_CONNECTED = "ALREADY_CONNECTED",
    UNRECOGNISED = "UNRECOGNISED",
    PERMANENT_ERROR = "PERMANENT_ERROR"
}
export declare const enum GatewayTunnelInfoType {
    HTTPS = "HTTPS",
    HTTP = "HTTP",
    TCP = "TCP"
}
export declare const enum HTTPMethod {
    GET = "GET",
    POST = "POST",
    PUT = "PUT",
    DELETE = "DELETE",
    OPTIONS = "OPTIONS",
    PATCH = "PATCH"
}
export declare const enum Auth {
    KEYCLOAK = "KEYCLOAK",
    BASIC = "BASIC",
    NONE = "NONE"
}
export declare const enum EventProviderType {
    WEBSOCKET = "WEBSOCKET",
    POLLING = "POLLING"
}
export declare const enum MapType {
    VECTOR = "VECTOR",
    RASTER = "RASTER"
}
export declare const enum NotificationSource {
    INTERNAL = "INTERNAL",
    CLIENT = "CLIENT",
    GLOBAL_RULESET = "GLOBAL_RULESET",
    REALM_RULESET = "REALM_RULESET",
    ASSET_RULESET = "ASSET_RULESET"
}
export declare const enum NotificationTargetType {
    REALM = "REALM",
    USER = "USER",
    ASSET = "ASSET",
    CUSTOM = "CUSTOM"
}
export declare const enum PushNotificationMessageMessagePriority {
    NORMAL = "NORMAL",
    HIGH = "HIGH"
}
export declare const enum PushNotificationMessageTargetType {
    DEVICE = "DEVICE",
    TOPIC = "TOPIC",
    CONDITION = "CONDITION"
}
export declare const enum RepeatFrequency {
    ALWAYS = "ALWAYS",
    ONCE = "ONCE",
    HOURLY = "HOURLY",
    DAILY = "DAILY",
    WEEKLY = "WEEKLY",
    MONTHLY = "MONTHLY",
    ANNUALLY = "ANNUALLY"
}
export declare const enum ErrorResponseMessageError {
    MESSAGE_INVALID = "MESSAGE_INVALID",
    CERTIFICATE_INVALID = "CERTIFICATE_INVALID",
    UNAUTHORIZED = "UNAUTHORIZED",
    FORBIDDEN = "FORBIDDEN",
    UNIQUE_ID_MISMATCH = "UNIQUE_ID_MISMATCH",
    CONFIG_DISABLED = "CONFIG_DISABLED",
    USER_DISABLED = "USER_DISABLED",
    SERVER_ERROR = "SERVER_ERROR",
    ASSET_ERROR = "ASSET_ERROR"
}
export declare const enum AssetQueryAccess {
    PRIVATE = "PRIVATE",
    PROTECTED = "PROTECTED",
    PUBLIC = "PUBLIC"
}
export declare const enum AssetQueryMatch {
    EXACT = "EXACT",
    BEGIN = "BEGIN",
    END = "END",
    CONTAINS = "CONTAINS"
}
export declare const enum AssetQueryOperator {
    EQUALS = "EQUALS",
    GREATER_THAN = "GREATER_THAN",
    GREATER_EQUALS = "GREATER_EQUALS",
    LESS_THAN = "LESS_THAN",
    LESS_EQUALS = "LESS_EQUALS",
    BETWEEN = "BETWEEN"
}
export declare const enum AssetQueryOrderBy$Property {
    CREATED_ON = "CREATED_ON",
    NAME = "NAME",
    ASSET_TYPE = "ASSET_TYPE",
    PARENT_ID = "PARENT_ID",
    REALM = "REALM"
}
export declare const enum DashboardQueryAssetAccess {
    RESTRICTED = "RESTRICTED",
    LINKED = "LINKED",
    REALM = "REALM"
}
export declare const enum DashboardQueryConditionMinAmount {
    AT_LEAST_ONE = "AT_LEAST_ONE",
    ALL = "ALL",
    NONE = "NONE"
}
export declare const enum LogicGroupOperator {
    AND = "AND",
    OR = "OR"
}
export declare const enum UserQueryOrderBy$Property {
    CREATED_ON = "CREATED_ON",
    FIRST_NAME = "FIRST_NAME",
    LAST_NAME = "LAST_NAME",
    USERNAME = "USERNAME",
    EMAIL = "EMAIL"
}
export declare const enum RulesEngineStatus {
    STOPPED = "STOPPED",
    RUNNING = "RUNNING",
    ERROR = "ERROR"
}
export declare const enum RulesetLang {
    JAVASCRIPT = "JAVASCRIPT",
    GROOVY = "GROOVY",
    JSON = "JSON",
    FLOW = "FLOW"
}
export declare const enum RulesetStatus {
    READY = "READY",
    DEPLOYED = "DEPLOYED",
    COMPILATION_ERROR = "COMPILATION_ERROR",
    VALIDITY_PERIOD_ERROR = "VALIDITY_PERIOD_ERROR",
    EXECUTION_ERROR = "EXECUTION_ERROR",
    LOOP_ERROR = "LOOP_ERROR",
    DISABLED = "DISABLED",
    PAUSED = "PAUSED",
    EXPIRED = "EXPIRED",
    REMOVED = "REMOVED",
    EMPTY = "EMPTY"
}
export declare const enum SunPositionTriggerPosition {
    SUNRISE = "SUNRISE",
    SUNSET = "SUNSET",
    TWILIGHT_MORNING_VISUAL = "TWILIGHT_MORNING_VISUAL",
    TWILIGHT_MORNING_VISUAL_LOWER = "TWILIGHT_MORNING_VISUAL_LOWER",
    TWILIGHT_MORNING_HORIZON = "TWILIGHT_MORNING_HORIZON",
    TWILIGHT_MORNING_CIVIL = "TWILIGHT_MORNING_CIVIL",
    TWILIGHT_MORNING_NAUTICAL = "TWILIGHT_MORNING_NAUTICAL",
    TWILIGHT_MORNING_ASTRONOMICAL = "TWILIGHT_MORNING_ASTRONOMICAL",
    TWILIGHT_MORNING_GOLDEN_HOUR = "TWILIGHT_MORNING_GOLDEN_HOUR",
    TWILIGHT_MORNING_BLUE_HOUR = "TWILIGHT_MORNING_BLUE_HOUR",
    TWILIGHT_MORNING_NIGHT_HOUR = "TWILIGHT_MORNING_NIGHT_HOUR",
    TWILIGHT_EVENING_VISUAL = "TWILIGHT_EVENING_VISUAL",
    TWILIGHT_EVENING_VISUAL_LOWER = "TWILIGHT_EVENING_VISUAL_LOWER",
    TWILIGHT_EVENING_HORIZON = "TWILIGHT_EVENING_HORIZON",
    TWILIGHT_EVENING_CIVIL = "TWILIGHT_EVENING_CIVIL",
    TWILIGHT_EVENING_NAUTICAL = "TWILIGHT_EVENING_NAUTICAL",
    TWILIGHT_EVENING_ASTRONOMICAL = "TWILIGHT_EVENING_ASTRONOMICAL",
    TWILIGHT_EVENING_GOLDEN_HOUR = "TWILIGHT_EVENING_GOLDEN_HOUR",
    TWILIGHT_EVENING_BLUE_HOUR = "TWILIGHT_EVENING_BLUE_HOUR",
    TWILIGHT_EVENING_NIGHT_HOUR = "TWILIGHT_EVENING_NIGHT_HOUR"
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
export declare const enum NodeDataType {
    NUMBER = "NUMBER",
    STRING = "STRING",
    BOOLEAN = "BOOLEAN",
    /**
     * @deprecated
     */
    TRIGGER = "TRIGGER",
    COLOR = "COLOR",
    ANY = "ANY"
}
/**
 * Values:
 * - `INPUT`
 * - `PROCESSOR`
 * - `OUTPUT`
 * - `THEN` - @deprecated
 */
export declare const enum NodeType {
    INPUT = "INPUT",
    PROCESSOR = "PROCESSOR",
    OUTPUT = "OUTPUT",
    /**
     * @deprecated
     */
    THEN = "THEN"
}
export declare const enum PickerType {
    TEXT = "TEXT",
    MULTILINE = "MULTILINE",
    NUMBER = "NUMBER",
    DROPDOWN = "DROPDOWN",
    DOUBLE_DROPDOWN = "DOUBLE_DROPDOWN",
    CHECKBOX = "CHECKBOX",
    ASSET_ATTRIBUTE = "ASSET_ATTRIBUTE",
    COLOR = "COLOR"
}
export declare const enum RuleActionUpdateAttributeUpdateAction {
    ADD = "ADD",
    ADD_OR_REPLACE = "ADD_OR_REPLACE",
    REPLACE = "REPLACE",
    DELETE = "DELETE",
    CLEAR = "CLEAR"
}
export declare const enum RuleRecurrenceScope {
    PER_ASSET = "PER_ASSET",
    GLOBAL = "GLOBAL"
}
export declare const enum ClientRole {
    READ_ADMIN = "read:admin",
    READ_LOGS = "read:logs",
    READ_USERS = "read:users",
    READ_MAP = "read:map",
    READ_ASSETS = "read:assets",
    READ_RULES = "read:rules",
    READ_INSIGHTS = "read:insights",
    WRITE_USER = "write:user",
    WRITE_ADMIN = "write:admin",
    WRITE_LOGS = "write:logs",
    WRITE_ASSETS = "write:assets",
    WRITE_ATTRIBUTES = "write:attributes",
    WRITE_RULES = "write:rules",
    WRITE_INSIGHTS = "write:insights",
    READ = "read",
    WRITE = "write"
}
export declare const enum SyslogCategory {
    ASSET = "ASSET",
    AGENT = "AGENT",
    NOTIFICATION = "NOTIFICATION",
    RULES = "RULES",
    PROTOCOL = "PROTOCOL",
    GATEWAY = "GATEWAY",
    MODEL_AND_VALUES = "MODEL_AND_VALUES",
    API = "API",
    DATA = "DATA"
}
export declare const enum SyslogLevel {
    INFO = "INFO",
    WARN = "WARN",
    ERROR = "ERROR"
}
export declare const enum ValueFormatStyleRepresentation {
    NUMERIC = "numeric",
    DIGIT_2 = "2-digit",
    FULL = "full",
    LONG = "long",
    MEDIUM = "medium",
    SHORT = "short",
    NARROW = "narrow"
}
export declare const enum RecurFrequency {
    SECONDLY = "SECONDLY",
    MINUTELY = "MINUTELY",
    HOURLY = "HOURLY",
    DAILY = "DAILY",
    WEEKLY = "WEEKLY",
    MONTHLY = "MONTHLY",
    YEARLY = "YEARLY"
}
export declare const enum RecurSkip {
    OMIT = "OMIT",
    BACKWARD = "BACKWARD",
    FORWARD = "FORWARD"
}
export declare const enum RecurRScale {
    JAPANESE = "JAPANESE",
    BUDDHIST = "BUDDHIST",
    ROC = "ROC",
    ISLAMIC = "ISLAMIC",
    ISO8601 = "ISO8601",
    CHINESE = "CHINESE",
    ETHIOPIC = "ETHIOPIC",
    HEBREW = "HEBREW",
    GREGORIAN = "GREGORIAN"
}
export declare const enum WeekDayDay {
    SU = "SU",
    MO = "MO",
    TU = "TU",
    WE = "WE",
    TH = "TH",
    FR = "FR",
    SA = "SA"
}
export declare const enum WellknownAssets {
    PEOPLECOUNTERASSET = "PeopleCounterAsset",
    MAILAGENT = "MailAgent",
    UDPAGENT = "UDPAgent",
    STORAGESIMULATORAGENT = "StorageSimulatorAgent",
    ROOMASSET = "RoomAsset",
    CITYASSET = "CityAsset",
    ELECTRICITYCONSUMERASSET = "ElectricityConsumerAsset",
    ELECTRICVEHICLEASSET = "ElectricVehicleAsset",
    VENTILATIONASSET = "VentilationAsset",
    TRADFRILIGHTASSET = "TradfriLightAsset",
    CONSOLEASSET = "ConsoleAsset",
    PRESENCESENSORASSET = "PresenceSensorAsset",
    LIGHTASSET = "LightAsset",
    KNXAGENT = "KNXAgent",
    PLUGASSET = "PlugAsset",
    ELECTRICITYCHARGERASSET = "ElectricityChargerAsset",
    WEATHERASSET = "WeatherAsset",
    ELECTRICITYBATTERYASSET = "ElectricityBatteryAsset",
    ELECTRICITYSUPPLIERASSET = "ElectricitySupplierAsset",
    THINGASSET = "ThingAsset",
    WEBSOCKETAGENT = "WebsocketAgent",
    TCPAGENT = "TCPAgent",
    GATEWAYASSET = "GatewayAsset",
    SERIALAGENT = "SerialAgent",
    BLUETOOTHMESHAGENT = "BluetoothMeshAgent",
    PARKINGASSET = "ParkingAsset",
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
    VELBUSSERIALAGENT = "VelbusSerialAgent",
    GROUNDWATERSENSORASSET = "GroundwaterSensorAsset",
    ELECTRICITYPRODUCERSOLARASSET = "ElectricityProducerSolarAsset",
    MICROPHONEASSET = "MicrophoneAsset",
    ZWAVEAGENT = "ZWaveAgent",
    ENERGYOPTIMISATIONASSET = "EnergyOptimisationAsset",
    HTTPAGENT = "HTTPAgent",
    DOORASSET = "DoorAsset",
    THERMOSTATASSET = "ThermostatAsset",
    ELECTRICITYPRODUCERASSET = "ElectricityProducerAsset",
    SHIPASSET = "ShipAsset"
}
export declare const enum WellknownAttributes {
    MILEAGEMINIMUM = "mileageMinimum",
    COLOURRGB = "colourRGB",
    CARBONIMPORT = "carbonImport",
    SUPPORTSEXPORT = "supportsExport",
    PRICEHOURLY = "priceHourly",
    WINDSPEEDMAX = "windSpeedMax",
    ENERGYSELFCONSUMPTION = "energySelfConsumption",
    PINGDISABLED = "pingDisabled",
    FLOW = "flow",
    CHILDASSETTYPE = "childAssetType",
    LASTACCESS = "lastAccess",
    CHARGERID = "chargerID",
    POSITION = "position",
    NETWORKKEY = "networkKey",
    CARBONSAVING = "carbonSaving",
    UNIVERSE = "universe",
    ROUTINGMODE = "routingMode",
    MSSINUMBER = "MSSINumber",
    LASTWILLRETAIN = "lastWillRetain",
    LASTWILLTOPIC = "lastWillTopic",
    SERIALBAUDRATE = "serialBaudrate",
    LEDCOUNT = "lEDCount",
    LOCKED = "locked",
    BASEURL = "baseURL",
    PROTOCOL = "protocol",
    TARIFFIMPORT = "tariffImport",
    ENERGYRENEWABLESHARE = "energyRenewableShare",
    REQUIREDVALUES = "requiredValues",
    REGION = "region",
    FINANCIALSAVING = "financialSaving",
    FLEETCATEGORY = "fleetCategory",
    CONSOLEPLATFORM = "consolePlatform",
    POWEREXPORTMIN = "powerExportMin",
    GATEWAYSTATUS = "gatewayStatus",
    NO2LEVEL = "NO2Level",
    EFFICIENCYEXPORT = "efficiencyExport",
    CHARGECYCLES = "chargeCycles",
    SERIALPORT = "serialPort",
    TAGS = "tags",
    MILEAGEMIN = "mileageMin",
    UNLOCK = "unlock",
    CHECKINTERVALSECONDS = "checkIntervalSeconds",
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
    CONNECTHEADERS = "connectHeaders",
    CONSOLENAME = "consoleName",
    WEBSOCKETQUERY = "websocketQuery",
    PREFERHTML = "preferHTML",
    COOLING = "cooling",
    POLLINGMILLIS = "pollingMillis",
    SOILTEMPERATURE = "soilTemperature",
    SUNIRRADIANCE = "sunIrradiance",
    ODOMETER = "odometer",
    SUPPORTSIMPORT = "supportsImport",
    MESSAGEMAXLENGTH = "messageMaxLength",
    ENERGYLEVELPERCENTAGEMIN = "energyLevelPercentageMin",
    LIGHTID = "lightId",
    PROXYADDRESS = "proxyAddress",
    REQUESTQUERYPARAMETERS = "requestQueryParameters",
    ENERGYLOCAL = "energyLocal",
    POWER = "power",
    ENERGYAUTARKY = "energyAutarky",
    MODEL = "model",
    STREET = "street",
    MANUFACTURER = "manufacturer",
    DELETEPROCESSEDMAIL = "deleteProcessedMail",
    ONOFF = "onOff",
    RESUMESESSION = "resumeSession",
    TARIFFEXPORT = "tariffExport",
    WINDSPEEDMIN = "windSpeedMin",
    CITY = "city",
    POWERSETPOINT = "powerSetpoint",
    PRESENCE = "presence",
    CONNECTORTYPE = "connectorType",
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
    COUNTOUTMINUTE = "countOutMinute",
    HOST = "host",
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
    SOURCEADDRESS = "sourceAddress",
    INCLUDEFORECASTWINDSERVICE = "includeForecastWindService",
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
    TUNNELINGSUPPORTED = "tunnelingSupported",
    ENINUMBER = "ENINumber",
    PARTICLESPM2_5 = "particlesPM2_5",
    AGENTDISABLED = "agentDisabled",
    DISABLED = "disabled",
    COUNTIN = "countIn",
    MESSAGESOURCEADDRESS = "messageSourceAddress",
    MILEAGECHARGED = "mileageCharged",
    FORCECHARGE = "forceCharge",
    TIMEINJECTIONINTERVAL = "timeInjectionInterval",
    TEMPERATURESETPOINT = "temperatureSetpoint",
    FINANCIALWEIGHTING = "financialWeighting",
    CONSOLEPROVIDERS = "consoleProviders",
    ENERGYLEVELPERCENTAGE = "energyLevelPercentage",
    POWERIMPORTMAX = "powerImportMax",
    COUNTTOTAL = "countTotal",
    SPACESBUFFER = "spacesBuffer",
    SPACESTOTAL = "spacesTotal",
    BINDHOST = "bindHost",
    VEHICLECATEGORY = "vehicleCategory",
    FANSPEED = "fanSpeed",
    MESSAGECONVERTBINARY = "messageConvertBinary",
    FINANCIALCOST = "financialCost",
    NOTES = "notes",
    MAILFOLDERNAME = "mailFolderName",
    MESSAGEDELIMITERS = "messageDelimiters",
    EFFICIENCYIMPORT = "efficiencyImport",
    TEMPERATURE = "temperature",
    AVAILABLECHARGINGSPACES = "availableChargingSpaces",
    ENERGYCAPACITY = "energyCapacity",
    SUNZENITH = "sunZenith",
    POWERIMPORTMIN = "powerImportMin",
    INTERVALSIZE = "intervalSize",
    SNMPVERSIONVALUE = "SNMPVersionValue",
    BRIGHTNESS = "brightness",
    POSTALCODE = "postalCode",
    SPACESOPEN = "spacesOpen",
    HUMIDITY = "humidity",
    SOUNDLEVEL = "soundLevel",
    MILEAGECAPACITY = "mileageCapacity",
    SECUREMODE = "secureMode",
    DIRECTION = "direction",
    ENERGYLEVELSCHEDULE = "energyLevelSchedule",
    UVINDEX = "uVIndex",
    CONNECTURL = "connectURL",
    CLIENTID = "clientId",
    NATMODE = "NATMode",
    UPDATEONWRITE = "updateOnWrite",
    GROUPID = "groupId",
    LASTWILLPAYLOAD = "lastWillPayload",
    VEHICLEID = "vehicleID",
    FOLLOWREDIRECTS = "followRedirects",
    CARBONCOST = "carbonCost",
    SUNAZIMUTH = "sunAzimuth",
    MESSAGECONVERTHEX = "messageConvertHex",
    AVAILABLEDISCHARGINGSPACES = "availableDischargingSpaces",
    WEBSOCKETMODE = "websocketMode",
    COLOURTEMPERATURE = "colourTemperature",
    VEHICLECONNECTED = "vehicleConnected"
}
export declare const enum WellknownMetaItems {
    FORMAT = "format",
    DATAPOINTSMAXAGEDAYS = "dataPointsMaxAgeDays",
    ACCESSRESTRICTEDWRITE = "accessRestrictedWrite",
    SECRET = "secret",
    RULEEVENT = "ruleEvent",
    ATTRIBUTELINKS = "attributeLinks",
    RULERESETIMMEDIATE = "ruleResetImmediate",
    RULESTATE = "ruleState",
    HASPREDICTEDDATAPOINTS = "hasPredictedDataPoints",
    FORECAST = "forecast",
    STOREDATAPOINTS = "storeDataPoints",
    LABEL = "label",
    CONSTRAINTS = "constraints",
    UNITS = "units",
    USERCONNECTED = "userConnected",
    SHOWONDASHBOARD = "showOnDashboard",
    READONLY = "readOnly",
    MULTILINE = "multiline",
    ACCESSPUBLICWRITE = "accessPublicWrite",
    MOMENTARY = "momentary",
    AGENTLINK = "agentLink",
    RULEEVENTEXPIRES = "ruleEventExpires",
    ACCESSRESTRICTEDREAD = "accessRestrictedRead",
    ACCESSPUBLICREAD = "accessPublicRead"
}
export declare const enum WellknownValueTypes {
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
    VALUEDESCRIPTOR = "valueDescriptor",
    PERIODDURATIONISO8601 = "periodDurationISO8601",
    LONG = "long",
    DATEANDTIME = "dateAndTime",
    BOOLEANMAP = "booleanMap",
    CONNECTORTYPE = "connectorType",
    NEGATIVEINTEGER = "negativeInteger",
    TCPIPPORTNUMBER = "TCP_IPPortNumber",
    GEOJSONPOINT = "GEO_JSONPoint",
    SNMPVERSION = "SNMPVersion",
    ASSETTYPE = "assetType",
    JSONOBJECT = "JSONObject",
    INTEGERMAP = "integerMap",
    BIGNUMBER = "bigNumber",
    ENERGYTYPE = "energyType",
    IPADDRESS = "IPAddress",
    EMAIL = "email",
    TIMESTAMP = "timestamp",
    KNXMESSAGESOURCEADDRESS = "kNXMessageSourceAddress",
    ATTRIBUTEREFERENCE = "attributeReference",
    UUID = "UUID",
    INTEGER = "integer",
    OAUTHGRANT = "oAuthGrant",
    NUMBER = "number",
    TEXTMAP = "textMap",
    WEBSOCKETSUBSCRIPTION = "websocketSubscription",
    BOOLEAN = "boolean",
    TIMEANDPERIODDURATIONISO8601 = "timeAndPeriodDurationISO8601",
    FORECASTCONFIGURATION = "forecastConfiguration",
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
export declare const enum WellknownUnitTypes {
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
    HECTO = "hecto",
    PEAK = "peak",
    SECOND = "second",
    MICRO = "micro",
    PER = "per",
    STONE = "stone"
}
export declare const enum WellknownRulesetMetaItems {
}
