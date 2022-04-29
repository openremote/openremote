@OpenRemote @Delete
Feature: Delete

    Background: Navigation
        Given Login to smartcity realm

    @Desktop @delete_assets
    Scenario Outline: delete assets
        Given Nevigate to asset page
        Then Delete asset named "<name>"

        Examples:
            |   name    |
            |   Battery |
            |   Solar   |
    
    @Desktop @delete_role
    Scenario: delete role
        Given Nevigate to role page
        Then Delete role