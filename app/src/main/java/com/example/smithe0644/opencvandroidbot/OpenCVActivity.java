package com.example.smithe0644.opencvandroidbot;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OpenCVActivity extends Activity
        implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies() {
        try {
//            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                Log.d("Buffer: ", buffer.toString());
            }
            is.close();
            os.close();
            // Load the cascade classifier
            String path = mCascadeFile.getAbsolutePath();

            Log.d("information","here");
            Log.d("Path: ", path);

            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            cascadeClassifier.load(mCascadeFile.getAbsolutePath());
            if(cascadeClassifier.empty()){
                Log.d("cascadeClassifier", "is empty");
            }else Log.d("cascadeClassifier", "not empty");

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        // And we are ready to go
        openCvCameraView.enableView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        openCvCameraView = new JavaCameraView(this, -1);
        setContentView(openCvCameraView);
        openCvCameraView.setCvCameraViewListener(this);

        
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {

//        Mat aInputFrame = rotate(InputFrame, 90);

        // Create a grayscale image

//        Imgproc.cvtColor(aInputFrame, aInputFrame, Imgproc.COLOR_RGBA2GRAY);


        MatOfRect faces = new MatOfRect();

        // Use the classifier to detect faces
        if (cascadeClassifier != null) {
//            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 0,null,
//                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            cascadeClassifier.detectMultiScale(aInputFrame, faces);
        }


        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();

        if(facesArray.length>=1) Log.d("face found", "we found a face mdudes");

        for (int i = 0; i <facesArray.length; i++)
            Imgproc.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);

//        output(aInputFrame);
//        aInputFrame = rotate(InputFrame, 270);

        return aInputFrame;
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }


//    public void output(Mat subimg){
//
//        Bitmap bmp = null;
//        try {
//            bmp = Bitmap.createBitmap(subimg.cols(), subimg.rows(), Bitmap.Config.RGB_565);
//            Utils.matToBitmap(subimg, bmp);
//        } catch (CvException e) {
//            Log.d("didn't work", e.getMessage());
//        }
//
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//
//
//        byte[] byteArray = stream.toByteArray();
//
//
//        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
//        File pictureFileDir=new File(root);
//
//        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
//            pictureFileDir.mkdirs();
//        }
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
//        String date = dateFormat.format(new Date());
//        String photoFile = "Frame with no rectangle" + "_" + date + ".jpeg";
//        String filename = pictureFileDir.getPath() + File.separator + photoFile;
//
//
//        File mainPicture = new File(filename);
//        try {
//            FileOutputStream fos = new FileOutputStream(mainPicture);
//            fos.write(byteArray);
//            fos.close();
//            Log.d("kkkk","image saved");
//        } catch (Exception error) {
//            Log.d("kkkk","Image could not be saved");
//        }
//
//    }


    public static Mat rotate(Mat src, double angle)
    {
        Mat dst = new Mat();
        if(angle == 180 || angle == -180) {
            Core.flip(src, dst, -1);
        } else if(angle == 90 || angle == -270) {
            Core.flip(src.t(), dst, 1);
        } else if(angle == 270 || angle == -90) {
            Core.flip(src.t(), dst, 0);
        }

        Imgproc.resize(dst, dst, src.size());


        return dst;
    }

}