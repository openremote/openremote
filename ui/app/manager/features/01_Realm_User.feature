@OpenRemote @add_settings
Feature: Add_Settings

    Background: Navigation
        Given Login OpenRemote local website as admin

    @Desktop @add_realm 
      Scenario: Add new Realm
        When Navigate to Realm page
        Then Add a new Realm

    @Desktop @switch_realm
    Scenario: Switch realm
        When Select smartcity realm
        Then We see the smartcity realm

    @Desktop @add_user
    Scenario: Add new user
        Given Switch to smartcity realm
        When Navigate to user page
        Then Add a new user

    @Desktop @add_role
    Scenario: Add new role
        Given Switch to smartcity realm
        When Navigate to role page
        Then Create a new role

    @Desktop @apply_role
    Scenario: Apply new role
        Given Switch to smartcity realm
        When Navigate to user page
        Then Select the new role and unselect others
        Then We should see assets permission are selected
        Then Switch back to origin

    @Desktop @switch_user
    Scenario: Switch user
        When Logout
        Then Go to new Realm and login
        Then Snapshot "smartcity and user"
