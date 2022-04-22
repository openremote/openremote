@OpenRemote @Asset
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
            | asset                     | name    | attribute_1       | A1_type            | attribute_2       | A2_type          | value_1 | value_2 |
            | Electricity battery asset | Battery | efficiencyExport  | Positive integer   | efficiencyImport  | Positive integer | 30      | 50      |
            | PV solar asset            | Solar   | energyExportTotal | Positive number    | power             | Number           | 30      | 70      |

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
        Then Update "<value_1>" to the "<attribute_1>" with type of "<A1_type>"
        When Go to modify mode
        Then Update "<location>"

        Examples:
            | asset                     | name    |  attribute_1    |  A1_type   |  value_1  |
            | Electricity battery asset | Battery |  powerSetpoint  |  number    |  70       |
            | PV solar asset            | Solar   |  powerForecast  |  number    |  100      |