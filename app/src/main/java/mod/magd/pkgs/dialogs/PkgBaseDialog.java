package mod.magd.pkgs.dialogs;

import pro.sketchware.R;

import android.content.Context;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;

// =========================================================
// PkgBaseDialog
// =========================================================

// Abstract base class for package-related dialogs.
// Provides common functionality:
//   - Search/filter EditText for live filtering
//   - Shared ListView layout
//   - Bottom-anchored dialog positioning
//   - DP-to-pixel conversion utilities

// PURPOSE:
//   Create a consistent UX for package picking and deletion.
//   Each subclass (PkgPickerDialog, PkgDeleteDialog) focuses on
//   its specific action while inheriting search + layout logic.

// USAGE:
//   Extend this class and override:
//     - getTitle() → dialog title
//     - createAdapter() → returns a BaseAdapter for the list
//     - onItemClicked() → handles item click (if applicable)

// =========================================================

public abstract class PkgBaseDialog {

    // =========================================================
    // PROTECTED VARIABLES
    // =========================================================

    /** Application context. */
    protected final Context context;

    /** Package registry — source of truth for all packages. */
    protected final PkgRegistry registry;

    /** Currently active package (may be highlighted in the list). */
    protected final PkgEntry activeEntry;

    /** The Dialog instance. Managed by show()/dismiss(). */
    protected Dialog dialog;

    /** Search EditText for filtering. */
    protected EditText searchEditText;

    /** ListView for displaying packages. */
    protected ListView listView;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Initialize the base dialog with required parameters.
     *
     * @param context Application context
     * @param registry Package registry
     * @param activeEntry Currently active package (may be null)
     */
    public PkgBaseDialog(
        Context context,
        PkgRegistry registry,
        PkgEntry activeEntry
    ) {
        if (context == null)
            throw new IllegalArgumentException("PkgBaseDialog: context must not be null.");
        if (registry == null)
            throw new IllegalArgumentException("PkgBaseDialog: registry must not be null.");

        this.context = context;
        this.registry = registry;
        this.activeEntry = activeEntry;
    }

    // =========================================================
    // PUBLIC
    // =========================================================

    /**
     * Display the dialog on screen.
     * Sets up window params, content view, and positioning.
     */
    public void show() {
        // Create the Material-styled dialog
        dialog = new Dialog(context, android.R.style.Theme_Material_Light_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(buildContentView());

        // Position at bottom of screen
        positionAtBottom(dialog);

        dialog.show();
    }

    /**
     * Hide the dialog if it's currently shown.
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    // =========================================================
    // ABSTRACT METHODS — Subclasses must override
    // =========================================================

    /**
     * @return The title text for this dialog.
     * Examples: "Choose Package", "Delete Packages"
     */
    protected abstract String getTitle();

    /**
     * Create and return the adapter for the ListView.
     * Called once during view construction.
     *
     * @param entries The filtered list of packages
     * @return A BaseAdapter for displaying packages
     */
    protected abstract android.widget.BaseAdapter createAdapter(ArrayList<PkgEntry> entries);

    /**
     * Called when a user clicks on an item in the list.
     * Subclasses implement their specific action (pick, select, etc).
     *
     * @param entry The clicked package entry
     */
    protected abstract void onItemClicked(PkgEntry entry);

    // =========================================================
    // PROTECTED — View Construction
    // =========================================================

    /**
     * Build the complete dialog content view.
     * Layout:
     *   [Title]
     *   [Divider]
     *   [Search EditText]
     *   [Divider]
     *   [ListView with packages]
     *
     * @return The root container for the dialog
     */
    protected View buildContentView() {
        // Root vertical container
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 32, 0, 32);
        root.setBackgroundColor(0xFFFFFFFF); // White background

        // ═════════════════════════════════════════════════════════════════════
        // Title
        // ═════════════════════════════════════════════════════════════════════
        TextView title = new TextView(context);
        title.setText(getTitle());
        title.setTextSize(16f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(dpToPx(48), dpToPx(24), dpToPx(48), dpToPx(24));
        title.setTextColor(0xFF212121); // Dark gray
        root.addView(title);

        // ═════════════════════════════════════════════════════════════════════
        // Divider 1
        // ═════════════════════════════════════════════════════════════════════
        root.addView(buildDivider());

        // ═════════════════════════════════════════════════════════════════════
        // Search EditText
        // ═════════════════════════════════════════════════════════════════════
        searchEditText = new EditText(context);
        searchEditText.setHint("Search packages...");
        searchEditText.setHintTextColor(0xFFBDBDBD); // Light gray
        searchEditText.setTextColor(0xFF212121);
        searchEditText.setBackgroundColor(0xFFFAFAFA); // Very light gray
        searchEditText.setPadding(dpToPx(48), dpToPx(12), dpToPx(48), dpToPx(12));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        searchEditText.setLayoutParams(searchParams);
        root.addView(searchEditText);

        // ═════════════════════════════════════════════════════════════════════
        // Divider 2
        // ═════════════════════════════════════════════════════════════════════
        root.addView(buildDivider());

        // ═════════════════════════════════════════════════════════════════════
        // Package ListView
        // ═════════════════════════════════════════════════════════════════════
        listView = new ListView(context);
        listView.setDivider(null); // No divider between items

        // Set the initial adapter (all packages, not filtered yet)
        ArrayList<PkgEntry> allPackages = registry.getAll();
        listView.setAdapter(createAdapter(allPackages));

        // Set up search filtering
        setupSearchFiltering(allPackages);

        // Set up item click listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            PkgEntry clicked = (PkgEntry) parent.getItemAtPosition(position);
            onItemClicked(clicked);
        });

