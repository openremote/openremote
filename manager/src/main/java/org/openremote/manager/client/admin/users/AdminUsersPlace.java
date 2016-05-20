package org.openremote.manager.client.admin.users;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import org.openremote.manager.client.admin.AdminPlace;

public class AdminUsersPlace extends AdminPlace {

    final String userId;

    public AdminUsersPlace(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    @Prefix("user")
    public static class Tokenizer implements PlaceTokenizer<AdminUsersPlace> {

        @Override
        public AdminUsersPlace getPlace(String token) {
            return new AdminUsersPlace(token);
        }

        @Override
        public String getToken(AdminUsersPlace place) {
            return place.getUserId() != null ? place.getUserId() : "";
        }
    }

}
