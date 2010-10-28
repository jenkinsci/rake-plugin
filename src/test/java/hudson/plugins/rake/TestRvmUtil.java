package hudson.plugins.rake;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class TestRvmUtil {

    @Test
    public void testSearchRvmInstallation() {
        if (!runTests()) return;

        Rvm rvm = new Rvm(rvm());

        RubyInstallation[] installs = RvmUtil.getRvmRubies(rvm);
        for (RubyInstallation r : installs) {
            String expectedPath = new File(r.getGemHome(), "bin/" + Util.execName()).getPath();
            assertEquals(expectedPath, r.getExecutable().getPath());
        }
    }

    private boolean runTests() {
        return new File(rvm()).exists();
    }

    private String rvm() {
        String home = System.getenv("HOME");
        return home + "/.rvm";
    }
}
