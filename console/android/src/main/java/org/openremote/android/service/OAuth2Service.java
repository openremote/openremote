package org.openremote.android.service;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.Map;

public interface OAuth2Service {
    @FormUrlEncoded
    @POST("/auth/realms/{realm}/protocol/openid-connect/token")
    Call<Map<String, String>> refreshToken(@Path("realm") String realm, @Field("grant_type") String grantType, @Field("client_id") String clientId, @Field("refresh_token") String refreshToken);

}
