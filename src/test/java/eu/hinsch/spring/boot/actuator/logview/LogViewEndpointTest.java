package eu.hinsch.spring.boot.actuator.logview;

import eu.hinsch.spring.boot.actuator.logview.LogViewEndpoint.FileEntry;
import eu.hinsch.spring.boot.actuator.logview.LogViewEndpoint.SortBy;
import org.apache.catalina.ssi.ByteArrayServletOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class LogViewEndpointTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Environment environment;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private LogViewEndpoint logViewEndpoint;

    private Model model;
    private long now;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(environment.getProperty("logging.path"))
                .thenReturn(temporaryFolder.getRoot().getAbsolutePath());
        model = new ExtendedModelMap();
        now = new Date().getTime();
    }

    @Test
    public void shouldReturnEmptyFileListForEmptyDirectory() throws IOException {
        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false);

        // then
        assertThat(model.containsAttribute("files"), is(true));
        assertThat(getFileEntries(), hasSize(0));
    }

    @Test
    public void shouldListSortedByFilename() throws IOException {
        // given
        createFile("B.log", "x", now);
        createFile("A.log", "x", now);
        createFile("C.log", "x", now);

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false);

        // then
        assertThat(getFileNames(), contains("A.log", "B.log", "C.log"));
    }

    @Test
    public void shouldListReverseSortedByFilename() throws IOException {
        // given
        createFile("B.log", "x", now);
        createFile("A.log", "x", now);
        createFile("C.log", "x", now);

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, true);

        // then
        assertThat(getFileNames(), contains("C.log", "B.log", "A.log"));
    }

    @Test
    public void shouldListSortedBySize() throws IOException {
        // given
        createFile("A.log", "xx", now);
        createFile("B.log", "x", now);
        createFile("C.log", "xxx", now);

        // when
        logViewEndpoint.list(model, SortBy.SIZE, false);

        // then
        assertThat(getFileNames(), contains("B.log", "A.log", "C.log"));
        assertThat(getFileSizes(), contains(1L, 2L, 3L));
    }

    @Test
    public void shouldListSortedByDate() throws IOException {
        // given
        // TODO java 8 date api
        createFile("A.log", "x", now);
        createFile("B.log", "x", now - 10 * 60 * 1000);
        createFile("C.log", "x", now - 5 * 60 * 1000);

        // when
        logViewEndpoint.list(model, SortBy.MODIFIED, false);

        // then
        assertThat(getFileNames(), contains("B.log", "C.log", "A.log"));
        assertThat(getFilePrettyTimes(), contains("10 minutes ago", "5 minutes ago", "moments ago"));
    }

    @Test
    public void shouldEndpointBeSensitive() {
        assertThat(logViewEndpoint.isSensitive(), is(true));
    }

    @Test
    public void shouldReturnContextPath() {
        assertThat(logViewEndpoint.getPath(), is("/log"));
    }

    @Test
    public void shouldNotAllowToListFileOutsideRoot() throws IOException {
        // given
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(containsString("this String argument must not contain the substring [..]"));

        // when
        logViewEndpoint.view("../somefile", null);
    }

    @Test
    public void shouldViewFile() throws IOException {
        // given
        createFile("file.log", "abc", now);
        ByteArrayServletOutputStream outputStream = new ByteArrayServletOutputStream();
        when(response.getOutputStream()).thenReturn(outputStream);

        // when
        logViewEndpoint.view("file.log", response);

        // then
        assertThat(new String(outputStream.toByteArray()), is("abc"));
    }

    private List<String> getFileNames() {
        return getFileEntries()
                .stream()
                .map(entry -> entry.getFilename())
                .collect(toList());
    }

    private List<Long> getFileSizes() {
        return getFileEntries()
                .stream()
                .map(entry -> entry.getSize())
                .collect(toList());
    }

    private List<String> getFilePrettyTimes() {
        return getFileEntries()
                .stream()
                .map(entry -> entry.getModifiedPretty())
                .collect(toList());
    }

    private List<FileEntry> getFileEntries() {
        return (List<FileEntry>) model.asMap().get("files");
    }

    private void createFile(String filename, String content, long modified) throws IOException {
        File file = new File(temporaryFolder.getRoot(), filename);
        FileUtils.write(file, content);
        file.setLastModified(modified);
    }

}