package com.khoa.carremotecontrol.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.khoa.carremotecontrol.databinding.ActivityCameraBinding;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerCSRT;
import org.opencv.tracking.TrackerMedianFlow;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements Handler.Callback {

    private ActivityCameraBinding mBinding;

    static final String TAG = "Loi";
    static final int MY_PERMISSIONS_REQUEST_CAMERA = 1242;
    private static final int MSG_CAMERA_OPENED = 1;
    private static final int MSG_SURFACE_READY = 2;
    private final Handler mHandler = new Handler(this);
    SurfaceView mSurfaceView;
    OverlayView mOverlayView;
    SurfaceHolder mSurfaceHolder;
    CameraManager mCameraManager;
    String[] mCameraIDsList;
    CameraDevice.StateCallback mCameraStateCB;
    CameraDevice mCameraDevice;
    CameraCaptureSession mCaptureSession;
    boolean mSurfaceCreated = true;
    boolean mIsCameraConfigured = false;
    private Surface mCameraSurface = null;
    private ImageReader mImageReader;
    private final Size mCamResolution = new Size(1280, 720);
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Point[] mPoints = new Point[2];
    private Mat mCurrentMat;
    private Tracker mTracker;

    private boolean isLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        this.mSurfaceView = mBinding.surfaceview;
        this.mOverlayView = mBinding.overlayView;
        this.mSurfaceHolder = this.mSurfaceView.getHolder();
        this.mSurfaceHolder.addCallback(sufaceHolderCallback);
        this.mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        try {
            mCameraIDsList = this.mCameraManager.getCameraIdList();
            for (String id : mCameraIDsList) {
                Log.e(TAG, "CameraID: " + id);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 2; i++) {
            mPoints[i] = new Point(0, 0);
        }

        mCameraStateCB = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Toast.makeText(getApplicationContext(), "onOpened", Toast.LENGTH_SHORT).show();

                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                Toast.makeText(getApplicationContext(), "onDisconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Toast.makeText(getApplicationContext(), "onError", Toast.LENGTH_SHORT).show();
            }
        };

        setupOverlayView();
        setupButton();
    }

    private void setupButton() {
        mBinding.btnLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isLocked) {
                    int dx = Math.abs(mPoints[0].x - mPoints[1].x);
                    int dy = Math.abs(mPoints[0].y - mPoints[1].y);
                    if (dx >= 0 && dy >= 0) {
                        int minX = (int) ((float) Math.min(mPoints[0].x, mPoints[1].x) / mOverlayView.getWidth() * mCurrentMat.cols());
                        int minY = (int) ((float) Math.min(mPoints[0].y, mPoints[1].y) / mOverlayView.getHeight() * mCurrentMat.rows());
                        int maxX = (int) ((float) Math.max(mPoints[0].x, mPoints[1].x) / mOverlayView.getWidth() * mCurrentMat.cols());
                        int maxY = (int) ((float) Math.max(mPoints[0].y, mPoints[1].y) / mOverlayView.getHeight() * mCurrentMat.rows());

                        mTracker = TrackerMedianFlow.create();
                        Rect2d initRectangle = new Rect2d(minX, minY, maxX - minX, maxY - minY);
                        Mat imageGrabInit = new Mat();
                        mCurrentMat.copyTo(imageGrabInit);
                        mTracker.init(imageGrabInit, initRectangle);

                        isLocked = true;
                    }
                }
            }
        });

        mBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLocked) {
                    isLocked = false;
                }
                mPoints[0].x = 0;
                mPoints[0].y = 0;
                mPoints[1].x = 0;
                mPoints[1].y = 0;
                mOverlayView.postInvalidate();
            }
        });
    }

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            final Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }

            ByteBuffer bb = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[bb.remaining()];
            bb.get(data);
            mCurrentMat = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
            org.opencv.core.Core.transpose(mCurrentMat, mCurrentMat);
            org.opencv.core.Core.flip(mCurrentMat, mCurrentMat, 1);
            org.opencv.imgproc.Imgproc.resize(mCurrentMat, mCurrentMat, new org.opencv.core.Size(240, 320));

            if (isLocked) {
                process();
            }
            image.close();
        }
    };

    private void process() {
        if (isLocked) {
            org.opencv.core.Rect2d trackingRectangle = new org.opencv.core.Rect2d(0, 0, 1, 1);
            mTracker.update(mCurrentMat, trackingRectangle);
            mPoints[0].x = (int) (trackingRectangle.x * (float) mOverlayView.getWidth() / (float) mCurrentMat.cols());
            mPoints[0].y = (int) (trackingRectangle.y * (float) mOverlayView.getHeight() / (float) mCurrentMat.rows());
            mPoints[1].x = mPoints[0].x + (int) (trackingRectangle.width * (float) mOverlayView.getWidth() / (float) mCurrentMat.cols());
            mPoints[1].y = mPoints[0].y + (int) (trackingRectangle.height * (float) mOverlayView.getHeight() / (float) mCurrentMat.rows());

            mOverlayView.postInvalidate();
        }
    }

    private void setupOverlayView() {
        mOverlayView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                final int X = (int) event.getX();
                final int Y = (int) event.getY();
//                Log.e("Loi", Integer.toString(X) + " : " + Integer.toString(Y));
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!isLocked) {
                            mPoints[0].x = X;
                            mPoints[0].y = Y;
                            mPoints[1].x = X;
                            mPoints[1].y = Y;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    case MotionEvent.ACTION_MOVE & MotionEvent.ACTION_CANCEL:
                        if (!isLocked) {
                            mPoints[1].x = X;
                            mPoints[1].y = Y;
                        }
                        break;
                }
                if (!isLocked) mOverlayView.postInvalidate();
                return true;
            }
        });

        mOverlayView.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                Paint paint = new Paint();
                paint.setColor(Color.rgb(0, 0, 255));
                paint.setStrokeWidth(10);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(mPoints[0].x, mPoints[0].y, mPoints[1].x, mPoints[1].y, paint);
                    /*
                    if (mDrawing == Drawing.TRACKING && mShowCordinate == true) {
                        paint.setColor(Color.rgb(0, 255, 0));
                        canvas.drawLine((mPoints[0].x + mPoints[1].x) / 2,
                                0,
                                (mPoints[0].x + mPoints[1].x) / 2,
                                mTrackingOverlay.getHeight(),
                                paint);
                        canvas.drawLine(0,
                                (mPoints[0].y + mPoints[1].y) / 2,
                                mTrackingOverlay.getWidth(),
                                (mPoints[0].y + mPoints[1].y) / 2,
                                paint);
                        paint.setColor(Color.YELLOW);
                        paint.setStrokeWidth(2);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTextSize(30);
                        String strX = Integer.toString((mPoints[0].x + mPoints[1].x) / 2) + "/" + Integer.toString(mTrackingOverlay.getWidth());
                        String strY = Integer.toString((mPoints[0].y + mPoints[1].y) / 2) + "/" + Integer.toString(mTrackingOverlay.getHeight());
                        canvas.drawText(strX, (mPoints[0].x + mPoints[1].x) / 4, (mPoints[0].y + mPoints[1].y) / 2 - 10, paint);
                        canvas.save();
                        canvas.rotate(90, (mPoints[0].x + mPoints[1].x) / 2 + 10, (mPoints[0].y + mPoints[1].y) / 4);
                        canvas.drawText(strY, (mPoints[0].x + mPoints[1].x) / 2 + 10, (mPoints[0].y + mPoints[1].y) / 4, paint);
                        canvas.restore();
                    }

                     */
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        //requesting permission
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                Toast.makeText(getApplicationContext(), "request permission", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "PERMISSION_ALREADY_GRANTED", Toast.LENGTH_SHORT).show();
            try {
                mCameraManager.openCamera(mCameraIDsList[0], mCameraStateCB, new Handler());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CAMERA_OPENED:
            case MSG_SURFACE_READY:
                if (mSurfaceCreated && (mCameraDevice != null) && !mIsCameraConfigured) {
                    configureCamera();
                }
                break;
        }

        return true;
    }

    private void configureCamera() {
        mImageReader = ImageReader.newInstance(mCamResolution.getWidth(), mCamResolution.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
        // prepare list of surfaces to be used in capture requests
        List<Surface> sfl = new ArrayList<Surface>();

        sfl.add(mCameraSurface); // surface for viewfinder preview
        sfl.add(mImageReader.getSurface());
        // configure camera with all the surfaces to be ever used
        try {
            mCameraDevice.createCaptureSession(sfl, sessionStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mIsCameraConfigured = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    try {
                        mCameraManager.openCamera(mCameraIDsList[0], mCameraStateCB, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                break;
        }
    }


    private SurfaceHolder.Callback sufaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mCameraSurface = holder.getSurface();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            holder.setFixedSize(mCamResolution.getWidth(), mCamResolution.getHeight());
            mCameraSurface = holder.getSurface();
            mSurfaceCreated = true;
            mHandler.sendEmptyMessage(MSG_SURFACE_READY);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mSurfaceCreated = false;
        }
    };

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigureFailed(final CameraCaptureSession session) {
            Log.e(TAG, "CaptureSessionConfigure failed");
        }

        @Override
        public void onConfigured(final CameraCaptureSession session) {
            Log.e(TAG, "CaptureSessionConfigure onConfigured");
            mCaptureSession = session;

            try {
                CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(mCameraSurface);
                previewRequestBuilder.addTarget(mImageReader.getSurface());
                mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    };


    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (!OpenCVLoader.initDebug()) {
            Log.e("Log", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBackgroundThread();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (mCaptureSession != null) {
                mCaptureSession.stopRepeating();
                mCaptureSession.close();
                mCaptureSession = null;
            }

            mIsCameraConfigured = false;
        } catch (final CameraAccessException e) {
            // Doesn't matter, cloising device anyway
            e.printStackTrace();
        } catch (final IllegalStateException e2) {
            // Doesn't matter, cloising device anyway
            e2.printStackTrace();
        } finally {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
                mCaptureSession = null;
            }
        }
    }

}