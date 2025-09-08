package org.openremote.container.security;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import jakarta.servlet.ServletContext;

import java.util.Set;

public class JwtServletExtension implements ServletExtension {

    private final JwtValidator validator;
    private final String realmHeader;

    public JwtServletExtension(JwtValidator validator, String realmHeader) {
        this.validator = validator;
        this.realmHeader = realmHeader;
    }

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        // Wrap the initial handler chain for this deployment with our security handlers
        deploymentInfo.addInnerHandlerChainWrapper(next ->
                SecurityHandlers.wrapWithJwtAuth(next, validator, realmHeader)
        );

        // Optionally declare security constraints if you want container-wide constraints
        // Or leave it to @RolesAllowed on your JAX-RS resources
    }
}

