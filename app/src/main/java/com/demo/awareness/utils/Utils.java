package com.demo.awareness.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;


public final class Utils {
	public static BitmapDescriptor getBitmapDescriptor(Context cxt, @DrawableRes int id) {
		return BitmapDescriptorFactory.fromBitmap(getBitmap(cxt, id));
	}


	private static Bitmap getBitmap(VectorDrawableCompat vectorDrawable) {
		Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		vectorDrawable.draw(canvas);
		return bitmap;
	}

	//see. http://qiita.com/konifar/items/aaff934edbf44e39b04a
	public static Bitmap getBitmap(Context context, @DrawableRes int drawableResId) {
		Drawable drawable = ContextCompat.getDrawable(context, drawableResId);
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		} else if (drawable instanceof VectorDrawableCompat) {
			return getBitmap((VectorDrawableCompat) drawable);
		} else {
			throw new IllegalArgumentException("Unsupported drawable type");
		}
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
