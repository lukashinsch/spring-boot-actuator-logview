package eu.hinsch.spring.boot.actuator.logview;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.stream.Collectors.toList;

/**
* Created by lh on 28/02/15.
*/
public class ZipArchiveFileProvider extends AbstractFileProvider {

    @Override
    public boolean canHandle(Path folder) {
        return isZip(folder);
    }

    @Override
    public List<FileEntry> getFileEntries(Path folder) throws IOException {
        ZipFile zipFile = new ZipFile(folder.toFile());
        return zipFile.stream()
                .map(ZipArchiveFileProvider::createFileEntry)
                .collect(toList());
    }

    private static FileEntry createFileEntry(ZipEntry entry) {
        FileEntry fileEntry = new FileEntry();
        try {
            fileEntry.setFilename(URLEncoder.encode(entry.getName(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("encoding error", e);
        }
        fileEntry.setDisplayFilename(entry.getName());
        fileEntry.setSize(entry.getSize());
        fileEntry.setFileType(entry.isDirectory() ? FileType.DIRECTORY : FileType.FILE);
        fileEntry.setModified(FileTime.fromMillis(entry.getTime()));
        fileEntry.setModifiedPretty(prettyTime.format(new Date(entry.getTime())));
        return fileEntry;
    }

    @Override
    public void streamContent(Path folder, String filename, OutputStream stream) throws IOException {
        ZipFile zipFile = new ZipFile(folder.toFile());
        ZipEntry entry = zipFile.getEntry(filename);
        IOUtils.copy(zipFile.getInputStream(entry), stream);
    }
}
