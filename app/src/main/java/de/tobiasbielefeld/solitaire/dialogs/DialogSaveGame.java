package de.tobiasbielefeld.solitaire.dialogs;

import static android.app.Activity.RESULT_OK;
import static de.tobiasbielefeld.solitaire.SharedData.gameLogic;
import static de.tobiasbielefeld.solitaire.SharedData.lg;
import static de.tobiasbielefeld.solitaire.SharedData.prefs;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.tobiasbielefeld.solitaire.R;
import de.tobiasbielefeld.solitaire.classes.CustomDialogFragment;
import de.tobiasbielefeld.solitaire.classes.GameIDHelper;

public class DialogSaveGame extends CustomDialogFragment {
    private static final String TAG = "DialogSaveGame";
    private static final int REQUEST_CODE_SAVE = 6384; // onActivityResult request
    private String gameState = null;
    private View view = null;

    private String getDefaultSaveFilename() {
        // use .bin because that's what android understands marks as octet-stream
        String cdate = new SimpleDateFormat("yyMMdd'T'HHmmss").format(new Date());
        return String.format("save_%s_%s.bin",
                lg.getSharedPrefName(),  cdate);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        view = inflater.inflate(R.layout.dialog_save_game, null);
        builder.setView(view)
                .setPositiveButton(R.string.dialog_load_save_ok_button_label,
                        (dialog, id) -> DialogSaveGame.this.getDialog().dismiss());

        Button copyStringButton = view.findViewById(R.id.copy_button);
        EditText editText = view.findViewById(R.id.save_game_id);

        copyStringButton.setOnClickListener(view -> {
            String gameId = editText.getText().toString();

            ClipboardManager clipboard = (ClipboardManager) requireActivity().
                    getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText("label", gameId);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getActivity(), R.string.dialog_save_copy_ok_toast, Toast.LENGTH_SHORT).show();
        });

        Button saveButton = view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(view -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");

                // use .bin because that's what android understands marks as octet-stream
                intent.putExtra(Intent.EXTRA_TITLE, getDefaultSaveFilename());
                startActivityForResult(intent, REQUEST_CODE_SAVE);
            } else {
                showError(getString(R.string.dialog_save_android_too_old_error));
            }
        });

        CheckBox includeState = view.findViewById(R.id.save_include_state);
        CheckBox includeUndo = view.findViewById(R.id.save_include_undo);

        if (savedState == null) {
            includeState.setChecked(prefs.getSaveIncludeState());
            includeUndo.setChecked(prefs.getSaveIncludeUndo());
        }

        includeState.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked && includeUndo.isChecked()) {
                includeUndo.setChecked(false);
            }
        });
        includeUndo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !includeState.isChecked()) {
                includeState.setChecked(true);
            }
        });

        boolean gameInProgress = gameLogic.isGameInProgress();

        if (savedState != null) {
            // we need to load saved gameState, because by the time we're recreated
            // some parts are not yet initialized due to intentional delay
            gameState = savedState.getString("gameState");
            gameInProgress = savedState.getBoolean("gameInProgress", gameInProgress);
        }

        if (gameState == null) {
            gameState = GameIDHelper.getCurrentStateAsId();
        }

        editText.setText(gameState);
        view.findViewById(R.id.save_settings_container).setVisibility(
                gameInProgress ? View.VISIBLE : View.GONE);

        return applyFlags(builder.create());
    }

    private void showError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

        /*
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
        */
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (gameState != null) {
            outState.putString("gameState", gameState);
            outState.putBoolean("gameInProgress", gameLogic.isGameInProgress());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, String.format("OAR: request: %d, result: %d", requestCode, resultCode));

        if (requestCode == REQUEST_CODE_SAVE && resultCode == RESULT_OK) {
            // Get the file URI from the intent
            Uri fileUri = data.getData();
            if (fileUri != null) {
                saveToFile(fileUri);
            }
        }
    }

    private void saveToFile(Uri fileUri) {
        Log.i(TAG, "Got uri: "+fileUri.toString());

        boolean includeState = ((CheckBox)view.findViewById(R.id.save_include_state)).isChecked();
        boolean includeUndo = ((CheckBox)view.findViewById(R.id.save_include_undo)).isChecked();

        prefs.putSaveIncludeState(includeState);
        prefs.putSaveIncludeUndo(includeUndo);

        JSONObject state = GameIDHelper.getCurrentStateJSON(includeState, includeUndo);
        byte[] saveContent = GameIDHelper.encodeJSON(state);

        try (OutputStream outputStream = getContext().getContentResolver()
                .openOutputStream(fileUri)) {
            outputStream.write(saveContent);

            Toast.makeText(getActivity(), R.string.dialog_save_file_ok_toast, Toast.LENGTH_SHORT).show();
            getDialog().dismiss();
        } catch (NullPointerException e) {
            showError(getString(R.string.dialog_save_file_error));
        } catch (IOException e) {
            showError(getString(R.string.dialog_save_file_io_error) + e);
        }
    }
}