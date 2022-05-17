@OpenRemote @map
Feature: Map

    Background: Navigation
        Given Login OpenRemote as "smartcity"
        Then Nevigate to "Map" tab

    @Desktop @markers
    Scenario Outline: check markers on map
        When Check "<asset>" on map
        Then Click and nevigate to "<asset>" page

        Examples:
            | asset   |
            | Battery |
            | Solar   |