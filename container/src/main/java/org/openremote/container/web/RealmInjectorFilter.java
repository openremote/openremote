package org.openremote.container.web;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.openremote.model.Constants;

import java.io.IOException;

/**
 * A {@link ClientRequestFilter} that injects the realm into the {@link Constants#REALM_PARAM_NAME} request header.
 */
public class RealmInjectorFilter implements ClientRequestFilter {

   protected String realm;

   public RealmInjectorFilter(String realm) {
      this.realm = realm;
   }

   @Override
   public void filter(ClientRequestContext requestContext) throws IOException {
      requestContext.getHeaders().putSingle(Constants.REALM_PARAM_NAME, realm);
   }
}
