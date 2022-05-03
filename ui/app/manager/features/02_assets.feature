@OpenRemote @asset
Feature: Assets

    Background: Navigation
        Given Login to smartcity realm
        Given Nevigate to asset page

    @Desktop @add_asset
    Scenario Outline: Add new asset
        Then Create a "<asset>" with name of "<name>"
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Give "<value_1>" to the "<attribute_1>" with type of "<A1_type>"
        Then Give "<value_2>" to the "<attribute_2>" with type of "<A2_type>"
        Then Save

        Examples:
            | asset                     | name    | attribute_1       | A1_type          | attribute_2      | A2_type          | value_1 | value_2 |
            | Electricity battery asset | Battery | efficiencyExport  | Positive integer | efficiencyImport | Positive integer | 30      | 50      |
            | PV solar asset            | Solar   | energyExportTotal | Positive number  | power            | Number           | 30      | 70      |

    @Desktop @select
    Scenario Outline: Select asset
        When Search for the "<name>"
        When Select the "<name>"
        Then We see the "<name>" page

        Examples:
            | asset                     | name    |
            | Electricity battery asset | Battery |
            | PV solar asset            | Solar   |

    @Desktop @update
    Scenario Outline: Update asset
        When Select the "<name>"
        Then Update "<value>" to the "<attribute>" with type of "<type>"
        When Go to modify mode
        Then Update <location_x> and <location_y>
        Then Save

        Examples:
            | asset                     | name    | attribute     | type   | value | location_x | location_y |
            | Electricity battery asset | Battery | powerSetpoint | number | 70    | 705        | 210        |
            | PV solar asset            | Solar   | powerForecast | number | 100   | 540        | 110        |

    @Desktop @readonly
    Scenario Outline: Set and cancel read-only
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Uncheck on readonly of "<attribute_1>"
        Then Check on readonly of "<attribute_2>"
        Then Save
        When Go to panel page
        Then We should see a button on the right of "<attribute_1>" 
        And No button on the right of "<attribute_2>"
       
        Examples:
            | name    | attribute_1     | attribute_2        |
            | Battery | energyLevel     | efficiencyExport   |
            | Solar   | power           | panelPitch         |


   @Desktop @set_conf_item
   Scenario Outline: Set configure item for Insight and Rule
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Check on "<item_1>" and "<item_2>" on "<attribute>"

        Examples:
            | name    | attribute     | item_1       | item_2            |
            | Battery | energyLevel   | Rule state   | Store data points |
            | Solar   | power         | Rule state   | Store data points |
