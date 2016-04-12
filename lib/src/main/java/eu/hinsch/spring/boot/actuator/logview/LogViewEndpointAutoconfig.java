package eu.hinsch.spring.boot.actuator.logview;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Arrays.asList;

@Configuration
public class LogViewEndpointAutoconfig {

    public static final String LOGGING_PATH = "logging.path";
    public static final String ENDPOINTS_LOGVIEW_PATH = "endpoints.logview.path";

    @ConditionalOnProperty(LOGGING_PATH)
    @Bean
    public LogViewEndpoint logViewEndpointWithDefaultPath(Environment environment, EndpointConfiguration configuration) {
        return new LogViewEndpoint(environment.getRequiredProperty(LOGGING_PATH), configuration.getStylesheets());
    }

    @ConditionalOnProperty(ENDPOINTS_LOGVIEW_PATH)
    @Bean
    public LogViewEndpoint logViewEndpointWithDeviatingPath(Environment environment, EndpointConfiguration configuration) {
        return new LogViewEndpoint(configuration.getPath(), configuration.getStylesheets());
    }

    @Component
    @ConfigurationProperties(prefix = "endpoints.logview")
    static class EndpointConfiguration {
        private List<String> stylesheets = asList("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css",
                "https://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css");
        private String path;

        public List<String> getStylesheets() {
            return stylesheets;
        }

        public void setStylesheets(List<String> stylesheets) {
            this.stylesheets = stylesheets;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
