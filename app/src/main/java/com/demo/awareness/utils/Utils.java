package com.demo.awareness.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;


public final class Utils {
	public static BitmapDescriptor getBitmapDescriptor(Context cxt, @DrawableRes int id) {
		return BitmapDescriptorFactory.fromBitmap(getBitmap(VectorDrawableCompat.create(cxt.getResources(), id, null)));
	}


	private static Bitmap getBitmap(VectorDrawableCompat vectorDrawable) {
		Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		vectorDrawable.draw(canvas);
		return bitmap;
	}


	public static Drawable setTint(Drawable drawable, int color) {
		final Drawable newDrawable = DrawableCompat.wrap(drawable);
		DrawableCompat.setTint(newDrawable, color);
		return newDrawable;
	}

	public static boolean fenceStateToBoolean(FenceState state) {
		return state.getCurrentState() == FenceState.TRUE;
	}
}
