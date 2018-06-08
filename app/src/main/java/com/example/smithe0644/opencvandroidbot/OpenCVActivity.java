package com.example.smithe0644.opencvandroidbot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.ParcelFileDescriptor;


public class OpenCVActivity extends Activity
        implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;

    private Mat grayscaleImage;

    private int height;
    private int width;

    IntentFilter usbFilter;

    Boolean rP;

    UsbAccessory megaADK;
    UsbManager usbManager;
    FileInputStream iS;
    FileOutputStream oS;
    ParcelFileDescriptor fileDescriptor;

    private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";


    //Stands for AVG x
    private double lastAVGx = 0;

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

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    //Theres only 1 accessory
                    UsbAccessory accessory = usbManager.getAccessoryList()[0];
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        megaADK = accessory;
                        setUp(megaADK);
                        Toast.makeText(context,"Permission Granted", Toast.LENGTH_SHORT);
                    } else {
                        Toast.makeText(context,"Permission denied", Toast.LENGTH_SHORT);

                    }
                    rP = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {

                UsbAccessory accessory = usbManager.getAccessoryList()[0];
                if(accessory == null) Log.d("Detached", "accessory no longer findable");
                Toast.makeText(context,"Accessory is detached", Toast.LENGTH_SHORT);
                oS = null;
                iS = null;
//              if (accessory != null && accessory.equals(megaADK)) {
//                    closeAccessory();
//              }
            }else if(UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)){
                UsbAccessory accessory = usbManager.getAccessoryList()[0];
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    megaADK = accessory;
                    setUp(megaADK);

                } else {
                    Log.d("MEGA ADK ", "Permission denied" + accessory);
                }
                rP = false;
            }
        }
    };

    private void initializeOpenCVDependencies() {
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "cascade.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                Log.d("Buffer: ", buffer.toString());
            }
            is.close();
            os.close();

            //Writes it into the file mCascadeFile

            // Load the cascade classifier

            String path = mCascadeFile.getAbsolutePath();

            cascadeClassifier = new CascadeClassifier(path);
            cascadeClassifier.load(path);
            if(cascadeClassifier.empty()){
                Log.d("cascadeClassifier", "is empty");
            }else Log.d("cascadeClassifier", "not empty");

            cascadeDir.delete();

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        //Enables view in UI
        openCvCameraView.enableView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
        usbFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        openCvCameraView = new JavaCameraView(this, -1);
        setContentView(openCvCameraView);
        openCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        // The faces will be a 20% of the height of the screen
        this.height = height;
        this.width = width;
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

            cascadeClassifier.detectMultiScale(aInputFrame, faces, 1.1, 3,2,
                    new Size(width/6,height/6), new Size(width/1.2, height/1.2));
//            cascadeClassifier.detectMultiScale(aInputFrame, faces);
        }


        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();


        for (int i = 0; i <facesArray.length; i++) {
            Imgproc.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
            if(facesArray.length==1){
                setAverage(Calculations(facesArray[i].tl(),facesArray[i].br()));
            }
        }



        if(facesArray.length>=1){
            Log.d("face found", "we found a face mdudes");
            output(aInputFrame);
        }

//        output(aInputFrame);
//        aInputFrame = rotate(InputFrame, 270);

        return aInputFrame;
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);

        registerReceiver(usbReceiver,usbFilter);

    }

    public double Calculations(Point tl, Point br){
        double avg = (tl.x+br.x)/2;
        if(avg>getAverage()){
            //Move right
        }else if(avg < getAverage()){
            //Move left
        }else{
            //Don't move
        }
        return avg;
    }


    public void output(Mat subimg){

        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(subimg.cols(), subimg.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(subimg, bmp);
        } catch (CvException e) {
            Log.d("didn't work", e.getMessage());
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);


        byte[] byteArray = stream.toByteArray();


        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File pictureFileDir=new File(root);

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            pictureFileDir.mkdirs();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "Frame with no rectangle" + "_" + date + ".jpeg";
        String filename = pictureFileDir.getPath() + File.separator + photoFile;


        File mainPicture = new File(filename);
        try {
            FileOutputStream fos = new FileOutputStream(mainPicture);
            fos.write(byteArray);
            fos.close();
            Log.d("kkkk","image saved");
        } catch (Exception error) {
            Log.d("kkkk","Image could not be saved");
        }

    }


    public double getAverage(){
        return lastAVGx;
    }

    public double setAverage(double avg){
        return lastAVGx = avg;
    }

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

    public void setUp(UsbAccessory accessory) {
        fileDescriptor = usbManager.openAccessory(accessory);
        if (fileDescriptor != null) {
            megaADK = accessory;
            iS = new FileInputStream(fileDescriptor.getFileDescriptor());
            oS = new FileOutputStream(fileDescriptor.getFileDescriptor());
        }
    }

}