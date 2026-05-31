package mod.magd.pkgs.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;
import mod.magd.pkgs.PkgStore;
import mod.magd.pkgs.PkgValidator;

import mod.jbk.util.LogUtil;




// =========================================================
// PkgRegistrySync
// =========================================================

// Synchronizes the package registry with the actual file
// system state in java_extra/ during compilation.

// PURPOSE:
//     // Ensures consistency between:
//         // - java_pkgs.json (registry)
//         // - actual folders in java_extra/ (file system)
//     // Detects and reports three types of inconsistencies:
//         // 1. Manually created packages not in registry (auto-add if valid)
//         // 2. Deleted packages in registry but missing on disk (recreate empty)
//         // 3. Invalid files (not folders) in java_extra (error)

// CALLED BY:
//     // The Sketchware compiler before compilation begins.
//     // If errors are found, compilation is stopped and user is shown errors.

// SYNC OPERATIONS:
//     // ✓ Auto-detect: scan java_extra/ for unknown folders
//     // ✓ Auto-validate: check detected folder names against 8 rules
//     // ✓ Auto-add: add valid packages to registry
//     // ✗ Auto-delete: DO NOT delete orphaned entries (user must decide)
//     // • Recreate: create empty folder for missing packages (prevents later errors)
//     // ✗ Auto-remove: reject files in java_extra/ with error

// IMPORTANT:
//     // - Only checks the java_extra/ directory, not the main java/ package
//     // - Files in java_extra/ are not allowed (error condition)
//     // - Deleted packages are recreated empty (safe fallback)
//     // - All validation uses PkgValidator.validate()

// USAGE (in compiler):
//     PkgRegistrySync sync = new PkgRegistrySync();
//     File projectDir = new File(...);
//     String mainPackage = "com.example.app";
//     PkgRegistry registry = new PkgRegistry(projectFilesDir);
//     SyncResult result = sync.synchronizeRegistry(projectDir, registry, mainPackage);
//
//     if (result.hasErrors()) {
//         throw new CompilerException(result.getErrorSummary());
//     }

// =========================================================

public final class PkgRegistrySync {




    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String TAG = "PkgRegistrySync";

    // The directory name where extra packages are stored
    private static final String JAVA_EXTRA_DIR = "java_extra";




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public PkgRegistrySync() {
        // Utility class
    }




    // =========================================================
    // PUBLIC
    // =========================================================

