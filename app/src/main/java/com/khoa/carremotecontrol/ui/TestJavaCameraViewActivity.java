package com.khoa.carremotecontrol.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.khoa.carremotecontrol.R;
import com.khoa.carremotecontrol.databinding.ActivityTestJavaCameraViewBinding;

import org.opencv.android.OpenCVLoader;

public class TestJavaCameraViewActivity extends AppCompatActivity {

    private ActivityTestJavaCameraViewBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityTestJavaCameraViewBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

//        mBinding.camera.open();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("Loi", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mBinding.camera.close();
    }
}
