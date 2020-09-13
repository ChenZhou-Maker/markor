/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.ui.AndroidSpinnerOnItemSelectedAdapter;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.io.File;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;
import other.de.stanetz.jpencconverter.PasswordStore;

public class NewFileDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "net.gsantner.markor.ui.NewFileDialog";
    public static final String EXTRA_DIR = "EXTRA_DIR";
    private Callback.a2<Boolean, File> callback;

    public static NewFileDialog newInstance(File sourceFile, Callback.a2<Boolean, File> callback) {
        NewFileDialog dialog = new NewFileDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DIR, sourceFile);
        dialog.setArguments(args);
        dialog.callback = callback;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_DIR);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = makeDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();
        Window w;
        if ((w = dialog.getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    @SuppressLint("SetTextI18n")
    private AlertDialog.Builder makeDialog(final File basedir, LayoutInflater inflater) {
        View root;
        AlertDialog.Builder dialogBuilder;
        final AppSettings appSettings = new AppSettings(inflater.getContext());
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), appSettings.isDarkThemeEnabled() ? R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        root = inflater.inflate(R.layout.new_file_dialog, null);

        final EditText fileNameEdit = root.findViewById(R.id.new_file_dialog__name);
        final EditText fileExtEdit = root.findViewById(R.id.new_file_dialog__ext);
        final CheckBox encryptCheckbox = root.findViewById(R.id.new_file_dialog__encrypt);
        final Spinner typeSpinner = root.findViewById(R.id.new_file_dialog__type);
        final Spinner templateSpinner = root.findViewById(R.id.new_file_dialog__template);
        final String[] typeSpinnerToExtension = getResources().getStringArray(R.array.new_file_types__file_extension);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && appSettings.hasPasswordBeenSetOnce()) {
            encryptCheckbox.setChecked(appSettings.getNewFileDialogLastUsedEncryption());
        } else {
            encryptCheckbox.setVisibility(View.GONE);
        }
        fileExtEdit.setText(appSettings.getNewFileDialogLastUsedExtension());
        fileNameEdit.requestFocus();
        new Handler().postDelayed(new ContextUtils.DoTouchView(fileNameEdit), 200);

        fileNameEdit.setFilters(new InputFilter[]{ContextUtils.INPUTFILTER_FILENAME});
        fileExtEdit.setFilters(fileNameEdit.getFilters());

        final AtomicBoolean typeSpinnerNoTriggerOnFirst = new AtomicBoolean(true);
        typeSpinner.setOnItemSelectedListener(new AndroidSpinnerOnItemSelectedAdapter(pos -> {
            if (typeSpinnerNoTriggerOnFirst.getAndSet(false)) {
                return;
            }
            String ext = pos < typeSpinnerToExtension.length ? typeSpinnerToExtension[pos] : "";

            if (ext != null) {
                if (encryptCheckbox.isChecked()) {
                    fileExtEdit.setText(ext + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
                } else {
                    fileExtEdit.setText(ext);
                }
            }
            fileNameEdit.setSelection(fileNameEdit.length());
        }));

        templateSpinner.setOnItemSelectedListener(new AndroidSpinnerOnItemSelectedAdapter(pos -> {
            String prefix = null;
            String d = null;
            String yyyy = null;
            String KW = null;
            //todo

           switch (pos) {
               case 0: {
                   prefix = "Notiz_" + SttCommander.DATEF_YYYY_MM_DD.format(new Date());
                   break;
               }
               case 1: {
                   prefix = "Tagebuch_" + SttCommander.DATEF_YYYY_MM_DD.format(new Date()) + "_";
                   break;
               }
               case 2: {
                   prefix = "Tagebuch_" + SttCommander.DATEF_YYYY_MM_DD.format(new Date());
                   break;
               }
               case 3: {
                   d = SttCommander.DATEF_YYYY_MM_DD.format(new Date());
                   yyyy = d.substring(0,4);
                   prefix = "Wochenplan_" + yyyy + "KW";
                   break;
               }
               case 4: {
                   d = SttCommander.DATEF_YYYY_MM_DD.format(new Date());
                   yyyy = d.substring(0,4);
                   prefix = "Jahresvorsatz_" + yyyy;
                   break;
               }
               case 5: {
                   prefix = "Person_";
                   break;
               }
               default: {
                   break;
                }

            }

            if (!TextUtils.isEmpty(prefix) ) {
                fileNameEdit.setText(prefix );
            }
            fileNameEdit.setSelection(fileNameEdit.length());
        }));

        encryptCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            final String currentExtention = fileExtEdit.getText().toString();
            if (isChecked) {
                if (!currentExtention.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)) {
                    fileExtEdit.setText(currentExtention + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
                }
            } else if (currentExtention.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)) {
                fileExtEdit.setText(currentExtention.replace(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION, ""));
            }
            appSettings.setNewFileDialogLastUsedEncryption(isChecked);
        });

        dialogBuilder.setView(root);
        fileNameEdit.requestFocus();

        final ShareUtil shareUtil = new ShareUtil(getContext());
        dialogBuilder
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
                    if (ez(fileNameEdit)) {
                        return;
                    }

                    appSettings.setNewFileDialogLastUsedExtension(fileExtEdit.getText().toString().trim());
                    final File f = new File(basedir, fileNameEdit.getText().toString().trim() + fileExtEdit.getText().toString().trim());
                    final byte[] templateContents = getTemplateContent(templateSpinner, basedir, encryptCheckbox.isChecked());
                    shareUtil.writeFile(f, false, (arg_ok, arg_fos) -> {
                        try {
                            if (f.exists() && f.length() < ShareUtil.MIN_OVERWRITE_LENGTH && templateContents != null) {
                                arg_fos.write(templateContents);
                            }
                        } catch (Exception ignored) {
                        }
                        callback(arg_ok || f.exists(), f);
                        dialogInterface.dismiss();
                    });
                })
                .setNeutralButton(R.string.folder, (dialogInterface, i) -> {
                    if (ez(fileNameEdit)) {
                        return;
                    }
                    File f = new File(basedir, fileNameEdit.getText().toString());
                    if (shareUtil.isUnderStorageAccessFolder(f)) {
                        DocumentFile dof = shareUtil.getDocumentFile(f, true);
                        callback(dof != null && dof.exists(), f);
                    } else {
                        callback(f.mkdirs() || f.exists(), f);
                    }
                    dialogInterface.dismiss();
                });

        return dialogBuilder;
    }

    private boolean ez(EditText et) {
        return et.getText().toString().isEmpty();
    }

    private void callback(boolean ok, File file) {
        try {
            callback.callback(ok, file);
        } catch (Exception ignored) {
        }
    }

    // How to get content out of a file:
    // 1) Replace \n with \\n | copy to clipboard
    //    cat markor-markdown-reference.md  | sed 's@\\@\\\\@g' | sed -z 's@\n@\\n@g'  | xclip
    //
    // 2) t = "<cursor>";  | ctrl+shift+v "paste without formatting"
    //
    private byte[] getTemplateContent(final Spinner templateSpinner, final File basedir, final boolean encrypt) {
        String t = null;
        byte[] bytes = null;
        switch (templateSpinner.getSelectedItemPosition()) {
            //todo change template
            case 1: { //tagebuch leer
                t = "---\n#tagebuchsystem \n#Täglich \ntitle: {{ template.timestamp_date_yyyy_mm_dd }} \n---\n";
                break;
            }
            case 2: { //tagebuch vorlage
                t = "---\n#tagebuchsystem \n#Täglich \ntitle: {{ template.timestamp_date_yyyy_mm_dd }} \n---\n# Morgen(Plan) \n- [ ] \n\n# Abend(Reflektion)\n## 🚹 Mein Gefühl - tolle Erlebnisse:\n+ \n\n## 🚹 Mein Gefühl - Sonstiges:  \n+ \n\n## 💪 Morgen mache ich besser:   \n+   \n\n## 👪 Meine Beziehungspflege:\n+ \n\n## 👪 Personen und deren Geschichte:  \n+ ";
                break;
            }
            case 3: { // wochenplan
                t = "---\n#Wöchentlich \n#tagebuchsystem\ntitle: Wochenplan & Review\n---\n\n# Ziele der aktuellen Woche  \n - [ ] \n\n# Reflektion \n- \n\n# Ziele der kommenden Woche\n - [ ] ";
                break;
            }
            case 4: { // jahresvorsatz
                t = "---\n#tagebuchsystem  \n#jährlich \n---\n# 💪 Säge schärfen  \n+ \n\n# 🚹 Individuum  \n+ \n\n# 👪 Familie \n+ \n\n# 🍻 Freunde und Verwandte  \n+ \n\n# 🛄 Karriere  \n+ ";
                break;
            }
            case 5: { // person
                t = "---\n#tagebuchsystem\n#person\n---\n# Name \n\n## Persönliche Informationen\n\n**Geburtstag:**   \n\n**Hobby:**   \n\n**Mögen:**   \n\n**Abneigung:**  \n\n**Wünsche:**   \n\n**Sorge/Angst:**   \n\n## Familie  \n\n\n## Ereignisse \n+ ";
                break;
            }

            default:
            case 0: { // notiz leer
                return null;
            }
        }
        t = t.replace("{{ template.timestamp_date_yyyy_mm_dd }}", SttCommander.DATEF_YYYY_MM_DD.format(new Date()));

        if (encrypt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bytes = new JavaPasswordbasedCryption(JavaPasswordbasedCryption.Version.V001, new SecureRandom()).encrypt(t, new PasswordStore(getContext()).loadKey(R.string.pref_key__default_encryption_password));
        } else {
            bytes = t.getBytes();
        }
        return bytes;
    }
}
