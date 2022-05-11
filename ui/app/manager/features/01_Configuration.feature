@OpenRemote @settings
Feature: Add_Settings

    Background: Navigation
        Given Setup for "fundamental"

    @Desktop @add_user
    Scenario: Add new user
        When Navigate to "Users" page
        Then Add a new user

    @Desktop @add_role
    Scenario: Add new role
        When Navigate to "Roles" page
        Then Create a new role

    @Desktop @apply_role
    Scenario: Apply new role
        When Navigate to "Users" page
        Then Select the new role and unselect others
        Then We should see assets permission are selected
        Then Switch back to origin

    @Desktop @switch_user
    Scenario: Switch user
        When Logout
        Then Go to new Realm and login
        Then Snapshot "smartcity and user"
