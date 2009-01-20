package hudson.plugins.rake;

import java.io.File;
import java.io.IOException;

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
	
	public File getCanonicalExecutable() throws IOException {
		return Util.getExecutable(getPath()).getCanonicalFile();
	}	
}
