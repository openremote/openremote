/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.http;

/**
 * Reusable JSON examples for common OpenAPI request bodies.
 */
public final class OpenApiExamples {

    public static final String ASSET_CREATE = """
        {
          "type": "ThingAsset",
          "name": "Boiler room sensor",
          "realm": "building",
          "attributes": {
            "temperature": {
              "name": "temperature",
              "type": "number",
              "value": 21.5
            },
            "humidity": {
              "name": "humidity",
              "type": "number",
              "value": 48
            }
          }
        }
        """;

    public static final String ASSET_UPDATE = """
        {
          "id": "7A6p4AnLTkKxJUCQAAABAA",
          "version": 3,
          "type": "ThingAsset",
          "name": "Boiler room sensor",
          "realm": "building",
          "accessPublicRead": false,
          "attributes": {
            "temperature": {
              "name": "temperature",
              "type": "number",
              "value": 22.1
            },
            "humidity": {
              "name": "humidity",
              "type": "number",
              "value": 48
            }
          }
        }
        """;

    public static final String ASSET_QUERY = """
        {
          "realm": {
            "name": "building"
          },
          "types": [
            "ThingAsset"
          ],
          "names": [
            {
              "match": "CONTAINS",
              "caseSensitive": false,
              "value": "sensor"
            }
          ],
          "select": {
            "attributes": [
              "temperature",
              "humidity"
            ]
          },
          "orderBy": {
            "property": "NAME",
            "descending": false
          },
          "limit": 100,
          "offset": 0
        }
        """;

    public static final String ATTRIBUTE_VALUE = "21.5";

    public static final String ATTRIBUTE_STATES = """
        [
          {
            "ref": {
              "id": "7A6p4AnLTkKxJUCQAAABAA",
              "name": "temperature"
            },
            "value": 21.5
          },
          {
            "ref": {
              "id": "7A6p4AnLTkKxJUCQAAABAA",
              "name": "humidity"
            },
            "value": 48
          }
        ]
        """;

    public static final String DATAPOINT_ALL_QUERY = """
        {
          "type": "all",
          "fromTime": "2026-01-01T00:00:00Z",
          "toTime": "2026-01-02T00:00:00Z"
        }
        """;

    public static final String DATAPOINT_INTERVAL_QUERY = """
        {
          "type": "interval",
          "fromTimestamp": 1767225600000,
          "toTimestamp": 1767312000000,
          "interval": "1 hour",
          "formula": "AVG",
          "gapFill": true
        }
        """;

    public static final String DATAPOINT_LTTB_QUERY = """
        {
          "type": "lttb",
          "fromTimestamp": 1767225600000,
          "toTimestamp": 1767312000000,
          "amountOfPoints": 500
        }
        """;

    public static final String DATAPOINT_NEAREST_QUERY = """
        {
          "type": "nearest",
          "fromTimestamp": 1767312000
        }
        """;

    public static final String USER_QUERY = """
        {
          "realmPredicate": {
            "name": "building"
          },
          "usernames": [
            {
              "match": "CONTAINS",
              "caseSensitive": false,
              "value": "alex"
            }
          ],
          "serviceUsers": false,
          "select": {
            "basic": true
          },
          "orderBy": {
            "property": "USERNAME",
            "descending": false
          },
          "limit": 100,
          "offset": 0
        }
        """;

    public static final String USER_CREATE = """
        {
          "username": "alex",
          "firstName": "Alex",
          "lastName": "Morgan",
          "email": "alex@example.com",
          "enabled": true,
          "attributes": {
            "locale": [
              "en"
            ]
          }
        }
        """;

    public static final String ALARM_CREATE = """
        {
          "title": "Temperature threshold exceeded",
          "content": "Boiler room temperature reached 42 °C.",
          "severity": "HIGH",
          "status": "OPEN",
          "realm": "building"
        }
        """;

    public static final String ALARM_ASSET_LINK_SINGLE = """
        [
          {
            "id": {
              "realm": "building",
              "alarmId": 1234,
              "assetId": "7A6p4AnLTkKxJUCQAAABAA"
            }
          }
        ]
        """;

