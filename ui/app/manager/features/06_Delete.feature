@OpenRemote @Delete
Feature: Delete

    Background: Navigation
        Given Setup "lv1"

    @Desktop @delete_realm
    Scenario: Delete realm
        When Login OpenRemote as "admin"
        Then Switch to "smartcity" realm
        When Navigate to "Realms" page
        Then Delete realm
        Then We should not see the Realm picker

    @Desktop @delete_role
    Scenario: Delete role
        When Login OpenRemote as "admin"
        Then Navigate to "Roles" page
        Then Create a new role
        Then Delete role
        Then We should not see the Custom role

    @Desktop @delete_user
    Scenario: Delete user
        Given Setup "lv2"
        When Login OpenRemote as "admin"
        Then Switch to "smartcity" realm
        When Navigate to "Users" page
        Then Delete user
        Then We should see an empty use page


    @Desktop @delete_assets
    Scenario: Delete assets
        Given Setup "lv3"
        When Login OpenRemote as "smartcity"
        When Delete assets
        Then We should see an empty asset column








