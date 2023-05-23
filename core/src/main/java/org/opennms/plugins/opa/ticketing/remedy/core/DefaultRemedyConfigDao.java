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
        return getStringProperty("remedy.username");
    }


    String getPassword() {
        return getStringProperty("remedy.password");
    }

    String getAuthentication() {
        return getStringProperty("remedy.authentication");
    }

    String getLocale() {
        return getStringProperty("remedy.locale");
    }

    String getTimeZone() {
        return getStringProperty("remedy.timezone");
    }

    String getEndPoint() {
        return getStringProperty("remedy.endpoint");
    }

    boolean getStrictSsl() {
        return getBooleanProperty("remedy.endpoint.strict-ssl");
    }

    String getPortName() {
        return getStringProperty("remedy.portname");
    }

    String getCreateEndPoint() {
        return getStringProperty("remedy.createendpoint");
    }

    boolean getCreateStrictSsl() {
        return getBooleanProperty("remedy.createendpoint.strict-ssl");
    }

    String getCreatePortName() {
        return getStringProperty("remedy.createportname");
    }

    List<String> getTargetGroups() {
        final String groupsString = getStringProperty("remedy.targetgroups");
        if (groupsString != null) {
            return Arrays.asList(groupsString.trim().split(":"));
        }
        return Collections.emptyList();
    }

    String getAssignedGroup() {
        return getStringProperty("remedy.assignedgroup");
    }

    String getAssignedGroup(final String targetGroup) {
        final String aGroup = getStringProperty("remedy.assignedgroup."+targetGroup);
        return aGroup == null? getAssignedGroup() : aGroup;
    }

    String getFirstName() {
        return getStringProperty("remedy.firstname");
    }

    String getLastName() {
        return getStringProperty("remedy.lastname");
    }

    String getServiceCI() {
        return getStringProperty("remedy.serviceCI");
    }

    String getServiceCIReconID() {
        return getStringProperty("remedy.serviceCIReconID");
    }

    String getAssignedSupportCompany() {
        return getStringProperty("remedy.assignedsupportcompany");
    }

    String getAssignedSupportCompany(final String targetGroup) {
        final String aCompany = getStringProperty("remedy.assignedsupportcompany."+targetGroup);
        return aCompany == null? getAssignedSupportCompany() : aCompany;
    }

    String getAssignedSupportOrganization() {
        return getStringProperty("remedy.assignedsupportorganization");
    }

    String getAssignedSupportOrganization(final String targetGroup) {
        final String anOrg = getStringProperty("remedy.assignedsupportorganization."+targetGroup);
        return anOrg == null? getAssignedSupportOrganization() : anOrg;
    }

    String getCategorizationtier1() {
        return getStringProperty("remedy.categorizationtier1");
    }

    String getCategorizationtier2() {
        return getStringProperty("remedy.categorizationtier2");
    }

    String getCategorizationtier3() {
        return getStringProperty("remedy.categorizationtier3");
    }

    String getServiceType() {
        return getStringProperty("remedy.serviceType");
    }

    String getReportedSource() {
        return getStringProperty("remedy.reportedSource");
    }

    String getImpact() {
        return getStringProperty("remedy.impact");
    }

    String getUrgency() {
        return getStringProperty("remedy.urgency");
    }

    String getResolution() {
        return getStringProperty("remedy.resolution");
    }

    String getReOpenStatusReason() {
        return getStringProperty("remedy.reason.reopen");
    }

    String getResolvedStatusReason() {
        return getStringProperty("remedy.reason.resolved");
    }

    String getCancelledStatusReason() {
        return getStringProperty("remedy.reason.cancelled");
    }
}