    public static final String ALARM_ASSET_LINK_MULTIPLE = """
        [
          {
            "id": {
              "realm": "building",
              "alarmId": 1234,
              "assetId": "7A6p4AnLTkKxJUCQAAABAA"
            }
          },
          {
            "id": {
              "realm": "building",
              "alarmId": 1234,
              "assetId": "2Qjr4AnLTkKxJUCQAAACAA"
            }
          }
        ]
        """;

    public static final String ALARM_ASSET_LINK_INVALID_MULTIPLE_ALARMS = """
        [
          {
            "id": {
              "realm": "building",
              "alarmId": 1234,
              "assetId": "7A6p4AnLTkKxJUCQAAABAA"
            }
          },
          {
            "id": {
              "realm": "building",
              "alarmId": 5678,
              "assetId": "2Qjr4AnLTkKxJUCQAAACAA"
            }
          }
        ]
        """;

    public static final String PUSH_NOTIFICATION = """
        {
          "name": "High temperature alert",
          "message": {
            "type": "push",
            "title": "Temperature alert",
            "body": "Boiler room temperature reached 42 °C.",
            "priority": "HIGH",
            "data": {
              "assetId": "7A6p4AnLTkKxJUCQAAABAA"
            }
          },
          "targets": [
            {
              "type": "USER",
              "id": "2f1c17e5-72b8-4dbe-9f8d-c49e66f82e10",
              "locale": "en"
            }
          ]
        }
        """;

    public static final String DASHBOARD_QUERY = """
        {
          "realm": {
            "name": "building"
          },
          "names": [
            {
              "match": "CONTAINS",
              "caseSensitive": false,
              "value": "operations"
            }
          ],
          "conditions": {
            "dashboard": {
              "access": [
                "PUBLIC",
                "SHARED"
              ]
            }
          },
          "start": 0,
          "limit": 50
        }
        """;

    public static final String DASHBOARD_CREATE = """
        {
          "realm": "building",
          "displayName": "Operations overview",
          "template": {
            "columns": 12,
            "refreshInterval": "FIVE_MIN",
            "screenPresets": [
              {
                "id": "desktop",
                "displayName": "Desktop",
                "breakpoint": 1024,
                "scalingPreset": "KEEP_LAYOUT"
              }
            ],
            "widgets": []
          }
        }
        """;

    public static final String DASHBOARD_UPDATE = """
        {
          "id": "operations-overview",
          "version": 2,
          "realm": "building",
          "ownerId": "2f1c17e5-72b8-4dbe-9f8d-c49e66f82e10",
          "access": "SHARED",
          "displayName": "Building operations overview",
          "template": {
            "columns": 12,
            "refreshInterval": "FIVE_MIN",
            "screenPresets": [
              {
                "id": "desktop",
                "displayName": "Desktop",
                "breakpoint": 1024,
                "scalingPreset": "KEEP_LAYOUT"
              }
            ],
            "widgets": []
          }
        }
        """;

    public static final String ASSET_RULESET_CREATE = """
        {
          "assetId": "7A6p4AnLTkKxJUCQAAABAA",
          "name": "High temperature alert",
          "lang": "JSON",
          "enabled": true,
          "accessPublicRead": false,
          "rules": "{\\"rules\\":[{\\"name\\":\\"High temperature alert\\",\\"when\\":{\\"operator\\":\\"OR\\",\\"groups\\":[{\\"operator\\":\\"AND\\",\\"items\\":[{\\"assets\\":{\\"ids\\":[\\"7A6p4AnLTkKxJUCQAAABAA\\"],\\"attributes\\":{\\"items\\":[{\\"name\\":{\\"predicateType\\":\\"string\\",\\"match\\":\\"EXACT\\",\\"value\\":\\"temperature\\"},\\"value\\":{\\"predicateType\\":\\"number\\",\\"operator\\":\\"GREATER_THAN\\",\\"value\\":40}}]}}}]}]},\\"then\\":[{\\"action\\":\\"alarm\\",\\"alarm\\":{\\"title\\":\\"High temperature\\",\\"content\\":\\"%TRIGGER_ASSETS%\\",\\"severity\\":\\"HIGH\\",\\"status\\":\\"OPEN\\"}}]}]}"
        }
        """;

    private OpenApiExamples() {
    }
}
