package eu.hinsch.spring.boot.actuator.logview;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Created by lh on 23/02/15.
 */
@Component
@ConditionalOnProperty("logging.path")
public class LogViewEndpoint implements MvcEndpoint{

    @Autowired
    private Environment environment;

    private final PrettyTime prettyTime = new PrettyTime();

    @RequestMapping("/")
    public String list(Model model,
                       @RequestParam(required = false, defaultValue = "FILENAME") SortBy sortBy,
                       @RequestParam(required = false, defaultValue = "false") boolean desc,
                       @RequestParam(required = false) String base) throws IOException {

        Path currentFolder = loggingPath(base);

        final List<FileEntry> files = getFileEntries(currentFolder);

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
        fileEntry.setFileType(fileType);

        return fileEntry;
    }

    private boolean isArchive(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".zip") || name.endsWith(".tar.gz");
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
        // basic security check
        Assert.doesNotContain(filename, "..");

        InputStream is = new FileInputStream(Paths.get(loggingPath(base).toString(), filename).toFile());
        IOUtils.copy(is, response.getOutputStream());
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
