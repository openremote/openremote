/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.setup.jsonforms;

import static org.openremote.model.Constants.MASTER_REALM;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeLink;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.auth.OAuthPasswordGrant;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.calendar.CalendarEvent;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.util.TimeUtil;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueFormat;
import org.openremote.model.value.ValueType;

/**
 * Fills the master realm with everything {@code or-json-forms} renders, so the editor can be
 * exercised by hand.
 *
 * <p>Both sets are discovered from the asset model rather than listed here, so a value type or a
 * meta item added later shows up without this setup being touched. That makes the value types a
 * superset of what the forms render: a handful of them are objects that the simple input has a
 * dedicated type for, such as the colour, cron and duration ones, and seeing those side by side is
 * what shows where the boundary runs. The values below are only the ones worth seeing filled in;
 * anything else is created empty, which is the state that shows the add parameter and add item
 * dialogs.
 *
 * <p>Creates, under a "JSON Forms" parent:
 *
 * <ul>
 *   <li>"Agents", holding one disabled asset of every agent type
 *   <li>"Complex Value Types", holding an attribute of every complex value type
 *   <li>"Complex Configuration Items", holding an attribute per complex meta item, and one per
 *       agent link type pointing at the matching agent
 * </ul>
 */
public class ManagerJsonFormsSetup extends ManagerSetup {

  private static final Logger LOG = Logger.getLogger(ManagerJsonFormsSetup.class.getName());

  /** 2026-01-02T10:30 in Europe/Amsterdam. */
  private static final Date WHEN = Date.from(Instant.parse("2026-01-02T09:30:00Z"));

  /** Rotterdam, as longitude and latitude. */
  private static final GeoJSONPoint WHERE = new GeoJSONPoint(4.4840943240837134, 51.91550228909358);

  /**
   * The value the attribute of a configuration item carries, keyed by item. Only the constraints
   * need one, since a constraint is applied to the value of the attribute that holds it, and the
   * ones on show reject a value that is absent, blank or outside their set.
   */
  private static final Map<String, String> CONFIGURATION_ITEM_VALUES =
      Map.of(MetaItemType.CONSTRAINTS.getName(), "one");

  /**
   * The fields an agent link rejects being stored without, keyed by link type. A link type added
   * later only belongs here once it declares a field of its own that may not be null.
   */
  private static final Map<String, Map<String, Object>> REQUIRED_AGENT_LINK_FIELDS =
      Map.of(
          "BluetoothMeshAgentLink", Map.of("appKeyIndex", 0, "modelName", "Generic OnOff Server"),
          "KNXAgentLink", Map.of("dpt", "1.001"),
          "OpenWeatherMapAgentLink", Map.of("weatherProperty", "TEMPERATURE"),
          "SNMPAgentLink", Map.of("oid", "1.3.6.1.2.1.1.1.0"),
          "VelbusAgentLink", Map.of("deviceAddress", 1, "deviceValueLink", "CH1"));

  public ManagerJsonFormsSetup(Container container) {
    super(container);
  }

  @Override
  public void onStart() throws Exception {
    super.onStart();

    ThingAsset root = new ThingAsset("JSON Forms");
    root.setRealm(MASTER_REALM);
    root.setLocation(WHERE);
    ThingAsset parent = assetStorageService.merge(root);

    Map<Class<? extends AgentLink<?>>, String> agentIds = createAgents(parent);
    createValueTypeAsset(parent);
    createConfigurationItemAsset(parent, agentIds);
  }

  /**
   * Creates one asset of every agent type, disabled so that no protocol tries to connect, and
   * returns the id of an agent for each agent link type.
   */
  protected Map<Class<? extends AgentLink<?>>, String> createAgents(Asset<?> parent)
      throws Exception {
    ThingAsset group = new ThingAsset("Agents");
    group.setParent(parent);
    ThingAsset stored = assetStorageService.merge(group);

    Map<Class<? extends AgentLink<?>>, String> agentIds = new LinkedHashMap<>();

    List<? extends AgentDescriptor<?, ?, ?>> descriptors =
        Arrays.stream(ValueUtil.getAssetDescriptors(null))
            .filter(AgentDescriptor.class::isInstance)
            .map(descriptor -> (AgentDescriptor<?, ?, ?>) descriptor)
            .sorted(Comparator.comparing(AssetDescriptor::getName))
            .toList();

    for (AgentDescriptor<?, ?, ?> descriptor : descriptors) {
      Asset<?> agent =
          (Asset<?>)
              descriptor.getType().getConstructor(String.class).newInstance(descriptor.getName());
      agent.setParent(stored);
      agent.getAttributes().getOrCreate(Agent.DISABLED).setValue(true);

      Asset<?> storedAgent = assetStorageService.merge(agent);
      agentIds.putIfAbsent(descriptor.getAgentLinkClass(), storedAgent.getId());
    }

    LOG.info(
        "Created " + descriptors.size() + " agents covering " + agentIds.size() + " link types");
    return agentIds;
  }

