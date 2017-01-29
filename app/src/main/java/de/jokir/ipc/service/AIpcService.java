package de.jokir.ipc.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Log;

/**
 *
 */
public abstract class AIpcService extends Service {

    private static final String TAG = "AIpcService";

    /** Action value in intent to start service. */
    public static final String ACTION_START = "de.jokir.ipc.service.ACTION_START";
    /** Action value in intent to stop service. */
    public static final String ACTION_STOP = "de.jokir.ipc.service.ACTION_STOP";

    /** What field in message where the replyTo field is set. */
    public static final int WHAT_CONNECT = 0;
    /** What field in message forward to derived class. */
    public static final int WHAT_MESSAGE = 1;
    /** Extra field in message if string value is set. */
    public static final String EXTRA_STRING_ARG = "de.jokir.ipc.service.EXTRA_STRING_ARG";

    /** Start ID of service, required to stop. */
    private int startId;

    private final Messenger localMessenger = new Messenger(new IncomingHandler());
    private Messenger remoteMessenger = null;

    private boolean isConnected = false;

    @Override
    public final int onStartCommand(@NonNull Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand");
        switch (intent.getAction()) {
            case ACTION_START:
                this.startId = startId;
                Notification notification = onServiceStarted();
                if (notification != null) {
                    startForeground(startId, notification);
                }
                break;

            case ACTION_STOP:
                isConnected = false;
                onServiceStopped();
                killService();
                break;

            default:
                onIntentReceived(intent);
        }

        return START_STICKY;
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return localMessenger.getBinder();
    }

    private final class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message message) {

            switch (message.what) {
                case WHAT_CONNECT:
                    remoteMessenger = message.replyTo;

                    try {
                        remoteMessenger.getBinder().linkToDeath(deathRecipient, 0);
                        isConnected = true;
                        onActivityConnected();
                    } catch (RemoteException e) {
                        onActivityDied();
                    }
                    break;

                case WHAT_MESSAGE:
                    int iArg1 = message.arg1;
                    int iArg2 = message.arg2;
                    String sArg = null;

                    Bundle data = message.getData();
                    if (data != null) {
                        sArg = data.getString(EXTRA_STRING_ARG, null);
                    }

                    onMessageReceived(iArg1, iArg2, sArg);
                    break;

                default:
            }

        }
    }

    private final IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            isConnected = false;
            onActivityDied();
        }
    };


    /**
     * Returns true if activity is connected, else false.
     *
     * @return true if activity is connected, else false
     */
    public final boolean isConnected() {
        return isConnected;
    }

    /**
     * Stops the service.
     */
    protected final void killService() {
        stopSelf(startId);
    }

    /**
     * Sends a message to the Activity. Returns true if send, else false.
     *
     * @param iArg1 int argument 1
     * @param iArg2 int argument 2
     * @param sArg  string argument
     * @return true if sent, else false
     */
    public final boolean sendMessage(int iArg1, int iArg2, @Nullable String sArg) {
        // if not connected return always false
        if (!isConnected) {
            return false;
        }

        // create message and set values
        Message message = Message.obtain();
        message.what = AIpcService.WHAT_MESSAGE;
        message.arg1 = iArg1;
        message.arg2 = iArg2;
        // set string arg only if present
        if (sArg != null) {
            Bundle data = new Bundle();
            data.putString(AIpcService.EXTRA_STRING_ARG, sArg);
            message.setData(data);
        }

        // send message and return if succeeded
        try {
            remoteMessenger.send(message);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*##############################################################################################
     * ABSTRACT METHODS FOR DERIVING CLASS
     * ########################################################################################## */

    /**
     * Service received start command and will start.
     * If service should be started in foreground a {@Link Notification} must be returned.
     *
     * @return notification if service should be started in foreground
     */
    @Nullable
    protected abstract Notification onServiceStarted();

    /**
     * Service received stop command and will stop itself.
     */
    protected abstract void onServiceStopped();

    /**
     * Activity connected, ready to send messages to activity.
     */
    protected abstract void onActivityConnected();

    /**
     * Activity process died.
     */
    protected abstract void onActivityDied();

    /**
     * Received message from activity.
     *
     * @param iArg1 int argument 1
     * @param iArg2 int argument 2
     * @param sArg  string argument
     */
    protected abstract void onMessageReceived(int iArg1, int iArg2, @Nullable String sArg);

    /**
     * Received an intent.
     *
     * @param intent received intent
     */
    protected abstract void onIntentReceived(@NonNull Intent intent);
}
