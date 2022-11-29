package com.example.tourOut.Controller;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.tourOut.R;


public class LocationService extends Service implements LocationListener {

    private static final String CHANNEL_ID = "TourOut";
    private static Location location = new Location("TourOut");

    private boolean stop;

    private static String message;

    public void setStop(boolean stop) {
        this.stop = stop;
        if (stop) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    public void setMessage(String message) {
        this.message = message;
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
        message = getString(R.string.localizando);
        new Thread(
                () -> {
                    while (!stop) {
                        creaNotification(getString(R.string.localidade_mais_proxima), message, "lat: " + location.getLatitude() + ", long: " + location.getLongitude());
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

    @Override
    public void onCreate () {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
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
        this.location = location;
    }

}
