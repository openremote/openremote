import {
    AgentDescriptor,
    AssetDescriptor,
    AssetTypeInfo, Attribute,
    AttributeDescriptor,
    MetaItemDescriptor,
    ValueDescriptor, ValueDescriptorHolder, ValueHolder, WellknownAssets, WellknownValueTypes
} from "./index";

export class AssetModelUtil {

    static _assetTypeInfos: AssetTypeInfo[] = [];
    static _metaItemDescriptors: MetaItemDescriptor[] = [];
    static _valueDescriptors: ValueDescriptor[] = [];

    public static getAssetDescriptors(): AssetDescriptor[] {
        return AssetModelUtil._assetTypeInfos.map(info => info.assetDescriptor as AgentDescriptor);
    }

    public static getMetaItemDescriptors(): MetaItemDescriptor[] {
        return [...this._metaItemDescriptors];
    }

    public static getValueDescriptors(): ValueDescriptor[] {
        return [...this._valueDescriptors];
    }

    public static getAssetTypeInfos(): AssetTypeInfo[] {
        return [...this._assetTypeInfos];
    }

    public static getAssetTypeInfo(type: string | AssetDescriptor | AssetTypeInfo): AssetTypeInfo | undefined {
        if (!type) {
            return;
        }

        if ((type as AssetTypeInfo).assetDescriptor) {
            return type as AssetTypeInfo;
        }

        if (typeof(type) !== "string") {
            type = (type as AssetDescriptor).name!;
        }

        return this._assetTypeInfos.find((assetTypeInfo) => {
            return assetTypeInfo.assetDescriptor!.name === type;
        });
    }

    public static getAssetDescriptor(type?: string | AssetDescriptor | AssetTypeInfo): AssetDescriptor | undefined {
        if (!type) {
            return;
        }

        if ((type as AssetTypeInfo).assetDescriptor) {
            return (type as AssetTypeInfo).assetDescriptor;
        }

        if (typeof(type) !== "string") {
            return type as AssetDescriptor;
        }

        const match = this._assetTypeInfos.find((assetTypeInfo) => {
            return assetTypeInfo.assetDescriptor!.name === type;
        });
        return match ? match.assetDescriptor : undefined;
    }

    public static getAttributeDescriptor(attributeName: string, assetTypeOrDescriptor: string | AssetDescriptor | AssetTypeInfo): AttributeDescriptor | undefined {
        if (!attributeName) {
            return;
        }

        const assetTypeInfo = this.getAssetTypeInfo(assetTypeOrDescriptor || WellknownAssets.THINGASSET);

        if (!assetTypeInfo || !assetTypeInfo.attributeDescriptors) {
            return;
        }

        return assetTypeInfo.attributeDescriptors.find((attributeDescriptor) => attributeDescriptor.name === attributeName);
    }

    public static getValueDescriptor(name?: string): ValueDescriptor | undefined {
        if (!name) {
            return;
        }

        // If name ends with [] then it's an array value type so lookup the base type and then convert to array
        let arrayDimensions: number | undefined;

        if (name.endsWith("[]")) {
            arrayDimensions = 0;
            while(name.endsWith("[]")) {
                name = name.substring(0, name.length - 2);
                arrayDimensions++;
            }
        }

        // Value descriptor names are globally unique
        let valueDescriptor = this._valueDescriptors.find((valueDescriptor) => valueDescriptor.name === name);
        if (valueDescriptor && arrayDimensions) {
            valueDescriptor = {...valueDescriptor, arrayDimensions: arrayDimensions};
        }
        return valueDescriptor;
    }

