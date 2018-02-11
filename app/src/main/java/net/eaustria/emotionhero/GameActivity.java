package net.eaustria.emotionhero;

import android.Manifest;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.t0ast.androidcommons.permissions.PermissionUtils;

public class GameActivity extends AppCompatActivity implements Detector.ImageListener
{
    public static final String TAG = GameActivity.class.getSimpleName();
    private static final Set<Face.EMOJI> EXCLUDED_EMOJI;
    private static final int TIME_PER_ROUND = 10;
    private static final int GAME_OVER_REQUEST_CODE = 0;

    private TextView scoreView, emojiView, detectedEmojiView, timerView;
    private SurfaceView cameraView;
    private FrameLayout cameraViewHolder;
    private ViewGroup cameraLoadingLayout, pauseLayout;
    private ProgressBar cameraLoadingSpinner;

    private CameraDetector detector;
    private int cameraPreviewWidth, cameraPreviewHeight;
    private int score, remainingTime;
    private Face.EMOJI currentEmoji;
    private Method currentEmojiValueMethod;
    private long correctFaceStartTimestamp; // Timestamp when the correct face was first detected
    private int consecutiveIncorrectFaces;
    private GameBackgroundTask backgroundTask;

    static
    {
        EXCLUDED_EMOJI = new HashSet<>();
        EXCLUDED_EMOJI.add(Face.EMOJI.FLUSHED);
        EXCLUDED_EMOJI.add(Face.EMOJI.RAGE);
//        EXCLUDED_EMOJI.add(Face.EMOJI.STUCK_OUT_TONGUE_WINKING_EYE);
        EXCLUDED_EMOJI.add(Face.EMOJI.LAUGHING);
        EXCLUDED_EMOJI.add(Face.EMOJI.UNKNOWN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        initActivity();
        newEmoji();
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
        this.remainingTime = TIME_PER_ROUND;
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
        this.detector.setDetectAllEmojis(true);
        this.detector.setMaxProcessRate(60);
    }
    // endregion

    // region Views
    private void initViews()
    {
        loadViews();
        prepareViews();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); //Just because it looks nice (not final)
    }

    private void loadViews()
    {
        this.scoreView = findViewById(R.id.score);
        this.emojiView = findViewById(R.id.emoji);
        this.detectedEmojiView = findViewById(R.id.detectedEmoji);
        this.timerView = findViewById(R.id.timer);

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
        this.cameraViewHolder.addView(this.cameraView, 0, cameraViewLayoutParams);
        this.cameraLoadingLayout = (ViewGroup) findViewById(R.id.cameraLoadingLayout);
        this.cameraLoadingSpinner = (ProgressBar) findViewById(R.id.cameraLoadingSpinner);
        this.pauseLayout = (ViewGroup) findViewById(R.id.pausedLayout);
    }

    private void prepareViews()
    {
        updateScoreView();

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
        resumeGame();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        pauseGame();
    }

    // region Pausing
    private void pauseGame()
    {
        Log.i(TAG, "pauseGame: Pausing");
        this.pauseLayout.setVisibility(View.VISIBLE);
        pauseBackgroundTask();
        pauseCameraDetector();
    }

    private void pauseBackgroundTask()
    {
        if(this.backgroundTask != null) this.backgroundTask.cancel(true);
        this.backgroundTask = null;
    }

    private void pauseCameraDetector()
    {
        if(!this.detector.isRunning()) return;
        this.detector.stop();
        this.cameraView.setVisibility(View.INVISIBLE);
    }

    private void resumeGame()
    {
        resumeCameraDetector();
        this.pauseLayout.setVisibility(View.INVISIBLE);
        resumeBackgroundTask();
    }

