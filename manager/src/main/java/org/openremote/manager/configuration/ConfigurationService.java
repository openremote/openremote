package org.openremote.manager.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.CodecUtil;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.configuration.ManagerConf;
import org.openremote.model.file.FileInfo;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.openremote.manager.web.ManagerWebService.OR_CUSTOM_APP_DOCROOT;
import static org.openremote.manager.web.ManagerWebService.OR_CUSTOM_APP_DOCROOT_DEFAULT;
import static org.openremote.container.util.MapAccess.getString;
public class ConfigurationService extends RouteBuilder implements ContainerService {

    protected ManagerIdentityService identityService;
    protected PersistenceService persistenceService;
    protected Path pathPublicRoot;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void configure() throws Exception {
        /* code not overridden yet */
    }

    @Override
    public void init(Container container) throws Exception {
        identityService = container.getService(ManagerIdentityService.class);
        persistenceService = container.getService(PersistenceService.class);
        pathPublicRoot = Paths.get(getString(container.getConfig(), OR_CUSTOM_APP_DOCROOT, OR_CUSTOM_APP_DOCROOT_DEFAULT));
        container.getService(ManagerWebService.class).addApiSingleton(
                new ConfigurationResourceImpl(
                        container.getService(TimerService.class),
                        identityService, this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
        /* code not overridden yet */
    }

    @Override
    public void stop(Container container) throws Exception {
        /* code not overridden yet */
    }


    public void saveMangerConfig(ManagerConf managerConfiguration) throws IOException {
        OutputStream out = new FileOutputStream(new File(pathPublicRoot + "/manager_config.json"));
        ObjectMapper mapper = new ObjectMapper();

        mapper
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT);

        out.write(mapper.writeValueAsString(managerConfiguration).getBytes());
        out.close();
    }


    public void saveImageFile(String path, FileInfo fileInfo) throws IOException {
        File file = new File(pathPublicRoot + path);
        file.getParentFile().mkdirs();
        file.delete();
        OutputStream out = new FileOutputStream(file);
        out.write(CodecUtil.decodeBase64(fileInfo.getContents()));
    }
}
