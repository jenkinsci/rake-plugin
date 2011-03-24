package hudson.plugins.rake;

import hudson.FilePath;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

class RvmUtil {

    public static Rvm getDefaultRvm() {
        String userHome = System.getProperty("user.home");
        Rvm rvm = null;

        if (userHome != null) {
            String rvmPath = userHome + File.separator + ".rvm";
            if (new File(rvmPath).exists()) {
                rvm = new Rvm(rvmPath);
            }
        }

        return rvm;
    }

    public static RubyInstallation[] getRvmRubies(Rvm rvm) {
        Collection<RubyInstallation> rubies = new LinkedHashSet<RubyInstallation>();

        try {
            FilePath rubiesPath = new FilePath(new File(rvm.getPath(), "rubies"));
            FilePath gemsPath = new FilePath(new File(rvm.getPath(), "gems"));

            if (rubiesPath.exists() && gemsPath.exists()) {
                for (FilePath candidate : rubiesPath.list()) {
                    String name = candidate.getName();

                    RvmFilenameFilter filter = new RvmFilenameFilter(name);
                    Collection<FilePath> gems = gemsPath.list(filter);

                    FilePath global = getGlobal(name, gemsPath);

                    for (FilePath gemCandidate : gems) {
                        FilePath specifications = getSpecifications(gemCandidate);
                        if (specifications != null) {
                            Collection<FilePath> specs = specifications.list(rakeFilter);
                            if (specs == null || specs.size() == 0) {
                                // We did not find the rake gem in this gemset's bin directory; check in global
                                specifications = getSpecifications(global);
                                if (specifications != null) {
                                    specs = specifications.list(rakeFilter);
                                    if (specs == null || specs.size() == 0) {
                                        // Rake not found in global either; this gemset is unusable
                                        continue;
                                    }
                                }
                            }
                        }

                        String path = new File(candidate.toURI()).getCanonicalPath();
                        RubyInstallation ruby = new RubyInstallation(gemCandidate.getName(), path);

                        ruby.setGemHome(new File(gemCandidate.toURI()).getCanonicalPath());
                        ruby.setGemPath(buildGemPath(ruby.getGemHome(), global, gems));

                        String newpath = new File(ruby.getGemHome(), "bin").getCanonicalPath();
                        path = ruby.getBinPath();
                        if (path == null || path.length() == 0) {
                            path = new String();
                            path.concat(newpath);
                        } else {
                            path.concat(File.pathSeparator).concat(newpath);
                        }
                        ruby.setBinPath(path);

                        rubies.add(ruby);
                    }

                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rubies.toArray(new RubyInstallation[rubies.size()]);
    }

    private static String buildGemPath(String currentGem, FilePath global, Collection<FilePath> candidateGems)
            throws InterruptedException, IOException {
        StringBuilder path = new StringBuilder();

        Collection<String> paths = new LinkedHashSet<String>();
        paths.add(currentGem);

        for (FilePath gem : candidateGems) {
            File gemFile = new File(gem.toURI());
            String canonicalPath = gemFile.getCanonicalPath();
            if ((canonicalPath.startsWith(currentGem + "@") && gem.child("specifications").exists())) {
                paths.add(canonicalPath);
            }
        }

        if (global != null && global.child("specifications").exists()) {
            File globalFile = new File(global.toURI());
            paths.add(globalFile.getCanonicalPath());
        }

        for (String canonical : paths) {
            path.append(File.pathSeparator).append(canonical);
        }

        return path.toString();
    }

    private static FilePath getGlobal(String name, FilePath gemsPath)
            throws InterruptedException, IOException {
        RvmGlobalFilter globalFilter = new RvmGlobalFilter(name);
        List<FilePath> globalGems = gemsPath.list(globalFilter);
        FilePath global = null;
        if (!globalGems.isEmpty()) {
            global = globalGems.get(0);
        }
        return global;
    }

    private static FilePath getSpecifications(FilePath candidate)
            throws InterruptedException, IOException {
        FilePath specification = candidate.child("specifications");
System.err.println("Candidate: " + candidate);
        if (specification.exists()) {
            return specification;
        }
        return null;
    }

    private static class RvmFilenameFilter implements FileFilter, Serializable {
        private final String name;

        public RvmFilenameFilter(String name) {
            this.name = name;
        }

        public boolean accept(File pathname) {
            return pathname.getName().startsWith(this.name)
                && !pathname.getName().endsWith("@");
        }
    }

    private static class RvmGlobalFilter implements FileFilter, Serializable {
        private final String name;

        public RvmGlobalFilter(String name) {
            this.name = name;
        }

        public boolean accept(File pathname) {
            return pathname.getName().startsWith(this.name)
                && pathname.getName().endsWith("@global");
        }
    }

    private static class RakeSpecFilter implements FileFilter, Serializable {
        private final Pattern rakePattern = Pattern.compile("rake\\-([\\d.]+).gemspec");
        public boolean accept(File pathname) {
            return rakePattern.matcher(pathname.getName()).matches();
        }
    }

    private static FileFilter rakeFilter = new RakeSpecFilter();
}
