package eu.hinsch.spring.boot.actuator.logview;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by lh on 23/02/15.
 */
public class LogViewEndpoint implements MvcEndpoint {

    private final List<FileProvider> fileProviders;
    private final Configuration      freemarkerConfig;
    private final String             loggingPath;
    private final List<String>       stylesheets;

    public LogViewEndpoint(final String loggingPath, final List<String> stylesheets) {
        this.loggingPath = loggingPath;
        this.stylesheets = stylesheets;

        // order is important!
        fileProviders = new LinkedList<>();
        fileProviders.add(new FileSystemFileProvider());
        fileProviders.add(new ZipArchiveFileProvider());
        fileProviders.add(new TarGzArchiveFileProvider());
        fileProviders.add(new GzArchiveFileProvider());

        freemarkerConfig = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates");
    }

    @RequestMapping
    public void redirect(final HttpServletResponse response) throws IOException {
        response.sendRedirect("log/");
    }

    @RequestMapping("/")
    @ResponseBody
    public String list(
        final Model model, // TODO model should no longer be injected
        @RequestParam(required = false, defaultValue = "FILENAME") final SortBy sortBy,
        @RequestParam(required = false, defaultValue = "false")    final boolean desc,
        @RequestParam(required = false)                            final String base
    ) throws IOException, TemplateException {
        securityCheck(base);

        final Path currentFolder = loggingPath(base);

        final List<FileEntry> files       = getFileProvider(currentFolder).getFileEntries(currentFolder);
        final List<FileEntry> sortedFiles = sortFiles(files, sortBy, desc);

        model.addAttribute("sortBy", sortBy);
        model.addAttribute("desc", desc);
        model.addAttribute("files", sortedFiles);
        model.addAttribute("currentFolder", currentFolder.toAbsolutePath().toString());
        model.addAttribute("base", base != null ? URLEncoder.encode(base, "UTF-8") : "");
        model.addAttribute("parent", getParent(currentFolder));
        model.addAttribute("stylesheets", stylesheets);

        return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfig.getTemplate("logview.ftl"), model);
    }

    private FileProvider getFileProvider(final Path folder) {
        return fileProviders.stream()
            .filter(provider -> provider.canHandle(folder))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("no file provider found for " + folder.toString()));
    }

    private String getParent(final Path loggingPath) {
        final Path basePath = loggingPath(null);
        String     parent   = "";
        if (!basePath.toString().equals(loggingPath.toString())) {
            parent = loggingPath.getParent().toString();
            if (parent.startsWith(basePath.toString())) {
                parent = parent.substring(basePath.toString().length());
            }
        }
        return parent;
    }

    private Path loggingPath(final String base) {
        return base != null ? Paths.get(loggingPath, base) : Paths.get(loggingPath);
    }

    private List<FileEntry> sortFiles(final List<FileEntry> files, final SortBy sortBy, final boolean desc) {
        Comparator<FileEntry> comparator = null;
        switch (sortBy) {
            case FILENAME:
                comparator = Comparator.comparing(FileEntry::getFilename);
                break;
            case SIZE:
                comparator = Comparator.comparingLong(FileEntry::getSize);
                break;
            case MODIFIED:
                comparator = Comparator.comparingLong(a -> a.getModified().toMillis());
                break;
        }
        final List<FileEntry> sortedFiles = files.stream().sorted(comparator).collect(toList());

        if (desc) {
            Collections.reverse(sortedFiles);
        }

        return sortedFiles;
    }

    @RequestMapping("/view")
    public void view(
        @RequestParam                   final String filename,
        @RequestParam(required = false) final String base,
        @RequestParam(required = false) final Integer tailLines,
        final HttpServletResponse response
    ) throws IOException {
        securityCheck(filename);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);

        final Path         path         = loggingPath(base);
        final FileProvider fileProvider = getFileProvider(path);
        if (tailLines != null) {
            fileProvider.tailContent(path, filename, response.getOutputStream(), tailLines);
        } else {
            fileProvider.streamContent(path, filename, response.getOutputStream());
        }
    }

    @RequestMapping("/search")
    public void search(@RequestParam final String term, final HttpServletResponse response) throws IOException {
        final Path            folder      = loggingPath(null);
        final List<FileEntry> files       = getFileProvider(folder).getFileEntries(folder);
        final List<FileEntry> sortedFiles = sortFiles(files, SortBy.MODIFIED, false);

        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        final ServletOutputStream outputStream = response.getOutputStream();

        sortedFiles.stream()
            .filter(file -> file.getFileType().equals(FileType.FILE))
            .forEach(file -> searchAndStreamFile(file, term, outputStream));
    }

    private void searchAndStreamFile(final FileEntry fileEntry, final String term, final OutputStream outputStream) {
        final Path folder = loggingPath(null);
        try {
            final List<String> lines = IOUtils.readLines(new FileInputStream(new File(folder.toFile().toString(), fileEntry.getFilename())))
                .stream()
                .filter(line -> line.contains(term))
                .map(line -> "[" + fileEntry.getFilename() + "] " + line)
                .collect(toList());
            for (final String line : lines) {
                outputStream.write(line.getBytes());
                outputStream.write(System.lineSeparator().getBytes());
            }
        } catch (final IOException e) {
            throw new RuntimeException("error reading file", e);
        }
    }

    private void securityCheck(final String filename) {
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