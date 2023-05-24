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

import java.util.Date;
import java.util.GregorianCalendar;

import javax.net.ssl.TrustManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.opennms.integration.api.v1.ticketing.Ticket;
import org.opennms.integration.api.v1.ticketing.Ticket.State;
import org.opennms.integration.api.v1.ticketing.TicketingPlugin;
import org.opennms.integration.api.v1.ticketing.immutables.ImmutableTicket;
import org.opennms.integration.api.v1.ticketing.immutables.ImmutableTicket.Builder;
import org.opennms.integration.remedy.ticketservice.AuthenticationInfo;
import org.opennms.integration.remedy.ticketservice.CreateInputMap;
import org.opennms.integration.remedy.ticketservice.GetInputMap;
import org.opennms.integration.remedy.ticketservice.GetOutputMap;
import org.opennms.integration.remedy.ticketservice.HPDIncidentInterfaceCreateWSPortTypePortType;
import org.opennms.integration.remedy.ticketservice.HPDIncidentInterfaceCreateWSService;
import org.opennms.integration.remedy.ticketservice.HPDIncidentInterfaceWSPortTypePortType;
import org.opennms.integration.remedy.ticketservice.HPDIncidentInterfaceWSService;
import org.opennms.integration.remedy.ticketservice.ReportedSourceType;
import org.opennms.integration.remedy.ticketservice.ServiceTypeType;
import org.opennms.integration.remedy.ticketservice.SetInputMap;
import org.opennms.integration.remedy.ticketservice.StatusReasonType;
import org.opennms.integration.remedy.ticketservice.StatusType;
import org.opennms.integration.remedy.ticketservice.VIPType;
import org.opennms.integration.remedy.ticketservice.WorkInfoSourceType;
import org.opennms.integration.remedy.ticketservice.WorkInfoTypeType;
import org.opennms.integration.remedy.ticketservice.WorkInfoViewAccessType;
import org.opennms.plugins.opa.ticketing.remedy.core.utils.AnyServerX509TrustManager;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenNMS Trouble Ticket Plugin API implementation for Remedy
 *
 * @author <a href="mailto:jonathan@opennms.org">Jonathan Sartin</a>
 * @author <a href="antonio@opennms.it">Antonio Russo</a>
 */
public class RemedyTicketerPlugin implements TicketingPlugin {
    private static final String DEFAULT_URGENCY_TYPE = "4-Low";

    private static final Logger LOG = LoggerFactory.getLogger(RemedyTicketerPlugin.class);

    private DefaultRemedyConfigDao m_configDao;

    private String m_endpoint;
    private String m_portname;
    private String m_createendpoint;
    private String m_createportname;

    public static final String ATTRIBUTE_NODE_LABEL_ID = "nodelabel";

    private static final String ACTION_CREATE="CREATE";
    private static final String ACTION_MODIFY="MODIFY";

    private static final String ATTRIBUTE_USER_COMMENT_ID = "remedy.user.comment";
    private static final String ATTRIBUTE_URGENCY_ID="remedy.urgency";
    private static final String ATTRIBUTE_ASSIGNED_GROUP_ID="remedy.assignedgroup";

    private static final int MAX_SUMMARY_CHARS=99;

    HPDIncidentInterfaceWSPortTypePortType readPort;
    HPDIncidentInterfaceCreateWSPortTypePortType createPort;

    // Remember:
    // Summary ---> alarm logmsg
    // Details ---> alarm descr
    // State   ---> OPEN,CLOSE, CANCELLED
    // User    ---> The owner of the ticket --who create the ticket
    // Attributes --->list of free form attributes in the Ticket.  Typically, from
    // the OnmsAlarm attributes.

    /**
     * <p>Constructor for RemedyTicketerPlugin.</p>
     */
    public RemedyTicketerPlugin(ConfigurationAdmin configAdmin) {
        m_configDao = new DefaultRemedyConfigDao(configAdmin);
        m_endpoint = m_configDao.getEndPoint();
        m_portname = m_configDao.getPortName();
        m_createendpoint = m_configDao.getCreateEndPoint();
        m_createportname = m_configDao.getCreatePortName();
    }

