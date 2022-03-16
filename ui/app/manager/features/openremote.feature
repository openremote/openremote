@OpenRemote
Feature: Playwright docs

  Background: Navigation
    Given Go to the OpenRemote demo website

  # @LoginMobile
  # Scenario: Login through Mobile
  #   When Type in username and password
  #   When Login
  #   Then We see the map
  #   Then Snapshot "Mobile OpenRemote Map "

  @Desktop @Login
  Scenario: login through Desktop
    When Type in username and password
    When Login
    Then We see the map
    Then Snapshot "Desktop OpenRemote Map"

