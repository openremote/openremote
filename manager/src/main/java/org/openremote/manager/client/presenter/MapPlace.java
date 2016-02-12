package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class MapPlace extends Place {

    @Prefix("map")
    public static class Tokenizer implements PlaceTokenizer<MapPlace> {

        @Override
        public MapPlace getPlace(String token) {
            return new MapPlace();
        }

        @Override
        public String getToken(MapPlace place) {
            return "";
        }
    }
}
