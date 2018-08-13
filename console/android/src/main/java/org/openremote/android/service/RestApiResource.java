package org.openremote.android.service;


import java.util.List;

import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface RestApiResource {
    @FormUrlEncoded
    @PUT("/{realm}/notification/token")
    Call<Void> updateToken(@Path("realm") String realm, @Header("Authorization") String authorization, @Field("token") String token, @Field("device_id") String deviceId, @Field("device_type") String deviceType);


    @GET("/{realm}/notification/alert")
    Call<List<AlertNotification>> getAlertNotification(@Path("realm") String realm, @Header("Authorization") String authorization);

    @DELETE("/{realm}/notification/alert/{id}")
    Call<Void> deleteNotification(@Path("realm") String realm, @Header("Authorization") String authorization,@Path("id") Long id);

    @PUT("/{realm}/asset/{assetId}/attribute/{attributeName}")
    @Headers("Content-Type:application/json")
    Call<Void> updateAssetAction(@Path("realm") String realm, @Header("Authorization") String accessToken,@Path("assetId") String assetId,@Path("attributeName") String attributeName,@Body String rawJson);

    @PUT("/{realm}/notification/{notificationId}/delivered")
    @Headers("Content-Type:application/json")
    Call<Void> notificationDelivered(@Path("realm") String realm, @Path("notificationId") Long notificationId, @Query("targetId") String targetId);

    @PUT("/{realm}/notification/{notificationId}/acknowledged")
    @Headers("Content-Type:application/json")
    Call<Void> notificationAcknowledged(@Path("realm") String realm, @Path("notificationId") Long notificationId, @Query("targetId") String targetId, @Body String acknowledgement);

}
