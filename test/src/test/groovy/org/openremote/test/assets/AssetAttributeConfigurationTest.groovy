package org.openremote.test.assets

import org.openremote.agent.protocol.modbus.ModbusAgentLink
import org.openremote.manager.asset.AssetAttributeConfigurationService
import org.openremote.model.asset.AssetAttributeConfigurationDocument
import org.openremote.model.asset.AssetAttributeConfigurationEntry
import org.openremote.model.asset.AssetAttributeConfigurationExportRequest
import org.openremote.model.asset.AssetAttributeConfigurationGenericParameter
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.ValueType
import spock.lang.Specification

import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.LABEL
import static org.openremote.model.value.MetaItemType.READ_ONLY
import static org.openremote.model.value.MetaItemType.STORE_DATA_POINTS

class AssetAttributeConfigurationTest extends Specification {

    def setupSpec() {
        ValueUtil.initialise(null)
    }

    def "Export attribute configuration document"() {
        given: "an asset with configured and unconfigured attributes"
        def asset = new ThingAsset("Indoor unit")
            .addOrReplaceAttributes(
                new Attribute<>("temperature", ValueType.NUMBER, 21d)
                    .addOrReplaceMeta(
                        new MetaItem<>(READ_ONLY, true),
                        new MetaItem<>(LABEL, "Current temperature")
                    ),
                new Attribute<>("humidity", ValueType.NUMBER, 45d)
                    .addOrReplaceMeta(new MetaItem<>(STORE_DATA_POINTS, true)),
                new Attribute<>("notes", ValueType.TEXT, "No exported meta")
            )

        when: "only some attributes are selected for export"
        def document = AssetAttributeConfigurationService.exportConfiguration(
            asset,
            new AssetAttributeConfigurationExportRequest(["temperature", "notes"])
        )

        then: "the document contains the version and asset type"
        document.version == AssetAttributeConfigurationDocument.CURRENT_VERSION
        document.assetType == asset.type

        and: "only selected attributes with non-empty meta are exported"
        document.attributes.keySet() == ["temperature"] as Set
        document.attributes.temperature.type == "number"
        document.attributes.temperature.meta.getValue(READ_ONLY).orElse(false)
        document.attributes.temperature.meta.getValue(LABEL).orElse(null) == "Current temperature"

        when: "no attribute selection is provided"
        document = AssetAttributeConfigurationService.exportConfiguration(asset, new AssetAttributeConfigurationExportRequest(null))

        then: "all attributes with non-empty meta are exported"
        document.attributes.keySet() == ["temperature", "humidity"] as Set
    }

    def "Export rejects empty configuration output"() {
        given: "an asset with configured and unconfigured attributes"
        def asset = new ThingAsset("Indoor unit")
            .addOrReplaceAttributes(
                new Attribute<>("temperature", ValueType.NUMBER)
                    .addOrReplaceMeta(new MetaItem<>(READ_ONLY, true)),
                new Attribute<>("notes", ValueType.TEXT)
            )

        when: "the user selected no attributes"
        AssetAttributeConfigurationService.exportConfiguration(
            asset,
            new AssetAttributeConfigurationExportRequest([])
        )

        then: "export fails"
        thrown(IllegalArgumentException)

        when: "the selected attributes have no metadata"
        AssetAttributeConfigurationService.exportConfiguration(
            asset,
            new AssetAttributeConfigurationExportRequest(["notes"])
        )

        then: "export fails"
        thrown(IllegalArgumentException)

        when: "the asset has no configured attributes"
        AssetAttributeConfigurationService.exportConfiguration(
            new ThingAsset("Empty asset")
                .addOrReplaceAttributes(new Attribute<>("notes", ValueType.TEXT)),
            new AssetAttributeConfigurationExportRequest(null)
        )

        then: "export fails"
        thrown(IllegalArgumentException)
    }