  /** Creates an attribute of every complex value type that an attribute is allowed to hold. */
  protected void createValueTypeAsset(Asset<?> parent) {
    ThingAsset asset = new ThingAsset("Complex Value Types");
    asset.setParent(parent);

    Map<String, Object> values = getValues();

    List<ValueDescriptor<?>> descriptors =
        ValueUtil.getValueDescriptors().values().stream()
            .filter(descriptor -> !descriptor.isMetaUseOnly())
            .filter(ManagerJsonFormsSetup::isComplex)
            .sorted(Comparator.comparing(ValueDescriptor::getName))
            .toList();

    for (ValueDescriptor<?> descriptor : descriptors) {
      addAttribute(asset, descriptor, values.get(descriptor.getName()));
    }

    // The array control is only reachable through an array, and nesting one shows it inside itself
    addAttribute(asset, ValueType.TEXT.asArray(), new String[] {"first", "second"});
    addAttribute(asset, ValueType.NUMBER.asArray(), new Double[] {1.5, 2.5});
    addAttribute(asset, ValueType.GEO_JSON_POINT.asArray(), new GeoJSONPoint[] {WHERE});
    addAttribute(asset, ValueType.JSON_OBJECT.asArray(), null);
    addAttribute(asset, ValueType.TEXT.asArray().asArray(), null);

    ThingAsset stored = assetStorageService.merge(asset);
    LOG.info("Created " + stored.getAttributes().size() + " complex value type attributes");
  }

  /**
   * Creates an attribute per complex configuration item, and one per agent link type so that every
   * variant of the agent link form can be opened.
   */
  protected void createConfigurationItemAsset(
      Asset<?> parent, Map<Class<? extends AgentLink<?>>, String> agentIds) {
    ThingAsset asset = new ThingAsset("Complex Configuration Items");
    asset.setParent(parent);

    Map<String, Object> metaValues = getMetaValues(parent.getId());

    List<MetaItemDescriptor<?>> descriptors =
        ValueUtil.getMetaItemDescriptors().values().stream()
            .filter(descriptor -> isComplex(descriptor.getType()))
            .sorted(Comparator.comparing(MetaItemDescriptor::getName))
            .toList();

    List<String> uncovered = new ArrayList<>();

    for (MetaItemDescriptor<?> descriptor : descriptors) {
      // The agent link gets an attribute per link type below, where it can carry a real agent
      if (descriptor.getName().equals(MetaItemType.AGENT_LINK.getName())) {
        continue;
      }

      Object value = metaValues.get(descriptor.getName());
      if (value == null) {
        uncovered.add(descriptor.getName());
        continue;
      }

      Attribute<String> attribute =
          new Attribute<>(
              descriptor.getName(),
              ValueType.TEXT,
              CONFIGURATION_ITEM_VALUES.get(descriptor.getName()));
      attribute.addOrReplaceMeta(getMetaItem(descriptor.getName(), descriptor.getType(), value));
      asset.getAttributes().addOrReplace(attribute);
    }

    for (Map.Entry<Class<? extends AgentLink<?>>, String> entry : agentIds.entrySet()) {
      AgentLink<?> link = getAgentLink(entry.getKey(), entry.getValue());
      Attribute<?> attribute =
          new Attribute<>(uncapitalize(entry.getKey().getSimpleName()), ValueType.TEXT);
      attribute.addOrReplaceMeta(
          getMetaItem(
              MetaItemType.AGENT_LINK.getName(), MetaItemType.AGENT_LINK.getType(), link));
      asset.getAttributes().addOrReplace(attribute);
    }

    ThingAsset stored = assetStorageService.merge(asset);
    LOG.info("Created " + stored.getAttributes().size() + " complex configuration item attributes");

    if (!uncovered.isEmpty()) {
      LOG.warning(
          "No value set up for complex configuration items: " + String.join(", ", uncovered));
    }
  }

  /** An attribute reaches the forms when its value is an object or an array. */
  protected static boolean isComplex(ValueDescriptor<?> descriptor) {
    return descriptor.isArray() || "object".equals(descriptor.getJsonType());
  }

  /**
   * Builds an agent link of the given type for an agent. The link is built from JSON rather than a
   * constructor, because the constructors differ per protocol, and it carries a value for every
   * field the link requires, since an invalid one is rejected when the asset is stored.
   */
  protected static AgentLink<?> getAgentLink(Class<? extends AgentLink<?>> type, String agentId) {
    Map<String, Object> link =
        new LinkedHashMap<>(
            REQUIRED_AGENT_LINK_FIELDS.getOrDefault(type.getSimpleName(), Map.of()));
    link.put("id", agentId);
    // The links are polymorphic on a type id, which is the simple class name while none of them
    // names its own
    link.put("type", type.getSimpleName());
    return ValueUtil.JSON.convertValue(link, type);
  }

