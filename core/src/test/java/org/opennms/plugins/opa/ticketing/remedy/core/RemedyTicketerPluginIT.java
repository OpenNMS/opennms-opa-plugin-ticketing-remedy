/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opennms.integration.api.v1.ticketing.Ticket;
import org.opennms.integration.api.v1.ticketing.Ticket.State;
import org.opennms.integration.api.v1.ticketing.immutables.ImmutableTicket;
import org.opennms.integration.remedy.ticketservice.AuthenticationInfo;
import org.opennms.integration.remedy.ticketservice.CreateInputMap;
import org.opennms.integration.remedy.ticketservice.CreateOutputMap;
import org.opennms.integration.remedy.ticketservice.HPDIncidentInterfaceCreateWSPortTypePortType;
import org.opennms.integration.remedy.ticketservice.HPDIncidentInterfaceWSPortTypePortType;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.github.skjolber.mockito.soap.SoapServiceExtension;

class RemedyTicketerPluginIT {
    private HPDIncidentInterfaceWSPortTypePortType mockIncidentPort;
    private HPDIncidentInterfaceCreateWSPortTypePortType mockIncidentCreatePort;

    @BeforeEach
    void setup(final SoapServiceExtension soap) {
        mockIncidentPort = soap.mock(HPDIncidentInterfaceWSPortTypePortType.class, "http://localhost:12345");
        mockIncidentCreatePort = soap.mock(HPDIncidentInterfaceCreateWSPortTypePortType.class, "http://localhost:12346");
    }

    @Test
    @ExtendWith(SoapServiceExtension.class)
    void testGetNoTicket() {
        final RemedyTicketerPlugin plugin = new RemedyTicketerPlugin(new MockConfigurationAdmin());
        plugin.readPort = mockIncidentPort;
        plugin.createPort = mockIncidentCreatePort;

        when(mockIncidentPort.helpDeskQueryService(any(), any())).thenReturn(null);

        RemedyTicketerException e = assertThrows(RemedyTicketerException.class, () -> {
            plugin.get("12345");
        });
        assertNotNull(e.getCause());
        assertEquals("Unable to retrieve ticket, or ticket ID '12345' invalid.", e.getCause().getMessage());
    }

    @Test
    @ExtendWith(SoapServiceExtension.class)
    void testCreateNewTicket() throws Exception {
        final RemedyTicketerPlugin plugin = new RemedyTicketerPlugin(new MockConfigurationAdmin());
        plugin.readPort = mockIncidentPort;
        plugin.createPort = mockIncidentCreatePort;

        final Ticket ticket = ImmutableTicket.newBuilder()
                .setAlarmId(3)
                .setDetails("Yo, this is a unit test ticket")
                .setIpAddress(InetAddress.getLocalHost())
                .setNodeId(1)
                .setState(State.OPEN)
                .setSummary("Test OpenNMS Integration")
                .setUser("ranger@opennms.com")
                .build();

        final CreateOutputMap outputMap = new CreateOutputMap();
        outputMap.setIncidentNumber("hehehe");
        when(mockIncidentCreatePort.helpDeskSubmitService(any(AuthenticationInfo.class), any(CreateInputMap.class))).thenReturn(outputMap);

        final String incidentNumber = plugin.saveOrUpdate(ticket);
        assertEquals("hehehe", incidentNumber);
    }

    @Test
    @ExtendWith(SoapServiceExtension.class)
    void testUpdateTicket() throws Exception {
        final RemedyTicketerPlugin plugin = new RemedyTicketerPlugin(new MockConfigurationAdmin());
        plugin.readPort = mockIncidentPort;
        plugin.createPort = mockIncidentCreatePort;

        final Ticket ticket = ImmutableTicket.newBuilder()
                .setId("hehehe")
                .setAlarmId(3)
                .setDetails("Yo, this is a unit test ticket")
                .setIpAddress(InetAddress.getLocalHost())
                .setNodeId(1)
                .setState(State.OPEN)
                .setSummary("Test OpenNMS Integration")
                .setUser("ranger@opennms.com")
                .build();

        final CreateOutputMap outputMap = new CreateOutputMap();
        outputMap.setIncidentNumber("hehehe");
        when(mockIncidentCreatePort.helpDeskSubmitService(any(AuthenticationInfo.class), any(CreateInputMap.class))).thenReturn(outputMap);

        final String incidentNumber = plugin.saveOrUpdate(ticket);
        assertEquals("hehehe", incidentNumber);
    }

    static class MockConfigurationAdmin implements ConfigurationAdmin {
        @Override
        public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public Configuration getConfiguration(String pid, String location) throws IOException {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public Configuration getConfiguration(String pid) throws IOException {
            return new MockConfiguration();
        }

        @Override
        public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
            throw new IllegalStateException("not yet implemented!");
        }
    }

    static class MockConfiguration implements Configuration {
        private Dictionary<String, Object> dict = new Hashtable<>();

        public MockConfiguration() {
            dict.put("username", "opennmstnn");
            dict.put("password", "TNNwsC4ll");
            dict.put("authentication", "ARSystem");
            dict.put("locale", "it_IT");
            dict.put("timezone", "CET");
            dict.put("endpoint", "http://localhost:12345");
            dict.put("portname", "HPD_IncidentInterface_WSPortTypeSoap");
            dict.put("createendpoint", "http://localhost:12346");
            dict.put("createportname", "HPD_IncidentInterface_Create_WSPortTypeSoap");
            dict.put("targetgroups", "TNnet:Tetranet");
            dict.put("assignedgroup.TNnet", "TNnet");
            dict.put("assignedgroup.Tetranet", "TNnet - Tetranet");
            dict.put("assignedsupportcompany.TNnet", "Trentino Network srl");
            dict.put("assignedsupportcompany.Tetranet", "Trentino Network srl");
            dict.put("assignedsupportorganization.TNnet", "Centro Gestione Rete");
            dict.put("assignedsupportorganization.Tetranet", "Centro Gestione Rete");

            dict.put("assignedgroup", "TNnet");
            dict.put("firstname", "Opennms");
            dict.put("lastname", "Tnn");
            dict.put("serviceCI", "Trentino Network Connettivit\uFFFD [C.TNNCN]");
            dict.put("serviceCIReconID", "RE00505688005e3s-nTg4KEI5gFSov");
            dict.put("assignedsupportcompany", "Trentino Network srl");
            dict.put("assignedsupportorganization", "Centro Gestione Rete");
            dict.put("categorizationtier1", "Incident");
            dict.put("categorizationtier2", "Generic");
            dict.put("categorizationtier3", "Non bloccante");
            dict.put("serviceType", "Infrastructure Event");
            dict.put("reportedSource", "Direct Input");
            dict.put("impact", "4-Minor/Localized");

            dict.put("urgency", "4-Low");
            dict.put("reason.reopen", "Pending Original Incident");
            dict.put("resolution", "Chiusura da OpenNMS Web Service");
            dict.put("reason.resolved", "Automated Resolution Reported");
            dict.put("reason.cancelled", "No longer a Causal CI");
        }

        @Override
        public String getPid() {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public Dictionary<String, Object> getProperties() {
            return dict;
        }

        @Override
        public void update(Dictionary<String, ?> properties) throws IOException {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public void delete() throws IOException {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public String getFactoryPid() {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public void update() throws IOException {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public void setBundleLocation(String location) {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public String getBundleLocation() {
            throw new IllegalStateException("not yet implemented!");
        }

        @Override
        public long getChangeCount() {
            throw new IllegalStateException("not yet implemented!");
        }
    }
}
