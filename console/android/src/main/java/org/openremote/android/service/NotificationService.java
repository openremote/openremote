package org.openremote.android.service;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface NotificationService {
    @FormUrlEncoded
    @PUT("/{realm}/notification/token")
    Call<Void> updateToken(@Path("realm") String realm, @Header("Authorization") String authorization, @Field("token") String token, @Field("device_id") String deviceId);


    @GET("/{realm}/notification/alert")
    Call<List<AlertNotification>> getAlertNotification(@Path("realm") String realm, @Header("Authorization") String authorization);

    @DELETE("/{realm}/notification/alert/{id}")
    Call<Void> deleteNotification(@Path("realm") String realm, @Header("Authorization") String authorization,@Path("id") Long id);

    @PUT("/{realm}/asset/{assetId}/attribute/{attributeName}")
    @Headers("Content-Type:application/json")
    Call<Void> updateAssetAction(@Path("realm") String realm, @Header("Authorization") String accessToken,@Path("assetId") String assetId,@Path("attributeName") String attributeName,@Body String rawJson);
}
