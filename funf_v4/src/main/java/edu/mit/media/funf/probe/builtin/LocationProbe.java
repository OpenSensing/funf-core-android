package edu.mit.media.funf.probe.builtin;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import edu.mit.media.funf.util.LogUtil;

/**
 * Updated location probe using fused location provider.
 * Supports both active schedule and passive mode.
 * @author astopczynski@google.com
 */
@DisplayName("Location Probe")
@RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION})
@RequiredFeatures("android.hardware.location")
@Schedule.DefaultSchedule(interval=1800)
public class LocationProbe extends Base implements ContinuousProbe, PassiveProbe, LocationKeys,
		ConnectionCallbacks, OnConnectionFailedListener {

	@Configurable
	private Integer passiveInterval = 60;

	@Configurable
	private Integer activeInterval = 5;

	private Gson gson;

	private LocationRequest mLocationRequest;
	private LocationRequest mLocationRequestPassive;
	private GoogleApiClient mGoogleApiClient;
	private ProbeLocationListener listener = new ProbeLocationListener();
	private ProbeLocationListener passiveListener = new ProbeLocationListener();

	private boolean toStartActiveMode = false;

	@Override
	protected void onEnable() {
		super.onEnable();
		gson = getGsonBuilder().addSerializationExclusionStrategy(new LocationExclusionStrategy()).create();
		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(getContext())
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.build();
		}

		toStartActiveMode = false;

		mLocationRequestPassive = new LocationRequest();
		mLocationRequestPassive.setInterval(passiveInterval*1000);
		mLocationRequestPassive.setFastestInterval(passiveInterval*1000);
		mLocationRequestPassive.setPriority(LocationRequest.PRIORITY_NO_POWER);

		mGoogleApiClient.connect();

	}

	private class LocationExclusionStrategy implements ExclusionStrategy {

		public boolean shouldSkipClass(Class<?> cls) {
			return false;
		}

		public boolean shouldSkipField(FieldAttributes f) {
			String name = f.getName();
			return (f.getDeclaringClass() == Location.class &&
					(name.equals("mResults")
							|| name.equals("mDistance")
							|| name.equals("mInitialBearing")
							|| name.equals("mLat1")
							|| name.equals("mLat2")
							|| name.equals("mLon1")
							|| name.equals("mLon2")
							|| name.equals("mLon2")
							|| name.equals("mHasSpeed")
							|| name.equals("mHasAccuracy")
							|| name.equals("mHasAltitude")
							|| name.equals("mHasBearing")
							|| name.equals("mHasSpeed")
							|| name.equals("mElapsedRealtimeNanos")
					)
			);
		}

	}

	private class ProbeLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				JsonObject data = gson.toJsonTree(location).getAsJsonObject();
				data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(data.get("mTime").getAsBigDecimal()));
				sendData(gson.toJsonTree(location).getAsJsonObject());
			}

		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		toStartActiveMode = true;

		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(activeInterval * 1000);
		mLocationRequest.setFastestInterval(activeInterval * 1000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		if (mGoogleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,
					listener);
			toStartActiveMode = false;
		} else {
			mGoogleApiClient.connect();
		}



	}

	@Override
	protected void onStop() {
		super.onStop();
		toStartActiveMode = false;
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, listener);
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		if (!mGoogleApiClient.isConnected()) {
			return;
		}
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, passiveListener);
		mGoogleApiClient.disconnect();
	}

	@Override
	public void onConnected(Bundle bundle) {
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
				mLocationRequestPassive, passiveListener);
		if (toStartActiveMode) {
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,
					listener);
			toStartActiveMode = false;
		}
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {

	}

}
