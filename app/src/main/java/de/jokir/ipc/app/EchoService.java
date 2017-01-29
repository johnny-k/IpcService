package de.jokir.ipc.app;

import android.app.Notification;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import de.jokir.ipc.service.AIpcService;

/**
 *
 */
public class EchoService extends AIpcService {

    private static final String TAG = "EchoService";

    @Nullable
    @Override
    protected Notification onServiceStarted() {
        // return null to get normal service
        return null;
    }

    @Override
    protected void onServiceStopped() {

    }

    @Override
    protected void onActivityConnected() {

    }

    @Override
    protected void onActivityDied() {

    }

    @Override
    protected void onMessageReceived(int iArg1, int iArg2, @Nullable String sArg) {
        Log.d(TAG, "echo message");
        sendMessage(iArg1, iArg2, sArg);
    }

    @Override
    protected void onIntentReceived(@NonNull Intent intent) {

    }
}
