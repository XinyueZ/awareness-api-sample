package com.demo.awareness.app.activities;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.databinding.DataBindingUtil;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.demo.awareness.R;
import com.demo.awareness.app.App;
import com.demo.awareness.app.fragments.LeaveForgroundDialogFragment;
import com.demo.awareness.app.fragments.TipForPinDialogFragment;
import com.demo.awareness.databinding.MapActivityBinding;
import com.demo.awareness.utils.Utils;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

import static android.R.attr.id;
import static com.demo.awareness.R.id.map;
import static com.demo.awareness.utils.Utils.ScreenSize;
import static com.demo.awareness.utils.Utils.fenceStateToBoolean;
import static com.demo.awareness.utils.Utils.getBitmapDescriptor;
import static com.demo.awareness.utils.Utils.getScreenSize;
import static com.demo.awareness.utils.Utils.setTint;

@RuntimePermissions
public final class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
                                                                    TipForPinDialogFragment.OnPinGeofenceListener,
                                                                    LeaveForgroundDialogFragment.LeaveForgroundListener,
                                                                    View.OnLongClickListener {
	private static final String TAG = MapsActivity.class.getName();
	private static final int LAYOUT = R.layout.activity_maps;

	private static final int REQUEST_CODE_RESOLVE_ERR = 0x98;
	private static final String ACTION_FENCE_HEADSET_PLUGGED_IN = "action_fence_headset_plugged_in";
	private static final String ACTION_FENCE_ACTIVITY = "action_fence_activity";
	private static final String ACTION_GEOFENCE = "action_geofence_activity";

	private static final long GEOFENCE_RADIUS = 50;
	private static final int ZOOM = 17;
	private static final int GEOFENCE_IN_DWELLINGTIME = 5000;

	private static final String FENCE_ENTERING = "FENCE_ENTERING";
	private static final String FENCE_EXITING = "FENCE_EXITING";
	private static final String FENCE_IN = "FENCE_IN";

	private static final String ACTIVITY_UNKNOWN = "UNKNOWN";
	private static final String ACTIVITY_ON_FOOT = "ON_FOOT";
	private static final String ACTIVITY_RUNNING = "RUNNING";
	private static final String ACTIVITY_WALKING = "WALKING";
	private static final String ACTIVITY_STILL = "STILL";
	private static final String ACTIVITY_ON_BICYCLE = "ON_BICYCLE";
	private static final String ACTIVITY_IN_VEHICLE = "IN_VEHICLE";
	private static final String ACTIVITY_TILTING = "TILTING";


	private MapActivityBinding mBinding;
	private GoogleApiClient mGoogleApiClient;
	private GoogleMap mGoogleMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);

	}

	@Override
	protected void onResume() {
		super.onResume();
		if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			initAll();
		} else {
			preInitAll();
		}
	}

	@Override
	protected void onPause() {
		if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			unregisterHeadsetFence();
			unregisterActivityFence();
		}
		super.onPause();
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		// Add a marker in Sydney and move the camera
		mGoogleMap = googleMap;

		UiSettings uiSettings = googleMap.getUiSettings();
		mGoogleMap.setMyLocationEnabled(true);
		uiSettings.setMyLocationButtonEnabled(false);

		mBinding.fingerBtn.setImageDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_long_tap, null));
		mBinding.fingerBtn.setOnLongClickListener(this);
		doAskMyLocation();
	}

	private void connectToGoolgePlayServices() {
		mGoogleApiClient = new GoogleApiClient.Builder(App.Instance, new GoogleApiClient.ConnectionCallbacks() {
			@Override
			public void onConnected(Bundle bundle) {

				// Obtain the SupportMapFragment and get notified when the map is ready to be used.
				initAll();
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

	private void initAll() {
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
		mapFragment.getMapAsync(MapsActivity.this);
		registerHeadsetFence();
		registerActivityFence();
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


	private void preInitAll() {
		MapsActivityPermissionsDispatcher.doPreInitAllWithCheck(this);
	}

	@NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
	void doPreInitAll() {
		connectToGoolgePlayServices();
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
				                     int zoom = ZOOM;//mGoogleMap.getMaxZoomLevel()
				                     mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoom));
			                     }
		                     });
	}

	public void askPlaces(@SuppressWarnings("UnusedParameters") View view) {
		MapsActivityPermissionsDispatcher.doAskPlacesWithCheck(this);
	}

	@NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
	void doAskPlaces() {
		clearPlaceMarkers();
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

	private void clearPlaceMarkers() {
		for (Marker marker : mPlacePreviousMarkList) {
			marker.remove();
		}
		mPlacePreviousMarkList.clear();
	}

	private final List<Marker> mPlacePreviousMarkList = new ArrayList<>();

	private void askPlacesPhotos(@NonNull final Place place) {
		Places.GeoDataApi.getPlacePhotos(mGoogleApiClient, place.getId())
		                 .setResultCallback(new ResultCallback<PlacePhotoMetadataResult>() {
			                 @Override
			                 public void onResult(@NonNull PlacePhotoMetadataResult photos) {
				                 if (!photos.getStatus()
				                            .isSuccess()) {

					                 //Error when getting photo of place, populate a default icon.
					                 mPlacePreviousMarkList.add(mGoogleMap.addMarker(new MarkerOptions().position(place.getLatLng())
					                                                                                    .icon(getBitmapDescriptor(App.Instance, R.drawable.ic_place_default_thumbnail))
					                                                                                    .anchor(0f, 0f)));
					                 return;
				                 }
				                 PlacePhotoMetadataBuffer photoMetadataBuffer = photos.getPhotoMetadata();
				                 if (photoMetadataBuffer.getCount() > 0) {
					                 int thumbSize = getResources().getDimensionPixelOffset(R.dimen.place_photo_thumbnail);
					                 photoMetadataBuffer.get(0)
					                                    .getScaledPhoto(mGoogleApiClient, thumbSize, thumbSize)
					                                    .setResultCallback(new ResultCallback<PlacePhotoResult>() {
						                                    @Override
						                                    public void onResult(@NonNull PlacePhotoResult placePhotoResult) {
							                                    if (!placePhotoResult.getStatus()
							                                                         .isSuccess()) {

								                                    //Error when getting photo of place, populate a default icon.
								                                    mPlacePreviousMarkList.add(mGoogleMap.addMarker(new MarkerOptions().position(place.getLatLng())
								                                                                                                       .icon(getBitmapDescriptor(App.Instance,
								                                                                                                                                 R.drawable
										                                                                                                                                 .ic_place_default_thumbnail))
								                                                                                                       .anchor(0f, 0f)));
								                                    return;
							                                    }

							                                    //Populate photo of place.
							                                    mPlacePreviousMarkList.add(mGoogleMap.addMarker(new MarkerOptions().position(place.getLatLng())
							                                                                                                       .icon(BitmapDescriptorFactory.fromBitmap(placePhotoResult.getBitmap
									                                                                                                       ()))
							                                                                                                       .anchor(0f, 0f)));
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


	private final BroadcastReceiver mHeadsetStatusHandler = new BroadcastReceiver() {
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

	private void unregisterHeadsetFence() {
		unregisterReceiver(mHeadsetStatusHandler);
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().removeFence(MapsActivity.ACTION_FENCE_HEADSET_PLUGGED_IN)
		                                                                .build())
		                  .setResultCallback(new ResultCallbacks<Status>() {
			                  @Override
			                  public void onSuccess(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + MapsActivity.ACTION_FENCE_HEADSET_PLUGGED_IN + " successfully removed.");
			                  }

			                  @Override
			                  public void onFailure(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + MapsActivity.ACTION_FENCE_HEADSET_PLUGGED_IN + " could NOT be removed.");
			                  }
		                  });
	}


	private void registerActivityFence() {
		PendingIntent activityIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), new Intent(ACTION_FENCE_ACTIVITY), PendingIntent.FLAG_UPDATE_CURRENT);
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().addFence(ACTIVITY_UNKNOWN, DetectedActivityFence.during(DetectedActivityFence.UNKNOWN), activityIntent)
		                                                                .addFence(ACTIVITY_ON_FOOT, DetectedActivityFence.during(DetectedActivityFence.ON_FOOT), activityIntent)
		                                                                .addFence(ACTIVITY_RUNNING, DetectedActivityFence.during(DetectedActivityFence.RUNNING), activityIntent)
		                                                                .addFence(ACTIVITY_WALKING, DetectedActivityFence.during(DetectedActivityFence.WALKING), activityIntent)
		                                                                .addFence(ACTIVITY_STILL, DetectedActivityFence.during(DetectedActivityFence.STILL), activityIntent)
		                                                                .addFence(ACTIVITY_ON_BICYCLE, DetectedActivityFence.during(DetectedActivityFence.ON_BICYCLE), activityIntent)
		                                                                .addFence(ACTIVITY_IN_VEHICLE, DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE), activityIntent)
		                                                                .addFence(ACTIVITY_TILTING, DetectedActivityFence.during(DetectedActivityFence.TILTING), activityIntent)
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


	private final BroadcastReceiver mActivityStatusHandler = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			FenceState fenceState = FenceState.extract(intent);
			int tintColor = fenceStateToBoolean(fenceState) ?
			                ResourcesCompat.getColor(getResources(), R.color.colorLimeDark, null) :
			                ResourcesCompat.getColor(getResources(), R.color.colorGrey, null);
			switch (fenceState.getFenceKey()) {
				case ACTIVITY_UNKNOWN:
					setTint(mBinding.unknownFab.getDrawable(), tintColor);
					break;
				case ACTIVITY_ON_FOOT:
					setTint(mBinding.onFootFab.getDrawable(), tintColor);
					break;
				case ACTIVITY_RUNNING:
					setTint(mBinding.onRunningFab.getDrawable(), tintColor);
					break;
				case ACTIVITY_WALKING:
					setTint(mBinding.onWalkingFab.getDrawable(), tintColor);
					break;
				case ACTIVITY_STILL:
					setTint(mBinding.onStillFab.getDrawable(), tintColor);
					break;
				case ACTIVITY_ON_BICYCLE:
					setTint(mBinding.onBicycleFab.getDrawable(), tintColor);
					break;
				case ACTIVITY_IN_VEHICLE:
					setTint(mBinding.inVehicleFab.getDrawable(), tintColor);
					break;
				case ACTIVITY_TILTING:
					setTint(mBinding.tiltingFab.getDrawable(), tintColor);
					break;
			}
		}
	};

	private void unregisterActivityFence() {
		unregisterReceiver(mActivityStatusHandler);
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().removeFence(MapsActivity.ACTION_FENCE_ACTIVITY)
		                                                                .build())
		                  .setResultCallback(new ResultCallbacks<Status>() {
			                  @Override
			                  public void onSuccess(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + MapsActivity.ACTION_FENCE_ACTIVITY + " successfully removed.");
			                  }

			                  @Override
			                  public void onFailure(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + MapsActivity.ACTION_FENCE_ACTIVITY + " could NOT be removed.");
			                  }
		                  });
	}

	private boolean mGeofenceAlreadyRegistered;

	private void registerLocationFence() {
		MapsActivityPermissionsDispatcher.doRegisterLocationFenceWithCheck(this);
	}

	@NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
	void doRegisterLocationFence() {
		PendingIntent geofenceIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), new Intent(ACTION_GEOFENCE), PendingIntent.FLAG_UPDATE_CURRENT);
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().addFence(FENCE_ENTERING,
		                                                                          LocationFence.entering(mGeofenceLatLng.latitude, mGeofenceLatLng.longitude, GEOFENCE_RADIUS),
		                                                                          geofenceIntent)
		                                                                .addFence(FENCE_EXITING,
		                                                                          LocationFence.exiting(mGeofenceLatLng.latitude, mGeofenceLatLng.longitude, GEOFENCE_RADIUS),
		                                                                          geofenceIntent)
		                                                                .addFence(FENCE_IN,
		                                                                          LocationFence.in(mGeofenceLatLng.latitude, mGeofenceLatLng.longitude, GEOFENCE_RADIUS, GEOFENCE_IN_DWELLINGTIME),
		                                                                          geofenceIntent)
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
		registerReceiver(mGeofenceStatusHandler, new IntentFilter(ACTION_GEOFENCE));
		mGeofenceAlreadyRegistered = true;
	}

	private final BroadcastReceiver mGeofenceStatusHandler = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			NotificationManager mgr = (NotificationManager) App.Instance.getSystemService(Context.NOTIFICATION_SERVICE);
			FenceState fenceState = FenceState.extract(intent);
			String pos = null;
			switch (fenceState.getFenceKey()) {
				case FENCE_ENTERING:
					if (fenceStateToBoolean(fenceState)) {
						pos = "Entering";
					}
					break;
				case FENCE_EXITING:
					if (fenceStateToBoolean(fenceState)) {
						pos = "Exiting";
					}
					break;
				case FENCE_IN:
					if (fenceStateToBoolean(fenceState)) {
						pos = "In";
					}
					break;
			}
			if (!TextUtils.isEmpty(pos)) {
				pos = String.format(getString(R.string.notify_you_got_fence), pos);
				Intent i = new Intent(MapsActivity.this, MapsActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				PendingIntent geofenceIntent = PendingIntent.getActivity(MapsActivity.this, (int) System.currentTimeMillis(), i, PendingIntent.FLAG_UPDATE_CURRENT);
				mgr.notify((int) System.currentTimeMillis(),
				           new NotificationCompat.Builder(App.Instance).setWhen(System.currentTimeMillis())
				                                                       .setContentIntent(geofenceIntent)
				                                                       .setSmallIcon(android.R.drawable.ic_notification_overlay)
				                                                       .setLargeIcon(Utils.getBitmap(VectorDrawableCompat.create(App.Instance.getResources(), R.drawable.ic_pin, null)))
				                                                       .setTicker(pos)
				                                                       .setContentTitle(pos)
				                                                       .setContentText(pos)
				                                                       .setAutoCancel(true)
				                                                       .build());
			}
		}
	};

	private void unregisterLocationFence() {
		if (mGeofenceAlreadyRegistered) {
			unregisterReceiver(mGeofenceStatusHandler);
			mGeofenceAlreadyRegistered = false;
		}
		Awareness.FenceApi.updateFences(mGoogleApiClient,
		                                new FenceUpdateRequest.Builder().removeFence(MapsActivity.ACTION_GEOFENCE)
		                                                                .build())
		                  .setResultCallback(new ResultCallbacks<Status>() {
			                  @Override
			                  public void onSuccess(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + MapsActivity.ACTION_GEOFENCE + " successfully removed.");
			                  }

			                  @Override
			                  public void onFailure(@NonNull Status status) {
				                  Log.i(TAG, "Fence " + MapsActivity.ACTION_GEOFENCE + " could NOT be removed.");
			                  }
		                  });
	}


	public void pinGeofence(@SuppressWarnings("UnusedParameters") View view) {
		mBinding.setFlag(true);
		TipForPinDialogFragment.newInstance()
		                       .show(getSupportFragmentManager(), null);
	}


	@Override
	public void onOk() {
		//No need to impl.
	}

	@Override
	public void onCancel() {
		clearAllGeofenceInfo();
	}


	private void clearAllGeofenceInfo() {
		mBinding.setFlag(false);
		clearGeofenceMarkersAndCircles();
	}


	private Marker mGeofencePreviousMarker;
	private Circle mGeofencePreviousCircle;
	private LatLng mGeofenceLatLng;

	private void clearGeofenceMarkersAndCircles() {
		if (mGeofencePreviousMarker != null && mGeofencePreviousCircle != null) {
			mGeofencePreviousCircle.remove();
			mGeofencePreviousMarker.remove();
			mGeofencePreviousCircle = null;
			mGeofencePreviousMarker = null;
			mGeofenceLatLng = null;
		}
	}

	@Override
	public boolean onLongClick(View view) {
		//Finger long tap for finding a geofence location.

		clearGeofenceMarkersAndCircles();

		ScreenSize screenSize = getScreenSize(App.Instance);
		Projection projection = mGoogleMap.getProjection();
		mGeofenceLatLng = projection.fromScreenLocation(new Point(screenSize.Width / 2, screenSize.Height / 2));


		mGeofencePreviousMarker = mGoogleMap.addMarker(new MarkerOptions().position(mGeofenceLatLng)
		                                                                  .title("Geofence")
		                                                                  .icon(getBitmapDescriptor(App.Instance, R.drawable.ic_pin)));
		mGeofencePreviousCircle = mGoogleMap.addCircle(new CircleOptions().center(mGeofenceLatLng)
		                                                                  .radius(GEOFENCE_RADIUS)
		                                                                  .strokeWidth(1)
		                                                                  .strokeColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryDark, null))
		                                                                  .fillColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryDark50, null)));

		mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(mGeofenceLatLng));
		//No need to "long-tap" , we have chosen one location to fence.
		mBinding.fingerBtn.setVisibility(View.GONE);

		unregisterLocationFence();
		registerLocationFence();
		return false;
	}

	@Override
	public void onGoOn() {
		unregisterLocationFence();

		ActivityCompat.finishAfterTransition(this);
	}

	@Override
	public void onStay() {

	}

	@Override
	public void onBackPressed() {
		LeaveForgroundDialogFragment.newInstance()
		                            .show(getSupportFragmentManager(), null);
	}
}
