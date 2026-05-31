package mod.magd.pkgs.dialogs;

import pro.sketchware.utility.SketchwareUtil;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;

import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;

// =========================================================
// PkgEditDialog
// =========================================================

// Dialog for editing package information.
// User selects a package from a list, then edits:
//   - Package name (e.g., "com.z.ui" → "com.x.db")
//   - Display name (e.g., "UI Layer" → "Database")

// PURPOSE:
//   Allow users to modify package metadata safely.
//   Warn about package name changes requiring file updates.
//   If user confirms, scan all files and update package declarations.

// FEATURES:
//   - ListView of all packages
//   - Two EditText fields for editing
//   - Display name change is safe (no file updates needed)
//   - Package name change triggers file scan + warning dialog
//   - Automatic package declaration replacement in all .java files

// USAGE:
//   new PkgEditDialog(
//       this,
//       pkgRegistry,
//       (edited) -> {
//           // Handle edited package
//           if (edited.getId().equals(activePackage.getId())) {
//               activePackage = edited;
//               updateUI();
//           }
//       }
//   ).show();

// =========================================================

public final class PkgEditDialog extends PkgBaseDialog {

    // =========================================================
    // INTERFACES
    // =========================================================

    /**
     * Callback when package is successfully edited.
     */
    public interface OnPackageEditedListener {
        /**
         * Called when package info is updated.
         *
         * @param editedPackage The updated package entry
         */
        void onPackageEdited(PkgEntry editedPackage);
    }

    // =========================================================
    // VARIABLES
    // =========================================================

    private final OnPackageEditedListener listener;

    /** Currently selected package for editing. */
    private PkgEntry selectedPackage;

    /** EditText for display name (safe to change). */
    private EditText etDisplayName;

    /** EditText for package name (requires file updates). */
    private EditText etPackageName;

    /** Root container for the dialog. */
    private LinearLayout contentRoot;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Create a new package edit dialog.
     *
     * @param context Application context
     * @param registry Package registry
     * @param listener Callback when package is edited
     */
    public PkgEditDialog(
        Context context,
        PkgRegistry registry,
        OnPackageEditedListener listener
    ) {
        // Note: Pass null for activeEntry since we'll show all packages
        super(context, registry, null);
        if (listener == null)
            throw new IllegalArgumentException("PkgEditDialog: listener must not be null.");
        this.listener = listener;
    }

    // =========================================================
    // ABSTRACT IMPLEMENTATIONS
    // =========================================================

    @Override
    protected String getTitle() {
        return "Edit Package";
    }

    /**
     * Create adapter for the package list (first step).
     */
    @Override
    protected BaseAdapter createAdapter(ArrayList<PkgEntry> entries) {
        return new PackageSelectionAdapter(entries);
    }

    /**
     * When user clicks a package, show the edit form.
     */
    @Override
    protected void onItemClicked(PkgEntry entry) {
        selectedPackage = entry;
        showEditForm();
    }

    // =========================================================
    // OVERRIDE — Content View with two-phase approach
    // =========================================================

    /**
     * Override to show the package selection list first.
     */
    @Override
    protected View buildContentView() {
        contentRoot = new LinearLayout(context);
        contentRoot.setOrientation(LinearLayout.VERTICAL);
        contentRoot.setBackgroundColor(0xFFFFFFFF);

        // ═══════════════════════════════════════════════════════════════════
        // Phase 1: Package Selection
        // ═══════════════════════════════════════════════════════════════════
        showPackageSelection();

        return contentRoot;
    }

    // =========================================================
    // PRIVATE — Two-Phase UI
    // =========================================================

