package hudson.plugins.rake;

import junit.framework.TestCase;
import java.io.File;
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
		if (execTest() && System.getenv("JRUBY_HOME") != null)
			assertEquals(2, Util.getRubyInstallations().size());
	}
	
	private boolean execTest() {
                if(Boolean.getBoolean("rake.test.skip"))
                    return false;
                return new File("/usr/lib/ruby").exists();
	}
}
