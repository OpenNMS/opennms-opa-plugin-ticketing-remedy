/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.opa.ticketing.remedy.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRemedyConfigDao {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRemedyConfigDao.class);
    public static final String REMEDY_CONFIG_PID = "org.opennms.plugins.opa.ticketing.remedy";

    private final ConfigurationAdmin configAdmin;

    public DefaultRemedyConfigDao(final ConfigurationAdmin configAdmin) {
        this.configAdmin = Objects.requireNonNull(configAdmin);
    }

    protected Dictionary<String, Object> getProperties() throws ConfigRetrievalException {
        try {
            final Configuration configuration = this.configAdmin.getConfiguration(REMEDY_CONFIG_PID);
            if (configuration == null) return null;

            return configuration.getProperties();
        } catch (final IOException e) {
            LOG.error("Unable to get configuration from OSGi from {}.cfg", REMEDY_CONFIG_PID, e);
            throw new ConfigRetrievalException(e);
        }
    }

    protected String getStringProperty(final String key) throws ConfigRetrievalException {
        final Dictionary<String, Object> props = getProperties();
        final Object value = props.get(key);
        return value == null? null : value.toString();
    }

    protected Boolean getBooleanProperty(final String key) throws ConfigRetrievalException {
        final Dictionary<String, Object> props = getProperties();
        final Object value = props.get(key);

        if (value == null) {
            return false;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.valueOf((String)value);
        }
        throw new ConfigRetrievalException("Configuration value " + value + " was of an unknown type");
    }

    /**
     * <p>getUserName</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUserName() {
        return getStringProperty("username");
    }


    String getPassword() {
        return getStringProperty("password");
    }

    String getAuthentication() {
        return getStringProperty("authentication");
    }

    String getLocale() {
        return getStringProperty("locale");
    }

    String getTimeZone() {
        return getStringProperty("timezone");
    }

    String getEndPoint() {
        return getStringProperty("endpoint");
    }

    boolean getStrictSsl() {
        return getBooleanProperty("endpoint.strict-ssl");
    }

    String getPortName() {
        return getStringProperty("portname");
    }

    String getCreateEndPoint() {
        return getStringProperty("createendpoint");
    }

    boolean getCreateStrictSsl() {
        return getBooleanProperty("createendpoint.strict-ssl");
    }

    String getCreatePortName() {
        return getStringProperty("createportname");
    }

    List<String> getTargetGroups() {
        final String groupsString = getStringProperty("targetgroups");
        if (groupsString != null) {
            return Arrays.asList(groupsString.trim().split(":"));
        }
        return Collections.emptyList();
    }

    String getAssignedGroup() {
        return getStringProperty("assignedgroup");
    }

    String getAssignedGroup(final String targetGroup) {
        final String aGroup = getStringProperty("assignedgroup."+targetGroup);
        return aGroup == null? getAssignedGroup() : aGroup;
    }

    String getFirstName() {
        return getStringProperty("firstname");
    }

    String getLastName() {
        return getStringProperty("lastname");
    }

    String getServiceCI() {
        return getStringProperty("serviceCI");
    }

    String getServiceCIReconID() {
        return getStringProperty("serviceCIReconID");
    }

    String getAssignedSupportCompany() {
        return getStringProperty("assignedsupportcompany");
    }

    String getAssignedSupportCompany(final String targetGroup) {
        final String aCompany = getStringProperty("assignedsupportcompany."+targetGroup);
        return aCompany == null? getAssignedSupportCompany() : aCompany;
    }

    String getAssignedSupportOrganization() {
        return getStringProperty("assignedsupportorganization");
    }

    String getAssignedSupportOrganization(final String targetGroup) {
        final String anOrg = getStringProperty("assignedsupportorganization."+targetGroup);
        return anOrg == null? getAssignedSupportOrganization() : anOrg;
    }

    String getCategorizationtier1() {
        return getStringProperty("categorizationtier1");
    }

    String getCategorizationtier2() {
        return getStringProperty("categorizationtier2");
    }

    String getCategorizationtier3() {
        return getStringProperty("categorizationtier3");
    }

    String getServiceType() {
        return getStringProperty("serviceType");
    }

    String getReportedSource() {
        return getStringProperty("reportedSource");
    }

    String getImpact() {
        return getStringProperty("impact");
    }

    String getUrgency() {
        return getStringProperty("urgency");
    }

    String getResolution() {
        return getStringProperty("resolution");
    }

    String getReOpenStatusReason() {
        return getStringProperty("reason.reopen");
    }

    String getResolvedStatusReason() {
        return getStringProperty("reason.resolved");
    }

    String getCancelledStatusReason() {
        return getStringProperty("reason.cancelled");
    }
}
