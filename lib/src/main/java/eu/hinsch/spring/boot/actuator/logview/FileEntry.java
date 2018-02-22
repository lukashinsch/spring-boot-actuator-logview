package eu.hinsch.spring.boot.actuator.logview;

import lombok.Data;
import org.apache.commons.io.FileUtils;

import java.nio.file.attribute.FileTime;

/**
 * Created by lh on 26/02/15.
 */
@Data
public class FileEntry {
    private String   filename;
    private String   displayFilename;
    private FileTime modified;
    private String   modifiedPretty;
    private long     size;
    private FileType fileType;

    public String getSizePretty() {
        return FileUtils.byteCountToDisplaySize(size);
    }
}