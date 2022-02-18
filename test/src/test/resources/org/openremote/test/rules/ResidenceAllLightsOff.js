rules = [{
    name: "Switch room lights off when residence ALL LIGHTS OFF push-button is pressed",
    when: function (facts) {

        var roomWithLightOn = facts.matchFirstAssetState( // Find asset state
            new AssetQuery()
                .types(ROOM) // for a room
                .attributeValue("lightSwitch", true) // where the light switch is on
        );

        // If we have a room where the light is on
        if (roomWithLightOn.isPresent()) {
            var room = roomWithLightOn.get();

            var residenceWithLightOff = facts.matchFirstAssetEvent( // Find asset event
                new AssetQuery()
                    .types(AssetType.RESIDENCE) // for a residence
                    .ids(room.parentId) // that is the parent asset of the room
                    .attributeValue("allLightsOffSwitch", true) // where the "all lights off" switch has been triggered
            );

            // If we have a residence where the light has been switched off
            if (residenceWithLightOff.isPresent()) {

                // Store the room for RHS consumption
                facts.bind("roomId", room.id);

                // Trigger the action
                return true;
            }
        }
        return false;

    },
    then: function (facts) {
        // Turn the light off in the room
        facts.updateAssetState(
            facts.bound("roomId"), "lightSwitch", false
        );
    }
}];