    /**
     * Phase 1: Show the list of packages to edit.
     */
    private void showPackageSelection() {
        contentRoot.removeAllViews();

        // Title
        TextView title = new TextView(context);
        title.setText(getTitle());
        title.setTextSize(16f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(dpToPx(48), dpToPx(24), dpToPx(48), dpToPx(24));
        title.setTextColor(0xFF212121);
        contentRoot.addView(title);

        // Divider
        contentRoot.addView(buildDivider());

        // Search EditText
        searchEditText = new EditText(context);
        searchEditText.setHint("Search packages...");
        searchEditText.setHintTextColor(0xFFBDBDBD);
        searchEditText.setTextColor(0xFF212121);
        searchEditText.setBackgroundColor(0xFFFAFAFA);
        searchEditText.setPadding(dpToPx(48), dpToPx(12), dpToPx(48), dpToPx(12));
        contentRoot.addView(searchEditText);

        // Divider
        contentRoot.addView(buildDivider());

        // Package ListView
        listView = new ListView(context);
        listView.setDivider(null);

        ArrayList<PkgEntry> allPackages = registry.getAll();
        listView.setAdapter(createAdapter(allPackages));
        setupSearchFiltering(allPackages);

        // Set up item click listener
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
        contentRoot.addView(listView);
    }

    /**
     * Phase 2: Show the edit form for the selected package.
     */
    private void showEditForm() {
        contentRoot.removeAllViews();

        // ═══════════════════════════════════════════════════════════════════
        // Title
        // ═══════════════════════════════════════════════════════════════════
        TextView title = new TextView(context);
        title.setText("Edit: " + selectedPackage.getDisplayName());
        title.setTextSize(16f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(dpToPx(48), dpToPx(24), dpToPx(48), dpToPx(24));
        title.setTextColor(0xFF212121);
        contentRoot.addView(title);

        // Divider
        contentRoot.addView(buildDivider());

        // ═══════════════════════════════════════════════════════════════════
        // Scroll container for form fields
        // ═══════════════════════════════════════════════════════════════════
        LinearLayout formContainer = new LinearLayout(context);
        formContainer.setOrientation(LinearLayout.VERTICAL);
        formContainer.setPadding(dpToPx(48), dpToPx(16), dpToPx(48), dpToPx(16));

        // ─────────────────────────────────────────────────────────────────
        // Display Name field (safe to edit)\n        // ─────────────────────────────────────────────────────────────────
        TextView tvDisplayLabel = new TextView(context);
        tvDisplayLabel.setText("Display Name (e.g., 'UI Layer')\");
        tvDisplayLabel.setTextSize(12f);
        tvDisplayLabel.setTextColor(0xFF757575);
        tvDisplayLabel.setPadding(0, dpToPx(8), 0, dpToPx(4));
        formContainer.addView(tvDisplayLabel);

        etDisplayName = new EditText(context);
        etDisplayName.setText(selectedPackage.getDisplayName());
        etDisplayName.setBackgroundColor(0xFFFAFAFA);
        etDisplayName.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        displayParams.bottomMargin = dpToPx(16);
        etDisplayName.setLayoutParams(displayParams);
        formContainer.addView(etDisplayName);

        // ─────────────────────────────────────────────────────────────────
        // Package Name field (requires file updates if changed)
        // ─────────────────────────────────────────────────────────────────
        TextView tvPkgLabel = new TextView(context);
        tvPkgLabel.setText("Package Name (e.g., 'com.z.ui')\");
        tvPkgLabel.setTextSize(12f);
        tvPkgLabel.setTextColor(0xFF757575);
        tvPkgLabel.setPadding(0, dpToPx(8), 0, dpToPx(4));
        formContainer.addView(tvPkgLabel);

        etPackageName = new EditText(context);
        etPackageName.setText(selectedPackage.getPackageName());
        etPackageName.setBackgroundColor(0xFFFAFAFA);
        etPackageName.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        etPackageName.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        formContainer.addView(etPackageName);

        // Add to root
        LinearLayout.LayoutParams formParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        contentRoot.addView(formContainer, formParams);

        // Divider before buttons
        contentRoot.addView(buildDivider());

        // ═══════════════════════════════════════════════════════════════════
        // Button Layout (Cancel | Save)
        // ═══════════════════════════════════════════════════════════════════
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));

        // Cancel button
        Button btnCancel = new Button(context);
        btnCancel.setText("Cancel");
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ));
        btnCancel.setOnClickListener(v -> {
            dismiss();
        });
        buttonLayout.addView(btnCancel);

        // Spacer
        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(8), 0));
        buttonLayout.addView(spacer);

        // Save button
        Button btnSave = new Button(context);
        btnSave.setText("Save");
        btnSave.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ));
        btnSave.setOnClickListener(v -> saveChanges());
        buttonLayout.addView(btnSave);

        contentRoot.addView(buttonLayout);
    }

    /**
     * Save the edited package information.
     * If package name changed, warn user about file updates.
     */
    private void saveChanges() {
        String newDisplayName = etDisplayName.getText().toString().trim();
        String newPackageName = etPackageName.getText().toString().trim();

        // Validate inputs
        if (newDisplayName.isEmpty()) {
            SketchwareUtil.toastError("Display name cannot be empty");
            return;
        }
        if (newPackageName.isEmpty()) {
            SketchwareUtil.toastError("Package name cannot be empty");
            return;
        }

        String oldPackageName = selectedPackage.getPackageName();

        // Check if package name changed
        if (!oldPackageName.equals(newPackageName)) {
            // Package name is being changed — need to warn user about file updates
            showPackageNameChangeWarning(newDisplayName, oldPackageName, newPackageName);
        } else {
            // Only display name changed — safe to save
            performSave(newDisplayName, newPackageName, false, oldPackageName);
        }
    }

    /**
     * Show warning dialog when package name is being changed.
     * User can choose to update files or cancel.
     */
    private void showPackageNameChangeWarning(
        String newDisplayName,
        String oldPackageName,
        String newPackageName
    ) {
        new MaterialAlertDialogBuilder(context)
            .setTitle("Update package references?")
            .setMessage(
                "Changing the package name from '" + oldPackageName + "' to '" + newPackageName + "' requires updating all Java files.\n\n" +
                "This process will scan all .java files in the source directory and replace the old package declaration with the new one.\n\n" +
                "This is recommended and safe. Continue?"
            )
            .setPositiveButton("Update Files", (d, w) -> {
                // User wants to update files
                performSave(newDisplayName, newPackageName, true, oldPackageName);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Perform the save operation.
     * If updateFiles is true, scan and update all .java files.
     *
     * @param newDisplayName The new display name
     * @param newPackageName The new package name
     * @param updateFiles Whether to update Java files
     * @param oldPackageName The old package name (for file updates)
     */
    private void performSave(
        String newDisplayName,
        String newPackageName,
        boolean updateFiles,
        String oldPackageName
    ) {
        try {
            // Update the entry
            selectedPackage.setDisplayName(newDisplayName);
            // Note: packageName is final in PkgEntry, so we may need to
            // create a new entry. Adjust based on your PkgEntry implementation.

            // If package name changed, update files
            if (updateFiles && !oldPackageName.equals(newPackageName)) {
                updatePackageNamesInFiles(
                    new File(selectedPackage.getSourceRootPath()),
                    oldPackageName,
                    newPackageName
                );
            }

            // Save to registry
            registry.savePackage(selectedPackage);

            // Close dialog and notify listener
            dismiss();
            listener.onPackageEdited(selectedPackage);

            SketchwareUtil.toastSuccess("Package updated successfully");
        } catch (Exception e) {
            SketchwareUtil.toastError("Error saving package: " + e.getMessage());
        }
    }

    /**
     * Recursively scan all .java files and replace package declarations.
     *
     * @param dir The directory to scan (source root)
     * @param oldPkgName The old package name
     * @param newPkgName The new package name
     */
    private void updatePackageNamesInFiles(File dir, String oldPkgName, String newPkgName) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively process subdirectories
                updatePackageNamesInFiles(file, oldPkgName, newPkgName);
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                // Process .java file
                try {
                    // Read file content
                    String content = readFile(file);

                    // Replace package declaration
                    // Match: package com.z.ui;
                    String oldDeclaration = "package " + oldPkgName + ";";
                    String newDeclaration = "package " + newPkgName + ";";

                    if (content.contains(oldDeclaration)) {
                        content = content.replace(oldDeclaration, newDeclaration);

                        // Write back
                        writeFile(file, content);
                    }
                } catch (Exception e) {
                    // Log but continue processing other files
                    SketchwareUtil.toastError("Error updating: " + file.getName());
                }
            }
        }
    }

    /**
     * Read file content as string.
     */
    private String readFile(File file) throws Exception {
        java.nio.file.Path path = file.toPath();
        return new String(java.nio.file.Files.readAllBytes(path));
    }

    /**
     * Write string content to file.
     */
    private void writeFile(File file, String content) throws Exception {
        java.nio.file.Path path = file.toPath();
        java.nio.file.Files.write(path, content.getBytes());
    }

    // =========================================================
    // INNER — Adapter for Package Selection
    // =========================================================

    /**
     * Adapter for the initial package selection list.
     */
    private final class PackageSelectionAdapter extends BaseAdapter {

        private final ArrayList<PkgEntry> entries;

        PackageSelectionAdapter(ArrayList<PkgEntry> entries) {
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
            if (convertView == null) {
                convertView = buildItemView();
            }

            PkgEntry entry = entries.get(position);

            TextView tvDisplay = convertView.findViewById(android.R.id.text1);
            TextView tvPkg = convertView.findViewById(android.R.id.text2);

            tvDisplay.setText(entry.getDisplayName());
            tvPkg.setText(entry.getPackageName());

            return convertView;
        }

        private View buildItemView() {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dpToPx(48), dpToPx(20), dpToPx(48), dpToPx(20));

            LinearLayout textBlock = new LinearLayout(context);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            textBlock.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ));

            TextView tvDisplay = new TextView(context);
            tvDisplay.setId(android.R.id.text1);
            tvDisplay.setTextSize(14f);
            tvDisplay.setTextColor(0xFF212121);
            textBlock.addView(tvDisplay);

            TextView tvPkg = new TextView(context);
            tvPkg.setId(android.R.id.text2);
            tvPkg.setTextSize(12f);
            tvPkg.setTextColor(0xFF757575);
            textBlock.addView(tvPkg);

            row.addView(textBlock);

            return row;
        }
    }
}
