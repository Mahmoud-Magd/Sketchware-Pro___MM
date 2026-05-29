package pro.sketchware.managers.java;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;




// =========================================================
// JavaPkgRegistry
// =========================================================

// Central manager for all Java packages belonging to a
// Sketchware project.

// PURPOSE:
    // The single source of truth for package operations:
        // loading and saving the package list
        // adding new packages (validation → folder → save)
        // removing packages (folder cleanup → save)
        // querying packages by id or name
        // building file paths for the registry and source roots

// REGISTRY FILE (per project):
    // {projectDataDir}/files/java_pkgs.json

// SOURCE ROOT LAYOUT:
    // Main package   → {projectDataDir}/files/java/
    // Extra packages → {projectDataDir}/files/java_extra/{packageName}/

// USAGE:
    // JavaPkgRegistry reg = new JavaPkgRegistry (projectDataDir);
    // reg.addPackage ("com.z.ui", "UI Layer");
    // ArrayList<JavaPkgEntry> all = reg.getAll();

// IMPORTANT:
    // One instance per project session is enough.
    // Not thread-safe — call from the main thread only.
    // Validation is done via JavaPkgValidator before any mutation.

// =========================================================

public final class JavaPkgRegistry {




    // =========================================================
    // CONSTANTS
    // =========================================================

    // Name of the registry file inside the project files dir.
    private static final String REGISTRY_FILE_NAME  = "java_pkgs.json";

    // Subfolder name for all extra (non-main) package source roots.
    private static final String EXTRA_ROOT_FOLDER   = "java_extra";

    // Subfolder name for the main package source root (unchanged).
    private static final String MAIN_ROOT_FOLDER    = "java";




    // =========================================================
    // VARIABLES
    // =========================================================

    // Absolute path to .sketchware/data/{projectId}/files/
    private final File projectFilesDir;

    // In-memory cache of the registry for this session.
    // Loaded lazily on first access.
    private ArrayList<JavaPkgEntry> cache;




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    // projectFilesDir → .sketchware/data/{projectId}/files/
    public JavaPkgRegistry (File projectFilesDir) {
        if (projectFilesDir == null)
            throw new IllegalArgumentException ("JavaPkgRegistry: projectFilesDir must not be null.");
        this.projectFilesDir = projectFilesDir;
    }




    // =========================================================
    // PUBLIC — Query
    // =========================================================

    // Returns all packages for this project.
    // Loads from disk on first call; returns cache on subsequent calls.
    public ArrayList<JavaPkgEntry> getAll() {
        ensureLoaded();
        return new ArrayList<> (cache); // defensive copy
    }

    // Returns the main package entry.
    // Guaranteed to exist after migration (Step 8) has run.
    // Returns null only if called before migration on a brand-new project.
    public JavaPkgEntry getMain() {
        ensureLoaded();
        for (JavaPkgEntry entry : cache) {
            if ( entry.isMain() ) return entry;
        }
        return null;
    }

    // Returns true if a package with this exact packageName already exists.
    public boolean exists (String packageName) {
        ensureLoaded();
        return findByName (packageName) != null;
    }

    // Returns the entry matching this packageName, or null if not found.
    public JavaPkgEntry findByName (String packageName) {
        ensureLoaded();
        if (packageName == null) return null;
        for (JavaPkgEntry entry : cache) {
            if ( entry.getPackageName().equals (packageName) ) return entry;
        }
        return null;
    }

    // Returns the entry matching this id, or null if not found.
    public JavaPkgEntry findById (String id) {
        ensureLoaded();
        if (id == null) return null;
        for (JavaPkgEntry entry : cache) {
            if ( entry.getId().equals (id) ) return entry;
        }
        return null;
    }




    // =========================================================
    // PUBLIC — Mutation
    // =========================================================

    // Adds a new package.
    // Validates packageName, creates the source root folder on disk, saves.
    // Returns the newly created entry.
    // Throws IllegalArgumentException if validation fails.
    // Throws JavaPkgStore.JavaPkgStoreException on I/O failure.
    public JavaPkgEntry addPackage (String packageName, String displayName) {
        ensureLoaded();

        JavaPkgValidator.Result validation = JavaPkgValidator.validate (packageName, cache);
        if ( ! validation.isValid() ) {
            throw new IllegalArgumentException (
                "Cannot add package '" + packageName + "': " + validation.getReason()
            );
        }

        String sourceRootPath = buildExtraSourceRoot (packageName);
        createFolderOnDisk (sourceRootPath);

        JavaPkgEntry entry = new JavaPkgEntry (
            generateId(),
            packageName,
            displayName,
            sourceRootPath,
            false // isMain = false — only migration creates the main entry
        );

        cache.add (entry);
        persistCache();

        return entry;
    }

