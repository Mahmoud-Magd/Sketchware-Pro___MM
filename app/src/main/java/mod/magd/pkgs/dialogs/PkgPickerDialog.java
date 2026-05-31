package mod.magd.pkgs.dialogs;

import pro.sketchware.R;

import android.content.Context;

import android.app.Dialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pro.sketchware.utility.SketchwareUtil;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;



import mod.magd.pkgs.PkgEntry;
import mod.magd.pkgs.PkgRegistry;



// =========================================================
// PkgPickerDialog
// =========================================================

// Dialog that lets the user choose an existing package from
// the project's registry.

// PURPOSE:
    // Presented when the user taps "Choose package" in the
    // Java/Kotlin Manager header.
    // Renders as a bottom-anchored dialog (like a sheet).
    // Lists all packages — main first, extras below.
    // Highlights the currently active package.
    // Calls OnPackagePickedListener on selection.

// USAGE:
    // new PkgPickerDialog (
        // context, registry, activeEntry, listener
    // ).show();

// =========================================================

public final class PkgPickerDialog {




    // =========================================================
    // INTERFACES
    // =========================================================

    public interface OnPackagePickedListener {
        void onPackagePicked (PkgEntry picked);
        void onPackageDeleted();
    }




    // =========================================================
    // VARIABLES
    // =========================================================

    private final Context context;
    private final PkgRegistry registry;
    private final PkgEntry activeEntry;
    private final OnPackagePickedListener listener;

    private Dialog dialog;




    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public PkgPickerDialog (
        Context context,
        PkgRegistry registry,
        PkgEntry activeEntry,
        OnPackagePickedListener listener
    ) {
        if (context  == null) throw new IllegalArgumentException ("PkgPickerDialog: context must not be null.");
        if (registry == null) throw new IllegalArgumentException ("PkgPickerDialog: registry must not be null.");
        if (listener == null) throw new IllegalArgumentException ("PkgPickerDialog: listener must not be null.");

        this.context     = context;
        this.registry    = registry;
        this.activeEntry = activeEntry;
        this.listener    = listener;
    }




    // =========================================================
    // PUBLIC
    // =========================================================

    public void show() {
        dialog = new Dialog (context, android.R.style.Theme_Material_Light_Dialog);
        dialog.requestWindowFeature (Window.FEATURE_NO_TITLE);
        dialog.setContentView ( buildContentView() );

        positionAtBottom (dialog);

        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }




    // =========================================================
    // PRIVATE — View Construction
    // =========================================================

