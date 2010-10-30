package hudson.plugins.rake;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.Test;

public class TestRvmUtil {

    @Test
    public void testSearchRvmInstallation() {
        if (!runTests()) return;

        Rvm rvm = new Rvm(rvm());

        RubyInstallation[] installs = RvmUtil.getRvmRubies(rvm);
        for (RubyInstallation r : installs) {
            String expectedPath = new File(r.getGemHome(), "bin/" + Util.execName()).getPath();

            assertExecutable(r.getGemPath(), r.getExecutable().getPath());
        }
    }

    private boolean runTests() {
        return new File(rvm()).exists();
    }

    private String rvm() {
        String home = System.getenv("HOME");
        return home + "/.rvm";
    }

    private void assertExecutable(String expectedPath, String executablePath) {
        String[] paths = expectedPath.split(File.pathSeparator);
        Collection<String> candidates = new LinkedHashSet<String>();
        for (String path : paths) {
            candidates.add(new File(path, "bin/" + Util.execName()).getPath());
        }

        assertTrue(candidates.contains(executablePath));
    }
}
