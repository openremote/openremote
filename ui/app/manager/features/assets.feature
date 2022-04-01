@OpenRemote
Feature: Assets

    Background: Navigation
        Given Navigate to smartcity realm

    @Desktop @add_asset
    Scenario: Add new asset
        Given Nevigate to asset page
        Then Create a "<asset>" with name of "<name>"
        When Goes to asset "<name>" info page
        Then Give value to the "<attribute_1>" of "<value_1>"
        Then Give value to the "<attribute_2>" of "<value_2>"

    @Desktop @asset
    Scenario: select asset
        When Search for the "<name>"
        Then We see "<name>" in the asset tree
        When Select the "<name>"
        Then We see the "<name>" page and History graph

        Examples:
            | asset                     | name    | attribute_1       | attribute_2       | value_1 | value_2 |
            | Electricity Battery Asset | Battery | Efficiency export | Efficiency import | 30      | 50      |
            | PV Solar Asset            | Sloar   | Efficiency export | Power             | 30      | 70      |