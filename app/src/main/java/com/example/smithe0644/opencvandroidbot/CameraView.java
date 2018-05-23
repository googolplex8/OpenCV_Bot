package com.example.smithe0644.opencvandroidbot;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.hardware.Camera;


//Simple view to display Camera, copied from
//https://developer.android.com/guide/topics/media/camera
//We did not make this

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder holder;
    private Camera camera;

    public CameraView(Context context, Camera camera){
        super(context);
        this.camera = camera;
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //not relevant currently
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(holder.getSurface()==null) return;

        try{
            camera.stopPreview();
        }catch(Exception e){
            e.printStackTrace();
        }

        try {
            camera.setPreviewDisplay(this.holder);
            camera.startPreview();

        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
