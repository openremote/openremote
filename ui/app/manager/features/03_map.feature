@OpenRemote @map
Feature: Map

    Background: Navigation
        Given Setup "lv4"

    @Desktop @markers
    Scenario Outline: check markers on map
        When Login OpenRemote as "smartcity"
        Then Navigate to "map" tab
        When Check "<asset>" on map
        Then Click and nevigate to "<asset>" page

        Examples:
            | asset   |
            | Battery |
            | Solar   |