@OpenRemote @Delete
Feature: Delete

    Background: Navigation
        Given Login OpenRemote as "smartcity"

    @Desktop @delete_conf_item
    Scenario Outline: Delete configure item
        Given Nevigate to asset page
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Delete "<item>" on "<attribute>"
        Then Save

        Examples:
            | name    | attribute        | item     |
            | Battery | efficiencyExport | Read only  |



    @Desktop @delete_assets
    Scenario Outline: Delete assets
        Given Nevigate to asset page
        Then Delete asset named "<name>"

        Examples:
            | name    |
            | Battery |
            | Solar   |

    @Desktop @delete_role
    Scenario: Delete role
        Given Nevigate to role page
        Then Delete role

    @Desktop @delete_user
    Scenario: Delete user
        Given Login OpenRemote as "admin"
        When Select smartcity realm
        When Navigate to user page
        Then Delete user
        