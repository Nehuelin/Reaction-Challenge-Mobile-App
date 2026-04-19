package com.example.reactionchallenge;

import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reactionchallenge.game.Difficulty;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView iterationsLabel;
    private TextView timeLimitLabel;
    private EditText playerNameInput;
    private SeekBar iterationsSeekbar;
    private SeekBar timeLimitSeekbar;
    private RadioGroup difficultyGroup;
    private CheckBox inverseModeCheckbox;
    private CheckBox dynamicDifficultyCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupConfigControls();
    }

    private void bindViews() {
        playerNameInput = findViewById(R.id.playerNameInput);
        difficultyGroup = findViewById(R.id.difficultyGroup);
        iterationsSeekbar = findViewById(R.id.iterationsSeekbar);
        timeLimitSeekbar = findViewById(R.id.timeLimitSeekbar);
        inverseModeCheckbox = findViewById(R.id.inverseModeCheckbox);
        dynamicDifficultyCheckbox = findViewById(R.id.dynamicDifficultyCheckbox);
        iterationsLabel = findViewById(R.id.iterationsLabel);
        timeLimitLabel = findViewById(R.id.timeLimitLabel);
        Button startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> startGame());
    }

    private void setupConfigControls() {
        iterationsSeekbar.setOnSeekBarChangeListener(new SimpleSeekbarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int iterations = progress + 5;
                iterationsLabel.setText(String.format(Locale.getDefault(), "Iteraciones por nivel: %d", iterations));
            }
        });

        timeLimitSeekbar.setOnSeekBarChangeListener(new SimpleSeekbarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = progress + 5;
                timeLimitLabel.setText(String.format(Locale.getDefault(), "Tiempo máximo de reacción: %d s", seconds));
            }
        });

        iterationsSeekbar.setProgress(15);
        timeLimitSeekbar.setProgress(15);
        updateDefaultTimeByDifficulty();
        difficultyGroup.setOnCheckedChangeListener((group, checkedId) -> updateDefaultTimeByDifficulty());
    }


    private void updateDefaultTimeByDifficulty() {
        Difficulty difficulty = getSelectedDifficulty();
        int defaultSeconds;
        switch (difficulty) {
            case HARD:
                defaultSeconds = 10;
                break;
            case MEDIUM:
                defaultSeconds = 15;
                break;
            default:
                defaultSeconds = 20;
                break;
        }
        timeLimitSeekbar.setProgress(defaultSeconds - 5);
    }

    private Difficulty getSelectedDifficulty() {
        int selectedId = difficultyGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.difficultyTraining) {
            return Difficulty.TRAINING;
        }
        if (selectedId == R.id.difficultyMedium) {
            return Difficulty.MEDIUM;
        }
        if (selectedId == R.id.difficultyHard) {
            return Difficulty.HARD;
        }
        return Difficulty.EASY;
    }

    private void startGame() {
        String playerName = playerNameInput.getText().toString().trim();
        if (TextUtils.isEmpty(playerName)) {
            playerNameInput.setError("Ingresa un nombre de jugador.");
            return;
        }

        Intent gameIntent = GameActivity.createIntent(
                this,
                playerName,
                getSelectedDifficulty(),
                iterationsSeekbar.getProgress() + 5,
                Math.min((timeLimitSeekbar.getProgress() + 5L) * 1000L, 30_000L),
                inverseModeCheckbox.isChecked(),
                dynamicDifficultyCheckbox.isChecked()
        );

        startActivity(gameIntent);
}

    private abstract static class SimpleSeekbarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}