package org.openremote.manager.client.admin.realms;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import org.openremote.manager.client.admin.AdminPlace;

public class AdminRealmsPlace extends AdminPlace {

    final String realm;

    public AdminRealmsPlace(String realm) {
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    @Prefix("realm")
    public static class Tokenizer implements PlaceTokenizer<AdminRealmsPlace> {

        @Override
        public AdminRealmsPlace getPlace(String token) {
            return new AdminRealmsPlace(token);
        }

        @Override
        public String getToken(AdminRealmsPlace place) {
            return place.getRealm() != null ? place.getRealm() : "";
        }
    }

}
