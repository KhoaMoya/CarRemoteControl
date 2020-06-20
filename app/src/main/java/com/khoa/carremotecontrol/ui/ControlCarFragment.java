package com.khoa.carremotecontrol.ui;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.khoa.carremotecontrol.CamButtonTouchListener;
import com.khoa.carremotecontrol.DirectionButtonTouchListener;
import com.khoa.carremotecontrol.Signal;
import com.khoa.carremotecontrol.UDPSocket;
import com.khoa.carremotecontrol.databinding.FragmentCameraBinding;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;


public class ControlCarFragment extends Fragment{

    private FragmentCameraBinding mBinding;
    private WebSocketClient mWebSocketClient;
    private UDPSocket mUDPSocket;
    private boolean mIsConnected = false;
    private String mCarIdAddress = "192.168.0.106";
    private int mUdpPort = 6868;
    private int mWebPort = 86;
    private boolean isLedOn = false;
    private boolean isStreamOn = false;

    public ControlCarFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentCameraBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        connectWebSocket();
        setupDirectionButton();
        setupCamButton();

        mBinding.toggleLed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isLedOn) {
                    mUDPSocket.sendSignal(Signal.LED_OFF);
                } else {
                    mUDPSocket.sendSignal(Signal.LED_ON);
                }
                isLedOn = !isLedOn;
            }
        });

        mBinding.toggleStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isStreamOn) {
                    mUDPSocket.sendSignal(Signal.STREAM_OFF);
                } else {
                    mUDPSocket.sendSignal(Signal.STREAM_ON);
                }
                isStreamOn = !isStreamOn;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDirectionButton(){
        mBinding.btnForward.setOnTouchListener(new DirectionButtonTouchListener(mUDPSocket, Signal.FORWARD));
        mBinding.btnBackward.setOnTouchListener(new DirectionButtonTouchListener(mUDPSocket, Signal.BACKWARD));
        mBinding.btnRight.setOnTouchListener(new DirectionButtonTouchListener(mUDPSocket, Signal.RIGHT));
        mBinding.btnLeft.setOnTouchListener(new DirectionButtonTouchListener(mUDPSocket, Signal.LEFT));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupCamButton(){
        mBinding.btnCamUp.setOnTouchListener(new CamButtonTouchListener(mUDPSocket, Signal.CAM_UP));
        mBinding.btnCamDown.setOnTouchListener(new CamButtonTouchListener(mUDPSocket, Signal.CAM_DOWN));
        mBinding.btnCamRight.setOnTouchListener(new CamButtonTouchListener(mUDPSocket, Signal.CAM_RIGHT));
        mBinding.btnCamLeft.setOnTouchListener(new CamButtonTouchListener(mUDPSocket, Signal.CAM_LEFT));
    }

    private void connectWebSocket() {
        mUDPSocket = new UDPSocket(mCarIdAddress, mUdpPort);
        try {
            mWebSocketClient = new WebSocketClient(new URI("ws://" + mCarIdAddress + ":" + mWebPort + "/")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    if (!mIsConnected) {
                        mIsConnected = true;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBinding.txtStatus.setText("Đã kết nối");
                            }
                        });
                    }
                }

                @Override
                public void onMessage(String message) {

                }

                @Override
                public void onMessage(final ByteBuffer byteBuffer) {
                    super.onMessage(byteBuffer);
                    streamVideo(byteBuffer);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.e("Loi", "on close");
                    mIsConnected = false;
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                    Log.e("Loi", "on error");
                    mIsConnected = false;
                }
            };
            mWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
            mIsConnected = false;
        }
    }

    private void streamVideo(final ByteBuffer byteBuffer){
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                byte[] bArr = new byte[byteBuffer.remaining()];
                byteBuffer.get(bArr);
                Bitmap decodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length);
                if (decodeByteArray != null) {
                    int width = mBinding.imgStream.getWidth();
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90.0f);
                    Bitmap createBitmap = Bitmap.createBitmap(decodeByteArray, 0, 0, decodeByteArray.getWidth(), decodeByteArray.getHeight(), matrix, true);
                    int height = (int) (((float) width) * (((float) createBitmap.getHeight()) / ((float) createBitmap.getWidth())));
                    mBinding.imgStream.setImageBitmap(Bitmap.createScaledBitmap(createBitmap, width, height, false));
                    // proccessing
                }
            }
        });
    }

}