    def "Export attribute configuration can make selected meta paths generic"() {
        given: "an asset with repeated agent link metadata"
        def asset = new ThingAsset("Indoor unit")
            .addOrReplaceAttributes(
                new Attribute<>("error", ValueType.BOOLEAN)
                    .addOrReplaceMeta(new MetaItem<>("agentLink", null, [
                        id    : "agent-1",
                        unitId: 1,
                        type  : "ModbusAgentLink"
                    ])),
                new Attribute<>("pumpDown", ValueType.BOOLEAN)
                    .addOrReplaceMeta(new MetaItem<>("agentLink", null, [
                        id    : "agent-1",
                        unitId: 1,
                        type  : "ModbusAgentLink"
                    ]))
            )

        when: "agent id and unit id are exported as generic values"
        def document = AssetAttributeConfigurationService.exportConfiguration(
            asset,
            new AssetAttributeConfigurationExportRequest(
                ["error", "pumpDown"],
                ["meta.agentLink.id", "meta.agentLink.unitId"]
            )
        )

        then: "generic parameters record the removed value locations and original value types"
        document.genericParameters.keySet() == ["agentLinkId", "agentLinkUnitId"] as Set
        document.genericParameters.agentLinkId.type == "text"
        document.genericParameters.agentLinkId.paths == [
            "attributes.error.meta.agentLink.id",
            "attributes.pumpDown.meta.agentLink.id"
        ]
        document.genericParameters.agentLinkUnitId.type == "number"
        document.genericParameters.agentLinkUnitId.paths == [
            "attributes.error.meta.agentLink.unitId",
            "attributes.pumpDown.meta.agentLink.unitId"
        ]

        and: "the concrete values are removed but the remaining meta hierarchy is preserved"
        document.attributes.error.meta.get("agentLink").get().value.orElse(null) == [type: "ModbusAgentLink"]
        document.attributes.pumpDown.meta.get("agentLink").get().value.orElse(null) == [type: "ModbusAgentLink"]

        when: "the document is serialized"
        def json = ValueUtil.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(document)

        then: "the concrete generic values are absent from the exported JSON"
        json.contains('"genericParameters"')
        !json.contains('"agent-1"')
        !json.contains('"unitId" : 1')
    }

    def "Export rejects generic paths with incompatible repeated values"() {
        given: "an asset with repeated paths containing different values"
        def asset = new ThingAsset("Indoor unit")
            .addOrReplaceAttributes(
                new Attribute<>("error", ValueType.BOOLEAN)
                    .addOrReplaceMeta(new MetaItem<>("agentLink", null, [unitId: 1])),
                new Attribute<>("pumpDown", ValueType.BOOLEAN)
                    .addOrReplaceMeta(new MetaItem<>("agentLink", null, [unitId: 2]))
            )

        when: "the path is requested as a shared generic value"
        AssetAttributeConfigurationService.exportConfiguration(
            asset,
            new AssetAttributeConfigurationExportRequest(
                ["error", "pumpDown"],
                ["meta.agentLink.unitId"]
            )
        )

        then: "export fails"
        thrown(IllegalArgumentException)
    }