    /** {@inheritDoc} */
    @Override
    public Ticket get(final String ticketId) {
        if (ticketId == null)  {

            LOG.error("No Remedy ticketID available in OpenNMS Ticket");
            throw new RemedyTicketerException("No Remedy ticketID available in OpenNMS Ticket");
        }

        LOG.debug("get: search ticket with id: {}", ticketId);
        final HPDIncidentInterfaceWSPortTypePortType port = getTicketServicePort(m_portname,m_endpoint);

        if (port == null) {
            throw new RemedyTicketerException("Unable to retrieve port for port=" + m_portname + ", endpoint=" + m_endpoint);
        }

        try {
            final GetOutputMap outputmap = port.helpDeskQueryService(getRemedyInputMap(ticketId), getRemedyAuthenticationHeader());

            if (outputmap == null || outputmap.getStatus() == null || outputmap.getUrgency() == null) {
                throw new RemedyTicketerException("Unable to retrieve ticket, or ticket ID '" + ticketId + "' invalid.");
            }

            LOG.info("get: found ticket: {} status: {}", ticketId, outputmap.getStatus());
            LOG.info("get: found ticket: {} urgency: {}", ticketId, outputmap.getUrgency());

            final Builder builder = ImmutableTicket.newBuilder();
            builder.setId(ticketId);
            builder.setSummary(outputmap.getSummary());
            builder.setDetails(outputmap.getNotes());
            builder.setState(getState(outputmap.getStatus()));
            builder.setUser(outputmap.getAssignedGroup());
            return builder.build();
        } catch (final Exception e) {
            throw new RemedyTicketerException("Problem getting ticket", e);
        }
    }


    private State getState(final StatusType status) {
        State state = State.OPEN;
        if (status == StatusType.CLOSED || status == StatusType.RESOLVED) {
            state = State.CLOSED;
        } else if (status == StatusType.CANCELLED)
            state = State.CANCELLED;
        return state;
    }

    /** {@inheritDoc} */
    @Override
    public String saveOrUpdate(final Ticket ticket) {
        if ((ticket.getId() == null) ) {
            return save(ticket);
        } else {
            update(ticket);
        }
        return ticket.getId();
    }

    private void update(final Ticket ticket) {
        final HPDIncidentInterfaceWSPortTypePortType port = getTicketServicePort(m_portname,m_endpoint);
        final String ticketId = ticket.getId();

        if (port != null) {
            try {
                final GetOutputMap remedy = port.helpDeskQueryService(getRemedyInputMap(ticket.getId()), getRemedyAuthenticationHeader());
                if (remedy == null) {
                    LOG.error("update: Remedy: Cannot find incident with incident_number: {}", ticket.getId());
                    return;
                }
                if (remedy.getStatus() == StatusType.CANCELLED) {
                    LOG.info("update: Remedy: Ticket Cancelled. Skipping updating ticket with incident_number: {}", ticketId);
                    return;
                }
                if (remedy.getStatus() == StatusType.CLOSED) {
                    LOG.info("update: Remedy: Ticket Closed. Skipping updating ticket with incident_number: {}", ticketId);
                    return;
                }

                SetInputMap output = getRemedySetInputMap(ticket,remedy);

                // The only things to update are urgency and state
                LOG.debug("update: Remedy: found urgency: {} - for ticket with incident_number: {}", output.getUrgency(), ticket.getId());
                output.setUrgency(getUrgency(ticket));

                LOG.debug("update: opennms status: {} - for ticket with incident_number: {}", ticket.getState(), ticket.getId());

                LOG.debug("update: Remedy: found status: {} - for ticket with incident_number: {}", output.getStatus(), ticket.getId());
                State outputState = getState(output.getStatus());
                LOG.debug("update: Remedy: found opennms status: {} - for ticket with incident_number: {}", outputState, ticket.getId());
                if (ticket.getState() != outputState) {
                    output = opennmsToRemedyState(output,ticket.getState());
                }

                port.helpDeskModifyService(output , getRemedyAuthenticationHeader());
            } catch (final Exception e) {
                throw new RemedyTicketerException("Problem creating ticket", e);
            }
        }

    }

