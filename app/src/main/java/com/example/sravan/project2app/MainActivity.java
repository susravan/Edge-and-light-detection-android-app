package com.example.sravan.project2app;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener,CameraBridgeViewBase.CvCameraViewListener2{
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba;
    double x=-1;
    double y=-1;
    private Scalar mBlobColorRgba,mBlobColorHsv;
    TextView touch_coordinates;
    TextView touch_color;

    // Variables for additional features
    private int count = 0;
    private RadioGroup radioGroup;
    private RadioButton radioButton;
    private Button btnDisplay;
    private int functionCode = -2;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
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
        Button otherCamera = (Button) findViewById(R.id.OtherCamera);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        touch_color=(TextView)findViewById(R.id.touch_color);
        touch_coordinates=(TextView)findViewById(R.id.touch_coordinates);
        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.opencv_tutorial_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        addListenerOnButton();
    }

    public void addListenerOnButton() {
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.edgeDetection:
                        functionCode = 0;
                        System.out.println("functionCode = " + functionCode);
                        break;
                    case R.id.lightSourceDetector:
                        functionCode = 1;
                        System.out.println("functionCode = " + functionCode);
                        break;
                }
            }
        });
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mOpenCvCameraView!=null){mOpenCvCameraView.disableView();}
    }

    @Override
    public void onResume(){
        super.onResume();
        if(!OpenCVLoader.initDebug())
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0,this,mLoaderCallback);
        }
        else
        {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView!=null)
        {mOpenCvCameraView.disableView();}
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            count = count+1;
        }

        System.out.println("Inside On touch");
        int cols=mRgba.cols();
        int rows=mRgba.rows();
        double yLow=(double) mOpenCvCameraView.getHeight()*0.2401961;
        double yHigh=(double) mOpenCvCameraView.getHeight()*0.7696078;
        double xScale=(double) cols/(double)mOpenCvCameraView.getWidth();
        double yScale=(double) rows/(yHigh-yLow);
        x=event.getX();
        y=event.getY();
        y=y-yLow;
        x=x*xScale;
        y=y*yScale;
        if((x<0)||(y<0)||(x>cols)||(y>rows)) return false;
        touch_coordinates.setText("X: "+ Double.valueOf(x) +"Y: "+Double.valueOf(y));
        Rect touchedRect=new Rect();
        touchedRect.x=(int)x;
        touchedRect.y=(int)y;
        touchedRect.width=8;
        touchedRect.height=8;
        Mat touchedRegionRgba=mRgba.submat(touchedRect);
        Mat touchedRegionHsv=new Mat();
        Imgproc.cvtColor(touchedRegionRgba,touchedRegionHsv,Imgproc.COLOR_RGB2HSV_FULL);

        mBlobColorHsv= Core.sumElems(touchedRegionHsv);
        int pointCount=touchedRect.width*touchedRect.height;
        for(int i=0;i<mBlobColorHsv.val.length;i++)
            mBlobColorHsv.val[i]/=pointCount;

        mBlobColorRgba=convertScalarHsv2Rgba(mBlobColorHsv);

        touch_color.setText("Color : #"+ String.format("%02X", (int)mBlobColorRgba.val[0])
                +String.format("02X",(int)mBlobColorRgba.val[1])
                +String.format("02X",(int)mBlobColorRgba.val[2]));

        touch_color.setTextColor(Color.rgb((int)mBlobColorRgba.val[0],
                (int)mBlobColorRgba.val[1],(int)mBlobColorRgba.val[2]));
        touch_coordinates.setTextColor(Color.rgb((int)mBlobColorRgba.val[0],
                (int)mBlobColorRgba.val[1],(int)mBlobColorRgba.val[2]));

        return false;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba=new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv=new Scalar(255);

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    private Mat detectLight(Mat mRgba, double gaussianBlurValue) {
//        Mat rgba = new Mat();
//        Utils.bitmapToMat(bitmap, rgba);

        Mat grayScaleGaussianBlur = new Mat();
        Imgproc.cvtColor(mRgba, grayScaleGaussianBlur, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayScaleGaussianBlur, grayScaleGaussianBlur, new Size(gaussianBlurValue, gaussianBlurValue), 0);

        Core.MinMaxLocResult minMaxLocResultBlur = Core.minMaxLoc(grayScaleGaussianBlur);
        Imgproc.circle(mRgba, minMaxLocResultBlur.maxLoc, 50, new Scalar(255), 3);
        return mRgba;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba=inputFrame.rgba();

        if(functionCode == 0) {
            if (count % 2 == 0) {
//                count = 0;
                return mRgba;
            } else {
                return detectEdges(mRgba);
            }
        }
        if (functionCode == 1) {
            return detectLight(mRgba, 7);
        }

        return mRgba;
    }


    private Mat detectEdges(Mat mRgba) {
        Mat edges = new Mat(mRgba.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(mRgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.Canny(edges, edges, 80, 100);
        return edges;
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba=new Mat();
        Mat pointMatHsv=new Mat(1,1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv,pointMatRgba,Imgproc.COLOR_HLS2RGB_FULL,4);
        return new  Scalar(pointMatRgba.get(0,0));
    }
}