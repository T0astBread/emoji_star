package net.eaustria.emotionhero;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class GameOverActivity extends AppCompatActivity
{
    public static final String EXTRA_SCORE = "score";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        ((TextView) findViewById(R.id.score)).setText(Integer.toString(getIntent().getIntExtra(EXTRA_SCORE, 0)));
    }
}
