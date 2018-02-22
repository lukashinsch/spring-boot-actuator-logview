package eu.hinsch.spring.boot.actuator.logview;

import org.ocpsoft.prettytime.PrettyTime;

import java.nio.file.Path;

/**
 * Created by lh on 28/02/15.
 */
public abstract class AbstractFileProvider implements FileProvider {
    protected static final PrettyTime prettyTime = new PrettyTime();

    protected boolean isArchive(final Path path) {
        return isZip(path) || isTarGz(path) || isGz(path);
    }

    protected boolean isGz(final Path path) {
        return !path.toFile().isDirectory() && path.getFileName().toString().endsWith(".gz");
    }

    protected boolean isTarGz(final Path path) {
        return !path.toFile().isDirectory() && path.getFileName().toString().endsWith(".tar.gz");
    }

    protected boolean isZip(final Path path) {
        return !path.toFile().isDirectory() && path.getFileName().toString().endsWith(".zip");
    }
}