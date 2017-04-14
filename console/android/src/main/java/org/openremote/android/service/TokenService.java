package org.openremote.android.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.openremote.android.R;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static android.R.attr.id;


public class TokenService {

    private final SharedPreferences sharedPref;
    private final String tokenKey;
    private final String refreshTokenKey;
    private final String tokenIdKey;
    private final String fcmTokenKey;
    private final String deviceIdKey;
    private final String realm;
    private final OAuth2Service oauth2Service;
    private final NotificationService notificationService;

    public TokenService(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(context.getString(R.string.OR_BASE_SERVER))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        oauth2Service = retrofit.create(OAuth2Service.class);
        notificationService = retrofit.create(NotificationService.class);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        tokenKey = context.getString(R.string.SHARED_PREF_TOKEN);
        refreshTokenKey = context.getString(R.string.SHARED_PREF_REFRESH_TOKEN);
        tokenIdKey = context.getString(R.string.SHARED_PREF_TOKEN_ID);
        fcmTokenKey = context.getString(R.string.SHARED_PREF_FCM_TOKEN);
        deviceIdKey = context.getString(R.string.SHARED_PREF_DEVICE_ID);
        realm = context.getString(R.string.OR_REALM);
        String refreshToken = sharedPref.getString(refreshTokenKey, null);
        String fcmToken =  sharedPref.getString(fcmTokenKey, null);
        String deviceId = sharedPref.getString(deviceIdKey, null);
        if (refreshToken != null && fcmToken != null) {
            sendFCMToken(fcmToken, deviceId);
        }
    }

    public void saveToken(String token, String refreshToken, String idToken) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(tokenKey, token);
        editor.putString(refreshTokenKey, refreshToken);
        editor.putString(tokenIdKey, idToken);
        editor.commit();

        String fcmToken =  sharedPref.getString(fcmTokenKey, null);
        String deviceId = sharedPref.getString(deviceIdKey, null);
        if (fcmToken != null && deviceId != null) {
            sendFCMToken(fcmToken, deviceId);
        }


    }

    public String getJsonToken() {

        String token = sharedPref.getString(tokenKey, null);
        String refreshToken = sharedPref.getString(refreshTokenKey, null);
        String idToken = sharedPref.getString(tokenIdKey, null);
        return refreshToken == null ? null : "{ \"token\" :\"" + token + "\", \"refreshToken\": \"" + refreshToken + "\", \"idToken\": \"" + idToken + "\"}";
    }

    public void getAuthorization(final TokenCallback callback) {
        Call<Map<String, String>> call = oauth2Service.refreshToken(realm, "refresh_token", "openremote", sharedPref.getString(refreshTokenKey, null));
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                Log.i("TOKEN_SERVICE", "response : " + response.body());
                if (response.body() != null) {
                    try {
                        callback.onToken("Bearer " + response.body().get("access_token"));
                    } catch (IOException e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new NullPointerException());
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void sendOrStoreFCMToken(String fcmToken, String deviceId) {
        String refreshToken_key = sharedPref.getString(this.refreshTokenKey, null);
        storeFCMToken(fcmToken, deviceId);
        if (refreshToken_key != null) {
            sendFCMToken(fcmToken, deviceId);
        }
    }

    private void storeFCMToken(String fcmToken, String deviceId) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(fcmTokenKey, fcmToken);
        editor.putString(deviceIdKey, deviceId);
        editor.commit();
    }

    private void sendFCMToken(final String fcmToken, final String id) {
        getAuthorization(new TokenCallback() {
            @Override
            public void onToken(String accessToken) {
                Call call = notificationService.updateToken(realm, accessToken, fcmToken, id);
                try {
                    Response response = call.execute();
                    if (response.code() != 204) {
                        Log.e("TOKEN_SERVICE", "save fcm token failed");
                    } else {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.remove(fcmTokenKey);
                        editor.remove(deviceIdKey);
                        editor.commit();
                    }
                } catch (IOException e) {
                    Log.e("TOKEN_SERVICE", "save fcm token failed ");
                }

            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("TOKEN_SERVICE", "refresh token failed");
            }
        });

    }

    public void getAlerts(final Callback<List<AlertNotification>> callback) {

        getAuthorization(new TokenCallback() {
            @Override
            public void onToken(String accessToken) {
                notificationService.getAlertNotification(realm, accessToken).enqueue(callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(null,t);
            }
        });
    }

    public void clearToken() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(tokenKey);
        editor.remove(refreshTokenKey);
        editor.remove(tokenIdKey);
        editor.commit();
    }

    public void deleteAlert(final Long id) {

        getAuthorization(new TokenCallback() {
            @Override
            public void onToken(String accessToken) throws IOException {
                notificationService.deleteNotification(realm, accessToken,id).execute();

            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("TOKEN_SERVICE","Error deleting notification",t);
            }
        });


    }

    public void executeAction(final AlertAction alertAction) {
        getAuthorization(new TokenCallback() {
            @Override
            public void onToken(String accessToken) throws IOException {
                notificationService.updateAssetAction(realm, accessToken,alertAction.getAssetId(),alertAction.getAttributeName(),alertAction.getRawJson()).execute();

            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("TOKEN_SERVICE","Error deleting notification",t);
            }
        });

    }
}
