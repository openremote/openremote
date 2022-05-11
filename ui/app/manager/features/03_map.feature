@OpenRemote @map
Feature: Map

    Background: Navigation
        Given Login to smartcity realm
        Then Nevigate to map page

    @Desktop @markers
    Scenario Outline: check markers on map
        When Check "<asset>" on map
        Then Click and nevigate to "<asset>" page

        Examples:
            | asset   |
            | Battery |
            | Solar   |