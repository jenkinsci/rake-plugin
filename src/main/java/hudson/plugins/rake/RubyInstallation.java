package hudson.plugins.rake;

import java.io.File;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Ruby installation paths.
 * @author David Calavera
 *
 */
public final class RubyInstallation {
	private final String name;
	private final String path;
	
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
	
	public File getExecutable() {
		return Util.getExecutable(getPath());
	}
	
	public String toString() {
		return "name: " + name + "\n"
			+ "path: " + path + "\n";
	}
}
