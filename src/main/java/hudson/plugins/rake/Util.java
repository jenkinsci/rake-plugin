package hudson.plugins.rake;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ruby utility class. It's used to detect ruby local installations among other features.
 * 
 * @author David Calavera
 */
public class Util {
	
	private static String[] RUBY_EXECUTABLES = {"ruby", "jruby"};
	
	public static File getExecutable(String path) {
        String execName = isWindows()?"rake.bat":"rake";
        File parent = null;
        if (isJruby(path)) {
        	parent = new File(path);
        } else {
        	parent = new File(path).getParentFile().getParentFile();        	
        }
        return new File(parent, "bin/" + execName);
    }
	
	public static boolean isWindows() {
		String name = System.getProperty("os.name");
		if (name != null) {
			return name.contains("Windows");
		} else {
			return File.separatorChar == '\\';
		}
	}
	
	public static boolean isJruby(String path) {
		String execName = isWindows()?"jruby.bat":"jruby";
		return new File(path, "bin/" + execName).exists();
	}
	
	public static boolean hasGemsInstalled(String path) {
		File gems = getGemsDir(path);
		return gems != null && gems.exists() && gems.isDirectory();
	}
	
	public static File getGemsDir(String path) {
		File gems = null;		
		if (path.startsWith("$")) path = System.getenv(path.substring(1));
		if (isJruby(path)) {
			gems = new File(path + "/lib/ruby/gems/1.8");			
		} else {
			gems = new File(path + "/gems/1.8");
		}	
		return gems;
	}
	
	public static boolean isRakeInstalled(File gemsDir) {
		File specPath = new File(gemsDir, "specifications");
		return specPath.exists() && specPath.listFiles(rakeFilter) != null;		
	}
	
	private static FilenameFilter rakeFilter = new FilenameFilter() {
		Pattern rakePattern = Pattern.compile("rake\\-([\\d.]+).gemspec");
		public boolean accept(File path, String file) {			
			return rakePattern.matcher(file).matches();
		}
	};
	
	public static Collection<File> getRubyInstallations() throws IOException {
		String systemPath = System.getenv("PATH");
		if (systemPath == null) systemPath = System.getenv("path");
		Collection<File> rubyVersions = new ArrayList<File>();
		
		if (systemPath != null) {
			Set<String> candidates = new LinkedHashSet<String>(Arrays.asList(systemPath.split(File.pathSeparator)));
			for (String path : candidates) {
				for (String ruby : RUBY_EXECUTABLES) {
					File rubyExec = isWindows()?new File(path, ruby + ".exe"):new File(path, ruby);
					if (rubyExec.isFile() && 
							!rubyVersions.contains(rubyExec.getCanonicalFile().getParentFile())) {
						File parent = rubyExec.getCanonicalFile().getParentFile();
						if (isJruby(parent.getParent())) {
							parent = parent.getParentFile();
						}
						
						File gemsDir = getGemsDir(parent.getAbsolutePath());
						if (gemsDir != null && isRakeInstalled(gemsDir)) {
							rubyVersions.add(parent);
						}
					}
				}
			}
		}
				
		return rubyVersions;
	}
	
	public static RubyInstallation[] getCanonicalRubies(RubyInstallation[] currentInstallations) {
		try {
			Collection<File> rubies = getRubyInstallations();
			Collection<RubyInstallation> currentList = new ArrayList<RubyInstallation>();
			
			for (File ruby : rubies) {
				currentList.add(new RubyInstallation(ruby.getName(), ruby.getAbsolutePath()));
			}
			
			for (RubyInstallation current : currentList) {
				File cur = new File(current.getPath());
				if (!rubies.contains(cur.getCanonicalFile())) {
					currentList.add(current);
				}
			}					
			
			return currentList.toArray(new RubyInstallation[0]);
		} catch (IOException e) {
			hudson.Util.displayIOException(e, null);    
		}
		return new RubyInstallation[0];
	}
	
	public static boolean isAlreadyInstalled(RubyInstallation[] current, String path) {
		try {
			for (RubyInstallation ruby : current) {
				if (new File(ruby.getPath()).getCanonicalPath()
						.equals(new File(path).getCanonicalPath())) {
					return true;
				}
			}
		} catch (IOException e) {
			hudson.Util.displayIOException(e, null);    
		}
		return false;
	}
	
}
