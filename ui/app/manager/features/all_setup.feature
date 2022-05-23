@OpenRemote 
Feature: Setup

    Background: Setup
        Given Setup "lv2"
        
    @setup_test
    Scenario: 1
        When Start to type your Then step here
        Then Clean up "lv3"
    