@OpenRemote @rule
Feature: Rules

    Background: Navigation
        Given Login to smartcity realm
        Given Nevigate to rule page

    @Desktop @create_whenthen_rule
    Scenario: Create a When-Then rule
        When Create a new when-then rule
        Then Name new rule "Energy"
        Then Create When condition on "energy level" of "Battery"
        Then Create Then action on


    @Desktop @create_flow_rule
    Scenario: Create a Flow rule
        When Create a new flow rule
        Then 



    @Desktop @check_rule_result
    Scenario: Check the rule result from API
        When Creata a Flow rule
        Then Name new rule "Solar"
        Then 
