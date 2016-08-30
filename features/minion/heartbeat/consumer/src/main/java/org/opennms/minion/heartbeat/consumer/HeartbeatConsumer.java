/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.minion.heartbeat.consumer;

import java.util.Collections;
import java.util.Date;

import org.opennms.core.ipc.sink.api.MessageConsumer;
import org.opennms.core.ipc.sink.api.MessageConsumerManager;
import org.opennms.core.ipc.sink.api.SinkModule;
import org.opennms.minion.heartbeat.common.HeartbeatModule;
import org.opennms.minion.heartbeat.common.MinionIdentityDTO;
import org.opennms.netmgt.dao.api.MinionDao;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.EventProxy;
import org.opennms.netmgt.events.api.EventProxyException;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.model.minion.OnmsMinion;
import org.opennms.netmgt.provision.persist.ForeignSourceRepository;
import org.opennms.netmgt.provision.persist.foreignsource.ForeignSource;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.annotation.Transactional;

public class HeartbeatConsumer implements MessageConsumer<MinionIdentityDTO>, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatConsumer.class);

    private static final HeartbeatModule heartbeatModule = new HeartbeatModule();

    @Autowired
    private MinionDao minionDao;

    @Autowired
    private MessageConsumerManager messageConsumerManager;

    @Autowired
    @Qualifier("deployed")
    private ForeignSourceRepository m_deployedForeignSourceRepository;

    @Autowired
    @Qualifier("eventProxy")
    private EventProxy m_eventProxy;

    @Override
    @Transactional
    public void handleMessage(MinionIdentityDTO minionHandle) {
        LOG.info("Received heartbeat for Minion with id: {} at location: {}",
                minionHandle.getId(), minionHandle.getLocation());
        OnmsMinion minion = minionDao.findById(minionHandle.getId());

        // defines whether the requisition has changed and has to be synchronized
        boolean requisitionUpdated = false;
        // defines whether this is the initial creation of the requisition
        boolean requisitionCreated = false;

        if (minion == null) {
            minion = new OnmsMinion();
            minion.setId(minionHandle.getId());
            minion.setLocation(minionHandle.getLocation());
        }

        final String foreignSourceName = "Minions@"+ minion.getLocation();

        Requisition requisition = m_deployedForeignSourceRepository.getRequisition(foreignSourceName);
        if (requisition == null) {
            requisition = new Requisition(foreignSourceName);

            requisitionCreated = true;
        }

        RequisitionNode requisitionNode = requisition.getNode(minion.getId());

        if (requisitionNode == null) {
            final RequisitionMonitoredService requisitionMonitoredService = new RequisitionMonitoredService();
            requisitionMonitoredService.setServiceName("Minion-Heartbeat");

            final RequisitionInterface requisitionInterface = new RequisitionInterface();
            requisitionInterface.setIpAddr("127.0.0.1");
            requisitionInterface.putMonitoredService(requisitionMonitoredService);

            requisitionNode = new RequisitionNode();
            requisitionNode.setNodeLabel(minion.getId());
            requisitionNode.setForeignId(minion.getLabel() != null ? minion.getLabel() : minion.getId());
            requisitionNode.setLocation(minion.getLocation());
            requisitionNode.putInterface(requisitionInterface);

            requisition.putNode(requisitionNode);

            requisitionUpdated = true;
        }

        if (requisitionCreated || requisitionUpdated) {
            requisition.setDate(new Date());
            m_deployedForeignSourceRepository.save(requisition);
            m_deployedForeignSourceRepository.flush();

            if (requisitionCreated) {
                final ForeignSource foreignSource = m_deployedForeignSourceRepository.getForeignSource(foreignSourceName);
                foreignSource.setDetectors(Collections.emptyList());
                foreignSource.setPolicies(Collections.emptyList());
                m_deployedForeignSourceRepository.save(foreignSource);
            }

            final EventBuilder eventBuilder = new EventBuilder(EventConstants.RELOAD_IMPORT_UEI, "Web");
            eventBuilder.addParam(EventConstants.PARM_URL, String.valueOf(m_deployedForeignSourceRepository.getRequisitionURL(foreignSourceName)));

            try {
                m_eventProxy.send(eventBuilder.getEvent());
            } catch (final EventProxyException e) {
                throw new DataAccessResourceFailureException("Unable to send event to import group " + foreignSourceName, e);
            }
        }

        Date lastUpdated = new Date();
        minion.setLastUpdated(lastUpdated);
        minionDao.saveOrUpdate(minion);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Automatically register the consumer on initialization
        messageConsumerManager.registerConsumer(this);
    }

    @Override
    public SinkModule<MinionIdentityDTO> getModule() {
        return heartbeatModule;
    }

}
