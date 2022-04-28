@OpenRemote @rule
Feature: Rules

    Background: Navigation
        Given Login to smartcity realm
        Given Goes to Rules page

    @Desktop @when_then @create_rule
    Scenario Outline: delete_assets
        Given Nevigate to asset page
        Then delete asset named "<name>"