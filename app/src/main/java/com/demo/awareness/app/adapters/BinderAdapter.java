package com.demo.awareness.app.adapters;

import android.databinding.BindingAdapter;
import android.support.design.widget.FloatingActionButton;

import com.demo.awareness.R;

/**
 * Created by xzhao on 08.07.16.
 */
public final class BinderAdapter {
	@BindingAdapter("pinFlag")
	public static void setPinButton(FloatingActionButton btn, boolean pinFlag) {
		btn.setImageResource(pinFlag?
		                     R.drawable.ic_pin_yet : R.drawable.ic_geofence_pin);
	}
}
