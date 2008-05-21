package hudson.plugins.rake;

import java.util.Arrays;

import junit.framework.TestCase;

public class TestUtil extends TestCase {

	public void testHasGemsInstalled() {
		assertEquals(true, Util.hasGemsInstalled("/usr/lib/ruby"));
	}
	
	public void testHasNotGemsInstalled() {
		assertEquals(false, Util.hasGemsInstalled("/usr/lib"));
	}
	
	public void testIsRakeInstalled() {
		assertEquals(true, Util.isRakeInstalled(Util.getGemsDir("/usr/lib/ruby")));
	}
	
	public void testIsJrubyRakeInstalled() {
		assertEquals(true, Util.isRakeInstalled(Util.getGemsDir("$JRUBY_HOME")));
	}
	
	public void testDetectRubyInstallations() throws Exception {		
		assertEquals(2, Util.getRubyInstallations().size());
	}
	
}
