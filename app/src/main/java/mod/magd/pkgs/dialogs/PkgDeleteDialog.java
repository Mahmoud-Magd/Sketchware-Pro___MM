package mod.magd.pkgs.dialogs;

import pro.sketchware.utility.SketchwareUtil;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;

// =========================================================
// PkgDeleteDialog
// =========================================================

// Dialog for selecting and deleting one or more packages.
// Extends PkgBaseDialog to inherit search, layout, and positioning.

// PURPOSE:
//   Allows user to:
//     - View all packages (except main) with search/filter
//     - Toggle selection by clicking each package item
//     - See which packages are selected (checkboxes)
//     - Click "Delete" to confirm deletion
//     - Confirm irreversible deletion with warning dialog

// FEATURES:
//   - Live search by display name or package name
//   - Multi-select with checkbox UI
//   - Click item to toggle selection
//   - Main package is grayed out (cannot be deleted)
//   - Cancel/Delete buttons at the bottom
//   - Warning dialog before permanent deletion
//   - Callback when deletion is complete

// USAGE:
//   new PkgDeleteDialog(
//       this,
//       pkgRegistry,
//       currentActivePackage,
//       () -> {
//           // Handle post-deletion (refresh UI, etc)
//           refreshPackageList();
//       }
//   ).show();

// =========================================================

public final class PkgDeleteDialog extends PkgBaseDialog {

    // =========================================================
    // INTERFACES
    // =========================================================

    /**
     * Callback when packages are successfully deleted.
     */
    public interface OnPackagesDeletedListener {
        /**
         * Called after the selected packages have been deleted.
         */
        void onPackagesDeleted();
    }

    // =========================================================
    // VARIABLES
    // =========================================================

    /** Callback listener for deletion completion. */
    private final OnPackagesDeletedListener listener;

    /** Set of package IDs currently selected for deletion. */
    private final Set<String> selectedPackageIds = new HashSet<>();

    /** The adapter for the deletion list (needs to be updated when selection changes). */
    private PackageDeleteAdapter currentAdapter;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Create a new package delete dialog.
     *
     * @param context Application context
     * @param registry Package registry
     * @param activeEntry Currently active package (may be null)
     * @param listener Callback when packages are deleted
     */
    public PkgDeleteDialog(
        Context context,
        PkgRegistry registry,
        PkgEntry activeEntry,
        OnPackagesDeletedListener listener
    ) {
        super(context, registry, activeEntry);
        if (listener == null)
            throw new IllegalArgumentException(
                "PkgDeleteDialog: listener must not be null."
            );
        this.listener = listener;
    }

    // =========================================================
    // ABSTRACT IMPLEMENTATIONS
    // =========================================================

    @Override
    protected String getTitle() {
        return "Delete Packages";
    }

    /**
     * Create the adapter for displaying packages in the delete dialog.
     * Each item shows:
     *   - Checkbox (checked if selected, grayed if main package)
     *   - Display name and package name
     *   - Delete icon indicator
     *
     * @param entries Filtered list of packages
     * @return A PackageDeleteAdapter
     */
    @Override
    protected BaseAdapter createAdapter(ArrayList<PkgEntry> entries) {
        currentAdapter = new PackageDeleteAdapter(entries);
        return currentAdapter;
    }

    /**
     * Handle item click: toggle selection of the package.
     * Main package cannot be selected for deletion.
     *
     * @param entry The clicked package
     */
    @Override
    protected void onItemClicked(PkgEntry entry) {
        // Don't allow selecting the main package for deletion
        if (entry.isMain()) {
            return;
        }

        // Toggle selection
        if (selectedPackageIds.contains(entry.getId())) {
            selectedPackageIds.remove(entry.getId());
        } else {
            selectedPackageIds.add(entry.getId());
        }

        // Update the adapter UI to reflect the change
        if (currentAdapter != null) {
            currentAdapter.notifyDataSetChanged();
        }
    }

    // =========================================================
    // OVERRIDE — Content View with Buttons
    // =========================================================

    /**
     * Override buildContentView to add Cancel/Delete buttons at the bottom.
     *
     * @return The complete content view with buttons
     */
    @Override
    protected View buildContentView() {
        // Root vertical container
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF); // White background

