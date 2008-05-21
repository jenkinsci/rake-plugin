package hudson.plugins.rake;

import hudson.CopyOnWrite;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Descriptor.FormException;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormFieldValidator;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
/**
 * Rake plugin main class.
 * 
 * @author David Calavera
 */
@SuppressWarnings({"unchecked", "serial"})
public class Rake extends Builder {

	public static final RakeDescriptor DESCRIPTOR = new RakeDescriptor();
	private final String rakeInstallation;
	private final String rakeFile;
	private final String rakeLibDir;
	private final String tasks;
	private final boolean silent;
	
	@DataBoundConstructor
    public Rake(String rakeInstallation, String rakeFile, String tasks, String rakeLibDir, boolean silent) {
		this.rakeInstallation = rakeInstallation;
        this.rakeFile = rakeFile;
        this.tasks = tasks;
        this.rakeLibDir = rakeLibDir;
        this.silent = silent;
    }
	
	private RakeInstallation getRake() {
		for (RakeInstallation rake : getDescriptor().getInstalltions()) {
			if (rakeInstallation != null && rake.getName().equals(rakeInstallation)) {
				return rake;
			}
		}
		return null;
	}
	
	public boolean perform(Build<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
		Project proj = build.getProject();
        ArgumentListBuilder args = new ArgumentListBuilder();
        String normalizedTasks = tasks.replaceAll("[\t\r\n]+"," ");
                
        RakeInstallation rake = getRake();
        if (rake != null) {
        	File exec = rake.getExecutable();
            if(!exec.exists()) {
                listener.fatalError(exec + " doesn't exist");
                return false;
            }
            args.add(exec.getPath());
        } else {
        	args.add(launcher.isUnix()?"rake":"rake.bat");
        }        
        
        if (rakeFile != null && rakeFile.length() > 0) {
        	args.add("--rakefile", rakeFile);
        }
        if (rakeLibDir != null && rakeLibDir.length() > 0) {
        	args.add("--libdir", rakeLibDir);
        }
        if (silent) {
        	args.add("--silent");
        }
        
        args.addTokenized(normalizedTasks);
        
        try {
            int r = launcher.launch(args.toCommandArray(), build.getEnvVars(), listener.getLogger(), proj.getModuleRoot()).join();
            return r == 0;
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace(listener.fatalError("rake execution failed"));
            return false;
        }
	}	
	
    public RakeDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
    
    public String getRakeFile() {
		return rakeFile;
	}

	public String getRakeLibDir() {
		return rakeLibDir;
	}

	public String getTasks() {
		return tasks;
	}

	public boolean isSilent() {
		return silent;
	}	
   
	public static final class RakeDescriptor extends Descriptor<Builder> {	
    	
		@CopyOnWrite
        private volatile RakeInstallation[] installations = new RakeInstallation[0];
		
    	private RakeDescriptor() {
            super(Rake.class);
            load();
        }

        public String getDisplayName() {
            return "Invoke Rake";
        }
        
        public Builder newInstance(StaplerRequest req) {            
        	return req.bindParameters(Rake.class,"rake.");
        }
        
        @Override
        public String getHelpFile() {
        	return "/plugin/rake/help.html";
        }

		@Override
		public boolean configure(StaplerRequest req) throws FormException {			
			installations = req.bindParametersToList(
				RakeInstallation.class, "rake.").toArray(new RakeInstallation[0]);
			save();
	        return true;
		}
		
		public RakeInstallation[] getInstalltions() {
			return installations;
		}
		
		public void doCheckRakeInstallation(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {        
	        new FormFieldValidator(req,rsp,true) {
	            public void check() throws IOException, ServletException {
	                File f = getFileParameter("value");
	                if(!f.isDirectory()) {
	                    error(f + " is not a directory");
	                    return;
	                }

	                if(!new File(f,"bin/rake").exists() && !new File(f,"bin/rake.bat").exists()) {
	                    error(f + " is not a Rake gem directory");
	                    return;
	                }

	                ok();
	            }
	        }.process();
	    }
    }	
}
