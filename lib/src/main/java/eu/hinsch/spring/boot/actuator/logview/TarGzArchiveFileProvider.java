package eu.hinsch.spring.boot.actuator.logview;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by lh on 28/02/15.
 */
public class TarGzArchiveFileProvider extends AbstractFileProvider {

    private static FileEntry createFileEntry(final TarArchiveEntry entry) {
        final FileEntry fileEntry = new FileEntry();
        try {
            fileEntry.setFilename(URLEncoder.encode(entry.getName(), "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("encoding error", e);
        }
        fileEntry.setDisplayFilename(entry.getName());
        fileEntry.setSize(entry.getSize());
        fileEntry.setFileType(entry.isDirectory() ? FileType.DIRECTORY : FileType.FILE);
        fileEntry.setModified(FileTime.fromMillis(entry.getLastModifiedDate().getTime()));
        fileEntry.setModifiedPretty(prettyTime.format(new Date(entry.getLastModifiedDate().getTime())));
        return fileEntry;
    }

    @Override
    public boolean canHandle(final Path folder) {
        return isTarGz(folder);
    }

    @Override
    public List<FileEntry> getFileEntries(final Path folder) throws IOException {
        final TarArchiveInputStream inputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(folder.toFile())));
        TarArchiveEntry             entry;
        final List<FileEntry>       files       = new ArrayList<>();
        while ((entry = inputStream.getNextTarEntry()) != null) {
            files.add(createFileEntry(entry));
        }
        return files;
    }

    @Override
    public void streamContent(final Path folder, final String filename, final OutputStream stream) throws IOException {
        final TarArchiveInputStream tarStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(folder.toFile())));
        TarArchiveEntry             entry;
        while ((entry = tarStream.getNextTarEntry()) != null) {
            if (entry.getName().equals(filename)) {
                // TODO why do we need the byte array in between???
                IOUtils.copy(new ByteArrayInputStream(IOUtils.toByteArray(tarStream)), stream);
            }
        }
    }
}
