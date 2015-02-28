package eu.hinsch.spring.boot.actuator.logview;

import org.ocpsoft.prettytime.PrettyTime;

import java.nio.file.Path;

/**
* Created by lh on 28/02/15.
*/
public abstract class AbstractFileProvider implements FileProvider {
    protected static final PrettyTime prettyTime = new PrettyTime();

    protected boolean isArchive(Path path) {
        return isZip(path) || isTarGz(path);
    }

    protected boolean isTarGz(Path path) {
        return !path.toFile().isDirectory() && path.getFileName().toString().endsWith(".tar.gz");
    }

    protected boolean isZip(Path path) {
        return !path.toFile().isDirectory() && path.getFileName().toString().endsWith(".zip");
    }
}
