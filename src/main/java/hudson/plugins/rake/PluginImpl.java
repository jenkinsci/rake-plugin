package hudson.plugins.rake;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * Rake plugin entry point.
 * 
 * @author David Calavera
 * @plugin
 */
public class PluginImpl extends Plugin {
	public void start() throws Exception {
        BuildStep.BUILDERS.add(Rake.DESCRIPTOR);
    }
}
