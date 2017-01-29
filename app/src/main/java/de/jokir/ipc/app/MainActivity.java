package de.jokir.ipc.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.jokir.ipc.service.AIpcService;
import de.jokir.ipc.service.IpcSocket;

/**
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    LinearLayout chatHistory;
    EditText inputText;
    Button sendButton;

    IpcSocket ipcSocket;

    UiHandler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiHandler = new UiHandler();

        // get elements and add listener
        chatHistory = (LinearLayout) findViewById(R.id.layout_history);
        inputText = (EditText) findViewById(R.id.edit_send);
        sendButton = (Button) findViewById(R.id.btn_send);

        sendButton.setOnClickListener(sendClickListener);

        // create IPC socket and connect
        ipcSocket = new IpcSocket(this,
                EchoService.class,
                ipcListener);
        ipcSocket.connect();
    }

    View.OnClickListener sendClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String text = inputText.getText().toString();
            inputText.setText("");

            if (text != null && !text.isEmpty()) {
                ipcSocket.sendMessage(0, 0, text);
            }
        }
    };

    /**
     * Listener for IPC service callbacks.
     */
    IpcSocket.Listener ipcListener = new IpcSocket.Listener() {
        @Override
        public void onServiceConnected() {
            Log.d(TAG, "onServiceConnected");
            addMessageToHistory("Service connected");
        }

        @Override
        public void onServiceDisconnected() {
            Log.d(TAG, "onServiceDisconnected");
            addMessageToHistory("Service disconnected");
        }

        @Override
        public void onServiceDied() {
            Log.d(TAG, "onServiceDied");
            addMessageToHistory("Service died");
        }

        @Override
        public void onMessageReceived(int iArg1, int iArg2, @Nullable String sArg) {
            Log.d(TAG, "onMessageReceived");
            addMessageToHistory("received iArg1: " + iArg1 + ", iArg2: " + iArg2 + ", sArg:" + sArg);
        }
    };

    /**
     * Adds a message to the chat history.
     *
     * @param text text to add
     */
    private void addMessageToHistory(@NonNull String text) {
        Bundle data = new Bundle();
        data.putString(AIpcService.EXTRA_STRING_ARG, text);

        Message message = Message.obtain();
        message.setData(data);

        uiHandler.dispatchMessage(message);
    }

    /**
     * Handler to write incoming message to the chat history.
     */
    private class UiHandler extends Handler {

        UiHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message message) {
            Log.d(TAG, "UiHandler.handleMessage");

            String text = message.getData().getString(AIpcService.EXTRA_STRING_ARG);
            TextView tv = new TextView(MainActivity.this);
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
            chatHistory.addView(tv);
            chatHistory.invalidate();
        }
    }
}
