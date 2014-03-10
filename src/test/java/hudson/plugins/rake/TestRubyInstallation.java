package hudson.plugins.rake;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestRubyInstallation {

    @Test
    public void testCompareToRubyInstallationCompareTo() {
        RubyInstallation inst1 = new RubyInstallation("a", "/usr/bin/a");
        RubyInstallation inst2 = new RubyInstallation("b", "/usr/bin/a");

        assertTrue(inst1.compareTo(inst2) == "a".compareTo("b"));
    }
}
