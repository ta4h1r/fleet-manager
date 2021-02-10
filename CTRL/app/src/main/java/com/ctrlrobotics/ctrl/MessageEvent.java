package com.ctrlrobotics.ctrl;

import java.util.Map;

public class MessageEvent {
    public Map<String, Object> mMessage;

    public MessageEvent(Map<String, Object> message) {
        mMessage = message;
    }

    public Map<String, Object> getMessage() {
        return mMessage;
    }

}
