@OpenRemote @settings
Feature: Add_Settings

    Background: Navigation
        Given Setup "lv1"

    @Desktop @add_user
    Scenario: Add new user
        When Login OpenRemote as "admin"
        Then Switch to "smartcity" realm
        When Navigate to "Users" page
        Then Add a new user
        Then We see a new user

    @Desktop @add_role
    Scenario: Add and apply new role
        Given Setup "lv2"
        When Login OpenRemote as "admin"
        Then Switch to "smartcity" realm
        When Navigate to "Roles" page
        Then Create a new role
        Then We see a new role
        When Navigate to "Map" tab
        When Navigate to "Users" page
        Then Select the new role and unselect others
        Then We see that assets permission are selected
        Then Switch back to origin




