package com.example.smithe0644.opencvandroidbot;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.*;
import org.opencv.core.Mat;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener{

    OpenCVLoader OpenCvLoader = new OpenCVLoader();

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
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        return null;
    }
    //hello world

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCvLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6,this, loaderCallback);
    }

}