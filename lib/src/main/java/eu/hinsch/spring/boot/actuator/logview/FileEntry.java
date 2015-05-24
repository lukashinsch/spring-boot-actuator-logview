package eu.hinsch.spring.boot.actuator.logview;

import java.nio.file.attribute.FileTime;

/**
* Created by lh on 26/02/15.
*/
public class FileEntry {
    private String filename;
    private String displayFilename;
    private FileTime modified;
    private String modifiedPretty;
    private long size;
    private FileType fileType;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public FileTime getModified() {
        return modified;
    }

    public void setModified(FileTime modified) {
        this.modified = modified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getModifiedPretty() {
        return modifiedPretty;
    }

    public void setModifiedPretty(String modifiedPretty) {
        this.modifiedPretty = modifiedPretty;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public void setDisplayFilename(String displayFilename) {
        this.displayFilename = displayFilename;
    }

    public String getDisplayFilename() {
        return displayFilename;
    }
}
