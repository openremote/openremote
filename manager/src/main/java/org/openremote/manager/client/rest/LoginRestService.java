package org.openremote.manager.client.rest;

import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;
import org.openremote.manager.shared.model.Credentials;
import org.openremote.manager.shared.model.LoginResult;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * Created by Richard on 19/02/2016.
 */
public interface LoginRestService extends RestService {

    @POST
    @Path("/login")
    void login(Credentials credentials, MethodCallback<LoginResult> loginCallback);

}