    private View buildContentView() {
        // Root container
        android.widget.LinearLayout root = new android.widget.LinearLayout (context);
        root.setOrientation (android.widget.LinearLayout.VERTICAL);
        root.setPadding (0, 32, 0, 32);
        root.setBackgroundColor (0xFFFFFFFF);

        // Title
        TextView title = new TextView (context);
        title.setText ("Choose Package");
        title.setTextSize (16f);
        title.setTypeface (android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding (48, 24, 48, 24);
        title.setTextColor (0xFF212121);
        root.addView (title);

        // Divider
        root.addView ( buildDivider() );

        // Package list
        ListView listView = new ListView (context);
        listView.setDivider (null);
        listView.setAdapter ( new PackageAdapter ( registry.getAll() ) );
        listView.setOnItemClickListener ( (parent, view, position, id) -> {
            PkgEntry picked = (PkgEntry) parent.getItemAtPosition (position);
            dismiss();
            listener.onPackagePicked (picked);
        });

        int listHeight = Math.min (registry.getAll().size() * dpToPx (72), dpToPx (360));
        listView.setLayoutParams ( new ViewGroup.LayoutParams (
            ViewGroup.LayoutParams.MATCH_PARENT,
            listHeight
        ));
        root.addView (listView);

        return root;
    }

    private View buildDivider() {
        View divider = new View (context);
        divider.setLayoutParams ( new ViewGroup.LayoutParams (
            ViewGroup.LayoutParams.MATCH_PARENT,
            1
        ));
        divider.setBackgroundColor (0xFFE0E0E0);
        return divider;
    }

    private void positionAtBottom (Dialog d) {
        Window window = d.getWindow();
        if (window == null) return;

        window.setGravity (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        window.setLayout (
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        );

        WindowManager.LayoutParams params = window.getAttributes();
        params.y = 0;
        window.setAttributes (params);
    }




    // =========================================================
    // PRIVATE — Helpers
    // =========================================================

    private int dpToPx (int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round (dp * density);
    }




    // =========================================================
    // INNER — Adapter
    // =========================================================

    private final class PackageAdapter extends BaseAdapter {

        private final ArrayList<PkgEntry> entries;

        PackageAdapter (ArrayList<PkgEntry> entries) {
            this.entries = entries;
        }

        @Override public int getCount()              { return entries.size(); }
        @Override public Object getItem (int pos)    { return entries.get (pos); }
        @Override public long getItemId (int pos)    { return pos; }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = buildItemView();
            }

            PkgEntry entry = entries.get (position);

            TextView tvDisplay = convertView.findViewById (android.R.id.text1);
            TextView tvPkg     = convertView.findViewById (android.R.id.text2);
            ImageView ivActive = (ImageView) convertView.getTag();

            tvDisplay.setText ( entry.getDisplayName() );
            tvPkg.setText     ( entry.getPackageName() );

            boolean isActive = activeEntry != null
                && activeEntry.getId().equals (entry.getId());

            ivActive.setVisibility ( isActive ? View.VISIBLE : View.INVISIBLE );
            tvDisplay.setTextColor  ( isActive ? 0xFF1565C0 : 0xFF212121 );


            // delete
            if (!entry.isMain()) {
                convertView.setOnLongClickListener(v -> {
                    new MaterialAlertDialogBuilder(context)
                        .setTitle("Delete package?")
                        .setMessage(entry.getPackageName())
                        .setPositiveButton("Delete", (d, w) -> {
                            try {
                                registry.removePackage(entry.getId());
                                dismiss();  // Close dialog
                                listener.onPackageDeleted();  // Callback to refresh UI
                            } catch (Exception e) {
                                SketchwareUtil.toastError(e.getMessage());
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                    return true;
                });
            }


            

            return convertView;
        }


        

        private View buildItemView() {
            android.widget.LinearLayout row = new android.widget.LinearLayout (context);
            row.setOrientation (android.widget.LinearLayout.HORIZONTAL);
            row.setPadding (48, 20, 48, 20);
            row.setGravity (android.view.Gravity.CENTER_VERTICAL);

            // Text block
            android.widget.LinearLayout textBlock = new android.widget.LinearLayout (context);
            textBlock.setOrientation (android.widget.LinearLayout.VERTICAL);
            android.widget.LinearLayout.LayoutParams textParams =
                new android.widget.LinearLayout.LayoutParams (0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textBlock.setLayoutParams (textParams);

            TextView tvDisplay = new TextView (context);
            tvDisplay.setId (android.R.id.text1);
            tvDisplay.setTextSize (14f);
            tvDisplay.setTextColor (0xFF212121);
            textBlock.addView (tvDisplay);

            TextView tvPkg = new TextView (context);
            tvPkg.setId (android.R.id.text2);
            tvPkg.setTextSize (12f);
            tvPkg.setTextColor (0xFF757575);
            textBlock.addView (tvPkg);

            row.addView (textBlock);

            // Active checkmark
            ImageView ivActive = new ImageView (context);
            ivActive.setImageResource (android.R.drawable.checkbox_on_background);
            ivActive.setVisibility (View.INVISIBLE);
            android.widget.LinearLayout.LayoutParams iconParams =
                new android.widget.LinearLayout.LayoutParams (dpToPx (24), dpToPx (24));
            iconParams.setMarginStart (16);
            ivActive.setLayoutParams (iconParams);

            row.setTag (ivActive);
            row.addView (ivActive);

            return row;
        }

    }




}
