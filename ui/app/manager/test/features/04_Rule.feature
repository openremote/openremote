@OpenRemote @rule
Feature: Rules

    Background: Navigation
        Given Setup "lv4" for rules

    @Desktop @create_both_rules
    Scenario Outline: Create both rules
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "Rule" tab
        When Create a new "When-Then" rule
        Then Name new rule "<name>"
        Then Create When condition on "<attribute_when>" of "<asset>" of "<attribute_type>" with threshold "<value>"
        Then Create Then action on "<attribute_then>" of "<asset>" of "<attribute_type>" with threshold "<value>"
        Then Save rule
        Then We see the rule with name of "<name>"
        When Create a new "Flow" rule
        Then Name new rule "Solar panel"
        Then Drag in the elements
        Then Set value
        Then Connect elements
        Then Snapshot "flow"
        Then Save rule
        Then We see the flow rule with name of "Solar panel"
        Then We should see 2 rules in total

        Examples:
            | name        | attribute_type            | asset   | attribute_when | attribute_then  | value |
            | Energy rule | Electricity battery asset | Battery | Energy level   | Energy capacity | 50    |

    @Desktop @create_whenthen_rule @separate
    Scenario Outline: Create a When-Then rule
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "Rule" tab
        When Create a new "When-Then" rule
        Then Name new rule "<name>"
        Then Create When condition on "<attribute_when>" of "<asset>" of "<attribute_type>" with threshold "<value>"
        Then Create Then action on "<attribute_then>" of "<asset>" of "<attribute_type>" with threshold "<value>"
        Then Save rule
        Then We see the rule with name of "<name>"

        Examples:
            | name        | attribute_type            | asset   | attribute_when | attribute_then  | value |
            | Energy rule | Electricity battery asset | Battery | Energy level   | Energy capacity | 50    |


    @Desktop @create_flow_rule @separate
    Scenario: Create a Flow rule
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "Rule" tab
        When Create a new "Flow" rule
        Then Name new rule "Solar panel"
        Then Drag in the elements
        Then Set value
        Then Connect elements
        Then Snapshot "flow"
        Then Save rule
        Then We see the flow rule with name of "Solar panel"


