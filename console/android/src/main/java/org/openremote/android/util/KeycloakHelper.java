/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openremote.android.util;

import android.app.Activity;
import android.util.Log;

import org.jboss.aerogear.android.authorization.AuthorizationManager;
import org.jboss.aerogear.android.authorization.AuthzModule;
import org.jboss.aerogear.android.authorization.oauth2.OAuth2AuthorizationConfiguration;
import org.jboss.aerogear.android.authorization.oauth2.OAuthWebViewDialog;
import org.jboss.aerogear.android.core.Callback;
import org.jboss.aerogear.android.pipe.PipeManager;
import org.jboss.aerogear.android.pipe.rest.RestfulPipeConfiguration;
import org.jboss.aerogear.android.pipe.rest.gson.GsonRequestBuilder;



import java.net.URL;

public class KeycloakHelper {

    private static final String MANAGER_SERVER_URL = "http://192.168.2.119:8080";
    private static final String AUTHZ_URL = MANAGER_SERVER_URL +"/auth";
    private static final String AUTHZ_ENDPOINT = "/realms/master/protocol/openid-connect/auth";
    private static final String ACCESS_TOKEN_ENDPOINT = "/realms/master/protocol/openid-connect/token";
    private static final String REFRESH_TOKEN_ENDPOINT = "/realms/master/protocol/openid-connect/token";
    private static final String AUTHZ_ACCOOUNT_ID = "keycloak-token";
    private static final String AUTHZ_CLIENT_ID = "openremote";
    private static final String AUTHZ_REDIRECT_URL = "org.openremote.console://oauth2Callback";
    private static final String MODULE_NAME = "KeyCloakAuthz";

    static {
        try {
            AuthorizationManager.config(MODULE_NAME, OAuth2AuthorizationConfiguration.class)
                    .setBaseURL(new URL(AUTHZ_URL))
                    .setAuthzEndpoint(AUTHZ_ENDPOINT)
                    .setAccessTokenEndpoint(ACCESS_TOKEN_ENDPOINT)
                    .setRefreshEndpoint(REFRESH_TOKEN_ENDPOINT)
                    .setAccountId(AUTHZ_ACCOOUNT_ID)
                    .setClientId(AUTHZ_CLIENT_ID)
                    .setRedirectURL(AUTHZ_REDIRECT_URL)
                    .asModule();

            PipeManager.config("kc-upload", RestfulPipeConfiguration.class).module(AuthorizationManager.getModule(MODULE_NAME))
                    .withUrl(new URL(MANAGER_SERVER_URL + "/master/asset?realm=master"))
                    .requestBuilder(new GsonRequestBuilder())
                    .forClass(TestHolder.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void connect(final Activity activity, final Callback callback) {
        try {
            final AuthzModule authzModule = AuthorizationManager.getModule(MODULE_NAME);

            authzModule.requestAccess(activity, new Callback<String>() {
                @SuppressWarnings("unchecked")
                @Override
                public void onSuccess(String s) {
                    callback.onSuccess(s);
                }

                @Override
                public void onFailure(Exception e) {
                    if (!e.getMessage().matches(OAuthWebViewDialog.OAuthReceiver.DISMISS_ERROR)) {
                        authzModule.deleteAccount();
                    }
                    callback.onFailure(e);
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void test(final Callback callback, final Activity activity) {
        if (!KeycloakHelper.isConnected()) {

            KeycloakHelper.connect(activity, new Callback() {
                        @Override
                        public void onSuccess(Object o) {
                            Log.d("KEYCLOAK","Token ?? :"+o);
                            PipeManager.getPipe("kc-upload", activity).read(callback);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.d("KEYCLOAK","error",e);
                        }
                    }
            );

        } else {
            PipeManager.getPipe("kc-upload", activity).read(callback);
        }




    }

    public static void testWebview(final Callback callback, final Activity activity) {


            KeycloakHelper.connect(activity, new Callback() {
                        @Override
                        public void onSuccess(Object o) {
                            callback.onSuccess(o);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    }
            );

    }

    public static void revokeToken() {
        AuthorizationManager.getModule(MODULE_NAME).deleteAccount();
        AuthorizationManager.getModule(MODULE_NAME).refreshAccess();
    }

    public static boolean isConnected() {
        return AuthorizationManager.getModule(MODULE_NAME).isAuthorized();
    }

}
