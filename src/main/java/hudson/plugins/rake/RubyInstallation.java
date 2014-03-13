package hudson.plugins.rake;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Ruby installation paths.
 * @author David Calavera
 *
 */
public final class RubyInstallation implements Comparable<RubyInstallation> {
    private final String name;
    private final String path;

    private String gemHome;
    private String gemPath;
    private String binPath;

    @DataBoundConstructor
    public RubyInstallation(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getGemHome() {
        return gemHome;
    }

    public void setGemHome(String gemHome) {
        this.gemHome = gemHome;
    }

    public String getGemPath() {
        return gemPath;
    }

    public void setGemPath(String gemPath) {
        this.gemPath = gemPath;
    }

    public String getBinPath() {
        return binPath;
    }

    public void setBinPath(String binPath) {
        this.binPath = binPath;
    }

    public File getExecutable() {
        return getGemHome() != null ? Util.getExecutable(getPath(), getGemHome(), getGemPath()) :
          Util.getExecutable(getPath());
    }

    public File getBundleExecutable() {
        return getGemHome() != null ? Util.getBundleExecutable(getPath(), getGemHome(), getGemPath()) :
          Util.getBundleExecutable(getPath());
    }

    public File getCanonicalExecutable() throws IOException {
        return Util.getExecutable(getPath()).getCanonicalFile();
    }

    public String toString() {
        return "\nN " + getName() +
            "\n P " + getPath() +
            "\n GH " + getGemHome() +
            "\n GP " + getGemPath() +
            "\n EXEC " + getExecutable();
    }

    @Override
    public int compareTo(RubyInstallation other) {
        return this.name.compareTo(other.name);
    }
}
