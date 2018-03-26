package org.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.netty4.http.NettyHttpComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NettyProducerIT {

	private static final Logger log = LoggerFactory.getLogger(NettyProducerIT.class);

	private DefaultCamelContext clientCamelContext;
	private ProducerTemplate template;

	@Inject
	@Filter("(camel.context.name=echo-service)")
	protected CamelContext camelContext;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
		return probe;
	}

	@Configuration
	public Option[] config() throws Exception {

		MavenArtifactUrlReference karafUrl =
				maven()
						.groupId("org.apache.karaf")
						.artifactId("apache-karaf")
						.versionAsInProject()
						.type("tar.gz");

		return new Option[] {

				karafDistributionConfiguration()
						.frameworkUrl(karafUrl)
						.unpackDirectory(new File("target/exam"))
						.useDeployFolder(false),

				keepRuntimeFolder(),
				logLevel(LogLevel.WARN),

				replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", new File("src/test/resources/etc/org.ops4j.pax.logging.cfg")),
				replaceConfigurationFile("deploy/camelContext.xml", new File("src/test/resources/camelContext.xml")),

				features(
						maven()
								.groupId("org.apache.camel.karaf")
								.artifactId("apache-camel")
								.type("xml")
								.classifier("features")
								.versionAsInProject(),
						"camel-blueprint",
						"camel-netty4-http"),
		};
	}

	@Before
	public void init() throws Exception {
		NettyHttpComponent component = new NettyHttpComponent();

		clientCamelContext = new DefaultCamelContext();
		clientCamelContext.addComponent("netty4-http", component);
		clientCamelContext.setTracing(true);
		clientCamelContext.setName("itest");
		clientCamelContext.setStreamCaching(true);
		clientCamelContext.start();

		template = clientCamelContext.createProducerTemplate();
		template.start();
	}

	@Test
	public void test() throws Exception {
		log.info("start test");
		final String msg = "She sells sea shells by the sea shore.";

		Exchange exchange = template.request("netty4-http:http://127.0.0.1:8080/echo", new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getIn().setBody(msg.getBytes());
			}
		});
		assertNotNull(exchange);
		assertTrue(exchange.hasOut());
		Message out = exchange.getOut();
		assertEquals(200, out.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class).intValue());

		byte[] payload = out.getBody(byte[].class);
		assertNotNull(payload);
		assertEquals(msg, new String(payload));
	}

}
