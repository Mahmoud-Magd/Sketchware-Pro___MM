package mod.magd.pkgs.dialogs;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;

// =========================================================
// PkgPickerDialog
// =========================================================

// Simple dialog for selecting a single package from the registry.
// Extends PkgBaseDialog to inherit search, layout, and positioning.

// PURPOSE:
//   Allows user to:
//     - View all packages with search/filter
//     - See which package is currently active (checkmark icon)
//     - Click a package to select it
//     - Get a callback when selection is made

// FEATURES:
//   - Live search by display name or package name
//   - Active package highlighted with checkmark + blue text
//   - Shows package display name + full package name
//   - Single responsibility: picking only (no delete logic)

// USAGE:
//   new PkgPickerDialog(
//       this,
//       pkgRegistry,
//       currentActivePackage,
//       picked -> {
//           // Handle picked package
//           switchToPackage(picked);
//       }
//   ).show();

// =========================================================

public final class PkgPickerDialog extends PkgBaseDialog {

    // =========================================================
    // INTERFACES
    // =========================================================

    /**
     * Callback when a package is successfully picked.
     */
    public interface OnPackagePickedListener {
        /**
         * Called when user selects a package from the picker.
         *
         * @param picked The selected package entry
         */
        void onPackagePicked(PkgEntry picked);
    }

    // =========================================================
    // VARIABLES
    // =========================================================

    /** Callback listener for package selection. */
    private final OnPackagePickedListener listener;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Create a new package picker dialog.
     *
     * @param context Application context
     * @param registry Package registry
     * @param activeEntry Currently active package (may be null)
     * @param listener Callback when a package is picked
     */
    public PkgPickerDialog(
        Context context,
        PkgRegistry registry,
        PkgEntry activeEntry,
        OnPackagePickedListener listener
    ) {
        super(context, registry, activeEntry);
        if (listener == null)
            throw new IllegalArgumentException(
                "PkgPickerDialog: listener must not be null."
            );
        this.listener = listener;
    }

    // =========================================================
    // ABSTRACT IMPLEMENTATIONS
    // =========================================================

    @Override
    protected String getTitle() {
        return "Choose Package";
    }

    /**
     * Create the adapter for displaying packages in the picker.
     * Each item shows:
     *   - Display name (e.g., "UI Layer") in dark color, or blue if active
     *   - Package name (e.g., "com.z.ui") in gray
     *   - Checkmark icon if this is the active package
     *
     * @param entries Filtered list of packages
     * @return A PackagePickerAdapter
     */
    @Override
    protected BaseAdapter createAdapter(ArrayList<PkgEntry> entries) {
        return new PackagePickerAdapter(entries);
    }

    /**
     * Handle item click: select the clicked package and close the dialog.
     *
     * @param entry The clicked package
     */
    @Override
    protected void onItemClicked(PkgEntry entry) {
        dismiss();
        listener.onPackagePicked(entry);
    }

    // =========================================================
    // INNER — Adapter for Picker List
    // =========================================================

    /**
     * Custom adapter for displaying packages in the picker.
     * Highlights the active package with a blue checkmark.
     */
    private final class PackagePickerAdapter extends BaseAdapter {

        /** The packages to display (may be filtered). */
        private final ArrayList<PkgEntry> entries;

        PackagePickerAdapter(ArrayList<PkgEntry> entries) {
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
            // Reuse the view if available (performance optimization)
            if (convertView == null) {
                convertView = buildItemView();
            }

            // Get the package entry at this position
            PkgEntry entry = entries.get(position);

            // Get references to the TextViews and ImageView
            TextView tvDisplay = convertView.findViewById(android.R.id.text1);
            TextView tvPkg = convertView.findViewById(android.R.id.text2);
            ImageView ivActive = (ImageView) convertView.getTag();

            // Set display name and package name
            tvDisplay.setText(entry.getDisplayName());
            tvPkg.setText(entry.getPackageName());

            // Check if this is the active package
            boolean isActive = activeEntry != null
                && activeEntry.getId().equals(entry.getId());

            // Show/hide the checkmark icon based on active state
            ivActive.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);

            // Color the display name blue if active, dark gray otherwise
            tvDisplay.setTextColor(isActive ? 0xFF1565C0 : 0xFF212121);

            return convertView;
        }

        /**
         * Build the view for a single package item.
         * Layout:
         *   [Display Name (14sp)]
         *   [Package Name (12sp, gray)]
         *   [Checkmark Icon] →
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
            // Text block (display name + package name)
            // ═════════════════════════════════════════════════════════════════════
            LinearLayout textBlock = new LinearLayout(context);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f // weight=1 (takes remaining space)
                );
            textBlock.setLayoutParams(textParams);

            // Display name (main label)
            TextView tvDisplay = new TextView(context);
            tvDisplay.setId(android.R.id.text1);
            tvDisplay.setTextSize(14f);
            tvDisplay.setTextColor(0xFF212121); // Dark gray
            textBlock.addView(tvDisplay);

            // Package name (secondary label)
            TextView tvPkg = new TextView(context);
            tvPkg.setId(android.R.id.text2);
            tvPkg.setTextSize(12f);
            tvPkg.setTextColor(0xFF757575); // Medium gray
            textBlock.addView(tvPkg);

            row.addView(textBlock);

            // ═════════════════════════════════════════════════════════════════════
            // Checkmark icon (right side)
            // ═════════════════════════════════════════════════════════════════════
            ImageView ivActive = new ImageView(context);
            ivActive.setImageResource(android.R.drawable.checkbox_on_background);
            ivActive.setVisibility(View.INVISIBLE); // Hidden by default
            LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                    dpToPx(24),
                    dpToPx(24)
                );
            iconParams.setMarginStart(dpToPx(16));
            ivActive.setLayoutParams(iconParams);

            // Store the ImageView reference in the row's tag for later access
            row.setTag(ivActive);
            row.addView(ivActive);

            return row;
        }
    }
}