    // Removes a package by id.
    // Deletes the source root folder and all .java files inside it.
    // Throws IllegalStateException if trying to remove the main package.
    // Throws IllegalArgumentException if no entry with this id exists.
    public void removePackage (String id) {
        ensureLoaded();

        JavaPkgEntry entry = findById (id);
        if (entry == null) {
            throw new IllegalArgumentException (
                "JavaPkgRegistry.removePackage(): no entry found with id '" + id + "'."
            );
        }
        if ( entry.isMain() ) {
            throw new IllegalStateException (
                "JavaPkgRegistry.removePackage(): the main package cannot be removed."
            );
        }

        deleteFolderOnDisk (entry.getSourceRootPath());
        cache.remove (entry);
        persistCache();
    }

    // Updates the display name of an existing package.
    // Does not affect the folder on disk.
    public void renameDisplay (String id, String newDisplayName) {
        ensureLoaded();

        JavaPkgEntry entry = findById (id);
        if (entry == null) {
            throw new IllegalArgumentException (
                "JavaPkgRegistry.renameDisplay(): no entry found with id '" + id + "'."
            );
        }

        entry.setDisplayName (newDisplayName);
        persistCache();
    }

    // Seeds the registry with a single main package entry.
    // Called by JavaPkgMigrator (Step 8) on first open of an existing project.
    // Throws IllegalStateException if a main entry already exists.
    public void seedMainPackage (String packageName, String displayName) {
        ensureLoaded();

        if (getMain() != null) {
            throw new IllegalStateException (
                "JavaPkgRegistry.seedMainPackage(): main package already exists."
            );
        }

        String sourceRootPath = buildMainSourceRoot();

        JavaPkgEntry mainEntry = new JavaPkgEntry (
            generateId(),
            packageName,
            displayName,
            sourceRootPath,
            true // isMain = true
        );

        cache.add (mainEntry);
        persistCache();
    }




    // =========================================================
    // PUBLIC — Path Helpers
    // =========================================================

    // Returns the File object for the registry JSON.
    public File getRegistryFile() {
        return new File (projectFilesDir, REGISTRY_FILE_NAME);
    }

    // Returns true if the registry file exists on disk.
    // Used by JavaPkgMigrator to detect first-time projects.
    public boolean isRegistryPresent() {
        return getRegistryFile().exists();
    }

    // Returns the base folder for all extra source roots.
    public File getExtraRootDir() {
        return new File (projectFilesDir, EXTRA_ROOT_FOLDER);
    }




    // =========================================================
    // PRIVATE — Lazy Load & Persist
    // =========================================================

    private void ensureLoaded() {
        if (cache == null) {
            cache = JavaPkgStore.read ( getRegistryFile() );
        }
    }

    private void persistCache() {
        JavaPkgStore.write ( getRegistryFile(), cache );
    }




    // =========================================================
    // PRIVATE — Path Builders
    // =========================================================

    private String buildMainSourceRoot() {
        return new File (projectFilesDir, MAIN_ROOT_FOLDER)
            .getAbsolutePath() + File.separator;
    }

    private String buildExtraSourceRoot (String packageName) {
        return new File (
            new File (projectFilesDir, EXTRA_ROOT_FOLDER),
            packageName
        ).getAbsolutePath() + File.separator;
    }




    // =========================================================
    // PRIVATE — Disk Ops
    // =========================================================

    private void createFolderOnDisk (String absolutePath) {
        File folder = new File (absolutePath);
        if ( ! folder.exists() ) {
            boolean created = folder.mkdirs();
            if (!created) {
                throw new JavaPkgStore.JavaPkgStoreException (
                    "Failed to create source root folder: " + absolutePath
                );
            }
        }
    }

    private void deleteFolderOnDisk (String absolutePath) {
        File folder = new File (absolutePath);
        if ( folder.exists() ) {
            deleteRecursive (folder);
        }
    }

    private void deleteRecursive (File file) {
        if ( file.isDirectory() ) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive (child);
                }
            }
        }
        file.delete();
    }




    // =========================================================
    // PRIVATE — ID Generation
    // =========================================================

    private String generateId() {
        return UUID.randomUUID().toString();
    }




}
