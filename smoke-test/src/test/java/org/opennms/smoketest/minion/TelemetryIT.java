package org.opennms.smoketest.minion;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import javax.xml.bind.JAXBException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.dao.api.EventDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.hibernate.EventDaoHibernate;
import org.opennms.netmgt.dao.hibernate.NodeDaoHibernate;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.telemetry.adapters.jti.proto.Port;
import org.opennms.netmgt.telemetry.adapters.jti.proto.TelemetryTop;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.netmgt.xml.event.Value;
import org.opennms.smoketest.NullTestEnvironment;
import org.opennms.smoketest.OpenNMSSeleniumTestCase;
import org.opennms.smoketest.utils.DaoUtils;
import org.opennms.smoketest.utils.HibernateDaoFactory;
import org.opennms.test.system.api.NewTestEnvironment.ContainerAlias;
import org.opennms.test.system.api.utils.SshClient;
import org.opennms.test.system.api.TestEnvironment;
import org.opennms.test.system.api.TestEnvironmentBuilder;

public class TelemetryIT {

    public static final String SENDER_IP = "192.168.1.1";
    public static final String SENDER_IP_MINION = "192.168.1.2";

    private static TestEnvironment m_testEnvironment;

    @ClassRule
    public static final TestEnvironment getTestEnvironment() {
        if (!OpenNMSSeleniumTestCase.isDockerEnabled()) {
            return new NullTestEnvironment();
        }
        try {
            final TestEnvironmentBuilder builder = TestEnvironment.builder().all();
            OpenNMSSeleniumTestCase.configureTestEnvironment(builder);
            m_testEnvironment = builder.build();
            return m_testEnvironment;
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Before
    public void checkForDocker() {
        Assume.assumeTrue(OpenNMSSeleniumTestCase.isDockerEnabled());
    }

    @Test
    public void verifyJtiListenerOnOpenNMS() throws IOException, InterruptedException, JAXBException {

        Date startOfTest = new Date();
        Event event = new Event();
        event.setUei("uei.opennms.org/internal/discovery/newSuspect");
        event.setHost(SENDER_IP);
        event.setInterface(SENDER_IP);
        event.setInterfaceAddress(Inet4Address.getByName(SENDER_IP));
        event.setSource("system-test");
        event.setSeverity("4");
        String xmlString = JaxbUtils.marshal(event);

        final InetSocketAddress opennmsHttp = m_testEnvironment.getServiceAddress(ContainerAlias.OPENNMS, 8980);
        final HttpHost opennmsHttpHost = new HttpHost(opennmsHttp.getAddress().getHostAddress(), opennmsHttp.getPort());

        HttpClient instance = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()) // Ignore
                                                                                                        // the
                                                                                                        // 302
                                                                                                        // response
                                                                                                        // to
                                                                                                        // the
                                                                                                        // POST
                .build();

        Executor executor = Executor.newInstance(instance).auth(opennmsHttpHost, "admin", "admin")
                .authPreemptive(opennmsHttpHost);

        executor.execute(Request.Post(String.format("http://%s:%d/opennms/rest/events",
                opennmsHttp.getAddress().getHostAddress(), opennmsHttp.getPort()))
                .bodyString(xmlString, ContentType.APPLICATION_XML)).returnContent();

        InetSocketAddress pgsql = m_testEnvironment.getServiceAddress(ContainerAlias.POSTGRES, 5432);
        HibernateDaoFactory daoFactory = new HibernateDaoFactory(pgsql);
        EventDao eventDao = daoFactory.getDao(EventDaoHibernate.class);
        NodeDao nodeDao = daoFactory.getDao(NodeDaoHibernate.class);

        Criteria criteria = new CriteriaBuilder(OnmsEvent.class)
                .eq("eventUei", EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI).ge("eventTime", startOfTest)
                .eq("ipAddr", Inet4Address.getByName(SENDER_IP)).toCriteria();

        await().atMost(1, MINUTES).pollInterval(10, SECONDS).until(DaoUtils.countMatchingCallable(eventDao, criteria),
                greaterThan(0));

        final OnmsNode onmsNode = await().atMost(1, MINUTES)
                .pollInterval(5, SECONDS).until(
                        DaoUtils.findMatchingCallable(nodeDao,
                                new CriteriaBuilder(OnmsNode.class).eq("label", SENDER_IP).toCriteria()),
                        notNullValue());

        final InetSocketAddress opennmsUdp = m_testEnvironment.getServiceAddress(ContainerAlias.OPENNMS, 50000, "udp");

        TelemetryTop.TelemetryStream jtiMsg = buildJtiMessage(SENDER_IP, "eth0", 100, 100);
        byte[] jtiMsgBytes = jtiMsg.toByteArray();
        DatagramPacket packet = new DatagramPacket(jtiMsgBytes, jtiMsgBytes.length, opennmsUdp);
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
            socket.send(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        await().atMost(30, SECONDS).pollDelay(0, SECONDS).pollInterval(5, SECONDS)
                .until(matchRrdFileFromNodeResource(opennmsHttp, executor, onmsNode.getId()));
        matchRrdFileFromNodeResource(opennmsHttp, executor, onmsNode.getId());

    }

    @Test
    public void verifyJtiListenerOnMinion() throws Exception {

        Date startOfTest = new Date();

        final InetSocketAddress sshAddr = m_testEnvironment.getServiceAddress(ContainerAlias.MINION, 8201);

        try (final SshClient sshClient = new SshClient(sshAddr, "admin", "admin")) {
            // Modify minion configuration for telemetry
            PrintStream pipe = sshClient.openShell();
            pipe.println("config:edit org.opennms.features.telemetry.listeners-udp-50000");
            pipe.println("config:property-set name JTI");
            pipe.println("config:property-set class-name org.opennms.netmgt.telemetry.listeners.udp.UdpListener");
            pipe.println("config:property-set listener.port 50000");
            pipe.println("config:update");
            pipe.println("logout");
            await().atMost(1, MINUTES).until(sshClient.isShellClosedCallable());
        }

        Event minionEvent = new Event();
        minionEvent.setUei("uei.opennms.org/internal/discovery/newSuspect");
        minionEvent.setHost(SENDER_IP_MINION);
        minionEvent.setInterface(SENDER_IP_MINION);
        minionEvent.setInterfaceAddress(Inet4Address.getByName(SENDER_IP_MINION));
        minionEvent.setSource("system-test");
        minionEvent.setSeverity("4");
        Parm parm = new Parm();
        parm.setParmName("location");
        Value minion = new Value("MINION");
        parm.setValue(minion);
        List<Parm> parms = new ArrayList<>();
        parms.add(parm);
        minionEvent.setParmCollection(parms);

        String xmlString = JaxbUtils.marshal(minionEvent);

        final InetSocketAddress opennmsHttp = m_testEnvironment.getServiceAddress(ContainerAlias.OPENNMS, 8980);
        final HttpHost opennmsHttpHost = new HttpHost(opennmsHttp.getAddress().getHostAddress(), opennmsHttp.getPort());

        HttpClient instance = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()) // Ignore
                                                                                                        // the
                                                                                                        // 302
                                                                                                        // response
                                                                                                        // to
                                                                                                        // the
                                                                                                        // POST
                .build();

        Executor executor = Executor.newInstance(instance).auth(opennmsHttpHost, "admin", "admin")
                .authPreemptive(opennmsHttpHost);

        executor.execute(Request.Post(String.format("http://%s:%d/opennms/rest/events",
                opennmsHttp.getAddress().getHostAddress(), opennmsHttp.getPort()))
                .bodyString(xmlString, ContentType.APPLICATION_XML)).returnContent();

        InetSocketAddress pgsql = m_testEnvironment.getServiceAddress(ContainerAlias.POSTGRES, 5432);
        HibernateDaoFactory daoFactory = new HibernateDaoFactory(pgsql);
        EventDao eventDao = daoFactory.getDao(EventDaoHibernate.class);
        NodeDao nodeDao = daoFactory.getDao(NodeDaoHibernate.class);

        Criteria criteria = new CriteriaBuilder(OnmsEvent.class)
                .eq("eventUei", EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI).ge("eventTime", startOfTest)
                .eq("ipAddr", Inet4Address.getByName(SENDER_IP_MINION)).toCriteria();

        await().atMost(1, MINUTES).pollInterval(10, SECONDS).until(DaoUtils.countMatchingCallable(eventDao, criteria),
                greaterThan(0));

        final OnmsNode onmsNode = await().atMost(1, MINUTES).pollInterval(5, SECONDS)
                .until(DaoUtils.findMatchingCallable(nodeDao,
                        new CriteriaBuilder(OnmsNode.class).eq("label", SENDER_IP_MINION).toCriteria()),
                notNullValue());

        assertThat(onmsNode.getLocation().getLocationName(), is("MINION"));

        final InetSocketAddress minionUdp = m_testEnvironment.getServiceAddress(ContainerAlias.MINION, 50000, "udp");

        TelemetryTop.TelemetryStream jtiMsg = buildJtiMessage(SENDER_IP_MINION, "eth0", 100, 100);
        byte[] jtiMsgBytes = jtiMsg.toByteArray();
        DatagramPacket packet = new DatagramPacket(jtiMsgBytes, jtiMsgBytes.length, minionUdp);
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
            socket.send(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        await().atMost(2, MINUTES).pollDelay(0, SECONDS).pollInterval(15, SECONDS)
                .until(matchRrdFileFromNodeResource(opennmsHttp, executor, onmsNode.getId()));
        matchRrdFileFromNodeResource(opennmsHttp, executor, onmsNode.getId());
    }

    public static Callable<Boolean> matchRrdFileFromNodeResource(InetSocketAddress opennmsHttp, Executor executor,
            Integer id) throws ClientProtocolException, IOException {
        return new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                HttpResponse response = executor
                        .execute(Request.Get(String.format("http://%s:%d/opennms/rest/resources/fornode/%d",
                                opennmsHttp.getAddress().getHostAddress(), opennmsHttp.getPort(), id)))
                        .returnResponse();

                String message = EntityUtils.toString(response.getEntity());
                System.out.println(message);
                if (message.contains("rrdFile")) {
                    return true;
                } else {
                    return false;
                }

            }
        };
    }

