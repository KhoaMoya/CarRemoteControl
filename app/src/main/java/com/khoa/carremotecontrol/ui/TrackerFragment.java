package com.khoa.carremotecontrol.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.khoa.carremotecontrol.R;
import com.khoa.carremotecontrol.databinding.FragmentTrackerBinding;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TrackerFragment extends Fragment {

    private FragmentTrackerBinding mBinding;
    private String cameraId;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CameraDevice cameraDevice;

    public TrackerFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentTrackerBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    private void createCameraPreview(){

    }

    private void openCamera(){
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        Log.e("Loi", "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
//            imageDimension = CamResolution;
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);

//                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                builder.setTitle(R.string.camera_permission_title);
//                builder.setMessage(R.string.camera_permission_message);
//                builder.setPositiveButton(android.R.string.ok,
//                        (dialog, which) -> requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION));
//                builder.show();
                Log.e("Loi", "Chưa cấp quyền camera");
                return;
            }
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
//            manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraOpenCloseLock.release();
            //This is called when the camera is open
            Log.e("Loi", "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e("Loi", "onDisconnected");
            cameraOpenCloseLock.release();
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e("Loi", "onError");
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void closeCamera() {

    }

    @Override
    public void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBinding = null;
    }
}
