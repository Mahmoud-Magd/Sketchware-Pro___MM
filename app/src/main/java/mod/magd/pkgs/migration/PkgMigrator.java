package mod.magd.pkgs.migration;

import java.io.File;
import java.util.UUID;

import a.a.a.lC;
import a.a.a.yB;

import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgStore;

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
    // 6. Main package name comes from: lC.b (sc_id) (which reads the project metadata file)

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
    //
    // sc_id: The project's unique identifier
    //   The main package name is loaded from the project metadata
    public static void ensureMigrated (String sc_id) {
        if (sc_id == null || sc_id.isEmpty()) {
            LogUtil.w(TAG, "ensureMigrated: sc_id is null or empty. Skipping migration.");
            return;
        }

        try {
            FilePathUtil fpu = new FilePathUtil();
            File projectFilesDir = fpu.getProjectFilesDir(sc_id);

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

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Step 2: Load the project's main package name
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            // Just read it from the project metadata file
            HashMap <String, Object> projectData = lC.b (sc_id);
            if (projectData == null) {
                LogUtil.w (TAG, "ensureMigrated: could not load project data for sc_id: " + sc_id);
                return;
            }
            
            String mainPackageName = yB.c (projectData, "my_sc_pkg_name");
            if ( mainPackageName == null || mainPackageName.isEmpty() ) {
                LogUtil.w (TAG, "ensureMigrated: package name is empty for sc_id: " + sc_id);
                return;
            }
            
            LogUtil.d (TAG, "Project " + sc_id + " needs migration. Creating registry with main package: " + mainPackageName);
            

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Step 3: Create the main package entry
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // The main package uses the existing files/java/ folder
            String sourceRootPath = projectFilesDir.getAbsolutePath() + "/java";

            PkgEntry mainEntry = new PkgEntry(
                UUID.randomUUID().toString(),  // stable, unique id
                mainPackageName,                // e.g. "com.example.myapp"
                "main",                         // display name
                sourceRootPath,                 // .../files/java/
                true                            // isMain = true
            );

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // Step 4: Save to registry file
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            java.util.ArrayList<PkgEntry> entries = new java.util.ArrayList<>();
            entries.add(mainEntry);

            // Use PkgStore to write (internal API)
            PkgStore.write(registryFile, entries);

            LogUtil.d(TAG, "Migration complete for project " + sc_id + " with package " + mainPackageName);

        } catch (Exception e) {
            LogUtil.e(TAG, "Migration failed for project " + sc_id, e);
            // Don't throw — let the app continue. Worst case, user creates the file manually.
        }
    }
    
    
    
    
}

