package com.example.smithe0644.opencvandroidbot;


import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgcodecs.*;
import org.opencv.core.Mat;


public class FacialRecognition {
    public void performFacialRecognition(String input) {
        CascadeClassifier face_cascade = new CascadeClassifier("haarcascade_frontalface_default.xml");
        Imgcodecs imageCodecs = new Imgcodecs();
        Mat image = imageCodecs.imread(input);
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);

        faces = face_cascade.detectMultiScale(gray, 1.3, 5);

    }
}
