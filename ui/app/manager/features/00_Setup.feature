@OpenRemote @setup
Feature: Setup

    Background: Setup
        Given Setup for "basic"
    
    @Desktop
    Scenario: Random test
        When Random
        Then Test
