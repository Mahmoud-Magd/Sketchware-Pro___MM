package mod.magd.pkgs.dialogs;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;

// =========================================================
// PkgManageDialog
// =========================================================

// Dialog that presents two management options:
//   1. Delete - Remove one or more packages
//   2. Edit - Modify package name or display name

// PURPOSE:
//   Acts as a dispatcher for package management actions.
//   User sees a simple question: "What would you like to do?"
//   with two action buttons.

// FEATURES:
//   - Centered dialog with title and two buttons
//   - Delete button launches PkgDeleteDialog
//   - Edit button launches PkgEditDialog
//   - Clean, simple UX

// USAGE:
//   new PkgManageDialog(
//       this,
//       pkgRegistry,
//       activePackage,
//       (deleted) -> refreshUI(),           // on delete
//       (edited) -> updateActivePackage()   // on edit
//   ).show();

// =========================================================

public final class PkgManageDialog {

    // =========================================================
    // INTERFACES
    // =========================================================

    /**
     * Callback when packages are deleted.
     */
    public interface OnPackagesDeletedListener {
        /**
         * Called when deletion is complete.
         * Automatically switches to main package if active package was deleted.
         */
        void onPackagesDeleted();
    }

    /**
     * Callback when a package is edited.
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

    private final Context context;
    private final PkgRegistry registry;
    private final PkgEntry activeEntry;
    private final OnPackagesDeletedListener deleteListener;
    private final OnPackageEditedListener editListener;

    private android.app.Dialog dialog;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Create a new package management dialog.
     *
     * @param context Application context
     * @param registry Package registry
     * @param activeEntry Currently active package
     * @param deleteListener Callback when delete is confirmed
     * @param editListener Callback when package is edited
     */
    public PkgManageDialog(
        Context context,
        PkgRegistry registry,
        PkgEntry activeEntry,
        OnPackagesDeletedListener deleteListener,
        OnPackageEditedListener editListener
    ) {
        if (context == null)
            throw new IllegalArgumentException("PkgManageDialog: context must not be null.");
        if (registry == null)
            throw new IllegalArgumentException("PkgManageDialog: registry must not be null.");
        if (deleteListener == null)
            throw new IllegalArgumentException("PkgManageDialog: deleteListener must not be null.");
        if (editListener == null)
            throw new IllegalArgumentException("PkgManageDialog: editListener must not be null.");

        this.context = context;
        this.registry = registry;
        this.activeEntry = activeEntry;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    // =========================================================
    // PUBLIC
    // =========================================================

    /**
     * Display the dialog on screen.
     */
    public void show() {
        dialog = new android.app.Dialog(context, android.R.style.Theme_Material_Light_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(buildContentView());

        // Position dialog at center
        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
            window.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.show();
    }

    /**
     * Close the dialog if shown.
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    // =========================================================
    // PRIVATE — View Construction
    // =========================================================

    /**
     * Build the management dialog content.
     * Layout:
     *   [Title: "What would you like to do?\"]\n
     *   \n
     *   [Delete Button]  [Edit Button]
     *
     * @return The root container
     */
    private View buildContentView() {
        // Root vertical container
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(48), dpToPx(32), dpToPx(48), dpToPx(32));
        root.setBackgroundColor(0xFFFFFFFF); // White background

        // ═══════════════════════════════════════════════════════════════════
        // Title
        // ═══════════════════════════════════════════════════════════════════
        TextView title = new TextView(context);
        title.setText("What would you like to do?");
        title.setTextSize(16f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF212121);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.bottomMargin = dpToPx(24);
        title.setLayoutParams(titleParams);
        root.addView(title);

        // ═══════════════════════════════════════════════════════════════════
        // Button Layout (Delete | Edit)
        // ═══════════════════════════════════════════════════════════════════
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Delete button
        Button btnDelete = new Button(context);
        btnDelete.setText("Delete");
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ));
        btnDelete.setOnClickListener(v -> {
            dismiss();
            // Show the delete dialog
            new PkgDeleteDialog(
                context,
                registry,
                activeEntry,
                deleteListener::onPackagesDeleted
            ).show();
        });
        buttonLayout.addView(btnDelete);

        // Spacer between buttons
        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(8), 0));
        buttonLayout.addView(spacer);

        // Edit button
        Button btnEdit = new Button(context);
        btnEdit.setText("Edit");
        btnEdit.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ));
        btnEdit.setOnClickListener(v -> {
            dismiss();
            // Show the edit dialog
            new PkgEditDialog(
                context,
                registry,
                (edited) -> editListener.onPackageEdited(edited)
            ).show();
        });
        buttonLayout.addView(btnEdit);

        root.addView(buttonLayout);

        return root;
    }

    // =========================================================
    // PRIVATE — Utilities
    // =========================================================

    /**
     * Convert dp to pixels.
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
  }
