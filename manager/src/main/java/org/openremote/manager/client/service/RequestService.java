package org.openremote.manager.client.service;

import org.openremote.manager.shared.http.Callback;
import org.openremote.manager.shared.http.Request;
import org.openremote.manager.shared.http.RequestParams;

public interface RequestService {

    <T> Request<T> createRequest(boolean withBearerAuthorization);

    <T> RequestParams<T> createRequestParams(Callback<T> callback);

}
