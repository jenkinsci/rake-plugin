package hudson.plugins.rake;

import java.io.File;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Rake installation paths.
 * @author David Calavera
 *
 */
public final class RakeInstallation {
	private final String name;
	private final String path;
	
	@DataBoundConstructor
	public RakeInstallation(String name, String path) {
		this.name = name;
		this.path = path;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPath() {
		return path;
	}
	
	public File getExecutable() {
        String execName = File.separatorChar == '\\'?"rake.bat":"rake";
        return new File(getPath(), "bin/" + execName);
    }
}
