package com.example.smithe0644.opencvandroidbot;

import android.app.Activity;
//import android.app.PendingIntent;
//import android.content.BroadcastReceiver;
import android.content.Context;
//import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
//import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
//import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//import android.os.ParcelFileDescriptor;


public class OpenCVActivity extends Activity
        implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;

    private int height;
    private int width;

    MqttHelper mqttManager;

    IntentFilter usbFilter;


    //Used in USB connection, can do through the CLOUD so obsolete
//    Boolean rP;
//
//    UsbAccessory megaADK;
//    UsbManager usbManager;
//    FileInputStream iS;
//    FileOutputStream oS;
//    ParcelFileDescriptor fileDescriptor;
//
//    PendingIntent permissionIntent;

    Context context;
    Boolean firstTime = true;
    int counter = 0;
    ArrayList<Double> pastFrames = new ArrayList<Double>();

    private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

    private double lastAVGx = 0;
    private double lastSize = 0;

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

//    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (ACTION_USB_PERMISSION.equals(action)) {
//                synchronized (this) {
//                    //Theres only 1 accessory
//                    UsbAccessory accessory = usbManager.getAccessoryList()[0];
//                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                        megaADK = accessory;
//                        setUp(megaADK);
//                        Toast.makeText(context,"Permission Granted", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(context,"Permission denied", Toast.LENGTH_SHORT).show();
//                    }
//                    rP = false;
//                }
//            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
//
//                UsbAccessory accessory = usbManager.getAccessoryList()[0];
//                if(accessory == null) Log.d("Detached", "accessory no longer findable");
//                Toast.makeText(context,"Accessory is detached", Toast.LENGTH_SHORT).show();
//
//                oS = null;
//                iS = null;
////              if (accessory != null && accessory.equals(megaADK)) {
////                    closeAccessory();
////              }
//            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
//                UsbAccessory accessory = usbManager.getAccessoryList()[0];
//                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                    megaADK = accessory;
//                    setUp(megaADK);
//
//                } else {
//                    Log.d("MEGA ADK ", "Permission denied" + accessory);
//                }
//                rP = false;
//            }
//        }
//    };

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
            }
            is.close();
            os.close();

            //Writes it into the file mCascadeFile

            // Load the cascade classifier

            String path = mCascadeFile.getAbsolutePath();

            cascadeClassifier = new CascadeClassifier(path);
            cascadeClassifier.load(path);
            if (cascadeClassifier.empty()) {
                Log.d("cascadeClassifier", "is empty");
            } else Log.d("cascadeClassifier", "not empty");


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

        context = getApplicationContext();
        beginMqtt();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
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
        counter++;
        if (counter > 150) {
            firstTime = true;
            pastFrames.clear();
        }
        MatOfRect faces = new MatOfRect();

        // Use the classifier to detect faces
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(aInputFrame, faces, 1.1, 3, 2,
                    new Size(width / 6, height / 6), new Size(width / 1.2, height / 1.2));
            counter = 0;
//            cascadeClassifier.detectMultiScale(aInputFrame, faces);
        }


        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        for (Rect face:facesArray) {
            Imgproc.rectangle(aInputFrame, face.tl(), face.br(), new Scalar(0, 255, 0, 255), 3);
        }

        if(facesArray.length > 0) {

            //We have to check if it is the first time to be able to get a reading on where the face is

            if (!firstTime) {
                Rect optimal = facesArray[0];
                if (facesArray.length > 1) {

                    //This will find the closest rectangle to one the previous face in the case that many is detected

                    double minDist = (double) Integer.MAX_VALUE;
                    for (Rect current : facesArray) {
                        double dist = Math.abs(getAverage() - calcAverage(current.tl(), current.br()));
                        if (minDist > dist) {
                            minDist = dist;
                            optimal = current;
                        }
                    }
                }

                //Add the average x position
                pastFrames.add(calcAverage(optimal.tl(), optimal.br()));
                if (pastFrames.size() > 5) {
                    pastFrames.remove(0);
                    Calculations(optimal.tl(), optimal.br());
                }

                setSize(calcSize(optimal.tl(), optimal.br()));

            } else {
                if (facesArray.length == 1) {
                    firstTime = false;
                    pastFrames.add(calcAverage(facesArray[0].tl(), facesArray[0].br()));
                    setSize(calcSize(facesArray[0].tl(), facesArray[0].br()));
                }
            }
            output(aInputFrame);
        }

//        aInputFrame = rotate(InputFrame, 270);

        return aInputFrame;
    }

    @Override
    public void onResume() {
        super.onResume();


        ////////////////////////////////
        ////////////////////////////////
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);

