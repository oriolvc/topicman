package com.oriolvc.topicman;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;

/** TopicMan has no login, so a default install must not be reachable from the network. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerBindingIT {

    @Autowired
    private ServletWebServerApplicationContext context;

    @Test
    void listensOnlyOnTheLoopbackAddressByDefault() {
        var server = (TomcatWebServer) context.getWebServer();

        var address = server.getTomcat().getConnector().getProperty("address");

        assertThat(address)
                .isInstanceOfSatisfying(
                        InetAddress.class,
                        bound -> assertThat(bound.isLoopbackAddress()).isTrue());
    }
}
