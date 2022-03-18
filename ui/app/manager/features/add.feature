@OpenRemote
Feature: Add

    Background: Navigation
        Given Navigation to master realm

    @Desktop @add
    Scenario: Add new user
        When Select smartcity realm
        Then We see the smartcity realm

    @Desktop @add
    Scenario: Add new asset
        When Search for the Parking Erasmusbrug
        Then We see Parking Erasmusbrug in the asset tree
        When Select the Parking Erasmusbrug
        Then We see the Parking Erasmusbrug page and History graph
    