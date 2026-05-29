package mod.magd.pkgs.bridges;

import java.io.File;
import java.util.ArrayList;



import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;





// =========================================================
// YqPkgBridge
// =========================================================

// Bridge between the existing yq path system and the new
// multi-package registry.

// PURPOSE:
    // yq.java cannot be safely modified — it is obfuscated,
    // has hundreds of methods, and is used everywhere in the
    // compiled codebase. Adding methods risks breaking
    // anything referencing it by position or ProGuard mapping.

    // Instead, this class sits NEXT TO yq and provides the
    // new path logic that the multi-package system needs,
    // delegating to the registry for all package-aware paths.

    // Think of it as the "extension of yq for packages."

// WHAT THIS CLASS KNOWS:
    // The Sketchware project data directory layout:
    //   .sketchware/data/{projectId}/files/java/       ← main source root (yq already knows this)
    //   .sketchware/data/{projectId}/files/java_extra/ ← new extra source roots

// WHAT THIS CLASS DOES NOT DO:
    // Does NOT modify, subclass, or wrap yq.
    // Does NOT read/write any JSON — that's PkgStore.
    // Does NOT validate package names — that's PkgValidator.

// USAGE:
    // File projectFilesDir = new File (".sketchware/data/611/files");
    // YqPkgBridge bridge = new YqPkgBridge (projectFilesDir);
    // File extraRoot   = bridge.getExtraJavaRoot();
    // File sourceRoot  = bridge.getSourceRootFor (entry);
    // ArrayList<File> allRoots = bridge.getAllSourceRoots (registry);

// =========================================================

public final class YqPkgBridge {




    // =========================================================
    // CONSTANTS
    // =========================================================

    // Folder name for the original main java source root.
    // Matches what yq already uses — do NOT change.
    private static final String FOLDER_JAVA       = "java";

    // Folder name for all extra (non-main) package source roots.
    // New — yq has no knowledge of this.
    private static final String FOLDER_JAVA_EXTRA = "java_extra";




    // =========================================================
    // VARIABLES
    // =========================================================

    // .sketchware/data/{projectId}/files/
    private final File projectFilesDir;




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public YqPkgBridge (File projectFilesDir) {
        if (projectFilesDir == null)
            throw new IllegalArgumentException ("YqPkgBridge: projectFilesDir must not be null.");
        this.projectFilesDir = projectFilesDir;
    }




    // =========================================================
    // PUBLIC — Path Getters
    // =========================================================

    // Returns the main java source root.
    // This is the exact same folder yq already uses.
    // Provided here so callers have a single consistent place
    // to get all source roots without calling yq directly.
    public File getMainJavaRoot() {
        return new File (projectFilesDir, FOLDER_JAVA);
    }

    // Returns the base directory that contains all extra
    // package source root subfolders.
    // Example: .sketchware/data/611/files/java_extra/
    public File getExtraJavaRoot() {
        return new File (projectFilesDir, FOLDER_JAVA_EXTRA);
    }

    // Returns the source root folder for a specific entry.
    // For the main entry → same as getMainJavaRoot().
    // For extra entries → subfolder inside java_extra/.
    public File getSourceRootFor (PkgEntry entry) {
        if (entry == null)
            throw new IllegalArgumentException ("YqPkgBridge.getSourceRootFor(): entry must not be null.");
        return new File (entry.getSourceRootPath());
    }

    // Returns the absolute path of a specific .java file
    // inside the given package entry's source root.
    // fileName should be just the filename, e.g. "MyClass.java"
    // Does NOT check whether the file actually exists.
    public File getJavaFileFor (PkgEntry entry, String fileName) {
        if (entry == null)
            throw new IllegalArgumentException ("YqPkgBridge.getJavaFileFor(): entry must not be null.");
        if (fileName == null || fileName.isEmpty())
            throw new IllegalArgumentException ("YqPkgBridge.getJavaFileFor(): fileName must not be null or empty.");
        return new File ( getSourceRootFor (entry), fileName );
    }




    // =========================================================
    // PUBLIC — Multi-Root Collection
    // =========================================================

    // Returns ALL source root directories across ALL packages.
    // Used by the compiler bridge (Step 7) to feed all source
    // roots into a single ECJ / javac call.
    //
    // Order: main root first, then extra roots in registry order.
    // Only includes roots that actually exist on disk.
    public ArrayList<File> getAllSourceRoots (PkgRegistry registry) {
        if (registry == null)
            throw new IllegalArgumentException ("YqPkgBridge.getAllSourceRoots(): registry must not be null.");

        ArrayList<File> roots = new ArrayList<>();

        for (PkgEntry entry : registry.getAll()) {
            File root = getSourceRootFor (entry);
            if ( root.exists() && root.isDirectory() ) {
                roots.add (root);
            }
        }

        return roots;
    }

    // Collects all .java files from ALL source roots across
    // ALL packages. Recursively walks each root.
    // Used by Step 7 to build the full file list for compilation.
    public ArrayList<File> collectAllJavaFiles (PkgRegistry registry) {
        if (registry == null)
            throw new IllegalArgumentException ("YqPkgBridge.collectAllJavaFiles(): registry must not be null.");

        ArrayList<File> javaFiles = new ArrayList<>();

        for (File root : getAllSourceRoots (registry)) {
            collectJavaFilesRecursive (root, javaFiles);
        }

        return javaFiles;
    }

    // Collects all .java files from a single source root,
    // walking subdirectories recursively.
    public ArrayList<File> collectJavaFiles (File sourceRoot) {
        if (sourceRoot == null)
            throw new IllegalArgumentException ("YqPkgBridge.collectJavaFiles(): sourceRoot must not be null.");

        ArrayList<File> javaFiles = new ArrayList<>();
        collectJavaFilesRecursive (sourceRoot, javaFiles);
        return javaFiles;
    }




    // =========================================================
    // PRIVATE — Recursive File Walk
    // =========================================================

    private void collectJavaFilesRecursive (File dir, ArrayList<File> result) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;

        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if ( child.isDirectory() ) {
                collectJavaFilesRecursive (child, result);
            } else if ( child.getName().endsWith (".java") ) {
                result.add (child);
            }
        }
    }




}


