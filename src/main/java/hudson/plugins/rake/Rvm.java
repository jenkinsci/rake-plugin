package hudson.plugins.rake;

import org.kohsuke.stapler.DataBoundConstructor;

public class Rvm {
    private final String path;

    @DataBoundConstructor
    public Rvm(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