    private static TelemetryTop.TelemetryStream buildJtiMessage(String ipAddress, String ifName, long ifInOctets,
            long ifOutOctets) {
        final Port.GPort port = Port.GPort.newBuilder()
                .addInterfaceStats(
                        Port.InterfaceInfos.newBuilder().setIfName(ifName).setInitTime(1457647123)
                                .setSnmpIfIndex(
                                        517)
                                .setParentAeName("ae0")
                                .setIngressStats(Port.InterfaceStats.newBuilder().setIfOctets(ifInOctets).setIfPkts(1)
                                        .setIf1SecPkts(1).setIf1SecOctets(1).setIfUcPkts(1).setIfMcPkts(1)
                                        .setIfBcPkts(1).build())
                .setEgressStats(Port.InterfaceStats.newBuilder().setIfOctets(ifOutOctets).setIfPkts(1).setIf1SecPkts(1)
                        .setIf1SecOctets(1).setIfUcPkts(1).setIfMcPkts(1).setIfBcPkts(1).build()).build())
                .build();

        final TelemetryTop.JuniperNetworksSensors juniperNetworksSensors = TelemetryTop.JuniperNetworksSensors
                .newBuilder().setExtension(Port.jnprInterfaceExt, port).build();

        final TelemetryTop.EnterpriseSensors sensors = TelemetryTop.EnterpriseSensors.newBuilder()
                .setExtension(TelemetryTop.juniperNetworks, juniperNetworksSensors).build();

        final TelemetryTop.TelemetryStream jtiMsg = TelemetryTop.TelemetryStream.newBuilder().setSystemId(ipAddress)
                .setComponentId(0).setSensorName("intf-stats").setSequenceNumber(49103)
                .setTimestamp(new Date().getTime()).setEnterprise(sensors).build();

        return jtiMsg;
    }

}
