
package com.example.reactionchallenge;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reactionchallenge.data.BestScoreRepository;
import com.example.reactionchallenge.game.Difficulty;
import com.example.reactionchallenge.game.GameConfig;
import com.example.reactionchallenge.game.GameEngine;
import com.example.reactionchallenge.game.StimulusRound;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView iterationsLabel;
    private TextView timeLimitLabel;
    private TextView statusText;
    private TextView ruleText;
    private TextView stimulusText;
    private TextView countdownText;
    private TextView statsText;
    private EditText playerNameInput;
    private SeekBar iterationsSeekbar;
    private SeekBar timeLimitSeekbar;
    private RadioGroup difficultyGroup;
    private CheckBox inverseModeCheckbox;
    private Button reactButton;
    private Button restartButton;

    private final GameEngine gameEngine = new GameEngine();
    private BestScoreRepository bestScoreRepository;
    private CountDownTimer roundTimer;
    private long roundStartMs;
    private boolean responseRegistered;

    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bestScoreRepository = new BestScoreRepository(this);

        bindViews();
        setupConfigControls();
        setupActions();
    }

    @Override
    protected void onDestroy () {
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        super.onDestroy();
    }

    private void bindViews () {
        playerNameInput = findViewById(R.id.playerNameInput);
        difficultyGroup = findViewById(R.id.difficultyGroup);
        iterationsSeekbar = findViewById(R.id.iterationsSeekbar);
        timeLimitSeekbar = findViewById(R.id.timeLimitSeekbar);
        inverseModeCheckbox = findViewById(R.id.inverseModeCheckbox);
        iterationsLabel = findViewById(R.id.iterationsLabel);
        timeLimitLabel = findViewById(R.id.timeLimitLabel);
        statusText = findViewById(R.id.statusText);
        ruleText = findViewById(R.id.ruleText);
        stimulusText = findViewById(R.id.stimulusText);
        countdownText = findViewById(R.id.countdownText);
        statsText = findViewById(R.id.statsText);
        Button startButton = findViewById(R.id.startButton);
        reactButton = findViewById(R.id.reactButton);
        restartButton = findViewById(R.id.restartButton);

        startButton.setOnClickListener(v -> startGame());
    }

    private void setupConfigControls () {
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

    private void setupActions () {
        reactButton.setOnClickListener(v -> {
            if (!gameEngine.isRunning() || responseRegistered) {
                return;
            }
            responseRegistered = true;
            long reactionTime = System.currentTimeMillis() - roundStartMs;
            resolveRound(true, reactionTime);
        });

        restartButton.setOnClickListener(v -> startGame());
    }

    private void updateDefaultTimeByDifficulty () {
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

    private Difficulty getSelectedDifficulty () {
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

    private void startGame () {
        String playerName = playerNameInput.getText().toString().trim();
        if (TextUtils.isEmpty(playerName)) {
            playerNameInput.setError("Ingresa un nombre de jugador.");
            return;
        }

        GameConfig config = new GameConfig(
                getSelectedDifficulty(),
                iterationsSeekbar.getProgress() + 5,
                Math.min((timeLimitSeekbar.getProgress() + 5L) * 1000L, 30_000L),
                inverseModeCheckbox.isChecked()
        );

        gameEngine.start(playerName, config);
        restartButton.setVisibility(android.view.View.GONE);
        reactButton.setEnabled(true);
        statusText.setText("Partida en curso");

        Toast.makeText(this, "Partida iniciada. ¡Atención!", Toast.LENGTH_SHORT).show();
        showCurrentStimulusAndStats();
        startRoundCountdown();
    }

    private void startRoundCountdown () {
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        responseRegistered = false;
        roundStartMs = System.currentTimeMillis();
        long roundDuration = gameEngine.getEffectiveReactionMs();

        roundTimer = new CountDownTimer(roundDuration, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownText.setText(String.format(Locale.getDefault(), "Tiempo restante: %.1f s", millisUntilFinished / 1000f));
            }

            @Override
            public void onFinish() {
                if (!responseRegistered) {
                    resolveRound(false, roundDuration);
                }
            }
        }.start();
    }

    private void resolveRound ( boolean userReacted, long reactionTimeMs){
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        GameEngine.RoundOutcome outcome = gameEngine.resolveRound(userReacted, reactionTimeMs);
        if (outcome.correct && userReacted) {
            playFeedbackSound();
        }

        if (outcome.gameOver) {
            finishGame(outcome.won);
            return;
        }

        if (outcome.levelUp) {
            Toast.makeText(this, "¡Subiste de nivel!", Toast.LENGTH_SHORT).show();
        }

        showCurrentStimulusAndStats();
        startRoundCountdown();
    }

    private void showCurrentStimulusAndStats () {
        StimulusRound stimulus = gameEngine.getCurrentStimulus();
        stimulusText.setText(stimulus.displayText);
        stimulusText.setTextColor(stimulus.textColor);
        ruleText.setText(stimulus.ruleDescription);
        statusText.setText(String.format(Locale.getDefault(), "Nivel %d de %d | Vidas: %d",
                gameEngine.getLevel(), GameEngine.MAX_LEVELS, gameEngine.getLives()));

        String bestText = bestScoreRepository.buildBestStatsText(
                gameEngine.getPlayerName(),
                gameEngine.getScore(),
                gameEngine.getAverageReactionMs(),
                false
        );
        statsText.setText(gameEngine.buildLiveStats(bestText));
    }

    private void finishGame ( boolean won){
        reactButton.setEnabled(false);
        restartButton.setVisibility(android.view.View.VISIBLE);

        String result = won ? "¡Ganaste todos los niveles!" : "Perdiste. Puedes reiniciar.";
        statusText.setText(result);

        if (gameEngine.getDifficulty() != Difficulty.TRAINING) {
            bestScoreRepository.saveIfBetter(
                    gameEngine.getPlayerName(),
                    gameEngine.getScore(),
                    gameEngine.getAverageReactionMs()
            );
        }

        String bestText = bestScoreRepository.buildBestStatsText(
                gameEngine.getPlayerName(),
                gameEngine.getScore(),
                gameEngine.getAverageReactionMs(),
                true
        );

        String finalStats = String.format(
                Locale.getDefault(),
                "%s\nAciertos: %d/%d\nPuntaje final: %d%s\nTiempo de reacción promedio: %.0f ms\n%s",
                result,
                gameEngine.getCorrectAnswers(),
                gameEngine.getTotalRounds(),
                gameEngine.getScore(),
                gameEngine.getDifficulty() == Difficulty.TRAINING ? " (modo entrenamiento)" : "",
                gameEngine.getAverageReactionMs(),
                bestText
        );
        statsText.setText(finalStats);
    }

    private void playFeedbackSound () {
        // Recurso genérico para que puedas reemplazar el archivo luego sin tocar código.
        MediaPlayer player = MediaPlayer.create(this, R.raw.entranceactivate);
        if (player == null) {
            return;
        }
        player.setOnCompletionListener(MediaPlayer::release);
        player.start();
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