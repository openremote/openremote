@OpenRemote @setup
Feature: Setup

    Background: Setup
        Given Setup "lv0"

    @Desktop @add_realm
    Scenario: Add new Realm
        When Login OpenRemote as "admin"
        When Navigate to "Realms" page
        Then Add a new Realm
        When Select smartcity realm
        Then We see the smartcity realm



