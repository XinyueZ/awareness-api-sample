package com.demo.awareness.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.hardware.display.DisplayManagerCompat;
import android.util.DisplayMetrics;
import android.view.Display;

import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;


public final class Utils {
	public static BitmapDescriptor getBitmapDescriptor(Context cxt, @DrawableRes int id) {
		return BitmapDescriptorFactory.fromBitmap(getBitmap(VectorDrawableCompat.create(cxt.getResources(), id, null)));
	}


	public static Bitmap getBitmap(VectorDrawableCompat vectorDrawable) {
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


	public static ScreenSize getScreenSize(Context cxt) {
		return getScreenSize(cxt, 0);
	}


	public static ScreenSize getScreenSize(Context cxt, int displayIndex) {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		Display[] displays = DisplayManagerCompat.getInstance(cxt).getDisplays();
		Display display = displays[displayIndex];
		display.getMetrics(displaymetrics);
		return new ScreenSize(displaymetrics.widthPixels, displaymetrics.heightPixels);
	}

	/**
	 * Screen-size in pixels.
	 */
	public static class ScreenSize {
		public int Width;
		public int Height;

		public ScreenSize(int _width, int _height) {
			Width = _width;
			Height = _height;
		}
	}
}
