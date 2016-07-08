package com.demo.awareness.app.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import com.demo.awareness.R;
import com.demo.awareness.app.App;


public final class LeaveForgroundDialogFragment extends AppCompatDialogFragment {

	public static DialogFragment newInstance() {
		return (DialogFragment) DialogFragment.instantiate(App.Instance, LeaveForgroundDialogFragment.class.getName());
	}

	public interface LeaveForgroundListener {
		void onGoOn();

		void onStay();
	}

	private LeaveForgroundListener mOnListener;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setCancelable(false);
	}

	@Override
	public void onAttach(Context context) {
		if (context instanceof LeaveForgroundListener) {
			mOnListener = (LeaveForgroundListener) context;
		}
		super.onAttach(context);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.msg_lost_fence) + "\n" + getString(R.string.msg_keep_fence))
		       .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
				       if (mOnListener != null) {
					       mOnListener.onGoOn();
				       }
				       dialog.dismiss();
			       }
		       })
		       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
				       if (mOnListener != null) {
					       mOnListener.onStay();
				       }
				       dialog.cancel();
			       }
		       })
		       .setCancelable(false);
		return builder.create();
	}
}
