package org.openremote.android.service;


import retrofit2.Call;
import retrofit2.http.*;

public interface RestApiResource {
    @FormUrlEncoded
    @PUT("/{realm}/notification/token")
    Call<Void> updateToken(@Path("realm") String realm, @Header("Authorization") String authorization, @Field("token") String token, @Field("device_id") String deviceId, @Field("device_type") String deviceType);

    @PUT("/{realm}/notification/{notificationId}/delivered")
    @Headers("Content-Type:application/json")
    Call<Void> notificationDelivered(@Path("realm") String realm, @Path("notificationId") Long notificationId, @Query("targetId") String targetId);

    @PUT("/{realm}/notification/{notificationId}/acknowledged")
    @Headers("Content-Type:application/json")
    Call<Void> notificationAcknowledged(@Path("realm") String realm, @Path("notificationId") Long notificationId, @Query("targetId") String targetId, @Body String acknowledgement);

}
