package pro.sketchware.managers.java;




// =========================================================
// JavaPkgEntry
// =========================================================

// Data model representing a single user-defined Java package
// attached to a Sketchware project.

// PURPOSE:
    // Carries all information needed to:
        // identify a package (id, packageName)
        // display it in the UI (displayName)
        // locate its files on disk (sourceRootPath)
        // protect the main package from deletion (isMain)

// IMPORTANT:
    // Plain data class — no logic, no I/O.
    // Created by JavaPkgRegistry, stored by JavaPkgStore.
    // Never instantiate directly from UI code.

// =========================================================

public final class JavaPkgEntry {




    // =========================================================
    // CONSTANTS
    // =========================================================

    // Sentinel display name reserved for the main package.
    // Used as a fallback if displayName is somehow missing.
    public static final String MAIN_DISPLAY_NAME = "main";




    // =========================================================
    // FIELDS
    // =========================================================

    // Stable unique identifier (UUID string).
    // Never changes after creation — even if packageName is edited later.
    private final String id;

    // Fully-qualified Java package name.
    // Example: "com.z.ui"  or  "com.x.db"
    // Validated by JavaPkgValidator before assignment.
    private final String packageName;

    // Short human-readable label shown in the UI picker.
    // Example: "UI Layer"  or  "Database"
    // Does not affect compilation — purely cosmetic.
    private String displayName;

    // Absolute path to the folder where .java files for this
    // package are stored on disk.
    // Main package   → .sketchware/data/{id}/files/java/
    // Extra packages → .sketchware/data/{id}/files/java_extra/{packageName}/
    private final String sourceRootPath;

    // True only for the app's original/main package.
    // Main package cannot be deleted or have its packageName changed.
    private final boolean isMain;




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public JavaPkgEntry (
        String id,
        String packageName,
        String displayName,
        String sourceRootPath,
        boolean isMain
    ) {
        if (id == null || id.isEmpty())
            throw new IllegalArgumentException ("JavaPkgEntry: id must not be null or empty.");
        if (packageName == null || packageName.isEmpty())
            throw new IllegalArgumentException ("JavaPkgEntry: packageName must not be null or empty.");
        if (sourceRootPath == null || sourceRootPath.isEmpty())
            throw new IllegalArgumentException ("JavaPkgEntry: sourceRootPath must not be null or empty.");

        this.id             = id;
        this.packageName    = packageName;
        this.displayName    = (displayName != null && !displayName.isEmpty())
                                ? displayName
                                : MAIN_DISPLAY_NAME;
        this.sourceRootPath = sourceRootPath;
        this.isMain         = isMain;
    }




    // =========================================================
    // GETTERS
    // =========================================================

    public String getId()             { return id;             }
    public String getPackageName()    { return packageName;    }
    public String getDisplayName()    { return displayName;    }
    public String getSourceRootPath() { return sourceRootPath; }
    public boolean isMain()           { return isMain;         }




    // =========================================================
    // SETTERS
    // =========================================================

    // Only displayName is mutable post-construction.
    // packageName and sourceRootPath are intentionally final
    // to avoid desync between the folder on disk and the model.
    public void setDisplayName (String displayName) {
        if (displayName == null || displayName.isEmpty())
            throw new IllegalArgumentException ("JavaPkgEntry: displayName must not be null or empty.");
        this.displayName = displayName;
    }




    // =========================================================
    // UTILITY
    // =========================================================

    // Convenience — converts packageName to a relative folder path.
    // Example: "com.z.ui" → "com/z/ui"
    // Used by JavaPkgRegistry when building sourceRootPath for new entries.
    public static String packageNameToPath (String packageName) {
        if (packageName == null) return "";
        return packageName.replace('.', '/');
    }

    @Override
    public String toString() {
        return "JavaPkgEntry{"
            + "id='"             + id             + '\''
            + ", packageName='"  + packageName     + '\''
            + ", displayName='"  + displayName     + '\''
            + ", sourceRootPath='" + sourceRootPath + '\''
            + ", isMain="          + isMain
            + '}';
    }




}
