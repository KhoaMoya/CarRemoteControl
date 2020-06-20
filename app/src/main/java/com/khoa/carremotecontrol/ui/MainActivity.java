package com.khoa.carremotecontrol.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.khoa.carremotecontrol.R;
import com.khoa.carremotecontrol.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportFragmentManager().beginTransaction().replace(R.id.palce_holder, new ControlCarFragment()).commit();
    }
}
