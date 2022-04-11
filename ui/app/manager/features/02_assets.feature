@OpenRemote @Asset
Feature: Assets

    Background: Navigation
        Given Login to smartcity realm

    @Desktop @add_asset
    Scenario Outline: Add new asset
        Given Nevigate to asset page
        Then Create a "<asset>" with name of "<name>"
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Give value to the "<attribute_1>" of "<value_1>"
        Then Give value to the "<attribute_2>" of "<value_2>"

        Examples:
            | asset                     | name    | attribute_1      | attribute_2       | value_1 | value_2 |
            | Electricity battery asset | Battery | efficiencyExport | efficiencyImport  | 30      | 50      |
            | PV solar asset            | Solar   | efficiencyExport | power             | 30      | 70      |

    @Desktop @select
    Scenario Outline: select asset
        When Search for the "<name>"
        Then We see "<name>" in the asset tree
        When Select the "<name>"
        Then We see the "<name>" page and History graph

        Examples:
            | asset                     | name    | 
            | Electricity battery asset | Battery |
            | PV solar asset            | Sloar   | 