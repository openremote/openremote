@OpenRemote @Delete
Feature: Delete

    Background: Navigation
        Given Login to smartcity realm

    @Desktop @delete_assets
    Scenario Outline: delete_assets
        Given Nevigate to asset page
        Then delete asset named "<name>"

        Examples:
            |   name    |
            |   Battery |
            |   Solar   |