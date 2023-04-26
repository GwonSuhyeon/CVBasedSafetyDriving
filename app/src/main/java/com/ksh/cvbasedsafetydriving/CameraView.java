/*
 * Create by KSH on 2020. 8. 16.
 * Copyright (c) 2020. KSH. All rights reserved.
 */

package com.ksh.cvbasedsafetydriving;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static org.opencv.imgproc.Imgproc.ellipse;

public class CameraView extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;

    private CameraBridgeViewBase mOpenCvCameraView;

    private TextView distance_tv = null;

    public static TextView bicycleRiskView = null;
    public static TextView roadRiskView = null;
    public static TextView roadRiskGrade = null;
    public static TextView driveState = null;
    public static ImageView waitView = null;

    private Button stopBtn = null;

    //public static View view = null;

    //public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    public native long loadCascade(String cascadeFileName );
    public native int detect(long cascadeClassifier_face,
                             long cascadeClassifier_body,
                              long matAddrInput, long matAddrResult);
    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_body = 0;

    private boolean isBound;
    private GpsService mGpsService;

    //private boolean running = false;
    private boolean running = true; // 지워야함

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            try
            {
                mGpsService = ((GpsService.ReturnBinder)service).getService();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className)
        {

        }
    };

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }
    }

    private void read_cascade_file(){
        copyFile("haarcascade_frontalface_default.xml");
        copyFile("haarcascade_fullbody.xml");

        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_face = loadCascade( "haarcascade_frontalface_default.xml");

        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_body = loadCascade( "haarcascade_fullbody.xml");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HomeActivity.opencvProgressDialog.dismiss();

        //view = new View(CameraView.this);

        setContentView(R.layout.activity_camera_view);

        bicycleRiskView = (TextView)findViewById(R.id.bicycleRiskView);
        roadRiskView = (TextView)findViewById(R.id.roadRiskView);
        roadRiskGrade = (TextView)findViewById(R.id.roadRiskGrade);
        driveState = (TextView)findViewById(R.id.driveState);
        waitView = (ImageView)findViewById(R.id.waitView);

        stopBtn = (Button)findViewById(R.id.stopBtn);

        waitView.setVisibility(View.INVISIBLE); // 지워야함
        stopBtn.setVisibility(View.GONE); // 지워야함

        //bicycleRiskView.setVisibility(View.INVISIBLE);
        //roadRiskView.setVisibility(View.INVISIBLE);
        //roadRiskGrade.setVisibility(View.INVISIBLE);
        //driveState.setVisibility(View.INVISIBLE);

        // GPS 저장 Service 실행
        isBound = bindService(new Intent(CameraView.this, GpsService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        //mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)


        distance_tv = findViewById(R.id.distance_test_tv);
        //distance_tv.setVisibility(View.INVISIBLE); // 주석 지워야함

        stopBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        HomeActivity.bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() { //데이터 수신
            public void onDataReceived(byte[] data, String message) {
                Log.d("TouchState", "value : " + message);

                if(message.equals("1"))
                {
                    waitView.setVisibility(View.INVISIBLE);
                    stopBtn.setVisibility(View.GONE);

                    /*
                    bicycleRiskView.setVisibility(View.VISIBLE);
                    roadRiskView.setVisibility(View.VISIBLE);
                    roadRiskGrade.setVisibility(View.VISIBLE);
                    driveState.setVisibility(View.VISIBLE);
                     */

                    running = true;
                }
                else
                {
                    waitView.setVisibility(View.VISIBLE);
                    stopBtn.setVisibility(View.VISIBLE);

                    /*
                    bicycleRiskView.setVisibility(View.INVISIBLE);
                    roadRiskView.setVisibility(View.INVISIBLE);
                    roadRiskGrade.setVisibility(View.INVISIBLE);
                    driveState.setVisibility(View.INVISIBLE);
                     */
                }
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if(isBound)
        {
            unbindService(mServiceConnection);
            isBound = false;
        }

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        HomeActivity.bt.stopService();
    }

    //@Override
    public void onCameraViewStarted(int width, int height) {

    }

    //@Override
    public void onCameraViewStopped() {

    }

    //@Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if(running == true)
        {
            stopBtn.setEnabled
            matInput = inputFrame.rgba();

            if ( matResult == null )
                matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

            //ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
            //Core.flip(matInput, matInput, 1); // 영상 180도 반전

            int distance = 0;

            distance = detect(cascadeClassifier_face, cascadeClassifier_body, matInput.getNativeObjAddr(),
                    matResult.getNativeObjAddr());

            if (distance == 1000) {
                //0 face
                ((HomeActivity)HomeActivity.mContext).goToArduinoSafety();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        driveState.setTextColor(Color.GREEN);
                    }
                });
            }
            else {
                Log.d("DistanceLog", "Min Distance : " + distance);
                final int finalDistance = distance;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        distance_tv.setText(String.valueOf(finalDistance));

                        //범위 안
                        if (finalDistance < 300) {
                            distance_tv.setBackgroundColor(Color.RED);
                            //Toast.makeText(CameraView.this, "OOOPPPPPPSSSS!!!!", Toast.LENGTH_SHORT).show();

                            ((HomeActivity)HomeActivity.mContext).goToArduino();

                            driveState.setTextColor(Color.RED);

                            mGpsService.locationKnown();
                        }
                        //범위 밖
                        else {
                            distance_tv.setBackgroundColor(Color.GREEN);

                            ((HomeActivity)HomeActivity.mContext).goToArduinoSafety();

                            driveState.setTextColor(Color.GREEN);
                        }
                    }
                });

            }

            //running = false; // 주석 지워야함
        }

        return matResult;
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();

                read_cascade_file();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( CameraView.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }
}