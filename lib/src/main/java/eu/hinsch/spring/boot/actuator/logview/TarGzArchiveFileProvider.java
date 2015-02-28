package eu.hinsch.spring.boot.actuator.logview;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    @Override
    public boolean canHandle(Path folder) {
        return isTarGz(folder);
    }

    @Override
    public List<FileEntry> getFileEntries(Path folder) throws IOException {
        TarArchiveInputStream inputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(folder.toFile())));
        TarArchiveEntry entry;
        List<FileEntry> files = new ArrayList<>();
        while ((entry = inputStream.getNextTarEntry()) != null) {
            files.add(createFileEntry(entry));
        }
        return files;
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

    @Override
    public void streamContent(Path folder, String filename, OutputStream stream) throws IOException {
        TarArchiveInputStream tarStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(folder.toFile())));
        TarArchiveEntry entry;
        while ((entry = tarStream.getNextTarEntry()) != null) {
            if (entry.getName().equals(filename)) {
                // TODO why do we need the byte array in between???
                IOUtils.copy(new ByteArrayInputStream(IOUtils.toByteArray(tarStream)), stream);
            }
        }
    }
}
