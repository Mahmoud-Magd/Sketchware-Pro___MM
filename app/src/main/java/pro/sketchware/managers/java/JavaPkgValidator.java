package pro.sketchware.managers.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;




// =========================================================
// JavaPkgValidator
// =========================================================

// Validates a user-supplied package name before it is added
// to the registry.

// PURPOSE:
    // Enforces Java package naming rules plus project-level rules.
    // Returns a typed Result so callers get a specific error
    // message to show in the UI, not just a boolean.

// RULES ENFORCED:
    // 1. Not null, not empty, not blank.
    // 2. Max 200 characters.
    // 3. Does not start or end with a dot.
    // 4. No consecutive dots (e.g. "com..ui").
    // 5. Each segment is a valid Java identifier:
        // - starts with letter or underscore
        // - contains only letters, digits, underscores
        // - not a Java reserved word
    // 6. Must have at least 2 segments (e.g. "com.ui" not "ui").
    // 7. Not a duplicate of any existing packageName in the project.
    // 8. Not a duplicate of the main package.

// USAGE:
    // JavaPkgValidator.Result r = JavaPkgValidator.validate (input, existingEntries);
    // if (!r.isValid()) showError (r.getReason());

// =========================================================

public final class JavaPkgValidator {




    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_LENGTH = 200;
    private static final int MIN_SEGMENTS = 2;

    // Full set of Java reserved words.
    // A package segment must not be any of these.
    private static final Set<String> RESERVED = new HashSet<> ( Arrays.asList (
        "abstract",   "assert",       "boolean",    "break",
        "byte",       "case",         "catch",       "char",
        "class",      "const",        "continue",    "default",
        "do",         "double",       "else",        "enum",
        "extends",    "final",        "finally",     "float",
        "for",        "goto",         "if",          "implements",
        "import",     "instanceof",   "int",         "interface",
        "long",       "native",       "new",         "package",
        "private",    "protected",    "public",      "return",
        "short",      "static",       "strictfp",    "super",
        "switch",     "synchronized", "this",        "throw",
        "throws",     "transient",    "try",         "void",
        "volatile",   "while",        "true",        "false",
        "null",       "record",       "sealed",      "permits",
        "var",        "yield"
    ) );




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    private JavaPkgValidator() {}




    // =========================================================
    // PUBLIC
    // =========================================================

    // Validates the given packageName against the existing registry entries.
    // existingEntries may be null or empty (treated as empty).
    public static Result validate (String packageName, ArrayList<JavaPkgEntry> existingEntries) {

        // ── Rule 1: Not null/empty/blank ──────────────────────
        if (packageName == null || packageName.trim().isEmpty()) {
            return Result.fail ("Package name must not be empty.");
        }

        String name = packageName.trim();

        // ── Rule 2: Max length ────────────────────────────────
        if (name.length() > MAX_LENGTH) {
            return Result.fail (
                "Package name is too long. Max " + MAX_LENGTH + " characters."
            );
        }

        // ── Rule 3: No leading/trailing dots ─────────────────
        if (name.startsWith (".") || name.endsWith (".")) {
            return Result.fail ("Package name must not start or end with a dot.");
        }

        // ── Rule 4: No consecutive dots ───────────────────────
        if (name.contains ("..")) {
            return Result.fail ("Package name must not contain consecutive dots.");
        }

        // ── Rule 5 & 6: Segments ─────────────────────────────
        String[] segments = name.split ("\\.", -1);

        if (segments.length < MIN_SEGMENTS) {
            return Result.fail (
                "Package name must have at least "
                + MIN_SEGMENTS + " segments (e.g. \"com.ui\")."
            );
        }

        for (String segment : segments) {
            Result segResult = validateSegment (segment);
            if ( ! segResult.isValid() ) return segResult;
        }

        // ── Rule 7: No duplicates ─────────────────────────────
        if (existingEntries != null) {
            for (JavaPkgEntry existing : existingEntries) {
                if ( existing.getPackageName().equals (name) ) {
                    return Result.fail (
                        "Package '" + name + "' already exists in this project."
                    );
                }
            }
        }

        return Result.ok();
    }




    // =========================================================
    // PRIVATE — Segment Validation
    // =========================================================

    private static Result validateSegment (String segment) {

        if (segment.isEmpty()) {
            return Result.fail ("Package segments must not be empty.");
        }

        // Must start with letter or underscore (not digit)
        char first = segment.charAt (0);
        if ( ! Character.isLetter (first) && first != '_' ) {
            return Result.fail (
                "Segment '" + segment + "' must start with a letter or underscore."
            );
        }

        // All chars: letters, digits, underscores only
        for (char c : segment.toCharArray()) {
            if ( ! Character.isLetterOrDigit (c) && c != '_' ) {
                return Result.fail (
                    "Segment '" + segment + "' contains invalid character: '" + c + "'."
                );
            }
        }

        // Not a reserved word
        if ( RESERVED.contains (segment.toLowerCase()) ) {
            return Result.fail (
                "'" + segment + "' is a Java reserved word and cannot be used as a package segment."
            );
        }

        return Result.ok();
    }




    // =========================================================
    // RESULT — Typed return value
    // =========================================================

    public static final class Result {

        private final boolean valid;
        private final String reason; // null if valid

        private Result (boolean valid, String reason) {
            this.valid  = valid;
            this.reason = reason;
        }

        public static Result ok() {
            return new Result (true, null);
        }

        public static Result fail (String reason) {
            return new Result (false, reason);
        }

        public boolean isValid() { return valid;  }
        public String getReason() { return reason; }

    }




}
