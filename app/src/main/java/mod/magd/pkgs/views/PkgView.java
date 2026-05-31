package mod.magd.pkgs.views;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.View;

import mod.magd.pkgs.PkgEntry;

// =========================================================
// PkgView
// =========================================================

// Custom LinearLayout component that displays the active package
// and provides action buttons (Switch, Manage/Delete).

// PURPOSE:
//   Replaces the header view building code in ManageJavaActivity.
//   Encapsulates the package header UI into a reusable component.
//   Simplifies ManageJavaActivity and makes the header logic testable.

// STRUCTURE:
//   [Package Display Name] (bold, large)
//   [package.name]         (gray, small)
//   [Switch Package] [Manage Packages]
//   ─────────────────────────────────────────────

// USAGE:
//   PkgView pkgHeader = new PkgView(this);
//   pkgHeader.setPackage(activePackage);
//   pkgHeader.setActionListener(new PkgView.OnPackageActionListener() {
//       @Override
//       public void onSwitchPackage() {
//           showPackagePickerDialog();
//       }
//
//       @Override
//       public void onManagePackages() {
//           showPackageDeleteDialog();
//       }
//   });
//   parentLayout.addView(pkgHeader, 0);

// WHY setPackage()?
//   It allows the view to be updated AFTER construction when the
//   active package changes. This is useful for refreshing the UI
//   without recreating the entire view.
//   Example: User picks a new package → onPackagePicked callback
//            → pkgHeader.setPackage(newPackage) → header updates

// =========================================================

public final class PkgView extends LinearLayout {

    // =========================================================
    // INTERFACES
    // =========================================================

    /**
     * Callback for package-related action buttons.
     */
    public interface OnPackageActionListener {
        /**
         * User clicked the "Switch Package" button.
         */
        void onSwitchPackage();

        /**
         * User clicked the "Manage Packages" button.
         */
        void onManagePackages();
    }

    // =========================================================
    // VARIABLES
    // =========================================================

    /** Current package being displayed. */
    private PkgEntry currentPackage;

    /** Listener for action button clicks. */
    private OnPackageActionListener actionListener;

    /** TextView for the package display name. */
    private TextView tvDisplayName;

    /** TextView for the full package name. */
    private TextView tvPackageName;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Create a new PkgView component.
     *
     * @param context Application context
     */
    public PkgView(Context context) {
        super(context);
        initializeView();
    }

    // =========================================================
    // PUBLIC — Configuration
    // =========================================================

    /**
     * Set the package to display in this view.
     * Updates the display name and package name TextViews.
     * Call this method to refresh the header when the active package changes.
     *
     * @param pkg The package to display (may be null)
     */
    public void setPackage(PkgEntry pkg) {
        this.currentPackage = pkg;
        updateDisplay();
    }

    /**
     * Set the listener for action button clicks.
     *
     * @param listener The callback listener
     */
    public void setActionListener(OnPackageActionListener listener) {
        this.actionListener = listener;
    }

    // =========================================================
    // PRIVATE — Initialization
    // =========================================================

    /**
     * Build and configure the view hierarchy.
     * Sets orientation, padding, and adds all child views.
     */
    private void initializeView() {
        // Configure this LinearLayout
        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        setPadding(24, 24, 24, 24);

        // ═════════════════════════════════════════════════════════════════════
        // Package Display Name ("UI Layer")
        // ═════════════════════════════════════════════════════════════════════
        tvDisplayName = new TextView(getContext());
        tvDisplayName.setTextSize(16f);
        tvDisplayName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvDisplayName.setTextColor(0xFF212121); // Dark gray
        tvDisplayName.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        addView(tvDisplayName);

        // ═════════════════════════════════════════════════════════════════════
        // Package Full Name ("com.z.ui")
        // ═════════════════════════════════════════════════════════════════════
        tvPackageName = new TextView(getContext());
        tvPackageName.setTextSize(12f);
        tvPackageName.setTextColor(0xFF757575); // Medium gray
        tvPackageName.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvPackageName.setPadding(0, 4, 0, 0); // Small top margin
        addView(tvPackageName);

        // ═════════════════════════════════════════════════════════════════════
        // Spacer View
        // ═════════════════════════════════════════════════════════════════════
        View spacer = new View(getContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(12)
        ));
        addView(spacer);

        // ═════════════════════════════════════════════════════════════════════
        // Button Layout (Switch + Manage)
        // ═════════════════════════════════════════════════════════════════════
        LinearLayout buttonLayout = new LinearLayout(getContext());
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // "Switch Package" button
        Button btnSwitch = new Button(getContext());
        btnSwitch.setText("Switch");
        btnSwitch.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f // weight=1 (equal width)
        ));
        btnSwitch.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onSwitchPackage();
            }
        });
        buttonLayout.addView(btnSwitch);

        // Spacer between buttons
        View buttonSpacer = new View(getContext());
        buttonSpacer.setLayoutParams(new LinearLayout.LayoutParams(
            dpToPx(8),
            0
        ));
        buttonLayout.addView(buttonSpacer);

        // "Manage Packages" button
        Button btnManage = new Button(getContext());
        btnManage.setText("Manage");
        btnManage.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f // weight=1 (equal width)
        ));
        btnManage.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onManagePackages();
            }
        });
        buttonLayout.addView(btnManage);

        addView(buttonLayout);

        // ═════════════════════════════════════════════════════════════════════
        // Divider Line
        // ═════════════════════════════════════════════════════════════════════
        View divider = new View(getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            1 // 1 pixel height
        ));
        divider.setBackgroundColor(0xFFE0E0E0); // Light gray
        divider.setMarginStart(0);
        addView(divider);
    }

    /**
     * Update the display TextViews with the current package information.
     * If no package is set, shows placeholder text.
     */
    private void updateDisplay() {
        if (currentPackage == null) {
            tvDisplayName.setText("No package selected");
            tvPackageName.setText("--");
        } else {
            tvDisplayName.setText(currentPackage.getDisplayName());
            tvPackageName.setText(currentPackage.getPackageName());
        }
    }

    // =========================================================
    // PRIVATE — Utilities
    // =========================================================

    /**
     * Convert density-independent pixels (dp) to physical pixels (px).
     * Uses the device's current display density.
     *
     * @param dp Size in density-independent pixels
     * @return Size in physical pixels
     */
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
