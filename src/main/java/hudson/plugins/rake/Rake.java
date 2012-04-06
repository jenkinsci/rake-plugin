package hudson.plugins.rake;

import static hudson.plugins.rake.Util.getCanonicalRubies;
import static hudson.plugins.rake.Util.getGemsDir;
import static hudson.plugins.rake.Util.hasGemsInstalled;
import static hudson.plugins.rake.Util.isRakeInstalled;
import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Rake plugin main class.
 *
 * @author David Calavera
 */
@SuppressWarnings({"unchecked", "serial"})
public class Rake extends Builder {

    @Extension
    public static final RakeDescriptor DESCRIPTOR = new RakeDescriptor();
    private final String rakeInstallation;
    private final String rakeFile;
    private final String rakeLibDir;
    private final String rakeWorkingDir;
    private final String tasks;
    private final boolean silent;

    @DataBoundConstructor
    public Rake(String rakeInstallation, String rakeFile, String tasks, String rakeLibDir, String rakeWorkingDir, boolean silent) {
        this.rakeInstallation = rakeInstallation;
        this.rakeFile = rakeFile;
        this.tasks = tasks;
        this.rakeLibDir = rakeLibDir;
        this.rakeWorkingDir = rakeWorkingDir;
        this.silent = silent;
    }

    private RubyInstallation getRake() {
        for (RubyInstallation rake : getDescriptor().getInstallations()) {
            if (rakeInstallation != null && rake.getName().equals(rakeInstallation)) {
                return rake;
            }
        }
        return null;
    }

    private Launcher getLastBuiltLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) {
        AbstractProject project = build.getProject();
        Node lastBuiltOn = project.getLastBuiltOn();
        Launcher lastBuiltLauncher = launcher;
        if (lastBuiltOn != null) {
            lastBuiltLauncher = lastBuiltOn.createLauncher(listener);
        }

        return lastBuiltLauncher;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        String normalizedTasks = tasks.replaceAll("[    \r\n]+"," ");

        Launcher lastBuiltLauncher = getLastBuiltLauncher(build, launcher, listener);

        RubyInstallation rake = getRake();
        if (rake != null) {
            File exec = rake.getExecutable();
            if(!exec.exists()) {
                listener.fatalError(exec + " doesn't exist");
                return false;
            }
            args.add(exec.getPath());
        } else {
            args.add(lastBuiltLauncher.isUnix()?"rake":"rake.bat");
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

        FilePath workingDir = build.getModuleRoot();

        if (rakeWorkingDir != null && rakeWorkingDir.length() > 0) {
            workingDir = new FilePath(build.getModuleRoot(), rakeWorkingDir);
        }

        args.addTokenized(normalizedTasks);

        try {
            EnvVars env = build.getEnvironment(listener);
            if (rake != null) {
                if (rake.getGemHome() != null) {
                    env.put("GEM_HOME", rake.getGemHome());
                }
                if (rake.getGemPath() != null) {
                    env.put("GEM_PATH", rake.getGemPath());
                }
                if (rake.getBinPath() != null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(rake.getBinPath());

                    String path = env.get("PATH");
                    if (path != null) {
                        builder.append(File.pathSeparator).append(path);
                    }

                    env.put("PATH", builder.toString());
                }
            }

            int r = lastBuiltLauncher.launch().cmds(args)
                .envs(env)
                .stdout(listener)
                .pwd(workingDir).join();
            return r == 0;
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace(listener.fatalError("rake execution failed"));
            return false;
        }
    }

    @Override
    public RakeDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public String getRakeInstallation() {
        return rakeInstallation;
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

    public String getRakeWorkingDir() {
        return rakeWorkingDir;
    }

    public static final class RakeDescriptor extends Descriptor<Builder> {

        @CopyOnWrite
        private volatile RubyInstallation[] installations = new RubyInstallation[0];

        @CopyOnWrite
        private volatile Rvm rvm;

        private RakeDescriptor() {
            super(Rake.class);
            load();
        }

        @Override
        public synchronized void load() {
            super.load();

            installations = getCanonicalRubies(installations);
            installations = getGlobalRubies(rvm, installations);
        }

        public String getDisplayName() {
            return "Invoke Rake";
        }

        @Override
        public Rake newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return (Rake) req.bindJSON(clazz, formData);
        }

        @Override
        public String getHelpFile() {
            return "/plugin/rake/help.html";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            installations = req.bindParametersToList(RubyInstallation.class, "rake.")
                .toArray(new RubyInstallation[0]);

            rvm = req.bindParameters(Rvm.class, "rvm.");
            installations = getGlobalRubies(rvm, installations);

            save();
            return true;
        }

        public Rvm getRvm() {
            return rvm;
        }

        public RubyInstallation[] getInstallations() {
            return installations;
        }

        public FormValidation doCheckRubyInstallation(@QueryParameter final String value) {
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) return FormValidation.ok();
            File f = new File(Util.fixNull(value));
            if(!f.isDirectory()) {
                return FormValidation.error(f + " is not a directory");
            }

            if (!hasGemsInstalled(f.getAbsolutePath())) {
                   return FormValidation.error("It seems that ruby gems is not installed");
            }

            if (!isRakeInstalled(getGemsDir(f.getAbsolutePath()))) {
                return FormValidation.error("It seems that rake is not installed");
            }

            return FormValidation.ok();
        }

        private RubyInstallation[] getGlobalRubies(Rvm rvm, RubyInstallation[] installations) {
            if (rvm == null || StringUtils.isEmpty(rvm.getPath())) {
                System.err.println(rvm);
                rvm = RvmUtil.getDefaultRvm();
            }

            if (rvm != null) {
                RubyInstallation[] rvmInstallations = RvmUtil.getRvmRubies(rvm);
                Collection<RubyInstallation> tmp = new LinkedHashSet<RubyInstallation>(Arrays.asList(installations));
                tmp.addAll(Arrays.asList(rvmInstallations));

                installations = tmp.toArray(new RubyInstallation[tmp.size()]);
            }

            return installations;
        }

    }
}
