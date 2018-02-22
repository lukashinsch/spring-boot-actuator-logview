package eu.hinsch.spring.boot.actuator.logview;

import org.apache.catalina.ssi.ByteArrayServletOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "SameParameterValue"})
public class LogViewEndpointTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private HttpServletResponse response;

    private LogViewEndpoint logViewEndpoint;

    private Model model;
    private long  now;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        logViewEndpoint = new LogViewEndpoint(temporaryFolder.getRoot().getAbsolutePath(),
            new LogViewEndpointAutoconfig.EndpointConfiguration().getStylesheets());
        model = new ExtendedModelMap();
        now = new Date().getTime();
    }

    @Test
    public void shouldReturnEmptyFileListForEmptyDirectory() throws Exception {
        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, null);

        // then
        assertThat(model.containsAttribute("files"), is(true));
        assertThat(getFileEntries(), hasSize(0));
    }

    @Test
    public void shouldListSortedByFilename() throws Exception {
        // given
        createFile("B.log", "x", now);
        createFile("A.log", "x", now);
        createFile("C.log", "x", now);

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, null);

        // then
        assertThat(getFileNames(), contains("A.log", "B.log", "C.log"));
    }

    @Test
    public void shouldListReverseSortedByFilename() throws Exception {
        // given
        createFile("B.log", "x", now);
        createFile("A.log", "x", now);
        createFile("C.log", "x", now);

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, true, null);

        // then
        assertThat(getFileNames(), contains("C.log", "B.log", "A.log"));
    }

    @Test
    public void shouldListSortedBySize() throws Exception {
        // given
        createFile("A.log", "xx", now);
        createFile("B.log", "x", now);
        createFile("C.log", "xxx", now);

        // when
        logViewEndpoint.list(model, SortBy.SIZE, false, null);

        // then
        assertThat(getFileNames(), contains("B.log", "A.log", "C.log"));
        assertThat(getFileSizes(), contains(1L, 2L, 3L));
    }

    @Test
    public void shouldListSortedByDate() throws Exception {
        // given
        // TODO java 8 date api
        createFile("A.log", "x", now);
        createFile("B.log", "x", now - 10 * 60 * 1000);
        createFile("C.log", "x", now - 5 * 60 * 1000);

        // when
        logViewEndpoint.list(model, SortBy.MODIFIED, false, null);

        // then
        assertThat(getFileNames(), contains("B.log", "C.log", "A.log"));
        assertThat(getFilePrettyTimes(), contains("10 minutes ago", "5 minutes ago", "moments ago"));
    }

    @Test
    public void shouldSetFileTypeForFile() throws Exception {
        // given
        createFile("A.log", "x", now);

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, null);

        // then
        assertThat(getFileEntries().get(0).getFileType(), is(FileType.FILE));
    }

    @Test
    public void shouldSetFileTypeForArchive() throws Exception {
        // given
        createFile("A.log.tar.gz", "x", now);

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, null);

        // then
        assertThat(getFileEntries().get(0).getFileType(), is(FileType.ARCHIVE));
    }

    @Test
    public void shouldContainEmptyParentLinkInBaseFolder() throws Exception {
        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, null);

        // then
        assertThat(model.asMap().get("parent"), is(""));
    }

    @Test
    public void shouldContainEmptyParentLinkInSubfolder() throws Exception {
        // given
        temporaryFolder.newFolder("subfolder");

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, "subfolder");

        // then
        assertThat(model.asMap().get("parent"), is(""));
    }

    @Test
    public void shouldContainEmptyParentLinkInNestedSubfolder() throws Exception {
        // given
        temporaryFolder.newFolder("subfolder");
        temporaryFolder.newFolder("subfolder", "nested");

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, "subfolder/nested");

        // then
        assertThat(model.asMap().get("parent"), is("/subfolder"));
    }

    @Test
    public void shouldIncludeSubfolderEntry() throws Exception {
        // given
        temporaryFolder.newFolder("subfolder");

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, null);

        // then
        final List<FileEntry> fileEntries = getFileEntries();
        assertThat(fileEntries, hasSize(1));
        final FileEntry fileEntry = fileEntries.get(0);
        assertThat(fileEntry.getFileType(), is(FileType.DIRECTORY));
        assertThat(fileEntry.getFilename(), is("subfolder"));
    }

    @Test
    public void shouldListZipContent() throws Exception {
        // given
        createZipArchive("file.zip", "A.log", "content");

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, "file.zip");

        // then
        final List<FileEntry> fileEntries = getFileEntries();
        assertThat(fileEntries, hasSize(1));
        final FileEntry fileEntry = fileEntries.get(0);
        assertThat(fileEntry.getFilename(), is("A.log"));
    }

    @Test
    public void shouldViewZipFileContent() throws Exception {
        // given
        createZipArchive("file.zip", "A.log", "content");
        final ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.view("A.log", "file.zip", null, response);

        // then
        assertThat(new String(outputStream.toByteArray()), is("content"));
    }

    private void createZipArchive(final String archiveFileName, final String contentFileName, final String content) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(temporaryFolder.getRoot(), archiveFileName)))) {
            final ZipEntry zipEntry = new ZipEntry(contentFileName);
            zos.putNextEntry(zipEntry);
            IOUtils.write(content, zos);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowExceptionWhenCallingTailForZip() throws Exception {
        // given
        createZipArchive("file.zip", "A.log", "content");

        // when
        logViewEndpoint.view("A.log", "file.zip", 1, response);

        // then -> exception
    }

    @Test
    public void shouldListTarGzContent() throws Exception {
        // given
        createTarGzArchive("file.tar.gz", "A.log", "content");

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, "file.tar.gz");

        // then
        final List<FileEntry> fileEntries = getFileEntries();
        assertThat(fileEntries, hasSize(1));
        final FileEntry fileEntry = fileEntries.get(0);
        assertThat(fileEntry.getFilename(), is("A.log"));
    }

    @Test
    public void shouldListGzContent() throws Exception {
        // given
        createGzArchive("file.gz", "A.log", "content");

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, "file.gz");

        // then
        final List<FileEntry> fileEntries = getFileEntries();
        assertThat(fileEntries, hasSize(1));
        final FileEntry fileEntry = fileEntries.get(0);
        assertThat(fileEntry.getFilename(), is("A.log"));
    }

    @Test
    public void shouldViewTarGzFileContent() throws Exception {
        // given
        createTarGzArchive("file.tar.gz", "A.log", "content");
        final ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.view("A.log", "file.tar.gz", null, response);

        // then
        assertThat(new String(outputStream.toByteArray()), is("content"));
    }

    @Test
    public void shouldViewGzFileContent() throws Exception {
        // given
        createGzArchive("file.gz", "A.log", "content");
        final ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.view("A.log", "file.gz", null, response);

        // then
        assertThat(new String(outputStream.toByteArray()), is("content"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowExceptionWhenCallingTailForTarGz() throws Exception {
        // given
        createTarGzArchive("file.tar.gz", "A.log", "content");

        // when
        logViewEndpoint.view("A.log", "file.tar.gz", 1, response);

        // then -> exception
    }

    private void createTarGzArchive(final String archiveFileName, final String contentFileName, final String content) throws Exception {

        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(new GZIPOutputStream(
            new BufferedOutputStream(new FileOutputStream(
                new File(temporaryFolder.getRoot(), archiveFileName)))))) {
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            final TarArchiveEntry archiveEntry = new TarArchiveEntry(contentFileName);
            archiveEntry.setSize(content.length());
            tos.putArchiveEntry(archiveEntry);
            IOUtils.write(content, tos);
            tos.closeArchiveEntry();
        }
    }

    private void createGzArchive(final String archiveFileName, final String contentFileName, final String content) throws Exception {
        final GzipParameters gzip = new GzipParameters();

        gzip.setFilename(contentFileName);

        final InputStream                is        = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        final OutputStream               out       = Files.newOutputStream(Paths.get(temporaryFolder.getRoot().getAbsolutePath(), archiveFileName));
        final GzipCompressorOutputStream outStream = new GzipCompressorOutputStream(new BufferedOutputStream(out), gzip);

        final byte[] buffer = new byte[1024];

        int n;

        while (-1 != (n = is.read(buffer))) {
            outStream.write(buffer, 0, n);
        }

        outStream.close();
        is.close();
    }

    @Test
    public void shouldRedirectWithoutTrainingSlash() throws IOException {
        // when
        logViewEndpoint.redirect(response);

        // then
        verify(response).sendRedirect("log/");
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
    public void shouldReturnNullEndpointType() {
        assertThat(logViewEndpoint.getEndpointType(), is(nullValue()));
    }

    @Test
    public void shouldNotAllowToListFileOutsideRoot() throws Exception {
        // given
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(containsString("this String argument must not contain the substring [..]"));

        // when
        logViewEndpoint.view("../somefile", null, null, null);
    }

    @Test
    public void shouldViewFile() throws Exception {
        // given
        createFile("file.log", "abc", now);
        final ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.view("file.log", null, null, response);

        // then
        assertThat(new String(outputStream.toByteArray()), is("abc"));
    }

    @Test
    public void shouldTailViewOnlyLastLine() throws Exception {
        // given
        createFile("file.log", "line1" + System.lineSeparator() + "line2" + System.lineSeparator(), now);
        final ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.view("file.log", null, 1, response);

        // then
        assertThat(new String(outputStream.toByteArray()), not(containsString("line1")));
        assertThat(new String(outputStream.toByteArray()), containsString("line2"));
    }

    @Test
    public void shouldSearchInFiles() throws Exception {
        // given
        final String sep = System.lineSeparator();
        createFile("A.log", "A-line1" + sep + "A-line2" + sep + "A-line3", now - 1);
        createFile("B.log", "B-line1" + sep + "B-line2" + sep + "B-line3", now);
        final ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.search("line2", response);

        // then
        final String output = new String(outputStream.toByteArray());
        assertThat(output, containsString("[A.log] A-line2"));
        assertThat(output, containsString("[B.log] B-line2"));
        assertThat(output, not(containsString("line1")));
        assertThat(output, not(containsString("line3")));
    }

    private ByteArrayServletOutputStream mockResponseOutputStream() throws Exception {
        final ByteArrayServletOutputStream outputStream = new ByteArrayServletOutputStream();
        when(response.getOutputStream()).thenReturn(outputStream);
        return outputStream;
    }

    private List<String> getFileNames() {
        return getFileEntries()
            .stream()
            .map(FileEntry::getFilename)
            .collect(toList());
    }

    private List<Long> getFileSizes() {
        return getFileEntries()
            .stream()
            .map(FileEntry::getSize)
            .collect(toList());
    }

    private List<String> getFilePrettyTimes() {
        return getFileEntries()
            .stream()
            .map(FileEntry::getModifiedPretty)
            .collect(toList());
    }

    private List<FileEntry> getFileEntries() {
        return (List<FileEntry>) model.asMap().get("files");
    }

    private void createFile(final String filename, final String content, final long modified) throws Exception {
        final File file = new File(temporaryFolder.getRoot(), filename);
        FileUtils.write(file, content);
        assertThat(file.setLastModified(modified), is(true));
    }

}