        // Set list height (max 360dp or actual size, whichever is smaller)
        int listHeight = Math.min(
            registry.getAll().size() * dpToPx(72),
            dpToPx(360)
        );
        listView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            listHeight
        ));
        root.addView(listView);

        return root;
    }

    /**
     * Build a thin horizontal divider line.
     * Color: Light gray (0xFFE0E0E0)
     * Height: 1px
     *
     * @return A View divider
     */
    protected View buildDivider() {
        View divider = new View(context);
        divider.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            1 // 1 pixel height
        ));
        divider.setBackgroundColor(0xFFE0E0E0); // Light gray
        return divider;
    }

    /**
     * Position the dialog at the bottom of the screen.
     * Width: MATCH_PARENT
     * Height: WRAP_CONTENT
     *
     * @param d The dialog to position
     */
    protected void positionAtBottom(Dialog d) {
        Window window = d.getWindow();
        if (window == null) return;

        window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        );

        WindowManager.LayoutParams params = window.getAttributes();
        params.y = 0; // Flush to bottom
        window.setAttributes(params);
    }

    // =========================================================
    // PROTECTED — Search & Filtering
    // =========================================================

    /**
     * Set up live search filtering on the ListView adapter.
     * As the user types in searchEditText, the adapter filters
     * packages by display name and package name (case-insensitive).
     *
     * @param allPackages The complete list of packages to filter from
     */
    protected void setupSearchFiltering(ArrayList<PkgEntry> allPackages) {
        searchEditText.addTextChangedListener(
            new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Get search query
                    String query = s.toString().toLowerCase().trim();

                    // Filter packages based on search query
                    ArrayList<PkgEntry> filtered = new ArrayList<>();
                    for (PkgEntry entry : allPackages) {
                        boolean matchesDisplay = entry.getDisplayName()
                            .toLowerCase().contains(query);
                        boolean matchesPackage = entry.getPackageName()
                            .toLowerCase().contains(query);

                        // Include if either display name or package name matches
                        if (query.isEmpty() || matchesDisplay || matchesPackage) {
                            filtered.add(entry);
                        }
                    }

                    // Update adapter with filtered results
                    listView.setAdapter(createAdapter(filtered));
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            }
        );
    }

    // =========================================================
    // PROTECTED — Utilities
    // =========================================================

    /**
     * Convert density-independent pixels (dp) to physical pixels (px).
     * Uses the device's current display density.
     *
     * @param dp Size in density-independent pixels
     * @return Size in physical pixels
     */
    protected int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
