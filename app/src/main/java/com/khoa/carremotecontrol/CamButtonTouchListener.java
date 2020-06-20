package com.khoa.carremotecontrol;

import android.view.MotionEvent;
import android.view.View;

public class CamButtonTouchListener implements View.OnTouchListener {
    private UDPSocket udpSocket;
    private String signal;

    public CamButtonTouchListener(UDPSocket udpSocket, String signal) {
        this.udpSocket = udpSocket;
        this.signal = signal;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                udpSocket.sendSignal(signal);
                break;
            case MotionEvent.ACTION_UP:
                udpSocket.sendSignal(Signal.CAM_STILL);
                break;
            case MotionEvent.ACTION_CANCEL:
                udpSocket.sendSignal(Signal.CAM_STILL);
                break;
        }
        return false;
    }
}
