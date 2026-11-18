package com.sentinelai.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.List;

/** Periodic local permission-declaration review. Not a runtime behavior monitor. */
public final class ThreatMonitorWorker extends Worker {
    private static final String CHANNEL = "sentinel_monitor";
    public ThreatMonitorWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }
    @NonNull @Override public Result doWork() {
        Context c = getApplicationContext();
        try {
            PackageManager pm = c.getPackageManager();
            List<PackageInfo> apps;
            if (Build.VERSION.SDK_INT >= 33) apps = pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
            else {
                //noinspection deprecation
                apps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
            }
            int count = 0;
            for (PackageInfo p : apps) {
                PermissionRiskAnalyzer.Result r = PermissionRiskAnalyzer.analyze(p);
                if (r.score >= 48) count++;
            }
            if (count > 0) notifyUser(c, count);
            return Result.success();
        } catch (SecurityException e) {
            return Result.failure();
        } catch (Exception e) {
            return Result.retry();
        }
    }
    private void notifyUser(Context c, int count) {
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Sentinel periodic reviews", NotificationManager.IMPORTANCE_DEFAULT));
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sentinel AI: review recommended")
            .setContentText(count + " visible apps have multiple selected sensitive permission declarations.")
            .setStyle(new NotificationCompat.BigTextStyle().bigText("Sentinel found " + count + " apps with multiple selected permission declarations. Open the app and review the evidence. This is not proof of spyware."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
        if (Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) nm.notify(7101, b.build());
    }
}
