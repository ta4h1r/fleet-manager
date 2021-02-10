package com.ctrlrobotics.ctrl.slam;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sanbot.opensdk.base.BindBaseService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

public class ArduinoService extends BindBaseService {

    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;

    /**
     * Command to the service to display a message
     */
    static final int MSG_SAY_HELLO = 0;
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;
    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;
    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;

    /**
     * Command to service to ...
     */
    static final int MSG_FOO = 4;     // Test
    static final int MSG_START_ARDUINO_LISTENER = 5;
    static final int MSG_STOP_ARDUINO_LISTENER = 6;
    static final int MSG_SEND_TO_SERIAL = 7;
    static final int MSG_TV_DISPLAY = 8;
    static final int MSG_SET_BOOLEAN_VALUE = 9;
    static final int MSG_ARDUINO_DETACHED = 10;

    static final int ARG_SEND_0 = 0;
    static final int ARG_SEND_1 = 1;
    static final int ARG_DOOR_SWITCH_CLOSED = 2;


    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SAY_HELLO:
                    Toast.makeText(getApplicationContext(), "hello!", Toast.LENGTH_SHORT).show();
                    Map<String, Object> mp = new HashMap<String, Object>();
                    mp.put("item", 1);
                    Message msgFoo = Message.obtain(null, MSG_FOO, 0, 0, mp.get("item"));
                    sendMessageToAllAvailableClients(msgFoo);
                    break;
                case MSG_SET_VALUE:
                    mValue = msg.arg1;
                    sendMessageToAllAvailableClients(Message.obtain(null,
                            MSG_SET_VALUE, mValue, 0));
                    break;
                case MSG_START_ARDUINO_LISTENER:
                    arduinoHandler.post(() -> {
                        arduino.setArduinoListener(arduinoListener);
                    });
                    break;
                case MSG_STOP_ARDUINO_LISTENER:
                    arduinoHandler.post(() -> {
                        arduino.close();
                        arduino.unsetArduinoListener();
                    });
                    break;
                case MSG_SEND_TO_SERIAL:
                    switch(msg.arg1) {
                        case ARG_SEND_1:
                            arduino.send("1".getBytes());
                            break;
                        case ARG_SEND_0:
                            arduino.send("0".getBytes());
                            break;
                        default:
                            Log.e("Tai", TAG + ": handleMessage: unexpected argument");
                            break;
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendMessageToAllAvailableClients(Message message) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(message);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Handles Arduino Serial events
     */
    ArduinoListener arduinoListener = new ArduinoListener() {
        // We thread all of the callbacks in this listener because they slow down the main thread and mess with the sensor data
        @Override
        public void onArduinoAttached(UsbDevice device) {
            arduinoHandler.post(() -> {
                Log.d("Tai", TAG +  ": onArduinoAttached ");
                arduinoTvDisplay("Arduino attached");
                arduino.open(device);
            });
        }

        @Override
        public void onArduinoDetached() {
            arduinoHandler.post(() -> {
                arduino.close();
                Log.d("Tai", TAG + ": onArduinoDetached ");
                arduinoTvDisplay("Arduino detached");
                Message msg = Message.obtain(null, MSG_ARDUINO_DETACHED, 0, 0, null);
                sendMessageToAllAvailableClients(msg);
                stopSelf();
                Toast.makeText(getApplicationContext(), "ArduinoService stopped", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onArduinoMessage(byte[] bytes) {
            arduinoHandler.post(() -> {
                String arduinoMessage = new String(bytes);
                Log.d("Tai", TAG + ": onArduinoMessage: arduinoMessage: " + arduinoMessage);
                arduinoTvDisplay(">>> " + arduinoMessage );

                String trimmedArduinoMessage = arduinoMessage.trim();

                if(trimmedArduinoMessage.length() > 1) {
                    String onSubstring = trimmedArduinoMessage.substring(Math.max(trimmedArduinoMessage.length() - 3, 0));     // last three characters of the message
                    String offSubstring = trimmedArduinoMessage.substring(Math.max(trimmedArduinoMessage.length() - 4, 0));     // last four characters of the message

                    String mOnComparator = "mOn";
                    String mOffComparator = "mOff";

                    String pOnComparator = "pOn";
                    String pOffComparator = "pOff";

                    // Magnetic switch
                    if(onSubstring.equalsIgnoreCase(mOnComparator)) {
                        setDoorSwitchClosed(true);
                    } else if (offSubstring.equalsIgnoreCase(mOffComparator)) {
                        setDoorSwitchClosed(false);
                    }

                    // Push button switch
                    if(onSubstring.equalsIgnoreCase(pOnComparator)) {
                        arduino.send("0".getBytes());         // Sending zero to unlock
                    } else if (offSubstring.equalsIgnoreCase(pOffComparator)) {
                        arduino.send("1".getBytes());
                    }

                }
            });

        }

        @Override
        public void onArduinoOpened() {
            Log.d("Tai", TAG + ": onArduinoOpened ");
        }
        @Override
        public void onUsbPermissionDenied() {
            Log.d("Tai", TAG + ": onUsbPermissionDenied: ");
            arduinoTvDisplay("Permission denied... New attempt in 3 sec");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    arduino.reopen();
                }
            }, 3000);
        }
    };

    private Arduino arduino;
    private Handler arduinoHandler;

    private final String TAG = "ArduinoService";
    @Override
    public void onCreate() {
        register(ArduinoService.class);
        Log.i("Tai", TAG + ": onCreate");
        arduinoHandler = new Handler();
        arduino = new Arduino(this);
        arduinoTvDisplay("\n\n");
        super.onCreate();
    }

    private void arduinoTvDisplay(String s) {
        // Use bundle because non-parcelables cannot be marshalled across processes
        Bundle bundle  = new Bundle();
        bundle.putString("textViewMessage", s);
        Message msg = Message.obtain(null, MSG_TV_DISPLAY, 0, 0, bundle);
        sendMessageToAllAvailableClients(msg);
    }
    private void setDoorSwitchClosed(boolean doorSwitchClosed) {
        if (doorSwitchClosed) {
            Message msg = Message.obtain(null, MSG_SET_BOOLEAN_VALUE, ARG_DOOR_SWITCH_CLOSED, 1, null);
            sendMessageToAllAvailableClients(msg);
        } else {
            Message msg = Message.obtain(null, MSG_SET_BOOLEAN_VALUE, ARG_DOOR_SWITCH_CLOSED, 0, null);
            sendMessageToAllAvailableClients(msg);
        }
    }

    @Override
    public void onDestroy() {
        arduinoHandler.post(() -> {
            arduino.close();
            arduino.unsetArduinoListener();
        });
        Toast.makeText(this, "ArduinoService destroyed", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "ArduinoService started", Toast.LENGTH_SHORT).show();
        arduinoHandler.post(() -> {
            arduino.setArduinoListener(arduinoListener);
        });
        return START_STICKY;
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "Binding to remote service", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    protected void onMainServiceConnected() {

    }





}
