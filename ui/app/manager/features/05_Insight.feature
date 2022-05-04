@OpenRemote @Insgith
Feature: Insight

    Background: Navigation
        Given Login to smartcity realm
    
     @Desktop @insert_attribute
     Scenario Outline: Insert attributes
        Given Nevigate to insight page
        When Select "<attribute>" from "<asset>"
        Then We should see the graph

        Examples:
            | attribute    | asset    | 
            | Energy level | Battery  | 
            | Power        | Solar    |