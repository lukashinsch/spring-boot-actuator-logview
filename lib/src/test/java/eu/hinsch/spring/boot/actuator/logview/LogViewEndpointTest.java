package eu.hinsch.spring.boot.actuator.logview;

import org.apache.catalina.ssi.ByteArrayServletOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

@SuppressWarnings("unchecked")
public class LogViewEndpointTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private HttpServletResponse response;

    private LogViewEndpoint logViewEndpoint;

    private Model model;
    private long now;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        logViewEndpoint = new LogViewEndpoint(temporaryFolder.getRoot().getAbsolutePath());
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
        List<FileEntry> fileEntries = getFileEntries();
        assertThat(fileEntries, hasSize(1));
        FileEntry fileEntry = fileEntries.get(0);
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
        List<FileEntry> fileEntries = getFileEntries();
        assertThat(fileEntries, hasSize(1));
        FileEntry fileEntry = fileEntries.get(0);
        assertThat(fileEntry.getFilename(), is("A.log"));
    }

    @Test
    public void shouldViewZipFileContent() throws Exception {
        // given
        createZipArchive("file.zip", "A.log", "content");
        ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.view("A.log", "file.zip", null, response);

        // then
        assertThat(new String(outputStream.toByteArray()), is("content"));
    }

    private void createZipArchive(String archiveFileName, String contentFileName, String content) throws Exception {
        try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(temporaryFolder.getRoot(), archiveFileName)))) {
            ZipEntry zipEntry = new ZipEntry(contentFileName);
            zos.putNextEntry(zipEntry);
            IOUtils.write(content, zos);
        }
    }

    @Test
    public void shouldListTarGzContent() throws Exception {
        // given
        createTarGzArchive("file.tar.gz", "A.log", "content");

        // when
        logViewEndpoint.list(model, SortBy.FILENAME, false, "file.tar.gz");

        // then
        List<FileEntry> fileEntries = getFileEntries();
        assertThat(fileEntries, hasSize(1));
        FileEntry fileEntry = fileEntries.get(0);
        assertThat(fileEntry.getFilename(), is("A.log"));
    }

    @Test
    public void shouldViewTarGzFileContent() throws Exception {
        // given
        createTarGzArchive("file.tar.gz", "A.log", "content");
        ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.view("A.log", "file.tar.gz", null, response);

        // then
        assertThat(new String(outputStream.toByteArray()), is("content"));
    }

    private void createTarGzArchive(String archiveFileName, String contentFileName, String content) throws Exception {

        try(TarArchiveOutputStream tos = new TarArchiveOutputStream(new GZIPOutputStream(
                new BufferedOutputStream(new FileOutputStream(
                        new File(temporaryFolder.getRoot(), archiveFileName)))))) {
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            TarArchiveEntry archiveEntry = new TarArchiveEntry(contentFileName);
            archiveEntry.setSize(content.length());
            tos.putArchiveEntry(archiveEntry);
            IOUtils.write(content, tos);
            tos.closeArchiveEntry();
        }
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
        ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.view("file.log", null, null, response);

        // then
        assertThat(new String(outputStream.toByteArray()), is("abc"));
    }

    @Test
    public void shouldSearchInFiles() throws Exception {
        // given
        String sep = System.lineSeparator();
        createFile("A.log", "A-line1" + sep + "A-line2" + sep + "A-line3", now - 1);
        createFile("B.log", "B-line1" + sep + "B-line2" + sep + "B-line3", now);
        ByteArrayServletOutputStream outputStream = mockResponseOutputStream();

        // when
        logViewEndpoint.search("line2", response);

        // then
        String output = new String(outputStream.toByteArray());
        assertThat(output, containsString("[A.log] A-line2"));
        assertThat(output, containsString("[B.log] B-line2"));
        assertThat(output, not(containsString("line1")));
        assertThat(output, not(containsString("line3")));
    }

    private ByteArrayServletOutputStream mockResponseOutputStream() throws Exception {
        ByteArrayServletOutputStream outputStream = new ByteArrayServletOutputStream();
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

    private void createFile(String filename, String content, long modified) throws Exception {
        File file = new File(temporaryFolder.getRoot(), filename);
        FileUtils.write(file, content);
        assertThat(file.setLastModified(modified), is(true));
    }

}