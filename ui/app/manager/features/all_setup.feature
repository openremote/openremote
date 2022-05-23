@OpenRemote @setup_test
Feature: Setup

    Background: Setup
        Given Login OpenRemote as "admin"
    
    Scenario: 1
        When Clean up "lv2"

    