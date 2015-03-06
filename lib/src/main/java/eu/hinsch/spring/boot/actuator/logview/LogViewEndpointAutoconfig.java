package eu.hinsch.spring.boot.actuator.logview;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnProperty("logging.path")
public class LogViewEndpointAutoconfig {

    @Bean
    public LogViewEndpoint logViewEndpoint(Environment environment) {
        return new LogViewEndpoint(environment);
    }
}
