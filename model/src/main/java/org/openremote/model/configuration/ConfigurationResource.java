package org.openremote.model.configuration;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Configuration")
@Path("configuration")
public interface ConfigurationResource {

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("manager")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    ManagerConf update(@BeanParam RequestParams requestParams, ManagerConf managerConfiguration) throws IOException;

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("manager/file")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    String fileUpload(
            @BeanParam RequestParams requestParams,
            @QueryParam("path")
            String path,
            FileInfo fileInfo
    ) throws IOException;


}