    private SetInputMap getRemedySetInputMap(Ticket ticket, GetOutputMap output) {
        DatatypeFactory datatypeFactory;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (final DatatypeConfigurationException e) {
            throw new RemedyTicketerException("Failed to initialize datatype factory", e);
        }
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(new Date());
        XMLGregorianCalendar cal = datatypeFactory.newXMLGregorianCalendar(gc);

        final SetInputMap sim = new SetInputMap();
        sim.setCategorizationTier1(output.getCategorizationTier1());
        sim.setCategorizationTier2(output.getCategorizationTier2());
        sim.setCategorizationTier3(output.getCategorizationTier3());
        sim.setClosureManufacturer(output.getClosureManufacturer());
        sim.setClosureProductCategoryTier1(output.getClosureProductCategoryTier1());
        sim.setClosureProductCategoryTier2(output.getClosureProductCategoryTier2());
        sim.setClosureProductCategoryTier3(output.getClosureProductCategoryTier3());
        sim.setClosureProductModelVersion(output.getClosureProductModelVersion());
        sim.setClosureProductName(output.getClosureProductName());
        sim.setCompany(output.getCompany());
        sim.setSummary(output.getSummary());
        sim.setNotes(output.getNotes());
        sim.setImpact(output.getImpact());
        sim.setManufacturer(output.getManufacturer());
        sim.setProductCategorizationTier1(output.getProductCategorizationTier1());
        sim.setProductCategorizationTier2(output.getProductCategorizationTier2());
        sim.setProductCategorizationTier3(output.getProductCategorizationTier3());
        sim.setProductModelVersion(output.getProductModelVersion());
        sim.setProductName(output.getProductName());
        sim.setReportedSource(output.getReportedSource());
        sim.setResolution(output.getResolution());
        sim.setResolutionCategory(output.getResolutionCategory());
        sim.setResolutionCategoryTier2(output.getResolutionCategoryTier2());
        sim.setResolutionCategoryTier3(output.getResolutionCategoryTier3());
        sim.setResolutionMethod("");
        sim.setServiceType(output.getServiceType());
        sim.setStatus(output.getStatus());
        sim.setUrgency(output.getUrgency());
        sim.setAction(ACTION_MODIFY);
        sim.setWorkInfoSummary("");
        sim.setWorkInfoNotes("");
        sim.setWorkInfoType(WorkInfoTypeType.SATISFACTION_SURVEY);
        sim.setWorkInfoDate(cal);
        sim.setWorkInfoSource(WorkInfoSourceType.EMAIL);
        sim.setWorkInfoLocked(VIPType.NO);
        sim.setWorkInfoViewAccess(WorkInfoViewAccessType.PUBLIC);
        sim.setIncidentNumber(ticket.getId());
        sim.setStatusReason(output.getStatusReason());
        sim.setServiceCI(output.getServiceCI());
        sim.setServiceCIReconID(output.getServiceCIReconID());
        sim.setHPDCI(output.getHPDCI());
        sim.setHPDCIReconID(output.getHPDCIReconID());
        sim.setHPDCIFormName(output.getHPDCIFormName());
        sim.setZ1DCIFormName(output.getZ1DCIFormName());
        sim.setWorkInfoAttachment1Name("");
        sim.setWorkInfoAttachment1Data(new byte[0]);
        sim.setWorkInfoAttachment1OrigSize(0);
        return sim;
    }

    private String getUrgency(final Ticket ticket) {
        try {
            if (ticket.getAttributes().get(ATTRIBUTE_URGENCY_ID) != null) {
                return ticket.getAttributes().get(ATTRIBUTE_URGENCY_ID);
            }
            return m_configDao.getUrgency();
        } catch (IllegalArgumentException e) {
            return  DEFAULT_URGENCY_TYPE;
        }
    }

    private String getAssignedGroup(Ticket ticket) {
        if (ticket.getAttributes().get(ATTRIBUTE_ASSIGNED_GROUP_ID) != null) {
            for ( String group : m_configDao.getTargetGroups()) {
                if (group.equals(ticket.getAttributes().get(ATTRIBUTE_ASSIGNED_GROUP_ID)))
                    return m_configDao.getAssignedGroup(group);
            }
        }
        return m_configDao.getAssignedGroup();
    }

    private String getAssignedSupportCompany(Ticket ticket) {
        if (ticket.getAttributes().get(ATTRIBUTE_ASSIGNED_GROUP_ID) != null) {
            for ( String group : m_configDao.getTargetGroups()) {
                if (group.equals(ticket.getAttributes().get(ATTRIBUTE_ASSIGNED_GROUP_ID)))
                    return m_configDao.getAssignedSupportCompany(group);
            }
        }
        return m_configDao.getAssignedSupportCompany();
    }

    private String getAssignedSupportOrganization(Ticket ticket) {
        if (ticket.getAttributes().get(ATTRIBUTE_ASSIGNED_GROUP_ID) != null) {
            for ( String group : m_configDao.getTargetGroups()) {
                if (group.equals(ticket.getAttributes().get(ATTRIBUTE_ASSIGNED_GROUP_ID)))
                    return m_configDao.getAssignedSupportOrganization(group);
            }
        }
        return m_configDao.getAssignedSupportOrganization();
    }


