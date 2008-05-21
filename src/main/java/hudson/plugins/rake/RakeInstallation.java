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
	
	public String toString() {
		return name + ": " + path;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPath() {
		return path;
	}
	
	public File getExecutable() {
        String execName = File.separatorChar == '\\'?"rake.bat":"rake";
        File parent = null;
        if (isJruby()) {
        	parent = new File(getPath());
        } else {
        	parent = new File(getPath()).getParentFile().getParentFile();        	
        }
        return new File(parent, "bin/" + execName);
    }
	
	private boolean isJruby() {
		String execName = File.separatorChar == '\\'?"jruby.bat":"jruby";
		return new File(getPath(), "bin/" + execName).exists();
	}
}
