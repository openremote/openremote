package org.openremote.manager.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.configuration.ConfigurationResource;
import org.openremote.model.configuration.ManagerConf;
import org.openremote.model.http.RequestParams;

import java.io.*;

public class ConfigurationResourceImpl extends ManagerWebResource implements ConfigurationResource {

    public ConfigurationResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
        super(timerService, identityService);
    }

    @Override
    public ManagerConf update(RequestParams requestParams, ManagerConf managerConfiguration) throws IOException {
        String path = System.getProperty("user.dir") + (System.getenv("OR_CUSTOM_APP_DOCROOT") != null ? System.getenv("OR_CUSTOM_APP_DOCROOT") : "/deployment/manager/app");
        OutputStream out = new FileOutputStream(new File(path + "/manager_config.json"));
        ObjectMapper mapper = new ObjectMapper();

        mapper
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT);
        out.write(mapper.writeValueAsString(managerConfiguration).getBytes());
        out.close();
        
        return managerConfiguration;
    }
}
