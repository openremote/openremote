import {AttributeRef} from "@openremote/model";

/**
 * A {@link WidgetConfig} interface for widgets where assets are involved.
 * The backend will understand this object type, for example to determine asset permissions.
 * This interface is often extended upon with additional configuration fields like 'size' or 'decimals'.
 */
export interface AssetWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
}

/**
 * Default config interface for widgets to re-use
 * Often extended upon with additional configuration fields like 'size' or 'decimals'.
 */
export interface WidgetConfig {
}
