package hudson.plugins.rake;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
	
	private static final String[] RUBY_EXECUTABLES = {"ruby(?:[\\w-_.]+)?", "jruby"};
	
	public static File getExecutable(String path) {
	    String rakePattern = "rake(?:[\\w-_.]+)?";
        String execName = isWindows() ? rakePattern + ".bat" : rakePattern;
        
        File parent = isJruby(path)? new File(path) : new File(path).getParentFile().getParentFile();
        File bin = new File(parent, "bin");
        FilenameFilter filter = new RegexFilenameFilter(execName);
                
        File[] candidates = bin.listFiles(filter);
        File rake = candidates != null && candidates.length > 0 ? candidates[0] : new File(bin, "rake");
        
        return rake;
    }
	
	public static boolean isWindows() {
		String name = System.getProperty("os.name");
		return name != null?name.contains("Windows") : File.separatorChar == '\\';		
	}
	
	public static boolean isLinux() {
		return System.getProperty("os.name").endsWith("Linux");
	}
	
	public static boolean isMac() {
	    return System.getProperty("os.name").equalsIgnoreCase("Mac OS X");
	}
	
	public static boolean isJruby(String path) {
		String execName = isWindows()?"jruby.bat":"jruby";
		return new File(path, "bin/" + execName).exists();
	}
	
	public static boolean hasGemsInstalled(String path) {
		File[] gems = getGemsDir(path);
		for (File gem : gems) {
		    if (gem != null && gem.exists() && gem.isDirectory()) {
		        return true;
		    }
	    }
	    return false;
	}
	
	public static File[] getGemsDir(String path) {			
		if (path.startsWith("$")) {
			path = System.getenv(path.substring(1));
		}
		File[] gemDirsFiltered = new File[0];
		
		File gemsBaseFile = new File(path + "/gems");
		if (gemsBaseFile.exists()) {
		    gemDirsFiltered = gemsBaseFile.listFiles(gemDirFilter);
		} else {
		    gemsBaseFile = new File(path + "/lib");
		    if (gemsBaseFile.exists()) {
		        FilenameFilter filter = new RegexFilenameFilter("ruby(?:[\\w-_.]+)?");
		        File[] gemsCustomDir = gemsBaseFile.listFiles(filter);
		        if (gemsCustomDir.length > 0) {
		            gemsCustomDir = gemsCustomDir[0].listFiles(new RegexFilenameFilter("gems"));
		            gemDirsFiltered = gemsCustomDir[0].listFiles(new RegexFilenameFilter("\\d+.\\d+(?:.\\d+)?"));
		        }
	        }
		}
		
	    // for (File gemsBaseFile : new File []{new File(path + "/lib/ruby/gems"), new File(path + "/gems")}) {
	    //             if (gemsBaseFile.exists()) {
	    //                 gemDirsFiltered = gemsBaseFile.listFiles(gemDirFilter);
	    //                 if (gemDirsFiltered.length > 0) {
	    //                     break;
	    //                 }
	    //             }
	    //         }
		
		return gemDirsFiltered;	
	}
	
	public static boolean isRakeInstalled(File... gemsDirArray) {
	    for (File gemsDir : gemsDirArray) {
		    File specPath = new File(gemsDir, "specifications");
		    if (specPath.exists() && specPath.listFiles(new RegexFilenameFilter("rake\\-([\\d.]+).gemspec")) != null) {
		        return true;
		    }
	    }
	    return false;
	}
	
	private static FilenameFilter rakeFilter = new FilenameFilter() {
		private final Pattern rakePattern = Pattern.compile("rake\\-([\\d.]+).gemspec");
		public boolean accept(File path, String file) {			
			return rakePattern.matcher(file).matches();
		}
	};
	
	private static FilenameFilter gemDirFilter = new FilenameFilter() {
	    Pattern gemVersionPattern = Pattern.compile("\\d+.\\d+(?:.\\d+)?");
	    public boolean accept(File path, String file) {			
			return gemVersionPattern.matcher(file).matches();
		}
	};
	
	public static Collection<File> getRubyInstallations() throws IOException {
		String systemPath = System.getenv("PATH");
		if (systemPath == null) systemPath = System.getenv("path");
		Collection<File> rubyVersions = new LinkedHashSet<File>();
		
		if (systemPath != null) {
			Set<String> candidates = new LinkedHashSet<String>(Arrays.asList(systemPath.split(File.pathSeparator)));
			for (String path : candidates) {
				for (String ruby : RUBY_EXECUTABLES) {
					File rubyExec = getExecutableWithExceptions(path, ruby);
					if (rubyExec.isFile() && 
							!rubyVersions.contains(rubyExec.getCanonicalFile().getParentFile())) {
						File parent = rubyExec.getCanonicalFile().getParentFile();
						File[] gemsDir = getGemsDir(parent.getAbsolutePath());
						
						if (!isRakeInstalled(gemsDir) && (isMac() || isJruby(parent.getParent()))) {
							parent = parent.getParentFile();
							gemsDir = getGemsDir(parent.getAbsolutePath());
						}
						 
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
			Collection<RubyInstallation> currentList = new LinkedHashSet<RubyInstallation>(Arrays.asList(currentInstallations));
			
out:	    for (File ruby : rubies) {
				for (RubyInstallation current : currentList) {
					if (current.getCanonicalExecutable().equals(getExecutable(ruby.getCanonicalPath()).getCanonicalFile())) {
						continue out;
					}
				}
				currentList.add(new RubyInstallation(ruby.getName(), ruby.getCanonicalPath()));
			}
					
			return currentList.toArray(new RubyInstallation[currentList.size()]);
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
	
	private static File getExecutableWithExceptions(String path, String exec) throws IOException {
		// File rubyExec = isWindows()?new File(path, exec + ".exe"):new File(path, exec);
		//        if (isLinux() && rubyExec.exists() && rubyExec.getAbsolutePath().equals("/usr/bin/ruby")) {
		//            rubyExec = new File("/usr/lib/ruby/ruby");
		//        }
		File binPath = new File(path);
		FilenameFilter filter = new RegexFilenameFilter(isWindows() ? exec + ".exe" : exec);
		
		File[] candidates = binPath.listFiles(filter);
		File rubyExec = candidates.length > 0 ? candidates[0] : 
		    isWindows()?new File(path, exec + ".exe"):new File(path, exec);
		
		return rubyExec;
	}
	
 	private static class RegexFilenameFilter implements FilenameFilter {
 	    private final String rubyName;
 	    private final Pattern rubyPattern;
 	    
 	    public RegexFilenameFilter(String rubyName) {
 	        this.rubyName = rubyName;
 	        rubyPattern = Pattern.compile(rubyName);
 	    }
 	    
        public boolean accept(File path, String file) {			
			return rubyPattern.matcher(file).matches();
		}
    }
	
}
