/*
 *
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.device.mgt.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.device.mgt.common.DeviceManagementConstants;
import org.wso2.carbon.device.mgt.common.License;
import org.wso2.carbon.device.mgt.common.LicenseManagementException;
import org.wso2.carbon.device.mgt.core.LicenseManager;
import org.wso2.carbon.device.mgt.core.LicenseManagerImpl;
import org.wso2.carbon.device.mgt.core.config.LicenseConfigurationManager;
import org.wso2.carbon.device.mgt.core.config.LicenseManagementConfig;
import org.wso2.carbon.device.mgt.core.license.mgt.GenericArtifactManagerFactory;
import org.wso2.carbon.device.mgt.core.service.LicenseManagementService;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

import javax.xml.namespace.QName;

/**
 * @scr.component name="org.wso2.carbon.license.manager" immediate="true"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="registryService.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 */
public class LicenseManagementServiceComponent {

    private static Log log = LogFactory.getLog(LicenseManagementServiceComponent.class);

    protected void activate(ComponentContext componentContext) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Initializing license management core bundle");
            }
            LicenseManager licenseManager = new LicenseManagerImpl();
            LicenseManagementDataHolder.getInstance().setLicenseManager(licenseManager);

            /* If -Dsetup option enabled then create creates default license management */
            String setupOption = System.getProperty(
                    org.wso2.carbon.device.mgt.core.DeviceManagementConstants.Common.PROPERTY_SETUP);

            if (setupOption != null) {
                if (log.isDebugEnabled()) {
                    log.debug("-Dsetup is enabled.Check default licenses and  add if not exists in registry");
                }
                LicenseConfigurationManager.getInstance().initConfig();
                LicenseManagementConfig licenseManagementConfig = LicenseConfigurationManager.getInstance()
                        .getLicenseMgtConfig();
                addDefaultLicenses(licenseManagementConfig);
            }
            if (log.isDebugEnabled()) {
                log.debug("Registering OSGi service LicenseManagementService");
            }
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext
                    .registerService(LicenseManagementService.class.getName(), new LicenseManagementService(), null);
            if (log.isDebugEnabled()) {
                log.debug("License management core bundle has been successfully initialized");
            }
        } catch (Throwable throwable) {
            String msg = "Error occurred while initializing license management core bundle";
            log.error(msg, throwable);
        }
    }

    private void addDefaultLicenses(LicenseManagementConfig licenseManagementConfig) throws LicenseManagementException {
        try {
            GenericArtifactManager artifactManager =
                    GenericArtifactManagerFactory.getTenantAwareGovernanceArtifactManager();

            GenericArtifact artifact;
            for (License license : licenseManagementConfig.getLicenses()) {
                artifact = artifactManager.newGovernanceArtifact(new QName("http://www.wso2.com",
                        DeviceManagementConstants.LicenseProperties.LICENSE_REGISTRY_KEY));
                artifact.setAttribute(DeviceManagementConstants.LicenseProperties.OVERVIEW_NAME, license.getName());
                artifact.setAttribute(DeviceManagementConstants.LicenseProperties.OVERVIEW_VERSION,
                        license.getVersion());
                artifact.setAttribute(DeviceManagementConstants.LicenseProperties.OVERVIEW_LANGUAGE,
                        license.getLanguage());
                artifact.setAttribute(DeviceManagementConstants.LicenseProperties.OVERVIEW_PROVIDER,
                        license.getProvider());
                artifact.setAttribute(DeviceManagementConstants.LicenseProperties.VALID_TO,
                        license.getValidTo().toString());
                artifact.setAttribute(DeviceManagementConstants.LicenseProperties.VALID_FROM,
                        license.getValidFrom().toString());
                artifact.setAttribute(DeviceManagementConstants.LicenseProperties.LICENSE, license.getText());
                artifactManager.addGenericArtifact(artifact);
            }
        } catch (GovernanceException e) {
            String msg = "Error occurred while initializing default licences";
            throw new LicenseManagementException(msg, e);
        }
    }

    /**
     * Sets Realm Service.
     *
     * @param realmService An instance of RealmService
     */
    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting Realm Service in license management");
        }
        LicenseManagementDataHolder.getInstance().setRealmService(realmService);
    }

    /**
     * Unsets Realm Service.
     *
     * @param realmService An instance of RealmService
     */
    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Un setting Realm Service in license management");
        }
        LicenseManagementDataHolder.getInstance().setRealmService(null);
    }

    /**
     * Sets Registry Service.
     *
     * @param registryService An instance of RegistryService
     */
    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting Registry Service");
        }
        LicenseManagementDataHolder.getInstance().setRegistryService(registryService);
    }

    /**
     * Unsets Registry Service.
     *
     * @param registryService An instance of RegistryService
     */
    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Unsetting Registry Service");
        }
        LicenseManagementDataHolder.getInstance().setRegistryService(null);
    }

}
