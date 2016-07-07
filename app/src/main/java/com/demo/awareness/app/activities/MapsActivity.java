package com.demo.awareness.app.activities;

import android.Manifest;
import android.content.IntentSender;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.demo.awareness.R;
import com.demo.awareness.app.App;
import com.demo.awareness.databinding.MapActivityBinding;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

import static com.demo.awareness.R.id.map;
import static com.google.android.gms.analytics.internal.zzy.m;

@RuntimePermissions
public final class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
	private static final int LAYOUT = R.layout.activity_maps;
	private static final int REQUEST_CODE_RESOLVE_ERR = 0x98;
	private MapActivityBinding mBinding;
	private GoogleApiClient mGoogleApiClient;
	private GoogleMap mGoogleMap;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		obtainMap();
	}


	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		// Add a marker in Sydney and move the camera
		mGoogleMap = googleMap;

		UiSettings uiSettings = googleMap.getUiSettings();
		mGoogleMap.setMyLocationEnabled(true);
		uiSettings.setMyLocationButtonEnabled(false);
		doAskMyLocation();
	}

	private void connectToGoolgePlayServices() {
		mGoogleApiClient = new GoogleApiClient.Builder(App.Instance, new GoogleApiClient.ConnectionCallbacks() {
			@Override
			public void onConnected(Bundle bundle) {
				Snackbar.make(mBinding.mapContainerFl, R.string.ready_play_service, Snackbar.LENGTH_SHORT)
				        .show();
			}

			@Override
			public void onConnectionSuspended(int i) {

			}
		}, new GoogleApiClient.OnConnectionFailedListener() {
			@Override
			public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
				if (connectionResult.hasResolution()) {
					try {
						connectionResult.startResolutionForResult(MapsActivity.this, REQUEST_CODE_RESOLVE_ERR);
					} catch (IntentSender.SendIntentException e) {
						mGoogleApiClient.connect();
					}
				} else {
					Snackbar.make(mBinding.mapContainerFl, R.string.err_play_cnn, Snackbar.LENGTH_INDEFINITE)
					        .setAction(getString(R.string.close_app), new View.OnClickListener() {
						        @Override
						        public void onClick(View v) {
							        ActivityCompat.finishAffinity(MapsActivity.this);
						        }
					        })
					        .show();
				}
			}
		}).addApi(Awareness.API)
		  .build();
		mGoogleApiClient.connect();
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		// NOTE: delegate the permission handling to generated method
		MapsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
	}


	@OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
	void showDeniedForLocadtion() {
		Snackbar.make(mBinding.mapContainerFl, R.string.err_no_fine_location_permission, Snackbar.LENGTH_INDEFINITE)
		        .setAction(getString(R.string.close_app), new View.OnClickListener() {
			        @Override
			        public void onClick(View v) {
				        ActivityCompat.finishAffinity(MapsActivity.this);
			        }
		        })
		        .show();
	}


	private void obtainMap() {
		MapsActivityPermissionsDispatcher.doObtainMapWithCheck(this);
	}

	@NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
	void doObtainMap() {
		connectToGoolgePlayServices();

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
		mapFragment.getMapAsync(this);
	}


	public void askWeather(@SuppressWarnings("UnusedParameters") View view) {
		MapsActivityPermissionsDispatcher.doAskWeatherWithCheck(this);
	}

	@NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
	void doAskWeather() {
		Awareness.SnapshotApi.getWeather(mGoogleApiClient)
		                     .setResultCallback(new ResultCallback<WeatherResult>() {
			                     @Override
			                     public void onResult(@NonNull WeatherResult weatherResult) {
				                     if (!weatherResult.getStatus()
				                                       .isSuccess()) {
					                     Snackbar.make(mBinding.mapContainerFl, R.string.err_weather_data, Snackbar.LENGTH_SHORT)
					                             .show();
					                     return;
				                     }
				                     Weather weather = weatherResult.getWeather();
				                     Snackbar.make(mBinding.mapContainerFl, weather.toString(), Snackbar.LENGTH_LONG)
				                             .show();
			                     }
		                     });
	}


	public void askMyLocation(@SuppressWarnings("UnusedParameters") View view) {
		MapsActivityPermissionsDispatcher.doAskMyLocationWithCheck(this);
	}

	@NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
	void doAskMyLocation() {
		Awareness.SnapshotApi.getLocation(mGoogleApiClient)
		                     .setResultCallback(new ResultCallback<LocationResult>() {
			                     @Override
			                     public void onResult(@NonNull LocationResult locationResult) {
				                     if (!locationResult.getStatus()
				                                        .isSuccess()) {
					                     Snackbar.make(mBinding.mapContainerFl, R.string.err_my_location, Snackbar.LENGTH_SHORT)
					                             .show();
					                     return;
				                     }
				                     Location location = locationResult.getLocation();
				                     mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), mGoogleMap.getMaxZoomLevel()));
			                     }
		                     });
	}
}
