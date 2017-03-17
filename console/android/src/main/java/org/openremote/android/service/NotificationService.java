package org.openremote.android.service;


import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface NotificationService {
    @FormUrlEncoded
    @PUT("/{realm}/notification/token")
    Call<Void> updateToken(@Path("realm") String realm, @Header("Authorization") String authorization, @Field("token") String token, @Field("device_id") String deviceId);
}
