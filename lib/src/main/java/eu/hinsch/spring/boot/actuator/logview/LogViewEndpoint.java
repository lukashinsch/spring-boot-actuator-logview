package eu.hinsch.spring.boot.actuator.logview;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.stream.Collectors.toList;

/**
 * Created by lh on 23/02/15.
 */
@Component
@ConditionalOnProperty("logging.path")
public class LogViewEndpoint implements MvcEndpoint{

    @Autowired
    private Environment environment;

    private static final PrettyTime prettyTime = new PrettyTime();

    @RequestMapping("/")
    public String list(Model model,
                       @RequestParam(required = false, defaultValue = "FILENAME") SortBy sortBy,
                       @RequestParam(required = false, defaultValue = "false") boolean desc,
                       @RequestParam(required = false) String base) throws IOException {
        securityCheck(base);

        Path currentFolder = loggingPath(base);

        List<FileEntry> files;
        if (isArchive(currentFolder)) {
            if (isZip(currentFolder.getFileName().toString())) {
                ZipFile zipFile = new ZipFile(currentFolder.toFile());
                files = zipFile.stream()
                        .map(LogViewEndpoint::createFileEntry)
                        .collect(toList());
            }
            else {
                TarArchiveInputStream inputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(currentFolder.toFile())));
                TarArchiveEntry entry;
                files = new ArrayList<>();
                while ((entry = inputStream.getNextTarEntry()) != null) {
                    files.add(createFileEntry(entry));
                }
            }
        }
        else {
            files = getFileEntries(currentFolder);
        }

        List<FileEntry> sortedFiles = sortFiles(files, sortBy, desc);

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("desc", desc);
        model.addAttribute("files", sortedFiles);
        model.addAttribute("currentFolder", currentFolder.toAbsolutePath().toString());
        model.addAttribute("base", base != null ? base : "");

        String parent = getParent(currentFolder);
        model.addAttribute("parent", parent);

        return "logview";
    }

    private static FileEntry createFileEntry(TarArchiveEntry entry) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setFilename(entry.getName());
        fileEntry.setSize(entry.getSize());
        fileEntry.setFileType(entry.isDirectory() ? FileType.DIRECTORY : FileType.FILE);
        fileEntry.setModified(FileTime.fromMillis(entry.getLastModifiedDate().getTime()));
        fileEntry.setModifiedPretty(prettyTime.format(new Date(entry.getLastModifiedDate().getTime())));
        return fileEntry;
    }

    private static FileEntry createFileEntry(ZipEntry entry) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setFilename(entry.getName());
        fileEntry.setSize(entry.getSize());
        fileEntry.setFileType(entry.isDirectory() ? FileType.DIRECTORY : FileType.FILE);
        fileEntry.setModified(FileTime.fromMillis(entry.getTime()));
        fileEntry.setModifiedPretty(prettyTime.format(new Date(entry.getTime())));
        return fileEntry;
    }

    private String getParent(Path loggingPath) {
        Path basePath = loggingPath(null);
        String parent = "";
        if (!basePath.toString().equals(loggingPath.toString())) {
            parent = loggingPath.getParent().toString();
            if (parent.startsWith(basePath.toString())) {
                parent = parent.substring(basePath.toString().length());
            }
        }
        return parent;
    }

    private Path loggingPath(String base) {
        String loggingPath = environment.getProperty("logging.path");
        return base != null ? Paths.get(loggingPath, base) : Paths.get(loggingPath);
    }

    private List<FileEntry> getFileEntries(Path loggingPath) throws IOException {
        final List<FileEntry> files = new ArrayList<>();
        Files.newDirectoryStream(loggingPath)
                .forEach((path) -> files.add(createFileEntry(path)));
        return files;
    }

    private FileEntry createFileEntry(Path path)  {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setFilename(path.getFileName().toString());
        try {
            fileEntry.setModified(Files.getLastModifiedTime(path));
            fileEntry.setSize(Files.size(path));
        } catch (IOException e) {
            throw new RuntimeException("unable to retrieve file attribute", e);
        }
        fileEntry.setModifiedPretty(prettyTime.format(new Date(fileEntry.getModified().toMillis())));
        fileEntry.setFileType(getFileType(path));

        return fileEntry;
    }

    private FileType getFileType(Path path) {
        FileType fileType = null;
        if (path.toFile().isDirectory()) {
            fileType = FileType.DIRECTORY;
        }
        else if (isArchive(path)) {
            fileType = FileType.ARCHIVE;
        }
        else {
            fileType = FileType.FILE;
        }
        return fileType;
    }

    private boolean isArchive(Path path) {
        if (path.toFile().isDirectory()) {
            return false;
        }
        String name = path.getFileName().toString();
        return isZip(name) || isTarGz(name);
    }

    private boolean isTarGz(String name) {
        return name.endsWith(".tar.gz");
    }

    private boolean isZip(String name) {
        return name.endsWith(".zip");
    }

    private List<FileEntry> sortFiles(List<FileEntry> files, SortBy sortBy, boolean desc) {
        Comparator<FileEntry> comparator = null;
        switch (sortBy) {
            case FILENAME:
                comparator = (a, b) -> a.getFilename().compareTo(b.getFilename());
                break;
            case SIZE:
                comparator = (a, b) -> Long.compare(a.getSize(), b.getSize());
                break;
            case MODIFIED:
                comparator = (a, b) -> Long.compare(a.getModified().toMillis(), b.getModified().toMillis());
                break;
        }
        List<FileEntry> sortedFiles = files.stream().sorted(comparator).collect(toList());

        if (desc) {
            Collections.reverse(sortedFiles);
        }
        return sortedFiles;
    }

    @RequestMapping("/view/{filename}/")
    public void view(@PathVariable String filename, @RequestParam(required = false) String base, HttpServletResponse response) throws IOException {
        securityCheck(filename);

        Path path = loggingPath(base);
        InputStream inputStream = null;
        if (isArchive(path)) {
            if (isZip(path.getFileName().toString())) {
                ZipFile zipFile = new ZipFile(path.toFile());
                ZipEntry entry = zipFile.getEntry(filename);
                inputStream = zipFile.getInputStream(entry);
            }
            else {
                TarArchiveInputStream tarStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(path.toFile())));
                TarArchiveEntry entry;
                while ((entry = tarStream.getNextTarEntry()) != null) {
                    if (entry.getName().equals(filename)) {
                        System.out.println("found file: " + entry.getName());
                        // TODO why is assigning tarStream to inputStream not working?
                        inputStream = new ByteArrayInputStream(IOUtils.toByteArray(tarStream));
                    }
                }
                Assert.notNull(inputStream, "file not found");
            }
        }
        else {
            inputStream = new FileInputStream(Paths.get(loggingPath(base).toString(), filename).toFile());
        }
        IOUtils.copy(inputStream, response.getOutputStream());
    }

    private void securityCheck(String filename) {
        Assert.doesNotContain(filename, "..");
    }

    @Override
    public String getPath() {
        return "/log";
    }

    @Override
    public boolean isSensitive() {
        return true;
    }


    @Override
    public Class<? extends Endpoint> getEndpointType() {
        return null;
    }

}
