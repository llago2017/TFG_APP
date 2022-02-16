package com.myapp.SafeCamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DialogoPass extends DialogFragment implements TextView.OnEditorActionListener {

    private EditNameDialogListener listener;
    private EditText userInput;
    private Button accept;

    public interface EditNameDialogListener {
        void onFinishEditDialog(String inputText);
    }

    @NonNull
        @Override
        public AlertDialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View v = inflater.inflate(R.layout.fragment_dialog, null);

            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            dialog.setView(v);
            dialog.setCancelable(false);

            userInput = v.findViewById(R.id.password);
            accept = v.findViewById(R.id.accept);

            accept.setOnClickListener(a -> {
                if (userInput.getText().toString().length() >= 3) {
                    Log.i("DIALOGO", "test -> " + userInput.getText().toString());
                    listener.onFinishEditDialog(userInput.getText().toString());
                    dismiss();
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "La contraseña debe tener al menos 3 carácteres", Toast.LENGTH_LONG).show();
                }
            });

            return dialog;
        }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the EditNameDialogListener so we can send events to the host
            listener = (EditNameDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement EditNameDialogListener");
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            // Return input text to activity
            EditNameDialogListener activity = (EditNameDialogListener) getActivity();
            activity.onFinishEditDialog(userInput.getText().toString());
            this.dismiss();
            return true;
        }
        return false;
    }

    }
