export class AssetModelUtil {
    static getAssetDescriptors() {
        return AssetModelUtil._assetTypeInfos.map(info => info.assetDescriptor);
    }
    static getMetaItemDescriptors() {
        return [...this._metaItemDescriptors];
    }
    static getValueDescriptors() {
        return [...this._valueDescriptors];
    }
    static getAssetTypeInfos() {
        return [...this._assetTypeInfos];
    }
    static getAssetTypeInfo(type) {
        if (!type) {
            return;
        }
        if (type.assetDescriptor) {
            return type;
        }
        if (typeof (type) !== "string") {
            type = type.name;
        }
        return this._assetTypeInfos.find((assetTypeInfo) => {
            return assetTypeInfo.assetDescriptor.name === type;
        });
    }
    static getAssetDescriptor(type) {
        if (!type) {
            return;
        }
        if (type.assetDescriptor) {
            return type.assetDescriptor;
        }
        if (typeof (type) !== "string") {
            return type;
        }
        const match = this._assetTypeInfos.find((assetTypeInfo) => {
            return assetTypeInfo.assetDescriptor.name === type;
        });
        return match ? match.assetDescriptor : undefined;
    }
    static getAttributeDescriptor(attributeName, assetTypeOrDescriptor) {
        if (!attributeName) {
            return;
        }
        const assetTypeInfo = this.getAssetTypeInfo(assetTypeOrDescriptor || "ThingAsset" /* WellknownAssets.THINGASSET */);
        if (!assetTypeInfo || !assetTypeInfo.attributeDescriptors) {
            return;
        }
        return assetTypeInfo.attributeDescriptors.find((attributeDescriptor) => attributeDescriptor.name === attributeName);
    }
    static getValueDescriptor(name) {
        if (!name) {
            return;
        }
        // If name ends with [] then it's an array value type so lookup the base type and then convert to array
        let arrayDimensions;
        if (name.endsWith("[]")) {
            arrayDimensions = 0;
            while (name.endsWith("[]")) {
                name = name.substring(0, name.length - 2);
                arrayDimensions++;
            }
        }
        // Value descriptor names are globally unique
        let valueDescriptor = this._valueDescriptors.find((valueDescriptor) => valueDescriptor.name === name);
        if (valueDescriptor && arrayDimensions) {
            valueDescriptor = Object.assign(Object.assign({}, valueDescriptor), { arrayDimensions: arrayDimensions });
        }
        return valueDescriptor;
    }
    static resolveValueDescriptor(valueHolder, descriptorOrValueType) {
        let valueDescriptor;
        if (descriptorOrValueType) {
            if (typeof (descriptorOrValueType) === "string") {
                valueDescriptor = AssetModelUtil.getValueDescriptor(descriptorOrValueType);
            }
            if (descriptorOrValueType.jsonType) {
                valueDescriptor = descriptorOrValueType;
            }
            else {
                // Must be a value descriptor holder or value holder
                valueDescriptor = AssetModelUtil.getValueDescriptor(descriptorOrValueType.type);
            }
        }
        if (!valueDescriptor && valueHolder) {
            // Try and determine the value descriptor based on the value type
            valueDescriptor = AssetModelUtil.getValueDescriptor("JSON" /* WellknownValueTypes.JSON */);
        }
        return valueDescriptor;
    }
    static resolveValueTypeFromValue(value) {
        if (value === null || value === undefined) {
            return undefined;
        }
        if (typeof value === "number") {
            return "number" /* WellknownValueTypes.NUMBER */;
        }
        if (typeof value === "string") {
            return "text" /* WellknownValueTypes.TEXT */;
        }
        if (typeof value === "boolean") {
            return "boolean" /* WellknownValueTypes.BOOLEAN */;
        }
        if (Array.isArray(value)) {
            let dimensions = 1;
            let v = value.find(v => v !== undefined && v !== null);
            while (Array.isArray(v)) {
                v = v.find(v => v !== undefined && v !== null);
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
            return "dateAndTime" /* WellknownValueTypes.DATEANDTIME */;
        }
    }
    static getAttributeAndValueDescriptors(assetType, attributeNameOrDescriptor, attribute) {
        let attributeDescriptor;
        let valueDescriptor;
        if (attributeNameOrDescriptor && typeof attributeNameOrDescriptor !== "string") {
            attributeDescriptor = attributeNameOrDescriptor;
        }
        else {
            const assetTypeInfo = this.getAssetTypeInfo(assetType || "ThingAsset" /* WellknownAssets.THINGASSET */);
            if (!assetTypeInfo) {
                return [undefined, undefined];
            }
            if (typeof (attributeNameOrDescriptor) === "string") {
                attributeDescriptor = this.getAttributeDescriptor(attributeNameOrDescriptor, assetTypeInfo);
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
    static getMetaItemDescriptor(name) {
        if (!name) {
            return;
        }
        // Meta item descriptor names are globally unique
        return this._metaItemDescriptors.find((metaItemDescriptor) => metaItemDescriptor.name === name);
    }
    static getAssetDescriptorColour(typeOrDescriptor, fallbackColor) {
        const assetDescriptor = this.getAssetDescriptor(typeOrDescriptor);
        return assetDescriptor && assetDescriptor.colour ? assetDescriptor.colour : fallbackColor;
    }
    static getAssetDescriptorIcon(typeOrDescriptor, fallbackIcon) {
        const assetDescriptor = this.getAssetDescriptor(typeOrDescriptor);
        return assetDescriptor && assetDescriptor.icon ? assetDescriptor.icon : fallbackIcon;
    }
}
AssetModelUtil._assetTypeInfos = [];
AssetModelUtil._metaItemDescriptors = [];
AssetModelUtil._valueDescriptors = [];
//# sourceMappingURL=util.js.map