//        registerReceiver(usbReceiver, usbFilter);
//        if (iS != null && oS != null) {
//            Toast.makeText(context, "is&&os null", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        //There sohuld only be 1 USB accessory which would be the Mega
//        if (usbManager == null) {
//            Toast.makeText(context, "usbManager is null", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        UsbAccessory accessory = usbManager.getAccessoryList()[0];
//        if (accessory != null) {
//            if (usbManager.hasPermission(accessory)) {
//                setUp(accessory);
//            } else {
//                synchronized (usbReceiver) {
//                    if (!rP) {
//                        Toast.makeText(context, "requesting permission", Toast.LENGTH_SHORT).show();
//                        usbManager.requestPermission(accessory, permissionIntent);
//                        rP = true;
//                    }
//                }
//            }
//        } else {
//            Log.d("Android Accessory", "Accessory is null");
//            //Beginning USB
//            beginUsb();
//        }
        ///////////////////////////////////////
        ///////////////////////////////////////

    }

    public double Calculations(Point tl, Point br) {
        double avg = (tl.x + br.x) / 2;
        double errorBound = 0.2 * (getSize());
        double dif = Math.abs(pastFrames.get(0) - pastFrames.get(4));

        if (avg > getAverage() && dif > errorBound) {
            MovingLeft();
        } else if (avg < getAverage() && dif > errorBound) {
            MovingRight();
        } else {
            Stopped();
        }
        return avg;
    }


    public void output(Mat subimg) {

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
        File pictureFileDir = new File(root);

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
            Log.d("kkkk", "image saved");
        } catch (Exception error) {
            Log.d("kkkk", "Image could not be saved");
        }

    }


    public double getAverage() {
        return pastFrames.get(0);
    }


    public double getSize() {
        return lastSize;
    }

    public double calcAverage(Point tl, Point br) {
        return (tl.x + br.x) / 2;
    }

    public double calcSize(Point tl, Point br) {
        return Math.abs((br.x - tl.x) * (br.y - tl.y));
    }

//    public double setAverage(double avg) {
//        return lastAVGx = avg;
//    }

    public double setSize(double size) {
        return lastSize = size;
    }

    public static Mat rotate(Mat src, double angle) {
        Mat dst = new Mat();
        if (angle == 180 || angle == -180) {
            Core.flip(src, dst, -1);
        } else if (angle == 90 || angle == -270) {
            Core.flip(src.t(), dst, 1);
        } else if (angle == 270 || angle == -90) {
            Core.flip(src.t(), dst, 0);
        }

        Imgproc.resize(dst, dst, src.size());


        return dst;
    }

//    public void beginUsb() {
//        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
//
//        if (usbManager == null) usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        if (usbManager == null) return;
//        if (usbManager.getAccessoryList() == null) {
//            Toast.makeText(context, "Accessory null from BeginUSB", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (megaADK != null) {
//            setUp(megaADK);
//        } else {
//            megaADK = usbManager.getAccessoryList()[0];
//            setUp(megaADK);
//        }
//    }
//
//    public void setUp(UsbAccessory accessory) {
//        fileDescriptor = usbManager.openAccessory(accessory);
//        if (fileDescriptor != null) {
//            megaADK = accessory;
//            iS = new FileInputStream(fileDescriptor.getFileDescriptor());
//            oS = new FileOutputStream(fileDescriptor.getFileDescriptor());
//        }
//    }

    //Obsolete USB code, possibly use in case of backup

//    public void Right() {
//        byte[] buffer = {(byte)3, (byte)4};
//        if(oS!= null){
//            try {
//                Toast.makeText(this,"Right", Toast.LENGTH_SHORT).show();
//                oS.write(buffer);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void Left() {
//        byte[] buffer = {(byte)3, (byte)5};
//        if(oS!= null){
//            try {
//                Toast.makeText(this,"Left", Toast.LENGTH_SHORT).show();
//                oS.write(buffer);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void Stop() {
//        byte[] buffer = {(byte)3, (byte)6};
//        if(oS!= null){
//            try {
//                Toast.makeText(this,"Stop", Toast.LENGTH_SHORT).show();
//                oS.write(buffer);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//    }

    private void MovingLeft() {
        byte[] buffer = {(byte) 'l', (byte) 'e', (byte) 'f', (byte) 't'};
        Log.d("Left", "  published");
        mqttManager.publish("Commands", buffer, 2, false);
    }

    private void MovingRight() {
        byte[] buffer = {(byte) 'r', (byte) 'i', (byte) 'g', (byte) 'h', (byte) 't'};
        Log.d("Left", "  published");
        mqttManager.publish("Commands", buffer, 2, false);
    }

    private void Stopped() {
        byte[] buffer = {(byte) 's', (byte) 't', (byte) 'o', (byte) 'p', (byte) 'p', (byte) 'e', (byte) 'd'};
        Log.d("Left", "  published");
        mqttManager.publish("Commands", buffer, 2, false);
    }

//    public void Right() {
//        byte[] buffer = {(byte) 3, (byte) 4};
//        if (oS != null) {
//            try {
//                Toast.makeText(this, "Right", Toast.LENGTH_SHORT).show();
//                oS.write(buffer);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void Left() {
//        byte[] buffer = {(byte) 3, (byte) 5};
//        if (oS != null) {
//            try {
//                Toast.makeText(this, "Left", Toast.LENGTH_SHORT).show();
//                oS.write(buffer);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void Stop() {
//        byte[] buffer = {(byte) 3, (byte) 6};
//        if (oS != null) {
//            try {
//                Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
//                oS.write(buffer);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    private void beginMqtt() {
        mqttManager = new MqttHelper(getApplicationContext());
        mqttManager.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(topic, message.toString());
//                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}