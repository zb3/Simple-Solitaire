package de.tobiasbielefeld.solitaire.dialogs;

import static android.app.Activity.RESULT_OK;

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
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tobiasbielefeld.solitaire.R;
import de.tobiasbielefeld.solitaire.classes.CustomDialogFragment;
import de.tobiasbielefeld.solitaire.classes.GameIDHelper;

public class DialogLoadGame extends CustomDialogFragment {
    private static final String TAG = "DialogLoadGame";
    private static final int REQUEST_CODE_LOAD = 6384; // onActivityResult request

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.dialog_load_game, null);

        builder.setView(view)
                .setPositiveButton(R.string.dialog_load_save_ok_button_label, (dialog, id) -> {});

        EditText gameIdField = view.findViewById(R.id.load_game_id);
        gameIdField.setText(extractGameId(getClipboardContent()));

        view.findViewById(R.id.load_game_id_button).setOnClickListener(v -> {
            String gameId = gameIdField.getText().toString();
            if (gameId.isEmpty()) {
                return;
            }
            loadGameFromId(gameId);
        });

        view.findViewById(R.id.load_button).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/octet-stream");
            startActivityForResult(intent, REQUEST_CODE_LOAD);
        });

        return applyFlags(builder.create());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, String.format("OAR: request: %d, result: %d", requestCode, resultCode));

        if (requestCode == REQUEST_CODE_LOAD && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                loadGameFromUri(uri);
            }
        }
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

    private void loadGameFromId(String gameId) {
        Log.i(TAG, "Game id to be loaded is: "+gameId);

        try {
            byte[] bytes = GameIDHelper.stateIdToBytes(gameId);
            loadGame(bytes);
        } catch (Exception e) {
            Log.e(TAG, "Decoding error", e);
            showError(getString(R.string.dialog_load_loading_error)+e.getMessage());
        }
    }

    private void loadGameFromUri(Uri uri) {
        Log.i(TAG, "Uri: " + uri.toString());

        InputStream inputStream = null;
        try {
            inputStream = getContext().getContentResolver().openInputStream(uri);
            byte[] bytes = getBytes(inputStream);
            loadGame(bytes);
        } catch(NullPointerException | IOException e) {
            showError(getString(R.string.dialog_load_file_io_error)+e.getMessage());
        } finally {
            // if the close method is the only thing to fail, it means the game was loaded
            // so don't display any error messages
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
        }
    }

    private void loadGame(byte[] gameData) {
        try {
            JSONObject state = GameIDHelper.decodeJSON(gameData);
            Log.i(TAG, "JSON State:" +state);

            GameIDHelper.loadGameFromJSONState(state, getActivity());
            dismiss(); // needed when called from GameSelector
        } catch (Exception e) {
            Log.e(TAG, "Loading error", e);
            showError(getString(R.string.dialog_load_loading_error));
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private String getClipboardContent() {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().
                getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null) {
                // prefer text items
                // otherwise use coerceToText on the first item
                for (int i = 0; i < clip.getItemCount(); i++) {
                    ClipData.Item item = clip.getItemAt(i);
                    if (item.getText() != null) {
                        return item.getText().toString();
                    }
                }
                if (clip.getItemCount() > 0) {
                    return clip.getItemAt(0).coerceToText(getActivity()).toString();
                }
            }
        }
        return "";
    }

    private String extractGameId(String src) {
        Pattern p = Pattern.compile("[A-Za-z0-9_.:-]+:[A-Za-z0-9_.:-]{10,}");
        Matcher m = p.matcher(src);
        if (m.find()) {
            return m.group();
        }
        return null;
    }
}