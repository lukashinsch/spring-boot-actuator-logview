package eu.hinsch.spring.boot.actuator.logview;

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
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * Created by lh on 23/02/15.
 */
@Component
@ConditionalOnProperty("logging.path")
public class LogViewEndpoint implements MvcEndpoint{

    private Environment environment;

    private static List<FileProvider> fileProviders;

    @Autowired
    public LogViewEndpoint(Environment environment) {
        this.environment = environment;
        fileProviders = asList(new FileSystemFileProvider(),
                new ZipArchiveFileProvider(),
                new TarGzArchiveFileProvider());
    }

    @RequestMapping("/")
    public String list(Model model,
                       @RequestParam(required = false, defaultValue = "FILENAME") SortBy sortBy,
                       @RequestParam(required = false, defaultValue = "false") boolean desc,
                       @RequestParam(required = false) String base) throws IOException {
        securityCheck(base);

        Path currentFolder = loggingPath(base);

        List<FileEntry> files = getFileProvider(currentFolder).getFileEntries(currentFolder);
        List<FileEntry> sortedFiles = sortFiles(files, sortBy, desc);

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("desc", desc);
        model.addAttribute("files", sortedFiles);
        model.addAttribute("currentFolder", currentFolder.toAbsolutePath().toString());
        model.addAttribute("base", base != null ? base : "");
        model.addAttribute("parent", getParent(currentFolder));

        return "logview";
    }

    private FileProvider getFileProvider(Path folder) {
        return fileProviders.stream()
                .filter(provider -> provider.canHandle(folder))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no file provider found for " + folder.toString()));
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
        getFileProvider(path).streamContent(path, filename, response.getOutputStream());
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
