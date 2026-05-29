package mod.magd.pkgs.bridges;

import java.io.File;
import java.util.ArrayList;

import mod.magd.pkgs.PkgRegistry;
import pro.sketchware.utility.FilePathUtil;


// =========================================================
// ProjectBuilderBridge
// =========================================================

// Helper class for ProjectBuilder to access multi-package
// compilation infrastructure without tight coupling.

// PURPOSE:
    // ProjectBuilder.compileJavaCode() needs to know about
    // all source roots (all packages) so it can pass them
    // to the ECJ compiler with the correct -sourcepath.
    //
    // This bridge provides that interface cleanly,
    // keeping ProjectBuilder's changes minimal.

// USAGE:
    // In ProjectBuilder.compileJavaCode():
    // File projectFilesDir = new File (fpu.getPathProjectFiles(sc_id));
    // ArrayList<File> allSources = ProjectBuilderBridge.collectAllJavaSourceFiles (
    //     sc_id,
    //     projectFilesDir
    // );
    // Pass allSources to the ECJ compiler args.

// =========================================================

public final class ProjectBuilderBridge {

    private ProjectBuilderBridge() {}

    // Collects all .java & .kt files from ALL packages
    // (main + extra) in a single project.
    // Returns absolute paths suitable for compiler args.
    public static ArrayList<File> collectAllJavaSourceFiles (
        String sc_id,
        File projectFilesDir
    ) {
        if (sc_id == null || sc_id.isEmpty()) {
            throw new IllegalArgumentException("sc_id must not be null or empty.");
        }
        if (projectFilesDir == null || !projectFilesDir.exists()) {
            throw new IllegalArgumentException("projectFilesDir must exist.");
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Load the package registry for this project
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        PkgRegistry registry = new PkgRegistry (projectFilesDir);

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Use YqPkgBridge to collect all .java files
        // from all source roots
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        YqPkgBridge bridge = new YqPkgBridge (projectFilesDir);
        return bridge.collectAllJavaFiles(registry);
    }

    // Returns all source root directories (including main + all extras)
    // for use in ECJ's -sourcepath argument.
    public static String getSourcePathForEcj (
        String sc_id,
        File projectFilesDir
    ) {
        if (sc_id == null || sc_id.isEmpty()) {
            throw new IllegalArgumentException("sc_id must not be null or empty.");
        }
        if (projectFilesDir == null || !projectFilesDir.exists()) {
            throw new IllegalArgumentException("projectFilesDir must exist.");
        }

        PkgRegistry registry = new PkgRegistry(projectFilesDir);
        YqPkgBridge bridge = new YqPkgBridge(projectFilesDir);
        ArrayList<File> allRoots = bridge.getAllSourceRoots(registry);

        // Convert to colon-separated path string for ECJ -sourcepath
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allRoots.size(); i++) {
            if (i > 0) sb.append(":");
            sb.append(allRoots.get(i).getAbsolutePath());
        }

        return sb.toString();
    }



  
}

