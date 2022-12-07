package org.openremote.manager.configuration;

import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.configuration.ConfigurationResource;
import org.openremote.model.configuration.ManagerConf;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;

import java.io.*;
import java.util.logging.Logger;

public class ConfigurationResourceImpl extends ManagerWebResource implements ConfigurationResource {

    protected ConfigurationService configurationService;
    private static final Logger LOG = Logger.getLogger(WebService.class.getName());

    public ConfigurationResourceImpl(TimerService timerService, ManagerIdentityService identityService, ConfigurationService configurationService) {
        super(timerService, identityService);
        this.configurationService = configurationService;
    }



    @Override
    public ManagerConf update(RequestParams requestParams, ManagerConf managerConfiguration) {
        LOG.info("Uploading new manger_config.json");
        try{
            this.configurationService.saveMangerConfig(managerConfiguration);
        } catch (IOException exception){
            LOG.warning(exception.getMessage());
        }
        LOG.info("manger_config.json saved");
        return managerConfiguration;
    }

    @Override
    public String fileUpload(RequestParams requestParams, String path, FileInfo fileInfo){
        LOG.info("Uploading image to: " + path);
        try{
            this.configurationService.saveImageFile(path, fileInfo);
        } catch (IOException exception){
            LOG.warning(exception.getMessage());
        }
        LOG.info("Image saved");
        return path;
    }
}
