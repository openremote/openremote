@OpenRemote @Delete
Feature: Delete

    Background: Navigation
        Given Login OpenRemote as "admin"

    @Desktop @delete_conf_item
    Scenario Outline: Delete configure item
        When Nevigate to "Asset" tab
        When Go to asset "<name>" info page
        Then Go to modify mode
        Then Delete "<item>" on "<attribute>"
        Then Save

        Examples:
            | name    | attribute        | item     |
            | Battery | efficiencyExport | Read only  |



    @Desktop @delete_assets
    Scenario Outline: Delete assets
        When Nevigate to "Asset" tab
        Then Delete asset named "<name>"

        Examples:
            | name    |
            | Battery |
            | Solar   |

    @Desktop @delete_role
    Scenario: Delete role
        When Navigate to "Roles" page
        Then Delete role

    @Desktop @delete_user
    Scenario: Delete user
        When Login OpenRemote as "admin"
        When Select smartcity realm
        When Navigate to "Users" page
        Then Delete user
        