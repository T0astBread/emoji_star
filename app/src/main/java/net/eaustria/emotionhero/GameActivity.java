package net.eaustria.emotionhero;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.t0ast.androidcommons.permissions.PermissionUtils;

public class GameActivity extends AppCompatActivity
{
    private SurfaceView cameraView;
    private FrameLayout cameraViewHolder;
    private ViewGroup cameraLoadingLayout, pauseLayout;
    private ProgressBar cameraLoadingSpinner;

    private CameraDetector detector;
    private int cameraPreviewWidth, cameraPreviewHeight;
    private boolean paused;


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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                    float viewAspectRatio = 1; //The preview SurfaceView is always a square
                    float cameraPreviewAspectRatio = (float) cameraPreviewWidth / cameraPreviewHeight;

                    if (cameraPreviewAspectRatio > viewAspectRatio)
                    {
                        width = (int) (measureHeight * cameraPreviewAspectRatio);
                        height = measureHeight;
                    }
                    else
                    {
                        width = measureWidth;
                        height = (int) (measureWidth / cameraPreviewAspectRatio);
                    }
                }
                setMeasuredDimension(width, height);
            }
        };
        FrameLayout.LayoutParams cameraViewLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        this.cameraViewHolder.addView(this.cameraView, cameraViewLayoutParams);
        this.cameraLoadingLayout = (ViewGroup) findViewById(R.id.cameraLoadingLayout);
        this.cameraLoadingSpinner = (ProgressBar) findViewById(R.id.cameraLoadingSpinner);
        this.pauseLayout = (ViewGroup) findViewById(R.id.pausedLayout);
    }

    private void attachBehaviour()
    {
        this.cameraView.setOnClickListener(v -> pauseGame());
        this.pauseLayout.setOnClickListener(v -> resumeGame());
        this.cameraLoadingSpinner.setIndeterminate(true);
    }
    // endregion
    // endregion


    @Override
    protected void onResume()
    {
        super.onResume();
        resumeCameraDetector();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        pauseCameraDetector();
    }

    private void pauseGame()
    {
        this.pauseLayout.setVisibility(View.VISIBLE);
        pauseCameraDetector();
    }

    // region Pausing
    private void pauseCameraDetector()
    {
        if(!this.detector.isRunning()) return;
        this.detector.stop();
        this.cameraView.setVisibility(View.INVISIBLE);
    }

    private void resumeGame()
    {
        this.pauseLayout.setVisibility(View.INVISIBLE);
        resumeCameraDetector();
    }

    private void resumeCameraDetector()
    {
        if(this.detector.isRunning()) return;
        if(!PermissionUtils.ensurePermissionIsGranted(this, Manifest.permission.CAMERA))
        {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            return;
        }
        this.cameraLoadingLayout.setVisibility(View.VISIBLE);
        this.detector.start();
        this.cameraView.setVisibility(View.VISIBLE);
        this.cameraLoadingLayout.setVisibility(View.INVISIBLE);
    }

    private void toggleGamePause()
    {
        if(this.paused) resumeGame();
        else pauseGame();
    }
    // endregion
}
