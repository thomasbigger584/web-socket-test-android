package com.twb.websocket;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.twb.LifecycleEvent;
import com.twb.client.StompMessage;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final WebSocketClient webSocketClient = WebSocketClient.getInstance();
    private SimpleAdapter mAdapter;
    private List<String> mDataSet = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private NumberPicker idNumberPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button connectButton = (Button) findViewById(R.id.connectButton);

        idNumberPicker = (NumberPicker) findViewById(R.id.idNumberPicker);
        idNumberPicker.setMinValue(0);
        idNumberPicker.setMaxValue(10000);

        int previousId = SharedPreferencesUtil.getPreviousId(this);
        idNumberPicker.setValue(previousId);
        setConnectButtonText(connectButton, previousId);

        idNumberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            setConnectButtonText(connectButton, newVal);
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mAdapter = new SimpleAdapter(mDataSet);
        mAdapter.setHasStableIds(true);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
    }

    public void connectStomp(View view) {
        hideKeyboard();
        int connectId = idNumberPicker.getValue();
        webSocketClient.connect(connectId, new WebSocketClient.WebSocketLifecycleListener() {
            @Override
            public void onOpened(LifecycleEvent lifecycleEvent) {
                SharedPreferencesUtil.saveId(MainActivity.this, connectId);
                updateIsConnectedUI(true);
                toast(lifecycleEvent.getType().name() + " (" + connectId + ")");
            }

            @Override
            public void onError(LifecycleEvent lifecycleEvent) {
                updateIsConnectedUI(false);
                toast(lifecycleEvent.getType().name());
            }

            @Override
            public void onClosed(LifecycleEvent lifecycleEvent) {
                updateIsConnectedUI(false);
                toast(lifecycleEvent.getType().name() + " (" + connectId + ")");
                clearAdapter();
            }

            @Override
            public void onMessage(StompMessage stompMessage) {
                String payload = stompMessage.getPayload();
                toast("Received " + payload);
                addItem(payload);
            }
        });
    }

    public void sendViaWebSocket(View view) {
        webSocketClient.sendViaWebSocket(new WebSocketClient.RequestListener() {
            @Override
            public void onSuccess() {
                toast("Successfully sent to WebSocket");
            }

            @Override
            public void onFailure(Throwable throwable) {
                toast(throwable.getMessage());
            }
        });
    }

    public void sendViaRest(View view) {
        webSocketClient.sendViaRest(new WebSocketClient.RequestListener() {
            @Override
            public void onSuccess() {
                toast("Successfully sent to REST");
            }

            @Override
            public void onFailure(Throwable throwable) {
                toast(throwable.getMessage());
            }
        });
    }

    public void disconnectStomp(View view) {
        webSocketClient.disconnect();
    }

    private void addItem(String payload) {
        mDataSet.add(payload);
        mAdapter.notifyDataSetChanged();
        mRecyclerView.smoothScrollToPosition(mDataSet.size() - 1);
    }

    private void clearAdapter() {
        mDataSet.clear();
        mAdapter.notifyDataSetChanged();
    }

    private void setConnectButtonText(Button connectButton, int newVal) {
        String initialText = getString(R.string.connect_stomp);
        if (newVal > 0) {
            String buttonText = String.format(Locale.getDefault(), "%s (%d)", initialText, newVal);
            connectButton.setText(buttonText);
        } else {
            connectButton.setText(initialText);
        }
    }

    private void updateIsConnectedUI(boolean isConnected) {
        if (isConnected) {
            setTitle(getString(R.string.app_name) + " (Connected)");
        } else {
            setTitle(getString(R.string.app_name));
        }
        idNumberPicker.setEnabled(!isConnected);
    }

    private void toast(String text) {
        Log.i(TAG, text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        webSocketClient.disconnect();
        super.onDestroy();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
