package mod.magd.pkgs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;




// =========================================================
// PkgStore
// =========================================================

// Low-level I/O layer for the per-project package registry file.

// PURPOSE:
    // Reads and writes java_pkgs.json to/from disk.
    // Knows the JSON schema — no other class does.
    // Knows nothing about business rules or validation.

// FILE LOCATION (per project):
    // .sketchware/data/{projectId}/files/java_pkgs.json

// JSON SCHEMA:
    // Array of objects, each:
    // {
    //   "id":             "uuid-string",
    //   "packageName":    "com.z.ui",
    //   "displayName":    "UI Layer",
    //   "sourceRootPath": "/storage/emulated/0/.sketchware/data/{projectId}/files/java_extra/com.z.ui/",
    //   "isMain":         false
    // }

// IMPORTANT:
    // Internal infrastructure — not for direct UI use.
    // All callers should go through PkgRegistry.

// =========================================================

public final class PkgStore {




    // =========================================================
    // CONSTANTS — JSON Keys
    // =========================================================

    private static final String KEY_ID              = "id";
    private static final String KEY_PACKAGE_NAME    = "packageName";
    private static final String KEY_DISPLAY_NAME    = "displayName";
    private static final String KEY_SOURCE_ROOT     = "sourceRootPath";
    private static final String KEY_IS_MAIN         = "isMain";




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    private PkgStore() {}




    // =========================================================
    // PUBLIC METHODS
    // =========================================================

    // Reads the registry file and returns all entries.
    // Returns an empty list (not null) if the file doesn't exist yet.
    // Throws PkgStoreException if the file exists but is malformed.
    public static ArrayList<PkgEntry> read (File registryFile) {
        if (registryFile == null)
            throw new IllegalArgumentException ("PkgStore.read(): registryFile must not be null.");

        ArrayList<PkgEntry> result = new ArrayList<>();

        if ( ! registryFile.exists() ) return result;

        String raw = readFileContents (registryFile);

        try {
            JSONArray array = new JSONArray (raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject (i);
                result.add ( parseEntry (obj) );
            }
        } catch (JSONException e) {
            throw new PkgStoreException (
                "Failed to parse java_pkgs.json at: "
                + registryFile.getAbsolutePath()
                + " — " + e.getMessage()
            );
        }

        return result;
    }

    // Serializes all entries and writes them to the registry file.
    // Creates parent directories if they don't exist.
    // Throws PkgStoreException on I/O failure.
    public static void write (File registryFile, ArrayList<PkgEntry> entries) {
        if (registryFile == null)
            throw new IllegalArgumentException ("PkgStore.write(): registryFile must not be null.");
        if (entries == null)
            throw new IllegalArgumentException ("PkgStore.write(): entries must not be null.");

        ensureParentExists (registryFile);

        JSONArray array = new JSONArray();
        for (PkgEntry entry : entries) {
            array.put ( serializeEntry (entry) );
        }

        writeFileContents (registryFile, array.toString (2));
    }




    // =========================================================
    // PRIVATE — Parse & Serialize
    // =========================================================

    private static PkgEntry parseEntry (JSONObject obj) throws JSONException {
        return new PkgEntry (
            obj.getString  (KEY_ID),
            obj.getString  (KEY_PACKAGE_NAME),
            obj.getString  (KEY_DISPLAY_NAME),
            obj.getString  (KEY_SOURCE_ROOT),
            obj.optBoolean (KEY_IS_MAIN, false)
        );
    }

    private static JSONObject serializeEntry (PkgEntry entry) {
        JSONObject obj = new JSONObject();
        try {
            obj.put (KEY_ID,           entry.getId());
            obj.put (KEY_PACKAGE_NAME, entry.getPackageName());
            obj.put (KEY_DISPLAY_NAME, entry.getDisplayName());
            obj.put (KEY_SOURCE_ROOT,  entry.getSourceRootPath());
            obj.put (KEY_IS_MAIN,      entry.isMain());
        } catch (JSONException e) {
            // JSONObject.put() only throws if the key is null — never happens here.
            throw new RuntimeException ("PkgStore: unexpected JSON serialization error.", e);
        }
        return obj;
    }




    // =========================================================
    // PRIVATE — File I/O
    // =========================================================

    private static String readFileContents (File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader (new FileReader (file))) {
            String line;
            while ( (line = reader.readLine()) != null ) {
                sb.append (line);
            }
        } catch (IOException e) {
            throw new PkgStoreException (
                "Failed to read java_pkgs.json: " + e.getMessage()
            );
        }
        return sb.toString();
    }

    private static void writeFileContents (File file, String content) {
        try (BufferedWriter writer = new BufferedWriter (new FileWriter (file, false))) {
            writer.write (content);
        } catch (IOException e) {
            throw new PkgStoreException (
                "Failed to write java_pkgs.json: " + e.getMessage()
            );
        }
    }

    private static void ensureParentExists (File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (!created) {
                throw new PkgStoreException (
                    "Failed to create directory: " + parent.getAbsolutePath()
                );
            }
        }
    }




    // =========================================================
    // INNER — Checked Exception
    // =========================================================

    // Unchecked so callers don't need try-catch everywhere,
    // but still typed so UI can catch it specifically and show
    // a proper error message instead of a generic crash.
    public static final class PkgStoreException extends RuntimeException {
        public PkgStoreException (String message) {
            super (message);
        }
    }




}


