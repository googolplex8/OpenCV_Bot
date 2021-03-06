package com.example.smithe0644.opencvandroidbot;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import org.opencv.android.*;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener{

    OpenCVLoader OpenCvLoader = new OpenCVLoader();
    Camera camera;

    ArrayList<String> filePaths = new ArrayList<String>();

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("Hello World ", "OpenCV loaded");

                }break;
                default:
                {
                    super.onManagerConnected(status);
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Camera Permission", "Permission not granted, in onCreate");
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        camera = getCameraInstance();
        Log.d("created","fok");
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(0); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }



    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        return null;
    }




    @Override
    public void onResume(){
        super.onResume();
        camera = Camera.open(0);
        OpenCvLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6,this, loaderCallback);
    }

    String mCurrentPhotoPath;

    public void picture(View v){
        Log.d("dese nuts", "before");
        CapturePhoto();
        Log.d("dese nuts", "capture");
// Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//        }
    }


    private void CapturePhoto() {

        Log.d("kkkk","Preparing to take photo");
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        final int frontCamera = 1;
        final int backCamera=0;

        Camera.getCameraInfo(backCamera, cameraInfo);

//        try {
////            camera = Camera.open(backCamera);
//        } catch (RuntimeException e) {
//            Log.d("kkkk","Camera not available: " + 1);
////            camera = null;
//            //e.printStackTrace();
//        }
//        camera = getCameraInstance();
        Camera.Parameters parameters = camera.getParameters();

        parameters.setJpegQuality(100);
        parameters.setJpegThumbnailQuality(100);
        parameters.setRotation(90);
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setExposureCompensation(0);
        parameters.setPictureFormat(ImageFormat.JPEG);

        camera.setParameters(parameters);
        try {
            if (camera == null) {
                Log.d("kkkk","Could not get camera instance");
            } else {
                Log.d("kkkk","Got the camera, creating the dummy surface texture");
                try {
                    camera.setPreviewTexture(new SurfaceTexture(0));
                    camera.startPreview();
                } catch (Exception e) {
                    Log.d("kkkk","Could not set the surface preview texture");
                    e.printStackTrace();
                }

                Log.d("quality ", String.valueOf(camera.getParameters().getJpegQuality()));
                camera.takePicture(null,null, new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                        File pictureFileDir=new File(root);

                        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                            pictureFileDir.mkdirs();
                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
                        String date = dateFormat.format(new Date());
                        String photoFile = "Picture" + "_" + date + ".jpg";
                        String filename = pictureFileDir.getPath() + File.separator + photoFile;

                        filePaths.add(filename);

                        File mainPicture = new File(filename);
                        try {
                            FileOutputStream fos = new FileOutputStream(mainPicture);
                            fos.write(data);
                            fos.close();
                            Log.d("kkkk","image saved");

                            Mat src = Imgcodecs.imread("/Phone/Pictures/" + filename);

                        } catch (Exception error) {
                            Log.d("kkkk","Image could not be saved");
                        }
//                        camera.release();
                    }
                });
            }
        } catch (Exception e) {
            camera.release();
        }

    }


    @Override
    public void onPause(){
        super.onPause();
        camera.release();
    }

}