package eu.hinsch.spring.boot.actuator.logview;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by lh on 28/02/15.
 */
public class FileSystemFileProvider extends AbstractFileProvider {
    @Override
    public boolean canHandle(final Path folder) {
        return folder.toFile().isDirectory();
    }

    @Override
    public List<FileEntry> getFileEntries(final Path loggingPath) throws IOException {
        final List<FileEntry> files = new ArrayList<>();
        Files.newDirectoryStream(loggingPath)
            .forEach((path) -> files.add(createFileEntry(path)));
        return files;
    }

    private FileEntry createFileEntry(final Path path) {
        final FileEntry fileEntry = new FileEntry();
        try {
            fileEntry.setFilename(URLEncoder.encode(path.getFileName().toString(), "UTF-8"));
            fileEntry.setDisplayFilename(path.getFileName().toString());
            fileEntry.setModified(Files.getLastModifiedTime(path));
            fileEntry.setSize(Files.size(path));
        } catch (final IOException e) {
            throw new RuntimeException("unable to retrieve file attribute", e);
        }
        fileEntry.setModifiedPretty(prettyTime.format(new Date(fileEntry.getModified().toMillis())));
        fileEntry.setFileType(getFileType(path));

        return fileEntry;
    }

    private FileType getFileType(final Path path) {
        final FileType fileType;
        if (path.toFile().isDirectory()) {
            fileType = FileType.DIRECTORY;
        } else if (isArchive(path)) {
            fileType = FileType.ARCHIVE;
        } else {
            fileType = FileType.FILE;
        }
        return fileType;
    }

    @Override
    public void streamContent(final Path folder, final String filename, final OutputStream stream) throws IOException {
        IOUtils.copy(new FileInputStream(getFile(folder, filename)), stream);
    }

    private File getFile(final Path folder, final String filename) {
        return Paths.get(folder.toString(), filename).toFile();
    }

    @Override
    public void tailContent(final Path folder, final String filename, final OutputStream stream, final int lines) throws IOException {
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(getFile(folder, filename))) {
            int                i       = 0;
            String             line;
            final List<String> content = new ArrayList<>();
            while ((line = reader.readLine()) != null && i++ < lines) {
                content.add(line);
            }
            Collections.reverse(content);
            IOUtils.writeLines(content, System.lineSeparator(), stream);
        }
    }
}
