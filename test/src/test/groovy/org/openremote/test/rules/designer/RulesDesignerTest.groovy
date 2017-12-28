package org.openremote.test.rules.designer

/*
import org.drools.workbench.models.commons.backend.oracle.PackageDataModelOracleImpl
import org.drools.workbench.models.commons.backend.rule.ActionCallMethodBuilder
import org.drools.workbench.models.commons.backend.rule.RuleModelDRLPersistenceImpl
import org.drools.workbench.models.datamodel.imports.Import
import org.drools.workbench.models.datamodel.oracle.DataType
import org.drools.workbench.models.datamodel.rule.*
import org.hamcrest.Matchers
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.AssetType
import org.openremote.model.rules.Assets
*/
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import java.util.logging.Logger


/*
TODO Build DRL programmatically

Requires in test/build.xml:

    compile "org.hamcrest:hamcrest-library:1.3"

Requires in manager/server/build.xml

    compile "org.drools:drools-workbench-models-commons:$droolsVersion"

TODO Examples can be found:

https://github.com/kiegroup/drools-wb/tree/master/drools-wb-screens/drools-wb-guided-rule-editor
https://github.com/kiegroup/drools/tree/master/drools-workbench-models/drools-workbench-models-commons/src/test/java/org/drools/workbench/models/commons/backend/rule
https://github.com/kiegroup/drools/blob/master/drools-workbench-models/drools-workbench-models-commons/src/test/java/org/drools/workbench/models/commons/backend/rule/RuleModelDRLPersistenceUnmarshallingTest.java
https://github.com/kiegroup/drools/blob/master/drools-workbench-models/drools-workbench-models-guided-dtable/src/test/java/org/drools/workbench/models/guided/dtable/backend/BRLRuleModelTest.java

========================================================

package org.openremote.test.rules;

import org.openremote.model.asset.*
import org.openremote.model.attribute.*
import org.openremote.model.value.*
import org.openremote.model.value.Values

global java.util.logging.Logger LOG;
global org.openremote.model.rules.Assets assets;

rule "Switch room lights off when apartment ALL LIGHTS OFF push-button is pressed"
when
    // In an asset of type RESIDENCE the All Lights Off push-button is pressed, which sends a 'true' value
    $e: AssetEvent(type == AssetType.RESIDENCE, attributeName == "allLightsOffSwitch", valueAsBoolean == true)
then
    assets.query() // Execute a query to get "other" assets
        .parent($e.getId()) // Where parent is the apartment in LHS
        .type(AssetType.ROOM) // And the children are of type ROOM
        .applyResults(assetIds -> assetIds.forEach(assetId -> assets.dispatch( // For each asset ID in result...
            new AttributeEvent(assetId, "lightSwitch", Values.create(false)) // Dispatch an event that turns the switch off
    )));
end

========================================================
 */

class RulesDesignerTest extends Specification implements ManagerContainerTrait {
/*
    def "Unmarshall DRL into model"() {

        when: "some DRL is parsed"

        def drl = getClass().getResource("/demo/rules/DemoApartmentAllLightsOff.drl").text

        def globals = ["LOG": Logger.class.getName(), "assets": Assets.class.getName()]
        def dmo = new PackageDataModelOracleImpl()
        dmo.addPackageGlobals(globals)

        def model = RuleModelDRLPersistenceImpl.getInstance().unmarshal(drl, Collections.emptyList(), dmo)

        then: "the result should be ok"
        def rhs0 = model.rhs[0] as ActionCallMethod
        rhs0.methodName == "query"
        rhs0.variable == "assets"
        def drlOut = RuleModelDRLPersistenceImpl.instance.marshal(model)
        println(drlOut)
        // Matchers.equalToIgnoringWhiteSpace(drl).matches(drlOut)
    }

    def "Marshall model into DRL"() {

        when: "a rules model is build"

        def model = new RuleModel()
        model.packageName = "org.openremote.test.rules"
        model.imports.addImport(new Import("org.openremote.model.asset.*"))
        model.imports.addImport(new Import("org.openremote.model.attribute.*"))
        model.imports.addImport(new Import("org.openremote.model.value.*"))
        model.imports.addImport(new Import("org.openremote.model.value.Values"))

        def globals = ["LOG": Logger.class.getName(), "assets": Assets.class.getName()]
        def dmo = new PackageDataModelOracleImpl()
        dmo.addPackageGlobals(globals)
        def actionCallMethodBuilder = new ActionCallMethodBuilder(model, dmo, true, new HashMap<String, String>())

        model.name = "Switch room lights off when apartment ALL LIGHTS OFF push-button is pressed"

        def assetEventFactPattern = new FactPattern(AssetEvent.class.getSimpleName())
        def assetEventFactPatternConstraints = new CompositeFieldConstraint()
        assetEventFactPatternConstraints.setCompositeJunctionType(CompositeFieldConstraint.COMPOSITE_TYPE_AND)
        assetEventFactPattern.addConstraint(assetEventFactPatternConstraints)
        model.addLhsItem(assetEventFactPattern)
        assetEventFactPattern.setBoundName("\$e")

        def assetTypeConstraint = new SingleFieldConstraint()
        assetEventFactPatternConstraints.addConstraint(assetTypeConstraint)
        assetTypeConstraint.setFieldName("type")
        assetTypeConstraint.setFieldType(DataType.TYPE_COMPARABLE)
        assetTypeConstraint.setOperator("==")
        assetTypeConstraint.setConstraintValueType(BaseSingleFieldConstraint.TYPE_ENUM)
        assetTypeConstraint.setValue(AssetType.class.getSimpleName() + "." + AssetType.RESIDENCE.name())

        def attributeNameConstraint = new SingleFieldConstraint()
        assetEventFactPatternConstraints.addConstraint(attributeNameConstraint)
        attributeNameConstraint.setFieldName("attributeName")
        attributeNameConstraint.setFieldType(DataType.TYPE_STRING)
        attributeNameConstraint.setOperator("==")
        attributeNameConstraint.setConstraintValueType(SingleFieldConstraint.TYPE_LITERAL)
        attributeNameConstraint.setValue("allLightsOffSwitch")
        // TODO Find out how to bind safe values
        // attributeNameConstraint.setValue("allLightsOffSwitch \" && injectionAttack == possible")

        def attributeValueConstraint = new SingleFieldConstraint()
        assetEventFactPatternConstraints.addConstraint(attributeValueConstraint)
        attributeValueConstraint.setFieldName("valueAsBoolean")
        attributeValueConstraint.setFieldType(DataType.TYPE_BOOLEAN)
        attributeValueConstraint.setOperator("==")
        attributeValueConstraint.setConstraintValueType(BaseSingleFieldConstraint.TYPE_LITERAL)
        attributeValueConstraint.setValue(Boolean.TRUE.toString())

        def dispatchAction = actionCallMethodBuilder.get("assets", "dispatch", "foo")
        model.addRhsItem(new ExpressionGlobalVariable("assets", Assets.class.getName(), Assets.class.getName()))
        model.addRhsItem(dispatchAction)

        String drl = RuleModelDRLPersistenceImpl.instance.marshal(model)
        println(drl)

        then: "it should be good"
        model.lhs.length == 1
        model.lhs[0] == assetEventFactPattern
    }
    */
}
