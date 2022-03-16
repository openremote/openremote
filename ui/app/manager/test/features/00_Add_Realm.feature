@OpenRemote @settings @add_realm
Feature: Setup

    Background: Setup
        Given Setup "lv0"

    @Desktop
    Scenario: Add new Realm
        When Login to OpenRemote "master" realm as "admin"
        When Navigate to "Realms" page
        Then Add a new Realm
        When Select smartcity realm
        Then We see the smartcity realm



