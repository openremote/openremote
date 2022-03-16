@OpenRemote
Feature: Playwright docs

  Background: Navigation
    Given Login OpenRemote demo website

  @Desktop @map
  Scenario: asset map panel
    When Click on the Parking Erasmusbrug
    Then We see a map panel with Parking Erasmusbrug
    Then Snapshot "Parking Erasmusbrug's panel"
    When Click on View button
    Then We see the Parking Erasmusbrug page and History graph
    And Asset option is selected
    Then Snapshot "Parking Erasmusbrug's Asset page"


  @Desktop @map @navigate
  Scenario: navigate to assets
    When Click on the Asset option
    Then Asset option is selected
    And We see the collapsed asset tree with nothing selected
    And We see text on the main panel