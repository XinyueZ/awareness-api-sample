package com.demo.awareness.app.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.demo.awareness.R;
import com.demo.awareness.app.App;
import com.demo.awareness.databinding.MapActivityBinding;
import com.demo.awareness.utils.Utils;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResult;
import com.google.android.gms.location.places.PlacePhotoResult;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

import static com.demo.awareness.R.id.map;
import static com.demo.awareness.utils.Utils.*;
import static com.demo.awareness.utils.Utils.fenceStateToBoolean;
import static com.google.android.gms.analytics.internal.zzy.v;

@RuntimePermissions
public final class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
	private static final String TAG = MapsActivity.class.getName();
	private static final int LAYOUT = R.layout.activity_maps;
	private static final int REQUEST_CODE_RESOLVE_ERR = 0x98;
	private static final String ACTION_FENCE_HEADSET_PLUGGED_IN = "action_fence_headset_plugged_in";
	private static final String ACTION_FENCE_ACTIVITY = "action_fence_activity";
	private MapActivityBinding mBinding;
	private GoogleApiClient mGoogleApiClient;
	private GoogleMap mGoogleMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		obtainMap();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerHeadsetFence();
		registerActivityFence();
	}

	@Override
	protected void onPause() {
		unregisterHeadsetFence(ACTION_FENCE_HEADSET_PLUGGED_IN);
		unregisterActivityFence(ACTION_FENCE_ACTIVITY);
		super.onPause();
	}

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
		  .addApi(Places.GEO_DATA_API)
		  .build();
		mGoogleApiClient.connect();
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
				                     int zoom = 17;//mGoogleMap.getMaxZoomLevel()
				                     mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoom));
			                     }
		                     });
	}

	public void askPlaces(@SuppressWarnings("UnusedParameters") View view) {
		MapsActivityPermissionsDispatcher.doAskPlacesWithCheck(this);
	}

	@NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
	void doAskPlaces() {
		mGoogleMap.clear();
		Awareness.SnapshotApi.getPlaces(mGoogleApiClient)
		                     .setResultCallback(new ResultCallback<PlacesResult>() {
			                     @Override
			                     public void onResult(@NonNull PlacesResult placesResult) {
				                     if (!placesResult.getStatus()
				                                      .isSuccess()) {
					                     Snackbar.make(mBinding.mapContainerFl, R.string.err_places, Snackbar.LENGTH_SHORT)
					                             .show();
					                     return;
				                     }

				                     List<PlaceLikelihood> placeLikelihoodList = placesResult.getPlaceLikelihoods();
				                     for (PlaceLikelihood placeLikelihood : placeLikelihoodList) {
					                     askPlacesPhotos(placeLikelihood.getPlace());
				                     }
			                     }
		                     });
	}

	private void askPlacesPhotos(@NonNull final Place place) {
		Places.GeoDataApi.getPlacePhotos(mGoogleApiClient, place.getId())
		                 .setResultCallback(new ResultCallback<PlacePhotoMetadataResult>() {
			                 @Override
			                 public void onResult(PlacePhotoMetadataResult photos) {
				                 if (!photos.getStatus()
				                            .isSuccess()) {

					                 //Error when getting photo of place, populate a default icon.
					                 mGoogleMap.addMarker(new MarkerOptions().position(place.getLatLng())
					                                                         .icon(getBitmapDescriptor(App.Instance, R.drawable.ic_place_default_thumbnail))
					                                                         .anchor(0f, 0f));
					                 return;
				                 }
				                 PlacePhotoMetadataBuffer photoMetadataBuffer = photos.getPhotoMetadata();
				                 if (photoMetadataBuffer.getCount() > 0) {
					                 int thumbSize = getResources().getDimensionPixelOffset(R.dimen.place_photo_thumbnail);
					                 photoMetadataBuffer.get(0)
					                                    .getScaledPhoto(mGoogleApiClient, thumbSize, thumbSize)
					                                    .setResultCallback(new ResultCallback<PlacePhotoResult>() {
						                                    @Override
						                                    public void onResult(PlacePhotoResult placePhotoResult) {
							                                    if (!placePhotoResult.getStatus()
							                                                         .isSuccess()) {

								                                    //Error when getting photo of place, populate a default icon.
								                                    mGoogleMap.addMarker(new MarkerOptions().position(place.getLatLng())
								                                                                            .icon(getBitmapDescriptor(App.Instance, R.drawable.ic_place_default_thumbnail))
								                                                                            .anchor(0f, 0f));
								                                    return;
							                                    }

							                                    //Populate photo of place.
							                                    mGoogleMap.addMarker(new MarkerOptions().position(place.getLatLng())
							                                                                            .icon(BitmapDescriptorFactory.fromBitmap(placePhotoResult.getBitmap()))
							                                                                            .anchor(0f, 0f));
						                                    }
					                                    });
				                 }
				                 photoMetadataBuffer.release();
			                 }
		                 });
	}


	private void registerHeadsetFence() {
		//Snapshot detecting: Fellowing codes are not important, because the fence-api can detect current status as well.

//		Awareness.SnapshotApi.getHeadphoneState(mGoogleApiClient)
//		                     .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
//			                     @Override
//			                     public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
//				                     if (!headphoneStateResult.getStatus()
//				                                              .isSuccess()) {
//					                     Snackbar.make(mBinding.mapContainerFl, R.string.err_headset, Snackbar.LENGTH_SHORT)
//					                             .show();
//					                     return;
//				                     }
//				                     HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
//				                     mBinding.headsetStatusFab.setImageResource(headphoneState.getState() == HeadphoneState.PLUGGED_IN ?
//				                                                                R.drawable.ic_headset_on :
//				                                                                R.drawable.ic_headset_off);
//			                     }
//		                     });

		// Fence detecting
		// Create a fence.
		AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);
		PendingIntent headsetIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), new Intent(ACTION_FENCE_HEADSET_PLUGGED_IN), PendingIntent.FLAG_UPDATE_CURRENT);
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().addFence(ACTION_FENCE_HEADSET_PLUGGED_IN, headphoneFence, headsetIntent)
		                                                                .build())
		                  .setResultCallback(new ResultCallback<Status>() {
			                  @Override
			                  public void onResult(@NonNull Status status) {
				                  if (!status.isSuccess()) {
					                  Snackbar.make(mBinding.mapContainerFl, R.string.err_headset, Snackbar.LENGTH_SHORT)
					                          .show();
				                  }
			                  }
		                  });

		registerReceiver(mHeadsetStatusHandler, new IntentFilter(ACTION_FENCE_HEADSET_PLUGGED_IN));
	}


	private BroadcastReceiver mHeadsetStatusHandler = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			FenceState fenceState = FenceState.extract(intent);

			if (TextUtils.equals(fenceState.getFenceKey(), ACTION_FENCE_HEADSET_PLUGGED_IN)) {
				mBinding.headsetStatusFab.setImageResource(fenceStateToBoolean(fenceState) ?
				                                           R.drawable.ic_headset_on :
				                                           R.drawable.ic_headset_off);
			}
		}
	};

	private void unregisterHeadsetFence(final String fenceKey) {
		unregisterReceiver(mHeadsetStatusHandler);
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().removeFence(fenceKey)
		                                                                .build())
		                  .setResultCallback(new ResultCallbacks<Status>() {
			                  @Override
			                  public void onSuccess(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + fenceKey + " successfully removed.");
			                  }

			                  @Override
			                  public void onFailure(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + fenceKey + " could NOT be removed.");
			                  }
		                  });
	}


	private void registerActivityFence() {
		PendingIntent activityIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), new Intent(ACTION_FENCE_ACTIVITY), PendingIntent.FLAG_UPDATE_CURRENT);
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().addFence("UNKNOWN", DetectedActivityFence.during(DetectedActivityFence.UNKNOWN), activityIntent)
		                                                                .addFence("ON_FOOT", DetectedActivityFence.during(DetectedActivityFence.ON_FOOT), activityIntent)
		                                                                .addFence("RUNNING", DetectedActivityFence.during(DetectedActivityFence.RUNNING), activityIntent)
		                                                                .addFence("WALKING", DetectedActivityFence.during(DetectedActivityFence.WALKING), activityIntent)
		                                                                .addFence("STILL", DetectedActivityFence.during(DetectedActivityFence.STILL), activityIntent)
		                                                                .addFence("ON_BICYCLE", DetectedActivityFence.during(DetectedActivityFence.ON_BICYCLE), activityIntent)
		                                                                .addFence("IN_VEHICLE", DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE), activityIntent)
		                                                                .addFence("TILTING", DetectedActivityFence.during(DetectedActivityFence.TILTING), activityIntent)
		                                                                .build())
		                  .setResultCallback(new ResultCallback<Status>() {
			                  @Override
			                  public void onResult(@NonNull Status status) {
				                  if (!status.isSuccess()) {
					                  Snackbar.make(mBinding.mapContainerFl, R.string.err_activity, Snackbar.LENGTH_SHORT)
					                          .show();
				                  }
			                  }
		                  });
		registerReceiver(mActivityStatusHandler, new IntentFilter(ACTION_FENCE_ACTIVITY));
	}


	private BroadcastReceiver mActivityStatusHandler = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			FenceState fenceState = FenceState.extract(intent);
			Log.d(TAG, "Fence activity" + fenceState.getFenceKey() + " - - cur: " + fenceState.getCurrentState() + ", previous: " + fenceState.getPreviousState());
			switch (fenceState.getFenceKey()) {
				case "UNKNOWN":
					mBinding.unknownFab.setImageDrawable(setTint(ContextCompat.getDrawable(context, R.drawable.ic_unknown),
					              fenceStateToBoolean(fenceState) ?
					              ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
					              ResourcesCompat.getColor(getResources(), R.color.colorGrey, null)));
					break;
				case "ON_FOOT":
					mBinding.onFootFab.setImageDrawable(setTint(ContextCompat.getDrawable(context, R.drawable.ic_on_foot),
					              fenceStateToBoolean(fenceState) ?
					              ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
					              ResourcesCompat.getColor(getResources(), R.color.colorGrey, null)));
					break;
				case "RUNNING":
					mBinding.onRunningFab.setImageDrawable(setTint(ContextCompat.getDrawable(context, R.drawable.ic_running),
					              fenceStateToBoolean(fenceState) ?
					              ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
					              ResourcesCompat.getColor(getResources(), R.color.colorGrey, null)));
					break;
				case "WALKING":
					mBinding.onWalkingFab.setImageDrawable(setTint(ContextCompat.getDrawable(context, R.drawable.ic_walking),
					              fenceStateToBoolean(fenceState) ?
					              ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
					              ResourcesCompat.getColor(getResources(), R.color.colorGrey, null)));
					break;
				case "STILL":
					mBinding.onStillFab.setImageDrawable(setTint(ContextCompat.getDrawable(context, R.drawable.ic_still),
					              fenceStateToBoolean(fenceState) ?
					              ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
					              ResourcesCompat.getColor(getResources(), R.color.colorGrey, null)));
					break;
				case "ON_BICYCLE":
					mBinding.onBicycleFab.setImageDrawable(setTint(ContextCompat.getDrawable(context, R.drawable.ic_on_bicycle),
					              fenceStateToBoolean(fenceState) ?
					              ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
					              ResourcesCompat.getColor(getResources(), R.color.colorGrey, null)));
					break;
				case "IN_VEHICLE":
					mBinding.inVehicleFab.setImageDrawable(setTint(ContextCompat.getDrawable(context, R.drawable.ic_in_vehicle),
					              fenceStateToBoolean(fenceState) ?
					              ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
					              ResourcesCompat.getColor(getResources(), R.color.colorGrey, null)));
					break;
				case "TILTING":
					mBinding.tiltingFab.setImageDrawable(setTint(ContextCompat.getDrawable(context, R.drawable.ic_tilting),
					              fenceStateToBoolean(fenceState) ?
					              ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
					              ResourcesCompat.getColor(getResources(), R.color.colorGrey, null)));
					break;
			}
		}
	};

	private void unregisterActivityFence(final String fenceKey) {
		unregisterReceiver(mActivityStatusHandler);
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().removeFence(fenceKey)
		                                                                .build())
		                  .setResultCallback(new ResultCallbacks<Status>() {
			                  @Override
			                  public void onSuccess(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + fenceKey + " successfully removed.");
			                  }

			                  @Override
			                  public void onFailure(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + fenceKey + " could NOT be removed.");
			                  }
		                  });
	}


	private void registerLocationFence() {
		MapsActivityPermissionsDispatcher.doRegisterLocationFenceWithCheck(this);
	}

	@NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
	void doRegisterLocationFence() {

	}

	private void unregisterLocationFence(final String fenceKey) {


	}
}