    private String getSummary(final Ticket ticket) {
        final StringBuilder summary = new StringBuilder();
        if (ticket.getAttributes().get(ATTRIBUTE_NODE_LABEL_ID) != null) {
            summary.append(ticket.getAttributes().get(ATTRIBUTE_NODE_LABEL_ID));
            summary.append(": OpenNMS: ");
        }
        summary.append(ticket.getSummary());
        if (summary.length() > MAX_SUMMARY_CHARS)
            return summary.substring(0,MAX_SUMMARY_CHARS-1);
        return summary.toString();
    }

    private String getNotes(Ticket ticket) {
        final StringBuilder notes = new StringBuilder("OpenNMS generated ticket by user: ");
        notes.append(ticket.getUser());
        notes.append("\n");
        notes.append("\n");
        if (ticket.getAttributes().get(ATTRIBUTE_USER_COMMENT_ID) != null ) {
             notes.append("OpenNMS user comment: ");
             notes.append(ticket.getAttributes().get(ATTRIBUTE_USER_COMMENT_ID));
            notes.append("\n");
            notes.append("\n");
        }
        notes.append("OpenNMS logmsg: ");
        notes.append(ticket.getSummary());
        notes.append("\n");
        notes.append("\n");
        notes.append("OpenNMS descr: ");
        notes.append(ticket.getDetails());
        return notes.toString();
    }

    private SetInputMap opennmsToRemedyState(SetInputMap inputmap, State state) {
        LOG.debug("getting remedy state from OpenNMS State: {}", state);

        switch (state) {
            case OPEN:
                inputmap.setStatus(StatusType.PENDING);
                inputmap.setStatusReason(StatusReasonType.fromValue(m_configDao.getReOpenStatusReason()));
                break;
            case CANCELLED:
                inputmap.setStatus(StatusType.CANCELLED);
                inputmap.setStatusReason(StatusReasonType.fromValue(m_configDao.getCancelledStatusReason()));
                break;
            case CLOSED:
                inputmap.setStatus(StatusType.RESOLVED);
                inputmap.setStatusReason(StatusReasonType.fromValue(m_configDao.getResolvedStatusReason()));
                inputmap.setResolution(m_configDao.getResolution());
                break;
            default:
                LOG.debug("No valid OpenNMS state on ticket skipping status change");
        }

        LOG.debug("OpenNMS state was        {}", state);
        LOG.debug("setting Remedy state ID to {}", inputmap.getStatus());


        return inputmap;
    }

    private GetInputMap getRemedyInputMap(String ticketId) {
        GetInputMap parameters = new GetInputMap();
        parameters.setIncidentNumber(ticketId);
        return parameters;

    }

    private AuthenticationInfo getRemedyAuthenticationHeader() {
        final AuthenticationInfo requestHeader = new AuthenticationInfo();
        requestHeader.setUserName(m_configDao.getUserName());
        requestHeader.setPassword(m_configDao.getPassword());

        final String authentication = m_configDao.getAuthentication();
        if (authentication != null) {
            requestHeader.setAuthentication(authentication);
        }
        final String locale = m_configDao.getLocale();
        if (locale != null && !locale.isEmpty()) {
            requestHeader.setLocale(locale);
        }
        final String timezone = m_configDao.getTimeZone();
        if (timezone != null && !timezone.isEmpty()) {
            requestHeader.setTimeZone(timezone);
        }
        return requestHeader;
    }


