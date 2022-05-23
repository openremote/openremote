@OpenRemote @settings
Feature: Add_Settings

    Background: Navigation
        Given Setup "lv1"

    @Desktop @add_user
    Scenario: Add new user
        When Navigate to "Users" page
        Then Add a new user
        Then Clean up "basic"

    @Desktop @role
    Scenario: Add and apply new role
        Given Setup "lv2"
        When Navigate to "Roles" page
        Then Create a new role
        When Navigate to "Map" tab
        When Navigate to "Users" page
        Then Select the new role and unselect others
        Then We should see assets permission are selected
        Then Switch back to origin




