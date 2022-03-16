@OpenRemote
Feature: Playwright docs

    Background: Navigation
        Given Navigation to master realm

    @Desktop @asset
    Scenario: switch realm
        When Select smartcity realm
        Then We see the smartcity realm

    @Desktop @asset
    Scenario: select asset
        When Search for the Parking Erasmusbrug
        Then We see Parking Erasmusbrug in the asset tree
        When Select the Parking Erasmusbrug
        Then We see the Parking Erasmusbrug page and History graph
    