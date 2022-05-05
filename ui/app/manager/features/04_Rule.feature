@OpenRemote @rule
Feature: Rules

    Background: Navigation
        Given Login to smartcity realm
        Given Nevigate to rule page

    @Desktop @create_whenthen_rule
    Scenario Outline: Create a When-Then rule
        When Create a new "When-Then" rule
        Then Name new rule "<name>"
        Then Create When condition on "<attribute_when>" of "<asset>" of "<attribute_type>" with threshold "<value>"
        Then Create Then action on "<attribute_then>" of "<asset>" of "<attribute_type>" with threshold "<value>"
        Then Save rule

        Examples:
            | name   | attribute_type            | asset   | attribute_when | attribute_then  | value |
            | Energy | Electricity battery asset | Battery | Energy level   | Energy capacity | 50    |


    @Desktop @create_flow_rule
    Scenario: Create a Flow rule
        When Create a new "Flow" rule
        Then Name new rule "Solar"
        Then Drag in the elements
        Then Select the attributes in the reader and writer
        Then Connect elements
        Then Save rule



    @Desktop @check_rule_result
    Scenario: Check the rule result from API

