@OpenRemote @setup_test
Feature: Setup

    Background: Setup
        Given Setup for "convention"
    
    @clean_up
    Scenario: test
        When Clean up "convention"

    