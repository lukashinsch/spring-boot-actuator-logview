package eu.hinsch.spring.boot.actuator.logview;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class LogViewEndpointAutoconfigTest {

    @Mock
    private Environment environment;

    @Mock
    private LogViewEndpointAutoconfig.EndpointConfiguration configuration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldCreateBeanForDefaultPath() {
        assertThat(new LogViewEndpointAutoconfig().logViewEndpointWithDefaultPath(environment, configuration), notNullValue());
    }

    @Test
    public void shouldCreateBeanForDeviatingPath() {
        assertThat(new LogViewEndpointAutoconfig().logViewEndpointWithDeviatingPath(environment, configuration), notNullValue());
    }
}