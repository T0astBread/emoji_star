package net.eaustria.emotionhero;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;

public class GameActivity extends AppCompatActivity
{
    private SurfaceView cameraView;
    private FrameLayout cameraViewHolder;

    private CameraDetector detector;
    private int cameraPreviewWidth, cameraPreviewHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        initActivity();
    }

    // region Initialization
    private void initActivity()
    {
        initVariables();
        initViews();
        initPostVariables();
    }

    // region Variables
    private void initVariables()
    {
    }

    private void initPostVariables()
    {
        this.detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, this.cameraView);
        this.detector.setOnCameraEventListener(new CameraDetector.CameraEventListener()
        {
            @Override
            public void onCameraSizeSelected(int w, int h, Frame.ROTATE rotate)
            {
                if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW)
                {
                    GameActivity.this.cameraPreviewWidth = h;
                    GameActivity.this.cameraPreviewHeight = w;
                }
                else
                {
                    GameActivity.this.cameraPreviewHeight = h;
                    GameActivity.this.cameraPreviewWidth = w;
                }
                cameraView.requestLayout();
            }
        });
    }
    // endregion

    // region Views
    private void initViews()
    {
        loadViews();
        attachBehaviour();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); //Just because it looks nice (not final)
    }

    private void loadViews()
    {
        this.cameraViewHolder = (FrameLayout) findViewById(R.id.cameraViewHolder);
        this.cameraView = new SurfaceView(this)
        {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
            {
                int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
                int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
                int width;
                int height;
                if (cameraPreviewHeight == 0 || cameraPreviewWidth == 0)
                {
                    width = measureWidth;
                    height = measureHeight;
                }
                else
                {
                    float viewAspectRatio = (float) measureWidth/measureHeight;
                    float cameraPreviewAspectRatio = (float) cameraPreviewWidth / cameraPreviewHeight;

                    if (cameraPreviewAspectRatio > viewAspectRatio)
                    {
                        width = measureWidth;
                        height =(int) (measureWidth / cameraPreviewAspectRatio);
                    }
                    else
                    {
                        width = (int) (measureHeight * cameraPreviewAspectRatio);
                        height = measureHeight;
                    }
                }
                setMeasuredDimension(width,height);
            }
        };
        this.cameraViewHolder.addView(this.cameraView);
    }

    private void attachBehaviour()
    {
    }
    // endregion
    // endregion


    @Override
    protected void onResume()
    {
        super.onResume();
        this.detector.start();
        this.cameraView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        this.detector.stop();
        this.cameraView.setVisibility(View.INVISIBLE);
    }
}
