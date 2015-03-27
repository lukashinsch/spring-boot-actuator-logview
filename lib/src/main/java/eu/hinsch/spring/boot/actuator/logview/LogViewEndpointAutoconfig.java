package eu.hinsch.spring.boot.actuator.logview;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LogViewEndpointAutoconfig {

    public static final String LOGGING_PATH = "logging.path";
    public static final String ENDPOINTS_LOGVIEW_PATH = "endpoints.logview.path";

    @ConditionalOnProperty(LOGGING_PATH)
    @Bean
    public LogViewEndpoint logViewEndpointWithDefaultPath(Environment environment) {
        return new LogViewEndpoint(environment.getRequiredProperty(LOGGING_PATH));
    }

    @ConditionalOnProperty(ENDPOINTS_LOGVIEW_PATH)
    @Bean
    public LogViewEndpoint logViewEndpointWithDeviatingPath(Environment environment) {
        return new LogViewEndpoint(environment.getRequiredProperty(ENDPOINTS_LOGVIEW_PATH));
    }
}
