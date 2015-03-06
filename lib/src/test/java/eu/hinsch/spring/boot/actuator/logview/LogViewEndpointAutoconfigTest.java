package eu.hinsch.spring.boot.actuator.logview;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class LogViewEndpointAutoconfigTest extends TestCase {

    @Mock
    private Environment environment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldCreateBean() {
        assertThat(new LogViewEndpointAutoconfig().logViewEndpoint(environment), notNullValue());
    }
}