package hudson.plugins.rake;

import hudson.FilePath;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

class RvmUtil {
		
	public static Rvm getDefaultRvm() {
		String userHome = System.getProperty("user.home");
		Rvm rvm = null;
		
		if (userHome != null) {
			String rvmPath = userHome + File.separator + ".rvm";
			if (new File(rvmPath).exists()) {
				rvm = new Rvm(rvmPath);
			}
		}
		
		return rvm;
	}
	
	public static RubyInstallation[] getRvmRubies(Rvm rvm) {
		Collection<RubyInstallation> rubies = new LinkedHashSet<RubyInstallation>();
		
		try {
			FilePath rubiesPath = new FilePath(new File(rvm.getPath(), "rubies"));
			FilePath gemsPath = new FilePath(new File(rvm.getPath(), "gems"));
			
			if (rubiesPath.exists() && gemsPath.exists()) {
				for (FilePath candidate : rubiesPath.list()) {
					String name = candidate.getName();
					
					RvmFilenameFilter filter = new RvmFilenameFilter(name);				
					Collection<FilePath> gems = gemsPath.list(filter);
					
					for (FilePath gemCandidate : gems) {
						FilePath specifications = gemCandidate.child("specifications");
						if (specifications.exists()) {
							Collection<FilePath> specs = specifications.list(rakeFilter);
							
							if (specs != null && specs.size() > 0) {
								RubyInstallation ruby = new RubyInstallation(gemCandidate.getName(),
										new File(candidate.toURI()).getCanonicalPath());
								
								ruby.setGemHome(new File(gemCandidate.toURI()).getCanonicalPath());
								ruby.setGemPath(buildGemPath(ruby.getGemHome(), gems));
								
								rubies.add(ruby);
							}
						}
					}
					
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		return rubies.toArray(new RubyInstallation[rubies.size()]);
	}
	
	private static String buildGemPath(String currentGem, Collection<FilePath> candidateGems) 
			throws InterruptedException, IOException {
		StringBuilder path = new StringBuilder(currentGem);
		
		for (FilePath gem : candidateGems) {
			File gemFile = new File(gem.toURI());
			if ((gemFile.getCanonicalPath().startsWith(currentGem + "@") && gem.child("specifications").exists())) {
				path.append(File.pathSeparator).append(gemFile.getCanonicalPath());
			}
		}
		
		return path.toString();
	}
	
	private static class RvmFilenameFilter implements FileFilter, Serializable {
		private final String name;		
		
		public RvmFilenameFilter(String name) {
			this.name = name;
		}
		
		public boolean accept(File pathname) {			
			return pathname.getName().startsWith(this.name)
				&& !pathname.getName().endsWith("@");
		}
	}
	
	private static class RakeSpecFilter implements FileFilter, Serializable {
		private final Pattern rakePattern = Pattern.compile("rake\\-([\\d.]+).gemspec");
        public boolean accept(File pathname) {
            return rakePattern.matcher(pathname.getName()).matches();
        }
	}
	
	private static FileFilter rakeFilter = new RakeSpecFilter();
}
