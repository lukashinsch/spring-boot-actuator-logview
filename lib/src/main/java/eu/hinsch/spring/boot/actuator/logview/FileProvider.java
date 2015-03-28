package eu.hinsch.spring.boot.actuator.logview;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

/**
* Created by lh on 28/02/15.
*/
public interface FileProvider {

    boolean canHandle(Path folder);

    List<FileEntry> getFileEntries(Path folder) throws IOException;

    void streamContent(Path folder, String filename, OutputStream stream) throws IOException;

    default void tailContent(Path folder, String filename, OutputStream stream, int lines) throws IOException {
        throw new UnsupportedOperationException("by default no tailing possible");
    }
}
