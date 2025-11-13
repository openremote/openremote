/*
Example group summation rule for the summation of child asset attributes to parent asset attributes:
 - The child and parent attributes must have the 'Rule state' configuration item added.
 - You must specify the parent asset ID below.
 */

package org.openremote.test.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets
import org.openremote.model.rules.Notifications
import org.openremote.model.rules.Users

import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Users users = binding.users
Notifications notifications = binding.notifications
Assets assets = binding.assets

// Put the parent asset ID here:
String parentAssetId = "47zxEJ7nZCPbOHLvYKWNs6"

// Put the attribute names for summation here (or leave empty to include all child asset attribute names with 'Rule state'):
String[] attributeNames = []
//String[] attributeNames = ["attributeName1", "attributeName2"] // Example

// Put if the child and parent asset type must match here:
boolean matchParentAssetType = true

rules.add()
        .name("Group summation rule")
        .when({ facts ->

            // Check if parent asset ID is valid
            Optional<AttributeInfo> parent = facts.matchFirstAssetState(new AssetQuery().ids(parentAssetId))

            if (parent.isEmpty()) {
                LOG.warning("No Parent Asset found with ID: '" + parentAssetId + "'; Check Parent Asset ID and if the rule state configuration is added to the attribute")
                return false
            }

            // Find attribute changes in group for your attribute names
            List<AttributeInfo> changes = facts
                    .matchAssetState(new AssetQuery().parents(parentAssetId).attributeNames(attributeNames))
                    .filter { attributeInfo ->
                        boolean timestampChanged = false

                        // Get previous attribute state from facts
                        Optional<AttributeInfo> previous = facts.matchFirst(attributeInfo.id + attributeInfo.name)

                        // Check if attribute timestamp has been updated (attribute value can be the same)
                        if (attributeInfo.timestamp > previous.map { it.timestamp }.orElse(0)) {
                            timestampChanged = true
                        }

                        return timestampChanged
                    }
                    .toList()

            // Get attribute names of changes
            String[] changesAttributeNames = changes
                    .collect { it.name }
                    .unique()

            // Find all relevant children attributes
            List<AttributeInfo> childrenAttributes = Collections.synchronizedList(new ArrayList<>())

            if (changesAttributeNames.length > 0) {
                childrenAttributes = facts
                        .matchAssetState(new AssetQuery().parents(parentAssetId).attributeNames(changesAttributeNames))
                        .toList()
            }

            // Bind attribute info for the then trigger
            if (!changes.isEmpty()) {
                facts.bind("parent", parent)
                facts.bind("changes", changes)

                if (!childrenAttributes.isEmpty()) {
                    facts.bind("changesAttributeNames", changesAttributeNames)
                    facts.bind("childrenAttributes", childrenAttributes)
                }
            }

            // Trigger the rule 'then' action if there are changes to process
            return !changes.isEmpty()
        })
        .then({ facts ->
            Optional<AttributeInfo> parent = facts.bound("parent")
            List<AttributeInfo> changes = facts.bound("changes")
            String[] changesAttributeNames = facts.bound("changesAttributeNames")
            List<AttributeInfo> childrenAttributes = facts.bound("childrenAttributes")

            // Create fact for each attribute change
            changes.forEach { attributeInfo -> facts.put(attributeInfo.id + attributeInfo.name, attributeInfo as Object) }

            // Stop when group is empty
            if (childrenAttributes == null) {
                return
            }

            // Filter children attributes
            List<AttributeInfo> childrenAttributesFiltered = childrenAttributes

            if (matchParentAssetType) {
                childrenAttributesFiltered = childrenAttributes
                        .findAll { it.assetType == parent.get().assetType }
            }

            // Sum values per attribute
            for (attributeName in changesAttributeNames) {
                // Validate value type
                boolean validValueType = childrenAttributesFiltered
                        .find { it.name == attributeName }
                        .collect { it.type.type.name }
                        .any { it in ["java.lang.Double", "java.lang.Integer", "java.lang.Long", "java.math.BigDecimal", "java.math.BigInteger"] }

                if (validValueType) {
                    def sum = childrenAttributesFiltered
                            .findAll { it.name == attributeName }
                            .findAll { it.value.isPresent() }
                            .collect { it.value.get() }
                            .sum()

                    // Update parent
                    if (sum != null) {
                        assets.dispatch(parentAssetId, attributeName, sum)
                    }
                }
            }
        })
