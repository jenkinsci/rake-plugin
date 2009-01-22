package hudson.plugins.rake;

import java.io.File;
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
		assertEquals(false, Util.hasGemsInstalled("/usr/lib"));
	}
	
	public void testIsRakeInstalled() {
		if (execTest())
			assertEquals(true, Util.isRakeInstalled(Util.getGemsDir("/usr/lib/ruby")));
	}
	
	public void testIsJrubyRakeInstalled() {
		if (System.getenv("JRUBY_HOME") != null)
			assertEquals(true, Util.isRakeInstalled(Util.getGemsDir("$JRUBY_HOME")));
	}
	
	public void testDetectRubyInstallations() throws Exception {
		if (execTest()) {			
			int expected = System.getenv("JRUBY_HOME") != null 
				&& System.getenv("PATH").contains(System.getenv("JRUBY_HOME"))?2:1;
			assertEquals(expected, Util.getRubyInstallations().size());
		}
	}
	
	public void testGetCanonicalRubies() {
		if (execTest() && System.getenv("JRUBY_HOME") != null) {
			File file = new File(System.getenv("JRUBY_HOME") + "/bin/jruby");
			RubyInstallation jruby = new RubyInstallation(file.getName(), file.getAbsolutePath());
			
			assertEquals(2, Util.getCanonicalRubies(new RubyInstallation[]{jruby}).length);
		}
	}
	
	public void testGetCanonicalRubiesLinked() {
		if (execTest()) {
			File file = new File("/usr/bin/ruby");
			RubyInstallation ruby = new RubyInstallation(file.getName(), file.getAbsolutePath());
			Collection<RubyInstallation> rubies = new ArrayList<RubyInstallation>();
			rubies.add(ruby);
			if (System.getenv("JRUBY_HOME") != null) {
				file = new File(System.getenv("JRUBY_HOME") + "/bin/jruby");
				RubyInstallation jruby = new RubyInstallation(file.getName(), file.getAbsolutePath());
				rubies.add(jruby);
			}
			
			assertEquals(rubies.size(), Util.getCanonicalRubies(rubies.toArray(new RubyInstallation[rubies.size()])).length);
		}
	}
	
	private boolean execTest() {
                if(Boolean.getBoolean("rake.test.skip"))
                    return false;
                return new File("/usr/lib/ruby").exists();
	}
}
