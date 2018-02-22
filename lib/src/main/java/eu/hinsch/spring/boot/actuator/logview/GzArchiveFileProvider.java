package eu.hinsch.spring.boot.actuator.logview;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by lh on 28/02/15.
 */
public class GzArchiveFileProvider extends AbstractFileProvider {
    @Override
    public boolean canHandle(final Path folder) {
        return isGz(folder);
    }

    @Override
    public List<FileEntry> getFileEntries(final Path folder) throws IOException {
        final FileEntry fileEntry = new FileEntry();

        try (final GzipCompressorInputStream stream = new GzipCompressorInputStream(new FileInputStream(folder.toFile()))) {
            fileEntry.setFilename(getRealFileName(stream, folder));
            fileEntry.setDisplayFilename(getRealFileName(stream, folder));
            fileEntry.setSize(getRealFileSize(stream));
            fileEntry.setFileType(FileType.FILE);
            fileEntry.setModified(FileTime.fromMillis(stream.getMetaData().getModificationTime()));
            fileEntry.setModifiedPretty(prettyTime.format(new Date(stream.getMetaData().getModificationTime())));
        }

        return Collections.singletonList(fileEntry);
    }

    private String getRealFileName(final GzipCompressorInputStream stream, final Path folder) {
        if (stream.getMetaData().getFilename() != null) {
            return stream.getMetaData().getFilename();
        } else {
            final String archiveName = folder.getFileName().toString();
            return archiveName.substring(0, archiveName.length() - 3);
        }
    }

    private int getRealFileSize(final GzipCompressorInputStream stream) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final byte[] buffer = new byte[1024];

        int n;
        while (-1 != (n = stream.read(buffer))) {
            out.write(buffer, 0, n);
        }

        final int size = out.size();

        out.close();

        return size;
    }

    @Override
    public void streamContent(final Path folder, final String filename, final OutputStream stream) throws IOException {
        final GzipCompressorInputStream is = new GzipCompressorInputStream(new FileInputStream(folder.toFile()));

        IOUtils.copy(new ByteArrayInputStream(IOUtils.toByteArray(is)), stream);
    }
}