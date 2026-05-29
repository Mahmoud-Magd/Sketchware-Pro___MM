package mod.magd.pkgs.migration;

import java.io.File;
import java.util.UUID;

import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;

import mod.jbk.util.LogUtil;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;


// =========================================================
// PkgMigrator
// =========================================================

// Handles backward compatibility for existing projects
// that were created before multi-package support existed.

// PURPOSE:
    // Ensures that when a user opens an old project (that has
    // no java_pkgs.json), the system automatically creates the
    // registry with ONE entry for the main app package.
    //
    // This entry represents the existing .sketchware/data/{id}/files/java/
    // folder, so no files are moved — everything stays exactly where it is.
    //
    // From that point on, the new multi-package system runs normally.

// MIGRATION STRATEGY:
    // 1. Check if java_pkgs.json exists for this project
    // 2. If YES → already migrated, do nothing
    // 3. If NO → create the registry with the main package entry
    // 4. Main entry has: isMain=true, sourceRootPath=files/java/
    // 5. Files stay untouched on disk

// USAGE:
    // In ManageJavaActivity.onCreate():
    // PkgMigrator.ensureMigrated(sc_id);
    // Now you can safely use PkgRegistry.

// =========================================================

public final class PkgMigrator {

    private static final String TAG = "PkgMigrator";




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    private PkgMigrator() {}




    // =========================================================
    // PUBLIC
    // =========================================================

    // Checks if the project has been migrated.
    // If not, performs migration silently.
    // Safe to call multiple times — idempotent.
    public static void ensureMigrated (String sc_id) {
        if (sc_id == null || sc_id.isEmpty()) {
            LogUtil.w(TAG, "ensureMigrated: sc_id is null or empty. Skipping migration.");
            return;
        }

        try {
            File projectFilesDir = new File (
                FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/files"
            );

            if ( !projectFilesDir.exists() ) {
                LogUtil.w(TAG, "ensureMigrated: project files directory does not exist: " + projectFilesDir);
                return;
            }

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Step 1: Check if already migrated
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            File registryFile = new File (projectFilesDir, "java_pkgs.json");
            if (registryFile.exists()) {
                LogUtil.d(TAG, "Project " + sc_id + " is already migrated.");
                return;
            }

            LogUtil.d(TAG, "Project " + sc_id + " needs migration. Creating registry...");

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Step 2: Get the app's main package name
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // We need this to create the main package entry.
            // It's stored in the project's metadata. For now,
            // we'll use a sensible default if we can't find it.
            String mainPackageName = getMainPackageNameForProject(sc_id);

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Step 3: Create and register the main package entry
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            PkgRegistry registry = new PkgRegistry(projectFilesDir);

            // The main package uses the existing files/java/ folder
            String sourceRootPath = projectFilesDir.getAbsolutePath() + "/java";

            PkgEntry mainEntry = new PkgEntry(
                UUID.randomUUID().toString(),  // stable, unique id
                mainPackageName,                // e.g. "com.example.myapp"
                "main",                         // display name
                sourceRootPath,                 // .../files/java/
                true                            // isMain = true
            );

            // Add the main entry to the registry and save
            registry.cache = new java.util.ArrayList<>();
            registry.cache.add(mainEntry);
            registry.save();

            LogUtil.d(TAG, "Migration complete for project " + sc_id + " with package " + mainPackageName);

        } catch (Exception e) {
            LogUtil.e(TAG, "Migration failed for project " + sc_id, e);
            // Don't throw — let the app continue. Worst case, user creates the file manually.
        }
    }




    // =========================================================
    // PRIVATE
    // =========================================================

    // Attempts to retrieve the project's main package name.
    // Returns a sensible default if it can't find it.
    private static String getMainPackageNameForProject (String sc_id) {
        try {
            // Try to read from project metadata
            FilePathUtil fpu = new FilePathUtil();
            // This path may vary — adjust if needed based on your actual metadata location
            String metadataPath = fpu.getPathSketchware(sc_id) + "/project.json";

            if (FileUtil.isExistFile(metadataPath)) {
                String content = FileUtil.readFile(metadataPath);
                // Parse JSON to find packageName
                // For now, using a simple regex — a proper JSON parser is better
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"packageName\"\\s*:\\s*\"([^\"]+)\"");
                java.util.regex.Matcher m = p.matcher(content);
                if (m.find()) {
                    String found = m.group(1);
                    LogUtil.d(TAG, "Found main package name: " + found);
                    return found;
                }
            }
        } catch (Exception e) {
            LogUtil.w(TAG, "Could not read project metadata: " + e.getMessage());
        }

        // Fallback: use a generic default based on sc_id
        String fallback = "com.sketchware.project." + sc_id;
        LogUtil.d(TAG, "Using fallback package name: " + fallback);
        return fallback;
    }




  
}


