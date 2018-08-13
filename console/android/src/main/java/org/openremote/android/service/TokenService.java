package org.openremote.android.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.openremote.android.R;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


public class TokenService {

    private static final Logger LOG = Logger.getLogger(TokenService.class.getName());

    private final SharedPreferences sharedPref;
    private final String refreshTokenKey;
    private final String fcmTokenKey;
    private final String deviceIdKey;
    private final String realm;
    private final OAuth2Service oauth2Service;
    private final RestApiResource restApiResource;

    public TokenService(Context context) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(context.getString(R.string.OR_BASE_SERVER))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create());
        if (Boolean.parseBoolean(context.getString(R.string.SSL_IGNORE))) {
            builder.client(getUnsafeOkHttpClient());
        }
        Retrofit retrofit = builder.build();

        oauth2Service = retrofit.create(OAuth2Service.class);
        restApiResource = retrofit.create(RestApiResource.class);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        refreshTokenKey = context.getString(R.string.SHARED_PREF_REFRESH_TOKEN);
        fcmTokenKey = context.getString(R.string.SHARED_PREF_FCM_TOKEN);
        deviceIdKey = context.getString(R.string.SHARED_PREF_DEVICE_ID);
        realm = context.getString(R.string.OR_REALM);
        String refreshToken = sharedPref.getString(refreshTokenKey, null);
        String fcmToken = sharedPref.getString(fcmTokenKey, null);
        String deviceId = sharedPref.getString(deviceIdKey, null);
        if (refreshToken != null && fcmToken != null && deviceId != null) {
            LOG.fine("On create, have refresh token, sending FCM token");
            sendFCMToken(fcmToken, deviceId);
        } else {
            LOG.fine("On create, no refresh or FCM token or device ID, skipping update...");
        }
    }

    public void saveToken(String refreshToken) {
        LOG.fine("Saving offline refresh token");
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(refreshTokenKey, refreshToken);
        editor.commit();

        String fcmToken = sharedPref.getString(fcmTokenKey, null);
        String deviceId = sharedPref.getString(deviceIdKey, null);
        if (fcmToken != null && deviceId != null) {
            LOG.fine("On save refresh token, sending FCM token");
            sendFCMToken(fcmToken, deviceId);
        } else {
            LOG.fine("On save refresh token, no device ID or FCM token, skipping update...");
        }
    }

    public String getJsonToken() {
        String refreshToken = sharedPref.getString(refreshTokenKey, null);
        return refreshToken == null ? null : "{ \"refreshToken\": \"" + refreshToken + "\"}";
    }

    public void withAccessToken(final TokenCallback callback) {
        Call<Map<String, String>> call = oauth2Service.refreshToken(realm, "refresh_token", "openremote", sharedPref.getString(refreshTokenKey, null));
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        try {
                            LOG.fine("Access token successfully updated");
                            callback.onToken("Bearer " + response.body().get("access_token"));
                        } catch (IOException e) {
                            callback.onFailure(e);
                        }
                    } else {
                        callback.onFailure(new NullPointerException("No response body on update access token request"));
                    }
                } else {
                    callback.onFailure(new IllegalStateException("Unsuccessful response in update access token request: " + response.code()));
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
        } else {
            LOG.fine("On send or store FCM token, no refresh token key (user never logged in on this device?), skipping update...");
        }
    }

    private void storeFCMToken(String fcmToken, String deviceId) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(fcmTokenKey, fcmToken);
        editor.putString(deviceIdKey, deviceId);
        editor.commit();
    }

    private void sendFCMToken(final String fcmToken, final String id) {
        LOG.fine("Sending FCM token for device ID: " + id);
        withAccessToken(new TokenCallback() {
            @Override
            public void onToken(String accessToken) {
                Call call = restApiResource.updateToken(realm, accessToken, fcmToken, id, "ANDROID");

                call.enqueue(new Callback() {
                    @Override
                    public void onResponse(Call call, Response response) {
                        if (response.code() != 204) {
                            LOG.severe("Sending FCM device token failed for device ID: " + id + ", response code: " + response.code());
                        } else {
                            LOG.fine("Sending FCM device token successful for device ID: " + id);

                            // #40: Don't clean-up the FCM information from SharedPreferences, this means they are always sent
                            // This ensures that if it gets cleaned from server, it is re-send again and notifications can be send
                                /*
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.remove(fcmTokenKey);
                                editor.remove(deviceIdKey);
                                editor.commit();
                                */
                        }
                    }

                    @Override
                    public void onFailure(Call call, Throwable t) {
                        LOG.log(Level.SEVERE, "Sending FCM device token failed for device ID: " + id, t);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.log(Level.SEVERE, "Sending FCM device token failed (no access token), for device ID: " + id, t);
            }
        });

    }

    public void getAlerts(final Callback<List<AlertNotification>> callback) {

        withAccessToken(new TokenCallback() {
            @Override
            public void onToken(String accessToken) {
                restApiResource.getAlertNotification(realm, accessToken).enqueue(callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(null, t);
            }
        });
    }

    public void clearToken() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(refreshTokenKey);
        editor.commit();
    }

    public void deleteAlert(final Long id) {

        withAccessToken(new TokenCallback() {
            @Override
            public void onToken(String accessToken) throws IOException {
                restApiResource.deleteNotification(realm, accessToken, id).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.code() != 204) {
                            LOG.severe("Error deleting alert notification: " + id + ", response code: " + response.code());
                        } else {
                            LOG.fine("Alert notification deleted successfully: " + id);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        LOG.log(Level.SEVERE, "Error deleting alert notification: " + id, t);
                    }
                });

            }

            @Override
            public void onFailure(Throwable t) {
                // TODO We should tell the user to login again or it will never work
                LOG.log(Level.SEVERE, "Error deleting notification (no access token): " + id, t);
            }
        });
    }

//    public void executeAction(final AlertAction alertAction) {
//        withAccessToken(new TokenCallback() {
//            @Override
//            public void onToken(String accessToken) throws IOException {
//                restApiResource.updateAssetAction(realm, accessToken, alertAction.getAssetId(), alertAction.getAttributeName(), alertAction.getRawJson()).enqueue(new Callback<Void>() {
//                    @Override
//                    public void onResponse(Call<Void> call, Response<Void> response) {
//                        if (response.code() != 204) {
//                            LOG.severe("Error executing asset write: " + alertAction + ", response code: " + response.code());
//                        } else {
//                            LOG.fine("Asset write executed successfully: " + alertAction);
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(Call<Void> call, Throwable t) {
//                        LOG.log(Level.SEVERE, "Error executing asset write: " + alertAction, t);
//                    }
//                });
//
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                // TODO We should tell the user to login again or it will never work
//                LOG.log(Level.SEVERE, "Error executing asset write (no access token): " + alertAction, t);
//            }
//        });
//
//    }

    public void notificationDelivered(final Long notificationId, final String fcmToken) {
        restApiResource.notificationDelivered(realm, notificationId, fcmToken).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }

    public void notificationAcknowledged(final Long notificationId, final String fcmToken, final String acknowledgement) {
        restApiResource.notificationAcknowledged(realm, notificationId, fcmToken, acknowledgement).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }

    static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
