package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * Created by Richard on 10/02/2016.
 */
public class LoginPlace extends Place {
    private Place redirectTo;

    public LoginPlace(Place redirectTo) {
        this.redirectTo = redirectTo;
    }

    @Prefix("login")
    public static class Tokenizer implements PlaceTokenizer<LoginPlace> {

        @Override
        public LoginPlace getPlace(String token) {
            return new LoginPlace(null);
        }

        @Override
        public String getToken(LoginPlace place) {
            return "";
        }
    }
}
