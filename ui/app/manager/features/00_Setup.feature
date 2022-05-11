@OpenRemote @add_realm
Feature: Setup

    Background: Setup
        Given Setup for "none"

    @Desktop
    Scenario: Add new Realm
        When Nevigate to "Realm" page
        Then Add a new Realm
        When Select smartcity realm
        Then We see the smartcity realm


