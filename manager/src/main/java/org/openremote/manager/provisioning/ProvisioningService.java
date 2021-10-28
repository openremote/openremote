/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.provisioning;

import org.apache.commons.io.IOUtils;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.impl.WeatherAsset;
import org.openremote.model.provisioning.ProvisioningConfig;
import org.openremote.model.provisioning.X509ProvisioningConfig;
import org.openremote.model.provisioning.X509ProvisioningData;
import org.openremote.model.provisioning.X509ProvisioningMessage;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.ValueUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class ProvisioningService implements ContainerService {

    protected static final Logger LOG = Logger.getLogger(ProvisioningService.class.getName());
    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;
    protected CertificateFactory certificateFactory;

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);


        container.getService(ManagerWebService.class).getApiSingletons().add(
            new ProvisioningResourceImpl(this, identityService)
        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public List<ProvisioningConfig<?>> getProvisioningConfigs() {
        String caPemStr = null;
        try {
            caPemStr = IOUtils.toString(getClass().getResource("/org/openremote/test/provisioning/ca_long.pem"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.singletonList(
            new X509ProvisioningConfig().setData(
                new X509ProvisioningData()
                    .setCACertPEM(caPemStr)
            ).setAssetTemplate(
                ValueUtil.asJSON(
                    new WeatherAsset("Weather Asset")
                ).orElse("")
            ).setRealm("building")
            .setRestrictedUser(true)
            .setUserRoles(new ClientRole[] {
                ClientRole.WRITE_ASSETS,
                ClientRole.READ_ASSETS
            })
        );
//
//        return persistenceService.doReturningTransaction(entityManager ->
//            entityManager.createQuery(
//                "select pc from " + ProvisioningConfig.class + " pc " +
//                    "order by pc.name desc",
//                ProvisioningConfig.class)
//                .getResultList());
    }
}
