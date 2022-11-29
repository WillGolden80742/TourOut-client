package com.example.tourOut.Controller;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.tourOut.R;

public class LocationService extends Service implements LocationListener {

    private static final String CHANNEL_ID = "TourOut";
    private static Location location = new Location("TourOut");
    private static boolean isRunning = false;
    private static String message = "Localizando...";

    public void setMessage(String message) {
        this.message = message;
    }

    public static boolean isIsRunning() {
        return isRunning;
    }

    public static void setIsRunning(boolean isRunning) {
        LocationService.isRunning = isRunning;
    }

    public static Location getLocation() {
        return location;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        new Thread(
                () -> {
                    while (true) {
                        creaNotification("tourOut", message,"lat: "+location.getLatitude() + ", long: " + location.getLongitude());
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void creaNotification (String title,  String simpleText, String subText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_touourt_logo)
                .setContentTitle(title)
                .setContentText(simpleText)
                .setSubText(subText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (!isIsRunning()) {
            setIsRunning(true);
        }
        this.location = location;
    }

}