  /** Builds a meta item of a descriptor whose value type is only known as a wildcard. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected static MetaItem<?> getMetaItem(
      String name, ValueDescriptor<?> valueDescriptor, Object value) {
    MetaItem item = new MetaItem<>(name, valueDescriptor);
    item.setValue(value);
    return item;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected static void addAttribute(Asset<?> asset, ValueDescriptor<?> descriptor, Object value) {
    Attribute attribute = new Attribute<>(getAttributeName(descriptor), descriptor);
    if (value != null) {
      attribute.setValue(value);
    }
    asset.getAttributes().addOrReplace(attribute);
  }

  /**
   * Turns a value descriptor name into an attribute name, where only word characters are allowed.
   */
  protected static String getAttributeName(ValueDescriptor<?> descriptor) {
    String name = uncapitalize(descriptor.getName().replaceAll("\\W", ""));
    int dimensions = descriptor.getArrayDimensions() == null ? 0 : descriptor.getArrayDimensions();
    return name + "Array".repeat(dimensions);
  }

  /**
   * Lowers the first letter, leaving names that start on an abbreviation such as JSONObject alone.
   */
  protected static String uncapitalize(String name) {
    if (name.isEmpty() || (name.length() > 1 && Character.isUpperCase(name.charAt(1)))) {
      return name;
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  /** The complex value types that say more about the forms when they arrive filled in. */
  protected static Map<String, Object> getValues() {
    ValueType.StringMap textMap = new ValueType.StringMap();
    textMap.put("first", "one");
    textMap.put("second", "two");

    ValueType.IntegerMap integerMap = new ValueType.IntegerMap();
    integerMap.put("first", 1);

    ValueType.MultivaluedStringMap multivaluedTextMap = new ValueType.MultivaluedStringMap();
    multivaluedTextMap.put("first", List.of("one", "two"));

    return Map.of(
        ValueType.TEXT_MAP.getName(), textMap,
        ValueType.INTEGER_MAP.getName(), integerMap,
        ValueType.MULTIVALUED_TEXT_MAP.getName(), multivaluedTextMap,
        ValueType.GEO_JSON_POINT.getName(), WHERE,
        ValueType.CALENDAR_EVENT.getName(),
            new CalendarEvent(WHEN, Date.from(WHEN.toInstant().plus(1, ChronoUnit.HOURS))),
        ValueType.USERNAME_AND_PASSWORD.getName(), new UsernamePassword("user", "secret"),
        ValueType.OAUTH_GRANT.getName(),
            new OAuthPasswordGrant(
                "https://example.com/token", "client", "secret", "scope", "user", "password"));
  }

  /** A value per complex configuration item, so each opens on a filled in form. */
  protected static Map<String, Object> getMetaValues(String assetId) {
    ValueType.ObjectMap agentLinkConfig = new ValueType.ObjectMap();
    agentLinkConfig.put("pollingMillis", 10000);
    agentLinkConfig.put("messageConvertHex", true);

    return Map.of(
        MetaItemType.AGENT_LINK_CONFIG.getName(), agentLinkConfig,
        MetaItemType.ATTRIBUTE_LINKS.getName(),
            new AttributeLink[] {
              new AttributeLink(new AttributeRef(assetId, Asset.LOCATION.getName()), null, null)
            },
        // A constraint is applied to the value of the attribute holding it, so these are the ones
        // that CONFIGURATION_ITEM_VALUES satisfies rather than the numeric and date ones
        MetaItemType.CONSTRAINTS.getName(),
            new ValueConstraint[] {
              new ValueConstraint.NotNull(),
              new ValueConstraint.NotBlank(),
              new ValueConstraint.NotEmpty(),
              new ValueConstraint.Size(1, 10),
              new ValueConstraint.Pattern("^[a-z]+$"),
              new ValueConstraint.AllowedValues("one", "two", "three")
            },
        MetaItemType.FORMAT.getName(),
            new ValueFormat().setUseGrouping(true).setMaximumFractionDigits(2),
        MetaItemType.FORECAST.getName(),
            new ForecastConfigurationWeightedExponentialAverage(
                new TimeUtil.ExtendedPeriodAndDuration("PT30M"),
                5,
                new TimeUtil.ExtendedPeriodAndDuration("PT30M"),
                24),
        MetaItemType.UNITS.getName(),
            new String[] {Constants.UNITS_DEGREE, Constants.UNITS_CELSIUS});
  }
}
