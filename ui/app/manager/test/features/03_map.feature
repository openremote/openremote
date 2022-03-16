@OpenRemote @map
Feature: Map

    Background: Navigation
        Given Setup "lv4" for map

    @Desktop @markers
    Scenario: check markers on map
        When Login to OpenRemote "smartcity" realm as "smartcity"
        Then Navigate to "map" tab
        When Check "Battery" on map
        Then Click and nevigate
        Then We are at "Battery" page
