package mod.magd.pkgs.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;




// =========================================================
// JavaPkgCreatorDialog
// =========================================================

// Dialog that lets the user define and create a brand-new
// fully-independent Java package for their project.

// PURPOSE:
    // Presented when the user taps "New package" in the
    // Java/Kotlin Manager header.
    // Collects:
    //   packageName  — fully-qualified, e.g. "com.z.ui"
    //   displayName  — short label, e.g. "UI Layer"
    // Runs live validation on packageName as the user types.
    // Shows a specific error message per validation rule failure.
    // On confirm → calls JavaPkgRegistry.addPackage() → notifies listener.
    // On failure → shows the error; stays open so user can fix it.

// USAGE:
    // new JavaPkgCreatorDialog (context, registry, listener).show();

// =========================================================

public final class JavaPkgCreatorDialog {




    // =========================================================
    // INTERFACES
    // =========================================================

    public interface OnPackageCreatedListener {
        void onPackageCreated (JavaPkgEntry created);
    }




    // =========================================================
    // VARIABLES
    // =========================================================

    private final Context context;
    private final JavaPkgRegistry registry;
    private final OnPackageCreatedListener listener;




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public JavaPkgCreatorDialog (
        Context context,
        JavaPkgRegistry registry,
        OnPackageCreatedListener listener
    ) {
        if (context  == null) throw new IllegalArgumentException ("JavaPkgCreatorDialog: context must not be null.");
        if (registry == null) throw new IllegalArgumentException ("JavaPkgCreatorDialog: registry must not be null.");
        if (listener == null) throw new IllegalArgumentException ("JavaPkgCreatorDialog: listener must not be null.");

        this.context  = context;
        this.registry = registry;
        this.listener = listener;
    }




    // =========================================================
    // PUBLIC
    // =========================================================

    public void show() {
        // Build content view
        LinearLayout content = buildContentView();

        EditText etPackage  = (EditText) content.getTag (R_PACKAGE);
        EditText etDisplay  = (EditText) content.getTag (R_DISPLAY);
        TextView tvError    = (TextView) content.getTag (R_ERROR);
        Button   btnConfirm = (Button)   content.getTag (R_CONFIRM);

        AlertDialog dialog = new AlertDialog.Builder (context)
            .setTitle ("New Package")
            .setView (content)
            .setNegativeButton ("Cancel", null)
            .create();

        // Live validation as user types
        etPackage.addTextChangedListener ( new TextWatcher() {
            @Override public void beforeTextChanged (CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged     (CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged (Editable s) {
                String input = s.toString().trim();
                JavaPkgValidator.Result result = JavaPkgValidator.validate (
                    input,
                    registry.getAll()
                );

                if ( result.isValid() ) {
                    tvError.setVisibility (android.view.View.GONE);
                    btnConfirm.setEnabled (true);
                } else {
                    tvError.setText ( result.getReason() );
                    tvError.setVisibility (android.view.View.VISIBLE);
                    btnConfirm.setEnabled (false);
                }
            }
        });

        // Confirm button — tries to create the package
        btnConfirm.setOnClickListener ( v -> {
            String packageName  = etPackage.getText().toString().trim();
            String displayName  = etDisplay.getText().toString().trim();

            // Final validation before writing to disk
            JavaPkgValidator.Result result = JavaPkgValidator.validate (
                packageName,
                registry.getAll()
            );

            if ( ! result.isValid() ) {
                tvError.setText ( result.getReason() );
                tvError.setVisibility (android.view.View.VISIBLE);
                return;
            }

            // Use package tail as display name fallback
            if (displayName.isEmpty()) {
                String[] segments = packageName.split ("\\.");
                displayName = segments [segments.length - 1];
            }

            try {
                JavaPkgEntry created = registry.addPackage (packageName, displayName);
                dialog.dismiss();
                listener.onPackageCreated (created);
            } catch (Exception e) {
                tvError.setText ("Failed to create package: " + e.getMessage());
                tvError.setVisibility (android.view.View.VISIBLE);
            }
        });

        dialog.show();

        // Replace the positive button with our custom one AFTER show()
        // so we control its enabled state without AlertDialog auto-dismissing.
        // (AlertDialog.Builder's setPositiveButton always dismisses on click.)
        dialog.getWindow().setLayout (
            (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }




    // =========================================================
    // PRIVATE — View Construction
    // =========================================================

    // Integer tags used to retrieve child views from the root.
    // Simple alternative to findViewById with no XML IDs needed.
    private static final int R_PACKAGE = 1001;
    private static final int R_DISPLAY = 1002;
    private static final int R_ERROR   = 1003;
    private static final int R_CONFIRM = 1004;

    private LinearLayout buildContentView() {
        LinearLayout root = new LinearLayout (context);
        root.setOrientation (LinearLayout.VERTICAL);
        int p = dpToPx (20);
        root.setPadding (p, p, p, 8);

        // Package name label
        root.addView ( buildLabel ("Package name (e.g. com.z.ui)") );

        // Package name input
        EditText etPackage = buildInput ("com.z.ui");
        etPackage.setInputType (
            android.text.InputType.TYPE_CLASS_TEXT |
            android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );
        root.addView (etPackage);

        // Error message
        TextView tvError = new TextView (context);
        tvError.setTextColor (0xFFB71C1C);
        tvError.setTextSize (12f);
        tvError.setPadding (0, 4, 0, 4);
        tvError.setVisibility (android.view.View.GONE);
        root.addView (tvError);

        // Spacer
        root.addView ( buildSpacer (12) );

        // Display name label
        root.addView ( buildLabel ("Display name (optional short label)") );

        // Display name input
        EditText etDisplay = buildInput ("e.g. UI Layer");
        root.addView (etDisplay);

        // Spacer
        root.addView ( buildSpacer (20) );

        // Confirm button
        Button btnConfirm = new Button (context);
        btnConfirm.setText ("Create Package");
        btnConfirm.setEnabled (false); // enabled only when valid
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams (
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnConfirm.setLayoutParams (btnParams);
        root.addView (btnConfirm);

        // Tag child views onto root for retrieval in show()
        root.setTag (R_PACKAGE, etPackage);
        root.setTag (R_DISPLAY, etDisplay);
        root.setTag (R_ERROR,   tvError);
        root.setTag (R_CONFIRM, btnConfirm);

        return root;
    }

    private TextView buildLabel (String text) {
        TextView tv = new TextView (context);
        tv.setText (text);
        tv.setTextSize (12f);
        tv.setTextColor (0xFF757575);
        tv.setPadding (0, 0, 0, 4);
        return tv;
    }

    private EditText buildInput (String hint) {
        EditText et = new EditText (context);
        et.setHint (hint);
        et.setTextSize (14f);
        et.setTextColor (0xFF212121);
        et.setSingleLine (true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams (
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        et.setLayoutParams (params);
        return et;
    }

    private android.view.View buildSpacer (int heightDp) {
        android.view.View spacer = new android.view.View (context);
        spacer.setLayoutParams ( new LinearLayout.LayoutParams (
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx (heightDp)
        ));
        return spacer;
    }




    // =========================================================
    // PRIVATE — Helpers
    // =========================================================

    private int dpToPx (int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round (dp * density);
    }




}