    public static resolveValueDescriptor(valueHolder: ValueHolder<any> | undefined, descriptorOrValueType: ValueDescriptorHolder | ValueDescriptor | string | undefined): ValueDescriptor | undefined {
        let valueDescriptor: ValueDescriptor | undefined;

        if (descriptorOrValueType) {
            if (typeof(descriptorOrValueType) === "string") {
                valueDescriptor = AssetModelUtil.getValueDescriptor(descriptorOrValueType);
            }
            if ((descriptorOrValueType as ValueDescriptor).jsonType) {
                valueDescriptor = descriptorOrValueType as ValueDescriptor;
            } else {
                // Must be a value descriptor holder or value holder
                valueDescriptor = AssetModelUtil.getValueDescriptor((descriptorOrValueType as ValueDescriptorHolder).type);
            }
        }

        if (!valueDescriptor && valueHolder) {
            // Try and determine the value descriptor based on the value type
            valueDescriptor = AssetModelUtil.getValueDescriptor(WellknownValueTypes.JSON);
        }

        return valueDescriptor;
    }

    public static resolveValueTypeFromValue(value: any): string | undefined {
        if (value === null || value === undefined) {
            return undefined;
        }

        if (typeof value === "number") {
            return WellknownValueTypes.NUMBER;
        }
        if (typeof value === "string") {
            return WellknownValueTypes.TEXT;
        }
        if (typeof value === "boolean") {
            return WellknownValueTypes.BOOLEAN;
        }
        if (Array.isArray(value)) {
            let dimensions = 1;
            let v = (value as any[]).find(v => v !== undefined && v !== null);

            while (Array.isArray(v)) {
                v = (v as any[]).find(v => v !== undefined && v !== null);
                dimensions++;
            }

            let valueType = this.resolveValueTypeFromValue(v);

            if (!valueType) {
                return;
            }

            while (dimensions > 0) {
                valueType += "[]";
                dimensions--;
            }

            return valueType;
        }
        if (value instanceof Date) {
            return WellknownValueTypes.DATEANDTIME;
        }
    }

    public static getAttributeAndValueDescriptors(assetType: string | undefined, attributeNameOrDescriptor: string | AttributeDescriptor | undefined, attribute?: Attribute<any>): [AttributeDescriptor | undefined, ValueDescriptor | undefined] {
        let attributeDescriptor: AttributeDescriptor | undefined;
        let valueDescriptor: ValueDescriptor | undefined;

        if (attributeNameOrDescriptor && typeof attributeNameOrDescriptor !== "string") {
            attributeDescriptor = attributeNameOrDescriptor as AttributeDescriptor;
        } else {
            const assetTypeInfo = this.getAssetTypeInfo(assetType || WellknownAssets.THINGASSET);

            if (!assetTypeInfo) {
                return [undefined, undefined];
            }

            if (typeof (attributeNameOrDescriptor) === "string") {
                attributeDescriptor = this.getAttributeDescriptor(attributeNameOrDescriptor as string, assetTypeInfo);
            }

            if (!attributeDescriptor && attribute) {
                attributeDescriptor = {
                    type: attribute.type,
                    name: attribute.name,
                    meta: attribute.meta
                };
            }
        }

        if (attributeDescriptor) {
            valueDescriptor = this.getValueDescriptor(attributeDescriptor.type);
        }

        return [attributeDescriptor, valueDescriptor];
    }

    public static getMetaItemDescriptor(name?: string): MetaItemDescriptor | undefined {
        if (!name) {
            return;
        }

        // Meta item descriptor names are globally unique
        return this._metaItemDescriptors.find((metaItemDescriptor) => metaItemDescriptor.name === name);
    }

    public static getAssetDescriptorColour(typeOrDescriptor: string | AssetTypeInfo | AssetDescriptor | undefined, fallbackColor?: string): string | undefined {
        const assetDescriptor = this.getAssetDescriptor(typeOrDescriptor);
        return assetDescriptor && assetDescriptor.colour ? assetDescriptor.colour : fallbackColor;
    }

    public static getAssetDescriptorIcon(typeOrDescriptor: string | AssetTypeInfo | AssetDescriptor | undefined, fallbackIcon?: string): string | undefined {
        const assetDescriptor = this.getAssetDescriptor(typeOrDescriptor);
        return assetDescriptor && assetDescriptor.icon ? assetDescriptor.icon : fallbackIcon;
    }
}
