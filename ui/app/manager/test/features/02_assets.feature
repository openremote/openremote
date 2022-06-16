@OpenRemote @asset
Feature: Assets

    Background: Navigation
        Given Setup "lv2"
        
    @Desktop @add_both_asset
    Scenario Outline: Add new asset
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        Then Create a "Electricity battery asset" with name of "Battery"
        When Go to asset "Battery" info page
        Then Go to modify mode
        Then Give "30" to the "energyLevel" with type of "Positive number"
        Then Give "50" to the "power" with type of "Number "
        Then Save
        When Unselect
        Then We see the asset with name of "Battery"
        Then Create a "PV solar asset " with name of "Solar panel"
        When Go to asset "Solar panel" info page
        Then Go to modify mode
        Then Give "30" to the "panelPitch" with type of "Positive integer"
        Then Give "70" to the "power" with type of "Number "
        Then Save
        Then We see the asset with name of "Solar panel"



    @Desktop @add_asset @separate
    Scenario Outline: Add new asset
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        Then Create a "<asset>" with name of "<name>"
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Give "<value_1>" to the "<attribute_1>" with type of "<A1_type>"
        Then Give "<value_2>" to the "<attribute_2>" with type of "<A2_type>"
        Then Save
        Then We see the asset with name of "<name>"

        Examples:
            | asset                     | name        | attribute_1 | A1_type          | attribute_2 | A2_type | value_1 | value_2 |
            | Electricity battery asset | Battery     | energyLevel | Positive number  | power       | Number  | 30      | 50      |
            | PV solar asset            | Solar panel | panelPitch  | Positive integer | power       | Number  | 30      | 70      |

    @Desktop @select
    Scenario Outline: Search and select asset
        Given Setup "lv3"
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        When Search for the "<name>"
        When Select the "<name>"
        Then We see the "<name>" page

        Examples:
            | asset                     | name    |
            | Electricity battery asset | Battery |

    @Desktop @update_both
    Scenario:Update both assets
        Given Setup "lv3"
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        When Select the "Battery"
        Then Update "70" to the "powerSetpoint" with type of "number"
        When Go to modify mode
        Then Update location of 705 and 210
        Then Save
        Then Unselect
        When Select the "Solar panel"
        Then Update "100" to the "powerForecast" with type of "number"
        When Go to modify mode
        Then Update location of 600 and 200
        Then Save

    @Desktop @update @separate
    Scenario Outline: Update asset
        Given Setup "lv3"
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        When Select the "<name>"
        Then Update "<value>" to the "<attribute>" with type of "<type>"
        When Go to modify mode
        Then Update location of <location_x> and <location_y>
        Then Save

        Examples:
            | name        | attribute     | type   | value | location_x | location_y |
            | Battery     | powerSetpoint | number | 70    | 705        | 210        |
            | Solar panel | powerForecast | number | 100   | 600        | 200        |

    @Desktop @readonly_both
    Scenario: Set and cancel read-only for both assets
        Given Setup "lv3"
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        When Go to asset "Battery" info page
        Then Go to modify mode
        Then Uncheck on readonly of "energyLevel"
        Then Check on readonly of "efficiencyExport"
        Then Save
        When Go to panel page
        Then We should see a button on the right of "energyLevel"
        And No button on the right of "efficiencyExport"
        When Go to asset "Solar panel" info page
        Then Go to modify mode
        Then Uncheck on readonly of "power"
        Then Check on readonly of "panelPitch"
        Then Save
        When Go to panel page
        Then We should see a button on the right of "power"
        And No button on the right of "panelPitch"

    @Desktop @readonly @separate
    Scenario Outline: Set and cancel read-only
        Given Setup "lv3"
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Uncheck on readonly of "<attribute_1>"
        Then Check on readonly of "<attribute_2>"
        Then Save
        When Go to panel page
        Then We should see a button on the right of "<attribute_1>"
        And No button on the right of "<attribute_2>"

        Examples:
            | name        | attribute_1 | attribute_2      |
            | Battery     | energyLevel | efficiencyExport |
            | Solar panel | power       | panelPitch       |

    @Desktop @set_conf_item_both
    Scenario: Set both assets' configure item for Insight and Rule
        Given Setup "lv3"
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        When Go to asset "Battery" info page
        Then Go to modify mode
        Then Select "Rule state" and "Store data points" on "energyLevel"
        Then Select "Rule state" and "Store data points" on "power"
        Then Save
        Then Unselect
        When Go to asset "Solar panel" info page
        Then Go to modify mode
        Then Select "Rule state" and "Store data points" on "power"
        Then Select "Rule state" and "Store data points" on "powerForecast"
        Then Save


    @Desktop @set_conf_item @separate
    Scenario Outline: Set configure item for Insight and Rule
        Given Setup "lv3"
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "asset" tab
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Select "<item_1>" and "<item_2>" on "<attribute_1>"
        Then Select "<item_1>" and "<item_2>" on "<attribute_2>"
        Then Save

        Examples:
            | name        | attribute_1 | attribute_2   | item_1     | item_2            |
            | Battery     | energyLevel | power         | Rule state | Store data points |
            | Solar panel | power       | powerForecast | Rule state | Store data points |
