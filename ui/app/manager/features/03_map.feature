@OpenRemote @Map
Feature: Map

    Background: Navigation
        Given Login to smartcity realm
        Given Nevigate to map page

    @Desktop @markers
    Scenario: check markers on map
        When Check markers on map

