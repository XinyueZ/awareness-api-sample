package com.demo.awareness.app.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import com.demo.awareness.R;
import com.demo.awareness.app.App;


public final class TipForPinDialogFragment extends AppCompatDialogFragment {

	public static DialogFragment newInstance() {
		return (DialogFragment) DialogFragment.instantiate(App.Instance, TipForPinDialogFragment.class.getName());
	}

	public interface OnPinGeofenceListener {
		void onOk();

		void onCancel();
	}

	private OnPinGeofenceListener mOnListener;

	@Override
	public void onAttach(Context context) {
		if (context instanceof OnPinGeofenceListener) {
			mOnListener = (OnPinGeofenceListener) context;
		}
		super.onAttach(context);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.msg_pin_for_geofence)
		       .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
				       if (mOnListener != null) {
					       mOnListener.onOk();
				       }
				       dialog.dismiss();
			       }
		       })
		       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
				       if (mOnListener != null) {
					       mOnListener.onCancel();
				       }
				       dialog.cancel();
			       }
		       });
		return builder.create();
	}
}