    // Synchronize the package registry with the file system.
    // Performs all checks and auto-corrections.
    // Returns detailed results (errors, warnings, added packages, etc.)
    public SyncResult synchronizeRegistry (
        File projectDir,
        PkgRegistry registry,
        String mainPackageName
    ) {
        SyncResult result = new SyncResult();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Validation
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if (projectDir == null) {
            result.addError("projectDir is null");
            return result;
        }

        if (registry == null) {
            result.addError("registry is null");
            return result;
        }

        if (mainPackageName == null || mainPackageName.isEmpty()) {
            result.addError("mainPackageName is null or empty");
            return result;
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Get java_extra directory
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        File javaExtraDir = new File(projectDir, JAVA_EXTRA_DIR);

        if (!javaExtraDir.exists()) {
            // java_extra doesn't exist yet (new project)
            // Create it
            boolean created = javaExtraDir.mkdirs();
            if (created) {
                result.addInfo("java_extra directory created");
            }
            return result;
        }

        if (!javaExtraDir.isDirectory()) {
            result.addError("java_extra exists but is not a directory");
            return result;
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Get all packages from registry
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        ArrayList<PkgEntry> registryPackages = registry.getAll();
        Set<String> registryPackageNames = new HashSet<>();
        for (PkgEntry entry : registryPackages) {
            registryPackageNames.add(entry.getPackageName());
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Get all folders and files in java_extra
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Set<String> actualFolders = new HashSet<>();
        Set<String> invalidFiles = new HashSet<>();

        File[] entries = javaExtraDir.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    actualFolders.add(entry.getName());
                } else if (entry.isFile()) {
                    invalidFiles.add(entry.getName());
                }
            }
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Check 1: Detect invalid files in java_extra
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        checkForInvalidFiles(invalidFiles, result);

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Check 2: Auto-detect and add manually created packages
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        checkForNewPackages(
            actualFolders,
            registryPackageNames,
            registry,
            mainPackageName,
            result
        );

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // Check 3: Detect deleted packages and recreate empty folders
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        checkForDeletedPackages(
            registryPackageNames,
            actualFolders,
            javaExtraDir,
            result
        );

        return result;
    }




    // =========================================================
    // PRIVATE — Check 1: Invalid Files
    // =========================================================

    // Check for files (not folders) in java_extra
    // Only folders are allowed in java_extra
    private void checkForInvalidFiles (
        Set<String> invalidFiles,
        SyncResult result
    ) {
        for (String fileName : invalidFiles) {
            result.addError(
                "Found file '" + fileName + "' in java_extra/. " +
                "Only package folders are allowed in java_extra. " +
                "Please remove this file: " + fileName
            );
        }
    }




    // =========================================================
    // PRIVATE — Check 2: New Packages (Auto-detect & Add)
    // =========================================================

    // Scan for folders not in registry and attempt to auto-add them
    private void checkForNewPackages (
        Set<String> actualFolders,
        Set<String> registryPackageNames,
        PkgRegistry registry,
        String mainPackageName,
        SyncResult result
    ) {
        // Find folders not in registry
        List<String> newPackages = new ArrayList<>();
        for (String folder : actualFolders) {
            if (!registryPackageNames.contains(folder)) {
                newPackages.add(folder);
            }
        }

        if (newPackages.isEmpty()) {
            return;
        }

        result.addInfo("Found " + newPackages.size() + " manually created package(s)");

        // Try to add each one
        for (String newPackageName : newPackages) {
            // ────────────────────────────────────────────────────
            // Validate the package name
            // ────────────────────────────────────────────────────
            PkgValidator.Result validationResult = PkgValidator.validate(
                newPackageName,
                null  // existingEntries (we'll check duplicates separately)
            );

            if (!validationResult.isValid()) {
                result.addError(
                    "Found extra package folder '" + newPackageName + "' in java_extra/. " +
                    "Tried to automatically add it but validation failed: " +
                    validationResult.getReason()
                );
                continue;
            }

            // ────────────────────────────────────────────────────
            // Check for duplicates manually
            // ────────────────────────────────────────────────────
            if (registryPackageNames.contains(newPackageName)) {
                result.addError(
                    "Package '" + newPackageName + "' already exists in registry"
                );
                continue;
            }

            // ────────────────────────────────────────────────────
            // Check if it's the main package
            // ────────────────────────────────────────────────────
            if (newPackageName.equals(mainPackageName)) {
                result.addError(
                    "Package '" + newPackageName + "' is the main package. " +
                    "It should not be in java_extra/"
                );
                continue;
            }

            // ────────────────────────────────────────────────────
            // Auto-add the package
            // ────────────────────────────────────────────────────
            try {
                // Add with empty display name (will use package name as fallback)
                registry.addPackage(newPackageName, "");

                result.addAutoAddedPackage(newPackageName);
                result.addInfo("Auto-added package: " + newPackageName);

                registryPackageNames.add(newPackageName);

            } catch (Exception e) {
                result.addError(
                    "Failed to auto-add package '" + newPackageName + "': " + e.getMessage()
                );
            }
        }
    }




    // =========================================================
    // PRIVATE — Check 3: Deleted Packages
    // =========================================================

    // Detect packages in registry but missing from disk
    // Recreate empty folder as a safety measure
    private void checkForDeletedPackages (
        Set<String> registryPackageNames,
        Set<String> actualFolders,
        File javaExtraDir,
        SyncResult result
    ) {
        List<String> deletedPackages = new ArrayList<>();

        for (String pkgName : registryPackageNames) {
            if (!actualFolders.contains(pkgName)) {
                deletedPackages.add(pkgName);
            }
        }

        if (deletedPackages.isEmpty()) {
            return;
        }

        result.addInfo("Found " + deletedPackages.size() + " deleted package(s) on disk");

        // Try to recreate each one
        for (String deletedPackage : deletedPackages) {
            File pkgFolder = new File(javaExtraDir, deletedPackage);

            try {
                boolean created = pkgFolder.mkdirs();
                if (created) {
                    result.addDeletedPackageRecreated(deletedPackage);
                    result.addInfo(
                        "Recreated missing package folder: " + deletedPackage
                    );
                }
            } catch (Exception e) {
                result.addError(
                    "Failed to recreate missing package folder '" + deletedPackage + "': " + e.getMessage()
                );
            }
        }
    }




    // =========================================================
    // INNER — Result Object
    // =========================================================

    public static final class SyncResult {

        private List<String> infos;
        private List<String> warnings;
        private List<String> errors;
        private List<String> autoAddedPackages;
        private List<String> deletedPackagesRecreated;

        public SyncResult() {
            this.infos = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.autoAddedPackages = new ArrayList<>();
            this.deletedPackagesRecreated = new ArrayList<>();
        }

        // ──────────────────────────────────────────────────────
        // Recording results
        // ──────────────────────────────────────────────────────

        void addInfo (String msg) {
            infos.add(msg);
        }

        void addWarning (String msg) {
            warnings.add(msg);
        }

        void addError (String msg) {
            errors.add(msg);
        }

        void addAutoAddedPackage (String pkgName) {
            autoAddedPackages.add(pkgName);
        }

        void addDeletedPackageRecreated (String pkgName) {
            deletedPackagesRecreated.add(pkgName);
        }

        // ──────────────────────────────────────────────────────
        // Querying results
        // ──────────────────────────────────────────────────────

        public List<String> getInfos() {
            return new ArrayList<>(infos);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getAutoAddedPackages() {
            return new ArrayList<>(autoAddedPackages);
        }

        public List<String> getDeletedPackagesRecreated() {
            return new ArrayList<>(deletedPackagesRecreated);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        // ──────────────────────────────────────────────────────
        // Summary
        // ──────────────────────────────────────────────────────

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══ Package Registry Sync ═══\n");

            if (errors.isEmpty() && infos.isEmpty()) {
                sb.append("✓ All packages in sync\n");
            }

            if (!infos.isEmpty()) {
                sb.append("Info:\n");
                for (String info : infos) {
                    sb.append("  ℹ ").append(info).append("\n");
                }
            }

            if (!autoAddedPackages.isEmpty()) {
                sb.append("Auto-Added: ");
                for (String pkg : autoAddedPackages) {
                    sb.append(pkg).append(", ");
                }
                sb.setLength(sb.length() - 2);  // Remove trailing comma+space
                sb.append("\n");
            }

            if (!deletedPackagesRecreated.isEmpty()) {
                sb.append("Recreated: ");
                for (String pkg : deletedPackagesRecreated) {
                    sb.append(pkg).append(", ");
                }
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }

            return sb.toString();
        }

        public String getDetailedSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══ Package Registry Sync (Detailed) ═══\n\n");

            if (!infos.isEmpty()) {
                sb.append("── Info ──\n");
                for (String info : infos) {
                    sb.append("  ℹ ").append(info).append("\n");
                }
                sb.append("\n");
            }

            if (!autoAddedPackages.isEmpty()) {
                sb.append("── Auto-Added Packages ──\n");
                for (String pkg : autoAddedPackages) {
                    sb.append("  ✓ ").append(pkg).append("\n");
                }
                sb.append("\n");
            }

            if (!deletedPackagesRecreated.isEmpty()) {
                sb.append("── Recreated Packages ──\n");
                for (String pkg : deletedPackagesRecreated) {
                    sb.append("  ⚠ ").append(pkg).append("\n");
                }
                sb.append("\n");
            }

            if (!errors.isEmpty()) {
                sb.append("── Errors ──\n");
                for (int i = 0; i < errors.size(); i++) {
                    sb.append("  ✗ ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
                }
            } else if (infos.isEmpty()) {
                sb.append("✓ All packages in sync\n");
            }

            return sb.toString();
        }

        public String getErrorSummary() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("Compilation blocked due to package synchronization errors:\n\n");
                for (int i = 0; i < errors.size(); i++) {
                    sb.append(i + 1).append(". ").append(errors.get(i)).append("\n");
                }
            }
            return sb.toString();
        }
    }




}