    def "Export generic paths from typed meta item values"() {
        given: "an asset with typed agent link metadata"
        def agentLink = new ModbusAgentLink("agent-1")
        agentLink.setUnitId(1)

        def asset = new ThingAsset("Indoor unit")
            .addOrReplaceAttributes(
                new Attribute<>("error", ValueType.BOOLEAN)
                    .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, agentLink))
            )

        when: "agent link properties are exported as generic values"
        def document = AssetAttributeConfigurationService.exportConfiguration(
            asset,
            new AssetAttributeConfigurationExportRequest(
                ["error"],
                ["meta.agentLink.id", "meta.agentLink.unitId"]
            )
        )

        then: "the generic parameters are created from the typed value"
        document.genericParameters.agentLinkId.type == "text"
        document.genericParameters.agentLinkUnitId.type == "number"

        and: "the concrete values are removed while the agent link type is preserved"
        def json = ValueUtil.JSON.writeValueAsString(document)
        def jsonNode = ValueUtil.JSON.readTree(json)
        def exportedAgentLink = jsonNode.get("attributes").get("error").get("meta").get("agentLink")
        exportedAgentLink.get("type").asText() == "ModbusAgentLink"
        !exportedAgentLink.has("id")
        !exportedAgentLink.has("unitId")
    }

    def "Export generic parameter types from meta schema"() {
        given: "an asset with generic metadata values whose runtime types do not match the schema"
        def asset = new ThingAsset("Indoor unit")
            .addOrReplaceAttributes(
                new Attribute<>("error", ValueType.BOOLEAN)
                    .addOrReplaceMeta(
                        new MetaItem<>("label", null, 42),
                        new MetaItem<>("readOnly", null, "true"),
                        new MetaItem<>("agentLink", null, [
                            id    : 123,
                            unitId: "1",
                            type  : "ModbusAgentLink"
                        ])
                    )
            )

        when: "those paths are exported as generic values"
        def document = AssetAttributeConfigurationService.exportConfiguration(
            asset,
            new AssetAttributeConfigurationExportRequest(
                ["error"],
                ["meta.label", "meta.readOnly", "meta.agentLink.id", "meta.agentLink.unitId"]
            )
        )

        then: "the generic parameter types come from the meta schema, not the current values"
        document.genericParameters.label.type == "text"
        document.genericParameters.readOnly.type == "boolean"
        document.genericParameters.agentLinkId.type == "text"
        document.genericParameters.agentLinkUnitId.type == "number"
    }

    def "Import attribute configuration previews compatible patch without changing the draft asset"() {
        given: "a target draft asset with existing metadata"
        def target = new ThingAsset("Target")
            .setId("0xxxxxxxxxxxxxxxxxxxxx")
            .addOrReplaceAttributes(
                new Attribute<>("temperature", ValueType.NUMBER, 18d)
                    .addOrReplaceMeta(
                        new MetaItem<>(READ_ONLY, false),
                        new MetaItem<>(LABEL, "Old label")
                    ),
                new Attribute<>("humidity", ValueType.NUMBER, 40d)
                    .addOrReplaceMeta(new MetaItem<>(LABEL, "Existing humidity label")),
                new Attribute<>("mode", ValueType.TEXT, "auto")
                    .addOrReplaceMeta(new MetaItem<>(LABEL, "Existing mode label"))
            )

        and: "an import document with compatible, missing, and type-mismatched attributes"
        def importedTemperatureMeta = new MetaMap([
            new MetaItem<>(READ_ONLY, true),
            new MetaItem<>("agentLink", null, [id: "agent-1", unitId: 1])
        ])
        def importedMissingMeta = new MetaMap([new MetaItem<>(LABEL, "Missing")])
        def importedModeMeta = new MetaMap([new MetaItem<>(LABEL, "Wrong type")])

        def document = new AssetAttributeConfigurationDocument(
            AssetAttributeConfigurationDocument.CURRENT_VERSION,
            RoomAsset.DESCRIPTOR.name,
            [
                temperature: new AssetAttributeConfigurationEntry("number", importedTemperatureMeta),
                missing    : new AssetAttributeConfigurationEntry("text", importedMissingMeta),
                mode       : new AssetAttributeConfigurationEntry("boolean", importedModeMeta)
            ]
        )

        when: "the configuration is imported"
        def result = AssetAttributeConfigurationService.previewImportConfiguration(target, document)

        then: "the result reports asset type mismatch separately"
        result.assetTypeMismatch.expected == target.type
        result.assetTypeMismatch.actual == RoomAsset.DESCRIPTOR.name

        and: "the result reports importable, missing, and type-mismatched attributes"
        result.importableAttributes*.name == ["temperature"]
        result.missingAttributes*.name == ["missing"]
        result.typeMismatches*.name == ["mode"]
        result.typeMismatches[0].importedType == "boolean"
        result.typeMismatches[0].targetType == "text"

        and: "the compatible attribute meta is replaced in the patched attributes"
        def patchedTemperature = result.patchedAttributes.get("temperature").get()
        patchedTemperature.type.name == "number"
        patchedTemperature.value.orElse(null) == 18d
        patchedTemperature.meta.getValue(READ_ONLY).orElse(false)
        !patchedTemperature.meta.has(LABEL)
        patchedTemperature.meta.get("agentLink").get().value.orElse(null) == [id: "agent-1", unitId: 1]

        and: "non-imported attributes are preserved in the patched attributes"
        result.patchedAttributes.get("humidity").get().meta.getValue(LABEL).orElse(null) == "Existing humidity label"
        result.patchedAttributes.get("mode").get().meta.getValue(LABEL).orElse(null) == "Existing mode label"

        and: "the target draft asset was not mutated by preview generation"
        target.getAttribute("temperature").get().meta.getValue(LABEL).orElse(null) == "Old label"
        !target.getAttribute("temperature").get().meta.getValue(READ_ONLY).orElse(false)
    }

    def "Import resolves generic parameters before previewing compatible patch"() {
        given: "a target draft asset"
        def target = new ThingAsset("Target")
            .addOrReplaceAttributes(
                new Attribute<>("error", ValueType.BOOLEAN)
                    .addOrReplaceMeta(new MetaItem<>(LABEL, "Old error label")),
                new Attribute<>("pumpDown", ValueType.BOOLEAN)
                    .addOrReplaceMeta(new MetaItem<>(LABEL, "Old pump down label"))
            )

        and: "an import document with generic agent link values removed"
        def document = new AssetAttributeConfigurationDocument(
            AssetAttributeConfigurationDocument.CURRENT_VERSION,
            target.type,
            [
                error   : new AssetAttributeConfigurationEntry("boolean", new MetaMap([
                    new MetaItem<>("agentLink", null, [type: "ModbusAgentLink"])
                ])),
                pumpDown: new AssetAttributeConfigurationEntry("boolean", new MetaMap([
                    new MetaItem<>("agentLink", null, [type: "ModbusAgentLink"])
                ]))
            ],
            [
                agentLinkId    : new AssetAttributeConfigurationGenericParameter(
                    "text",
                    [
                        "attributes.error.meta.agentLink.id",
                        "attributes.pumpDown.meta.agentLink.id"
                    ]
                ),
                agentLinkUnitId: new AssetAttributeConfigurationGenericParameter(
                    "number",
                    [
                        "attributes.error.meta.agentLink.unitId",
                        "attributes.pumpDown.meta.agentLink.unitId"
                    ]
                )
            ]
        )

        when: "the generic values are supplied"
        def result = AssetAttributeConfigurationService.previewImportConfiguration(
            target,
            document,
            [
                agentLinkId    : "agent-2",
                agentLinkUnitId: 3
            ]
        )

        then: "the generic values are injected into every referenced import path"
        result.importableAttributes*.name == ["error", "pumpDown"]
        result.patchedAttributes.get("error").get().meta.get("agentLink").get().value.orElse(null) == [
            type  : "ModbusAgentLink",
            id    : "agent-2",
            unitId: 3
        ]
        result.patchedAttributes.get("pumpDown").get().meta.get("agentLink").get().value.orElse(null) == [
            type  : "ModbusAgentLink",
            id    : "agent-2",
            unitId: 3
        ]

        and: "the uploaded configuration document is not mutated"
        document.attributes.error.meta.get("agentLink").get().value.orElse(null) == [type: "ModbusAgentLink"]
        document.attributes.pumpDown.meta.get("agentLink").get().value.orElse(null) == [type: "ModbusAgentLink"]

        when: "a generic value is missing"
        AssetAttributeConfigurationService.previewImportConfiguration(
            target,
            document,
            [
                agentLinkId: "agent-2"
            ]
        )

        then: "the import fails"
        thrown(IllegalArgumentException)

        when: "a generic value does not match the declared type"
        AssetAttributeConfigurationService.previewImportConfiguration(
            target,
            document,
            [
                agentLinkId    : 3,
                agentLinkUnitId: 3
            ]
        )

        then: "the import fails"
        thrown(IllegalArgumentException)

        when: "a generic parameter type does not match the target path schema"
        AssetAttributeConfigurationService.previewImportConfiguration(
            target,
            new AssetAttributeConfigurationDocument(
                AssetAttributeConfigurationDocument.CURRENT_VERSION,
                target.type,
                [
                    error: new AssetAttributeConfigurationEntry("boolean", new MetaMap([
                        new MetaItem<>("agentLink", null, [type: "ModbusAgentLink"])
                    ]))
                ],
                [
                    agentLinkUnitId: new AssetAttributeConfigurationGenericParameter(
                        "text",
                        ["attributes.error.meta.agentLink.unitId"]
                    )
                ]
            ),
            [
                agentLinkUnitId: "3"
            ]
        )

        then: "the import fails"
        thrown(IllegalArgumentException)
    }

    def "Import rejects unsupported and unusable configuration documents"() {
        given: "a target draft asset"
        def target = new ThingAsset("Target")
            .addOrReplaceAttributes(new Attribute<>("temperature", ValueType.NUMBER))

        when: "the version is unsupported"
        AssetAttributeConfigurationService.previewImportConfiguration(
            target,
            new AssetAttributeConfigurationDocument(2, target.type, [
                temperature: new AssetAttributeConfigurationEntry("number", new MetaMap([new MetaItem<>(READ_ONLY, true)]))
            ])
        )

        then: "the import fails"
        thrown(IllegalArgumentException)

        when: "there are no importable attributes"
        AssetAttributeConfigurationService.previewImportConfiguration(
            target,
            new AssetAttributeConfigurationDocument(AssetAttributeConfigurationDocument.CURRENT_VERSION, target.type, [
                missing: new AssetAttributeConfigurationEntry("number", new MetaMap([new MetaItem<>(READ_ONLY, true)]))
            ])
        )

        then: "the import fails"
        thrown(IllegalArgumentException)

        when: "generic parameters have not yet been resolved"
        AssetAttributeConfigurationService.previewImportConfiguration(
            target,
            new AssetAttributeConfigurationDocument(
                AssetAttributeConfigurationDocument.CURRENT_VERSION,
                target.type,
                [
                    temperature: new AssetAttributeConfigurationEntry("number", new MetaMap([new MetaItem<>(READ_ONLY, true)]))
                ],
                [
                    agentLinkId: new AssetAttributeConfigurationGenericParameter(
                        "text",
                        ["attributes.temperature.meta.agentLink.id"]
                    )
                ]
            )
        )

        then: "the import fails"
        thrown(IllegalArgumentException)

        when: "an attribute entry is malformed"
        AssetAttributeConfigurationService.previewImportConfiguration(
            target,
            new AssetAttributeConfigurationDocument(AssetAttributeConfigurationDocument.CURRENT_VERSION, target.type, [
                temperature: new AssetAttributeConfigurationEntry(null, new MetaMap([new MetaItem<>(READ_ONLY, true)]))
            ])
        )

        then: "the import fails"
        thrown(IllegalArgumentException)
    }

    def "Configuration document JSON is user editable and omits attribute names"() {
        given: "an export document"
        def asset = new ThingAsset("Indoor unit")
            .addOrReplaceAttributes(
                new Attribute<>("temperature", ValueType.NUMBER)
                    .addOrReplaceMeta(new MetaItem<>(READ_ONLY, true))
            )
        def document = AssetAttributeConfigurationService.exportConfiguration(asset, new AssetAttributeConfigurationExportRequest(null))

        when: "the document is serialized"
        def json = ValueUtil.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(document)

        then: "the JSON contains the expected editable shape"
        json.contains('"version"')
        json.contains('"assetType"')
        json.contains('"attributes"')
        json.contains('"temperature"')
        json.contains('"type" : "number"')
        json.contains('"readOnly" : true')
        !json.contains('"name"')
    }
}
