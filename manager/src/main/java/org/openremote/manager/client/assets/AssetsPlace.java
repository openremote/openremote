package org.openremote.manager.client.assets;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class AssetsPlace extends Place {

    @Prefix("assets")
    public static class Tokenizer implements PlaceTokenizer<AssetsPlace> {

        @Override
        public AssetsPlace getPlace(String token) {
            return new AssetsPlace();
        }

        @Override
        public String getToken(AssetsPlace place) {
            return "";
        }
    }
}
