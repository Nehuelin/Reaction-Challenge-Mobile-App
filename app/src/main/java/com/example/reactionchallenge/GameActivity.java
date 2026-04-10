package com.example.reactionchallenge;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reactionchallenge.data.BestScoreRepository;
import com.example.reactionchallenge.game.Difficulty;
import com.example.reactionchallenge.game.GameConfig;
import com.example.reactionchallenge.game.GameEngine;
import com.example.reactionchallenge.game.StimulusRound;

import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private static final String EXTRA_PLAYER_NAME = "extra_player_name";
    private static final String EXTRA_DIFFICULTY = "extra_difficulty";
    private static final String EXTRA_ITERATIONS = "extra_iterations";
    private static final String EXTRA_REACTION_LIMIT_MS = "extra_reaction_limit_ms";
    private static final String EXTRA_INVERSE_MODE = "extra_inverse_mode";

    private TextView statusText;
    private TextView ruleText;
    private TextView stimulusText;
    private TextView countdownText;
    private TextView statsText;
    private Button reactButton;
    private Button restartButton;

    private final GameEngine gameEngine = new GameEngine();
    private BestScoreRepository bestScoreRepository;
    private CountDownTimer roundTimer;
    private long roundStartMs;
    private boolean responseRegistered;

    public static Intent createIntent(
            Context context,
            String playerName,
            Difficulty difficulty,
            int iterationsPerLevel,
            long reactionLimitMs,
            boolean inverseMode
    ) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(EXTRA_PLAYER_NAME, playerName);
        intent.putExtra(EXTRA_DIFFICULTY, difficulty.name());
        intent.putExtra(EXTRA_ITERATIONS, iterationsPerLevel);
        intent.putExtra(EXTRA_REACTION_LIMIT_MS, reactionLimitMs);
        intent.putExtra(EXTRA_INVERSE_MODE, inverseMode);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        bestScoreRepository = new BestScoreRepository(this);
        bindViews();
        setupActions();
        startGameFromIntent();
    }

    @Override
    protected void onDestroy() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        super.onDestroy();
    }

    private void bindViews() {
        statusText = findViewById(R.id.statusText);
        ruleText = findViewById(R.id.ruleText);
        stimulusText = findViewById(R.id.stimulusText);
        countdownText = findViewById(R.id.countdownText);
        statsText = findViewById(R.id.statsText);
        reactButton = findViewById(R.id.reactButton);
        restartButton = findViewById(R.id.restartButton);
    }

    private void setupActions() {
        reactButton.setOnClickListener(v -> {
            if (!gameEngine.isRunning() || responseRegistered) {
                return;
            }
            responseRegistered = true;
            long reactionTime = System.currentTimeMillis() - roundStartMs;
            resolveRound(true, reactionTime);
        });

        restartButton.setOnClickListener(v -> {
            restartButton.setVisibility(android.view.View.GONE);
            reactButton.setEnabled(true);
            startGameFromIntent();
        });
    }

    private void startGameFromIntent() {
        String playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        String difficultyName = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        int iterations = getIntent().getIntExtra(EXTRA_ITERATIONS, 20);
        long reactionLimitMs = getIntent().getLongExtra(EXTRA_REACTION_LIMIT_MS, 20_000L);
        boolean inverseMode = getIntent().getBooleanExtra(EXTRA_INVERSE_MODE, false);

        Difficulty difficulty = Difficulty.EASY;
        if (difficultyName != null) {
            difficulty = Difficulty.valueOf(difficultyName);
        }

        GameConfig config = new GameConfig(difficulty, iterations, reactionLimitMs, inverseMode);
        gameEngine.start(playerName, config);

        statusText.setText("Partida en curso");
        Toast.makeText(this, "Partida iniciada. ¡Atención!", Toast.LENGTH_SHORT).show();
        showCurrentStimulusAndStats();
        startRoundCountdown();
    }

    private void startRoundCountdown() {
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

    private void resolveRound(boolean userReacted, long reactionTimeMs) {
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

    private void showCurrentStimulusAndStats() {
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

    private void finishGame(boolean won) {
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

    private void playFeedbackSound() {
        MediaPlayer player = MediaPlayer.create(this, R.raw.entranceactivate);
        if (player == null) {
            return;
        }
        player.setOnCompletionListener(MediaPlayer::release);
        player.start();
    }
}