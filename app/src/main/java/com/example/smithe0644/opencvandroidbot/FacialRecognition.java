package com.example.smithe0644.opencvandroidbot;


import org.opencv.core.MatOfRect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgcodecs.*;
import org.opencv.core.Mat;


public class FacialRecognition {
    public void performFacialRecognition(String input) {
        CascadeClassifier face_cascade = new CascadeClassifier("lbpcascade_frontalface.xml");
        Imgcodecs imageCodecs = new Imgcodecs();
        Mat image = imageCodecs.imread(input);
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);

        MatOfRect rect = new MatOfRect();

        face_cascade.detectMultiScale(gray, rect);

//        faces = face_cascade.detectMultiScale(gray, 1.3, 5);

    }
}