    private CreateInputMap getRemedyCreateInputMap(final Ticket newTicket) {
        final CreateInputMap createInputMap = new CreateInputMap();

        // the only data set by the opennms ticket alarm
        createInputMap.setSummary(getSummary(newTicket));
        createInputMap.setNotes(getNotes(newTicket));

        // all this is mandatory and set using the configuration file
        createInputMap.setFirstName(m_configDao.getFirstName());
        createInputMap.setLastName(m_configDao.getLastName());
        createInputMap.setServiceCI(m_configDao.getServiceCI());
        createInputMap.setServiceCIReconID(m_configDao.getServiceCIReconID());
        createInputMap.setImpact(m_configDao.getImpact());
        createInputMap.setReportedSource(ReportedSourceType.fromValue(m_configDao.getReportedSource()));
        createInputMap.setServiceType(ServiceTypeType.fromValue(m_configDao.getServiceType()));
        createInputMap.setUrgency(getUrgency(newTicket));
        createInputMap.setStatus(StatusType.NEW);
        createInputMap.setAction(ACTION_CREATE);
        createInputMap.setCategorizationTier1(m_configDao.getCategorizationtier1());
        createInputMap.setCategorizationTier2(m_configDao.getCategorizationtier2());
        createInputMap.setCategorizationTier3(m_configDao.getCategorizationtier3());
        createInputMap.setAssignedGroup(getAssignedGroup(newTicket));
        createInputMap.setAssignedSupportCompany(getAssignedSupportCompany(newTicket));
        createInputMap.setAssignedSupportOrganization(getAssignedSupportOrganization(newTicket));

        return createInputMap;
    }

    private String save(final Ticket newTicket) {
        final HPDIncidentInterfaceCreateWSPortTypePortType port = getCreateTicketServicePort(m_createportname,m_createendpoint);

        try {
            final String incidentNumber = port.helpDeskSubmitService(getRemedyAuthenticationHeader(), getRemedyCreateInputMap(newTicket)).getIncidentNumber();
            LOG.debug("created new remedy ticket with reported incident number: {}", incidentNumber);
            return incidentNumber;
        } catch (final Exception e) {
            throw new RemedyTicketerException("Problem saving ticket", e);
        }

    }

    /**
     * Convenience method for initializing the ticketServicePort and correctly setting the endpoint.
     *
     * @return TicketServicePort to connect to the remote service.
     */

    private HPDIncidentInterfaceWSPortTypePortType getTicketServicePort(final String portname, final String endpoint) {
        if (readPort == null) {
            final QName hpdPortname = new QName("HPD_IncidentInterface_WS", portname);
            final HPDIncidentInterfaceWSService service = new HPDIncidentInterfaceWSService(HPDIncidentInterfaceWSService.WSDL_LOCATION, hpdPortname);
            final HPDIncidentInterfaceWSPortTypePortType port = service.getHPDIncidentInterfaceWSPortTypeSoap();

            final Client cxfClient = ClientProxy.getClient(port);

            cxfClient.getRequestContext().put(Message.ENDPOINT_ADDRESS, endpoint);
            final HTTPConduit http = (HTTPConduit) cxfClient.getConduit();

            if (!m_configDao.getStrictSsl()) {
                LOG.debug("Disabling strict SSL checking.");
                // Accept all certificates
                final TrustManager[] simpleTrustManager = new TrustManager[] { new AnyServerX509TrustManager() };
                final TLSClientParameters tlsParams = new TLSClientParameters();
                tlsParams.setTrustManagers(simpleTrustManager);
                tlsParams.setDisableCNCheck(true);
                http.setTlsClientParameters(tlsParams);
            }
            readPort = port;
        }

        return readPort;
    }


    /**
     * Convenience method for initializing the ticketServicePort and correctly setting the endpoint.
     *
     * @return TicketServicePort to connect to the remote service.
     */

    private HPDIncidentInterfaceCreateWSPortTypePortType getCreateTicketServicePort(final String portname, final String endpoint) {
        if (createPort == null) {
            final QName hpdPortname = new QName("urn:HPD_IncidentInterface_Create_WS", portname);
            final HPDIncidentInterfaceCreateWSService service = new HPDIncidentInterfaceCreateWSService(HPDIncidentInterfaceCreateWSService.WSDL_LOCATION, hpdPortname);
            final HPDIncidentInterfaceCreateWSPortTypePortType port = service.getHPDIncidentInterfaceCreateWSPortTypeSoap();

            final Client cxfClient = ClientProxy.getClient(port);

            cxfClient.getRequestContext().put(Message.ENDPOINT_ADDRESS, endpoint);
            final HTTPConduit http = (HTTPConduit) cxfClient.getConduit();

            if (!m_configDao.getStrictSsl()) {
                LOG.debug("Disabling strict SSL checking.");
                // Accept all certificates
                final TrustManager[] simpleTrustManager = new TrustManager[] { new AnyServerX509TrustManager() };
                final TLSClientParameters tlsParams = new TLSClientParameters();
                tlsParams.setTrustManagers(simpleTrustManager);
                tlsParams.setDisableCNCheck(true);
                http.setTlsClientParameters(tlsParams);
            }

            createPort = port;
        }

        return createPort;
    }
}
