package com.khoa.carremotecontrol;

import android.view.MotionEvent;
import android.view.View;

public class DirectionButtonTouchListener implements View.OnTouchListener {

    private UDPSocket udpSocket;
    private String signal;

    public DirectionButtonTouchListener(UDPSocket udpSocket, String signal) {
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
                udpSocket.sendSignal(Signal.STOP);
                break;
            case MotionEvent.ACTION_CANCEL:
                udpSocket.sendSignal(Signal.STOP);
                break;
        }
        return false;
    }

}
