package hudson.plugins.rake;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;
/**
 *
 * @author David Calavera
 *
 */
public class TestUtil extends TestCase {

    public void testHasGemsInstalled() {
        if (execTest())
            assertEquals(true, Util.hasGemsInstalled("/usr/lib/ruby"));
    }

    public void testHasNotGemsInstalled() {
    if (execTest())
            assertEquals(false, Util.hasGemsInstalled("/usr/lib"));
    }

    public void testIsRakeInstalled() {
        if (execTest())
            assertEquals(true, Util.isRakeInstalled(Util.getGemsDir("/usr/lib/ruby")));
    }

    public void testIsJrubyRakeInstalled() {
        if (execTest() && System.getenv("JRUBY_HOME") != null)
            assertEquals(true, Util.isRakeInstalled(Util.getGemsDir("$JRUBY_HOME")));
    }

    public void testDetectRubyInstallations() throws Exception {
        if (execTest()) {
            int expected = System.getenv("JRUBY_HOME") != null
                && System.getenv("PATH").contains(System.getenv("JRUBY_HOME"))?2:1;
            assertEquals(expected, Util.getRubyInstallations("/usr/bin").size());
        }
    }

    public void testFindInPath() throws IOException {
        String temp = System.getenv("TEMP");
        if (temp == null || temp.length() == 0) {
            temp = "/tmp";
        }
        final File folderOne = new File(temp, "testFindInPath-folderOne");
        final File folderTwo = new File(temp, "testFindInPath-folderTwo");
        final File emptyFile = new File(folderTwo, "testFindInPath-emptyFile.txt");
        try {
            folderOne.mkdir();
            folderTwo.mkdir();
            emptyFile.createNewFile();
            final String path = folderOne.getAbsolutePath() + File.pathSeparator + folderTwo.getAbsolutePath();
            
            final String actual = Util.findInPath(emptyFile.getName(), path);
            
            assertEquals(emptyFile.getAbsolutePath(), actual);
        }
        finally {
            if (emptyFile.exists()) {
                emptyFile.delete();
            }
            if (folderTwo.exists()) {
                folderTwo.delete();
            }
            if (folderOne.exists()) {
                folderOne.delete();
            }
        }
    }
    
    public void testGetCanonicalRubies() throws IOException {
        if (execTest() && System.getenv("JRUBY_HOME") != null) {
            File file = new File(System.getenv("JRUBY_HOME") + "/bin/jruby");
            RubyInstallation jruby = new RubyInstallation(file.getName(), file.getCanonicalPath());
            int rubies = Util.getCanonicalRubies(new RubyInstallation[]{jruby}).length;

            assertEquals(2, rubies);
        }
    }

    public void testGetCanonicalRubiesLinked() throws IOException {
        if (execTest()) {
            File file = new File("/usr/bin/ruby");
            String path = !Util.isMac()? file.getCanonicalPath() :
                file.getCanonicalFile().getParentFile().getParentFile().getCanonicalPath();
            RubyInstallation ruby = new RubyInstallation(file.getName(), path);
            Collection<RubyInstallation> rubies = new ArrayList<RubyInstallation>();
            rubies.add(ruby);
            if (System.getenv("JRUBY_HOME") != null) {
                file = new File(System.getenv("JRUBY_HOME") + "/bin/jruby");
                RubyInstallation jruby = new RubyInstallation(file.getName(), file.getCanonicalPath());
                rubies.add(jruby);
            }

            RubyInstallation[] currentInstalled = rubies.toArray(new RubyInstallation[rubies.size()]);
            Collection<File> candidates = Util.getRubyInstallations("/usr/bin");

            assertEquals(rubies.size(), Util.getCanonicalRubies(currentInstalled, candidates).length);
        }
    }

    private boolean execTest() {
        if(Boolean.getBoolean("rake.test.skip"))
            return false;

        return new File("/usr/lib/ruby").exists();
    }
}