    private void resumeBackgroundTask()
    {
        pauseBackgroundTask();
        this.backgroundTask = new GameBackgroundTask();
        this.backgroundTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        this.detector.setImageListener(this);
        this.cameraView.setVisibility(View.VISIBLE);
        this.cameraLoadingLayout.setVisibility(View.INVISIBLE);
    }
    // endregion


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode != GAME_OVER_REQUEST_CODE) return;
        Log.i(TAG, "onActivityResult: Returned from GameOverActivity");
        finish();
    }

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v)
    {
        if(list.size() < 1) return;
//        Log.d(TAG, "onImageResults: Faces: " + list.size());
        Face face = list.get(0);

//        Face.EMOJI domEm = face.emojis.getDominantEmoji();
//        Log.d(TAG, "onImageResults: Dominant face is: " + domEm);
////        Log.d(TAG, "onImageResults: Wink: " + face.emojis.getWink());
//        this.detectedEmojiView.setText(domEm.getUnicode());
//        boolean isMakingCorrectFace = domEm == this.currentEmoji;

        float emojiValue = 0;
        try
        {
            emojiValue = (float) this.currentEmojiValueMethod.invoke(face.emojis) * getEmojiValueMultiplier(this.currentEmoji);
        }
        catch(Exception e)
        {
            Log.e(TAG, "onImageResults: Reflection error calling emoji value mathod", e);
        }
        Log.i(TAG, "onImageResults: Emoji value: " + emojiValue);
        boolean isMakingCorrectFace = emojiValue > .5f;

        if(isMakingCorrectFace)
        {
            Log.i(TAG, "onImageResults: Correct face");
            this.consecutiveIncorrectFaces = 0;
            if(this.correctFaceStartTimestamp == -1) this.correctFaceStartTimestamp = System.currentTimeMillis();
            else if(Math.abs(System.currentTimeMillis() - this.correctFaceStartTimestamp) >= 100)
            {
                Log.d(TAG, "onImageResults: Score up");
                raiseScore();
                newEmoji();
            }
        }
        else
        {
            if(++this.consecutiveIncorrectFaces > 5) this.correctFaceStartTimestamp = -1;
        }
    }

    private void newEmoji()
    {
        Face.EMOJI[] emojis = Face.EMOJI.values();
        do
        {
            this.currentEmoji = emojis[(int) (Math.random() * emojis.length)];
        }
        while(EXCLUDED_EMOJI.contains(this.currentEmoji));
        Log.i(TAG, "newEmoji: New emoji is: " + this.currentEmoji.getShortcode());
        this.currentEmojiValueMethod = getCurrentEmojiValueMethod();
        this.consecutiveIncorrectFaces = 0;
        this.correctFaceStartTimestamp = -1;
        this.emojiView.setText(this.currentEmoji.getUnicode());
    }

    private Method getCurrentEmojiValueMethod()
    {
        String emojiName = this.currentEmoji.getShortcode();
        emojiName = emojiName.substring(1, emojiName.length() - 1);
        while(emojiName.contains("_"))
        {
            int underscoreIndex = emojiName.indexOf("_");
            emojiName = emojiName.replaceFirst("_\\w", Character.toString(Character.toUpperCase(emojiName.charAt(underscoreIndex + 1))));
        }
        emojiName = emojiName.substring(0, 1).toUpperCase().concat(emojiName.substring(1));
        try
        {
            return Face.Emojis.class.getDeclaredMethod("get" + emojiName);
        }
        catch(NoSuchMethodException e)
        {
            Log.e(TAG, "getScoreForCurrentEmoji: Invalid emoji: " + this.currentEmoji.getShortcode(), e);
            return null;
        }
    }

    private float getEmojiValueMultiplier(Face.EMOJI emoji)
    {
        switch(emoji)
        {
            case LAUGHING:
                return 10;
            case WINK:
                return 2;
            case STUCK_OUT_TONGUE:
                return 4;
            case STUCK_OUT_TONGUE_WINKING_EYE:
                return 10;
            case SMILEY:
                return 2;
            case SMIRK:
                return 1.5f;
            default:
                return 1;
        }
    }

    private void raiseScore()
    {
        this.score++;
        updateScoreView();
    }

    private void updateScoreView()
    {
        this.scoreView.setText(Integer.toString(this.score));
    }

    private void endGame()
    {
        pauseGame();
        Intent gameOverActivityIntent = new Intent(this, GameOverActivity.class);
        gameOverActivityIntent.putExtra(GameOverActivity.EXTRA_SCORE, this.score);
        startActivityForResult(gameOverActivityIntent, GAME_OVER_REQUEST_CODE);
    }

    private class GameBackgroundTask extends AsyncTask<Void, Boolean, Void>
    {
        private static final int CYCLE_TIME = 1000;

        @Override
        protected Void doInBackground(Void... voids)
        {
            for(int cycles = 0; !isCancelled(); cycles++)
            {
                publishProgress(remainingTime <= 0);
                try
                {
                    Thread.sleep(CYCLE_TIME);
                }
                catch(InterruptedException e)
                {
                    Log.e(TAG, "doInBackground: Interrupted", e);
                    cancel(false);
                }
                if(--remainingTime < 0) remainingTime = 0;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Boolean... booleans)
        {
            boolean finished = booleans[0];
            if(finished) endGame();
            timerView.setText(getString(R.string.time, remainingTime/60, remainingTime%60));
        }
    }
}
