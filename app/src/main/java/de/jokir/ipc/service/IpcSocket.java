package de.jokir.ipc.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 *
 */
public class IpcSocket {

    private static final String TAG = "IpcSocket";

    private final Context context;
    private final Class<? extends AIpcService> serviceClass;

    private Messenger localMessenger = new Messenger(new IncomingHandler());
    private Messenger remoteMessenger;
    boolean isConnected = false;
    final Listener listener;

    public IpcSocket(@NonNull Context context,
                     @NonNull Class<? extends AIpcService> serviceClass,
                     @NonNull Listener listener) {
        this.context = context;
        this.serviceClass = serviceClass;
        this.listener = listener;
    }


    public void connect() {
        if (!isConnected) {
            Log.d(TAG, "connect service");
            // start intent
            Intent startIntent = new Intent(context, serviceClass);
            startIntent.setAction(AIpcService.ACTION_START);
            context.startService(startIntent);

            // bind intent
            Intent bindIntent = new Intent(context, serviceClass);
            context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public final void disconnect() {
        if (isConnected) {
            Log.d(TAG, "disconnect service");
            // unbind
            context.unbindService(serviceConnection);

            // stop intent
            Intent intent = new Intent(context, serviceClass);
            intent.setAction(AIpcService.ACTION_STOP);
            context.startService(intent);
        }
    }

    public final boolean isServiceConnected() {
        return isConnected;
    }

    /**
     * Sends a message to the service. Returns true if send, else false.
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

        Log.d(TAG, "sendMessage");

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

    /**
     * Sends an intent to the service, overrides the class to the set service class.
     * If Action equals {@Link AIpcService.ACTION_START} or {@Link AIpcService.ACTION_STOP}
     * the intent is ignored.
     *
     * @param intent intent to send
     */
    public final void sendIntent(@NonNull Intent intent) {
        if (!intent.getAction().equals(AIpcService.ACTION_START) &&
                !intent.getAction().equals(AIpcService.ACTION_STOP)) {

            intent.setClass(context, serviceClass);
            context.startService(intent);
        }
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            isConnected = true;
            remoteMessenger = new Messenger(service);

            try {
                // listener to get notified when service is killed
                service.linkToDeath(deathRecipient, 0);

                // send connect message to service so service can message back
                Message message = Message.obtain();
                message.what = AIpcService.WHAT_CONNECT;
                message.replyTo = localMessenger;
                remoteMessenger.send(message);
                listener.onServiceConnected();
            } catch (RemoteException e) {
                e.printStackTrace();
                listener.onServiceDied();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isConnected = false;
            listener.onServiceDisconnected();
        }
    };

    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            isConnected = false;
            listener.onServiceDied();
        }
    };


    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message message) {

            switch (message.what) {
                case AIpcService.WHAT_MESSAGE:
                    int iArg1 = message.arg1;
                    int iArg2 = message.arg2;
                    String sArg = null;

                    Bundle data = message.getData();
                    if (data != null) {
                        sArg = data.getString(AIpcService.EXTRA_STRING_ARG, null);
                    }

                    listener.onMessageReceived(iArg1, iArg2, sArg);
                    break;

                default:
            }

        }
    }

    /**
     * Listener for events occurring with the service.
     */
    public interface Listener {

        /**
         * Connection to service was established.
         */
        void onServiceConnected();

        /**
         * Connection to service was cleared.
         */
        void onServiceDisconnected();

        /**
         * Service died. Probably process was killed.
         */
        void onServiceDied();

        /**
         * Received a message from the service.
         *
         * @param iArg1 int argument 1
         * @param iArg2 int argument 2
         * @param sArg  string argument
         */
        void onMessageReceived(int iArg1, int iArg2, @Nullable String sArg);
    }
}