        // ════════════════════════════════════════════════════════════════���════
        // Title
        // ═════════════════════════════════════════════════════════════════════
        TextView title = new TextView(context);
        title.setText(getTitle());
        title.setTextSize(16f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(dpToPx(48), dpToPx(24), dpToPx(48), dpToPx(24));
        title.setTextColor(0xFF212121);
        root.addView(title);

        // ═════════════════════════════════════════════════════════════════════
        // Divider
        // ═════════════════════════════════════════════════════════════════════
        root.addView(buildDivider());

        // ═════════════════════════════════════════════════════════════════════
        // Search EditText
        // ═════════════════════════════════════════════════════════════════════
        searchEditText = new android.widget.EditText(context);
        searchEditText.setHint("Search packages...");
        searchEditText.setHintTextColor(0xFFBDBDBD);
        searchEditText.setTextColor(0xFF212121);
        searchEditText.setBackgroundColor(0xFFFAFAFA);
        searchEditText.setPadding(dpToPx(48), dpToPx(12), dpToPx(48), dpToPx(12));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        searchEditText.setLayoutParams(searchParams);
        root.addView(searchEditText);

        // ═════════════════════════════════════════════════════════════════════
        // Divider
        // ═════════════════════════════════════════════════════════════════════
        root.addView(buildDivider());

        // ═════════════════════════════════════════════════════════════════════
        // Package ListView
        // ═════════════════════════════════════════════════════════════════════
        listView = new android.widget.ListView(context);
        listView.setDivider(null);

        ArrayList<PkgEntry> allPackages = registry.getAll();
        listView.setAdapter(createAdapter(allPackages));
        setupSearchFiltering(allPackages);

        // Item click toggles selection
        listView.setOnItemClickListener((parent, view, position, id) -> {
            PkgEntry clicked = (PkgEntry) parent.getItemAtPosition(position);
            onItemClicked(clicked);
        });

        int listHeight = Math.min(
            registry.getAll().size() * dpToPx(72),
            dpToPx(360)
        );
        listView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            listHeight
        ));
        root.addView(listView);

        // ═════════════════════════════════════════════════════════════════════
        // Divider before buttons
        // ═════════════════════════════════════════════════════════════════════
        root.addView(buildDivider());

        // ═════════════════════════════════════════════════════════════════════
        // Button Layout (Cancel | Delete)
        // ═════════════════════════════════════════════════════════════════════
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
        buttonLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Cancel button
        Button btnCancel = new Button(context);
        btnCancel.setText("Cancel");
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        );
        btnCancel.setLayoutParams(cancelParams);
        btnCancel.setOnClickListener(v -> dismiss());
        buttonLayout.addView(btnCancel);

        // Spacer between buttons
        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(8), 0));
        buttonLayout.addView(spacer);

        // Delete button
        Button btnDelete = new Button(context);
        btnDelete.setText("Delete");
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        );
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setOnClickListener(v -> showConfirmationAndDelete());
        buttonLayout.addView(btnDelete);

        root.addView(buttonLayout);
        root.setPadding(0, dpToPx(32), 0, dpToPx(32));

        return root;
    }

    // =========================================================
    // PRIVATE — Deletion Logic
    // =========================================================

    /**
     * Show a warning dialog confirming the user wants to permanently delete
     * the selected packages. If confirmed, performs the deletion.
     */
    private void showConfirmationAndDelete() {
        // If no packages selected, show a message
        if (selectedPackageIds.isEmpty()) {
            SketchwareUtil.toastLong("No packages selected for deletion");
            return;
        }

        // Count selected packages
        int count = selectedPackageIds.size();
        String message = count == 1
            ? "Delete 1 package? This process is irreversible."
            : "Delete " + count + " packages? This process is irreversible.";

        // Show confirmation dialog
        new MaterialAlertDialogBuilder(context)
            .setTitle("Delete packages?")
            .setMessage(message)
            .setPositiveButton("Delete", (dialog, which) -> {
                performDeletion();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Perform the actual deletion of selected packages from the registry.
     * Calls the listener callback when complete.
     */
    private void performDeletion() {
        try {
            // Delete each selected package
            for (String packageId : selectedPackageIds) {
                registry.removePackage(packageId);
            }

            // Close this dialog
            dismiss();

            // Notify listener that deletion is complete
            listener.onPackagesDeleted();
        } catch (Exception e) {
            SketchwareUtil.toastError("Error deleting package: " + e.getMessage());
        }
    }

    // =========================================================
    // INNER — Adapter for Delete List
    // =========================================================

    /**
     * Custom adapter for displaying packages in the delete dialog.
     * Each item has a checkbox that can be toggled to select for deletion.
     * The main package cannot be selected (grayed out).
     */
    private final class PackageDeleteAdapter extends BaseAdapter {

        /** The packages to display (may be filtered). */
        private final ArrayList<PkgEntry> entries;

        PackageDeleteAdapter(ArrayList<PkgEntry> entries) {
            this.entries = entries;
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int pos) {
            return entries.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Reuse the view if available
            if (convertView == null) {
                convertView = buildItemView();
            }

            PkgEntry entry = entries.get(position);
            boolean isSelected = selectedPackageIds.contains(entry.getId());
            boolean isMainPackage = entry.isMain();

            // Get the views
            CheckBox checkbox = convertView.findViewById(android.R.id.checkbox);
            TextView tvDisplay = convertView.findViewById(android.R.id.text1);
            TextView tvPkg = convertView.findViewById(android.R.id.text2);
            ImageView ivDelete = (ImageView) convertView.getTag();

            // Set the text
            tvDisplay.setText(entry.getDisplayName());
            tvPkg.setText(entry.getPackageName());

            // Update checkbox state
            checkbox.setChecked(isSelected);
            checkbox.setEnabled(!isMainPackage); // Disable if main package

            // Gray out main package
            if (isMainPackage) {
                tvDisplay.setTextColor(0xFFBDBDBD); // Light gray
                tvPkg.setTextColor(0xFFE0E0E0);
            } else {
                tvDisplay.setTextColor(0xFF212121); // Dark gray
                tvPkg.setTextColor(0xFF757575); // Medium gray
            }

            // Show delete icon if selected
            ivDelete.setVisibility(isSelected && !isMainPackage ? View.VISIBLE : View.INVISIBLE);

            return convertView;
        }

        /**
         * Build the view for a single deletable package item.
         * Layout:
         *   [Checkbox] [Display Name] [Delete Icon]
         *            [Package Name]
         *
         * @return The item view
         */
        private View buildItemView() {
            // Root horizontal container
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dpToPx(48), dpToPx(20), dpToPx(48), dpToPx(20));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // ═════════════════════════════════════════════════════════════════════
            // Checkbox (left side)
            // ═════════════════════════════════════════════════════════════════════
            CheckBox checkbox = new CheckBox(context);
            checkbox.setId(android.R.id.checkbox);
            LinearLayout.LayoutParams checkboxParams = new LinearLayout.LayoutParams(
                dpToPx(24),
                dpToPx(24)
            );
            checkboxParams.setMarginEnd(dpToPx(16));
            checkbox.setLayoutParams(checkboxParams);
            checkbox.setClickable(false); // Handle click via parent list item
            row.addView(checkbox);

            // ═════════════════════════════════════════════════════════════════════
            // Text block (display name + package name)
            // ═════════════════════════════════════════════════════════════════════
            LinearLayout textBlock = new LinearLayout(context);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            );
            textBlock.setLayoutParams(textParams);

            // Display name
            TextView tvDisplay = new TextView(context);
            tvDisplay.setId(android.R.id.text1);
            tvDisplay.setTextSize(14f);
            tvDisplay.setTextColor(0xFF212121);
            textBlock.addView(tvDisplay);

            // Package name
            TextView tvPkg = new TextView(context);
            tvPkg.setId(android.R.id.text2);
            tvPkg.setTextSize(12f);
            tvPkg.setTextColor(0xFF757575);
            textBlock.addView(tvPkg);

            row.addView(textBlock);

            // ═════════════════════════════════════════════════════════════════════
            // Delete Icon (right side, shown when selected)
            // ═════════════════════════════════════════════════════════════════════
            ImageView ivDelete = new ImageView(context);
            ivDelete.setImageResource(android.R.drawable.ic_menu_delete);
            ivDelete.setVisibility(View.INVISIBLE);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(24),
                dpToPx(24)
            );
            iconParams.setMarginStart(dpToPx(16));
            ivDelete.setLayoutParams(iconParams);

            row.setTag(ivDelete);
            row.addView(ivDelete);

            return row;
        }
    }
}
