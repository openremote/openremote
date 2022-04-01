@OpenRemote @Add_Settings
Feature: Add_Settings

    Background: Navigation
        Given Login OpenRemote local website as admin

    @Desktop @Add_realm
      Scenario: Add new Realm
        When Navigate to Realm page
        Then Add a new Realm

    @Desktop @Switch_realm
    Scenario: Switch realm
        When Select smartcity realm
        Then We see the smartcity realm

    @Desktop @Add_user
    Scenario: Add new user
        Given Switch to smartcity realm
        When Navigate to user page
        Then Add a new user

    @Desktop @Switch_user
    Scenario: Switch user
        When Logout
        Then Go to new Realm and login
        Then Snapshot "smartcity and user"
