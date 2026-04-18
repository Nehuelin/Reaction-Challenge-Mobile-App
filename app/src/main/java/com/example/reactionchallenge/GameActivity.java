package com.example.reactionchallenge;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.reactionchallenge.data.BestScoreRepository;
import com.example.reactionchallenge.game.Difficulty;
import com.example.reactionchallenge.game.GameConfig;
import com.example.reactionchallenge.game.GameEngine;
import com.example.reactionchallenge.game.StimulusRound;

import java.util.ArrayList;
import java.util.List;
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
    private LinearLayout answerOptionsContainer;
    private final List<Button> optionButtons = new ArrayList<>();
    private Button restartButton;

    private final GameEngine gameEngine = new GameEngine();
    private BestScoreRepository bestScoreRepository;
    private CountDownTimer roundTimer;
    private CountDownTimer preRoundTimer;
    private long roundStartMs;
    private boolean responseRegistered;
    private float defaultRuleTextSizeSp;

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
        if (preRoundTimer != null) {
            preRoundTimer.cancel();
        }
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
        answerOptionsContainer = findViewById(R.id.answerOptionsContainer);
        optionButtons.add(findViewById(R.id.optionButton1));
        optionButtons.add(findViewById(R.id.optionButton2));
        optionButtons.add(findViewById(R.id.optionButton3));
        optionButtons.add(findViewById(R.id.optionButton4));
        defaultRuleTextSizeSp = ruleText.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
    }

    private void setupActions() {
        reactButton.setOnClickListener(v -> {
            if (!gameEngine.isRunning() || responseRegistered) {
                return;
            }
            responseRegistered = true;
            long reactionTime = System.currentTimeMillis() - roundStartMs;
            resolveReactionRound(true, reactionTime);
        });

        for (Button optionButton : optionButtons) {
            optionButton.setOnClickListener(v -> onAnswerSelected(optionButton.getText().toString()));
        }

        restartButton.setOnClickListener(v -> {
            restartButton.setVisibility(android.view.View.GONE);
            reactButton.setEnabled(true);
            setOptionsEnabled(true);
            startGameFromIntent();
        });
    }

    private void onAnswerSelected(String selectedOption) {
        if (!gameEngine.isRunning() || responseRegistered) {
            return;
        }
        responseRegistered = true;
        long reactionTime = System.currentTimeMillis() - roundStartMs;
        resolveChoiceRound(selectedOption, reactionTime);
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
        updateInteractionMode();
        showCurrentStimulusAndStats();
        startPreRoundCountdown();
    }

    private void startPreRoundCountdown() {
        if (preRoundTimer != null) {
            preRoundTimer.cancel();
        }
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        responseRegistered = true;
        setOptionsEnabled(false);

        StimulusRound stimulus = gameEngine.getCurrentStimulus();
        ruleText.setText(stimulus.ruleDescription);
        ruleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
        stimulusText.setText(stimulus.ruleDescription);
        stimulusText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        stimulusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f);

        preRoundTimer = new CountDownTimer(4_000L, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = (millisUntilFinished + 999L) / 1_000L;
                countdownText.setText(String.format(
                        Locale.getDefault(),
                        "Nueva ronda en: %d s",
                        secondsLeft
                ));
            }

            @Override
            public void onFinish() {
                restoreRoundView();
                showCurrentStimulusAndStats();
                startRoundCountdown();
            }
        }.start();
    }

    private void restoreRoundView() {
        ruleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultRuleTextSizeSp);
        stimulusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 46f);
        setOptionsEnabled(true);
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
                    if (gameEngine.isInverseMode()) {
                        resolveReactionRound(false, roundDuration);
                    } else {
                        resolveChoiceRound(null, roundDuration);
                    }
                }
            }
        }.start();
    }

    private void resolveReactionRound(boolean userReacted, long reactionTimeMs) {
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
            startPreRoundCountdown();
            return;
        }

        showCurrentStimulusAndStats();
        startRoundCountdown();
    }

    private void resolveChoiceRound(String selectedOption, long reactionTimeMs) {
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        GameEngine.RoundOutcome outcome = gameEngine.resolveChoice(selectedOption, reactionTimeMs);
        if (outcome.correct) {
            playFeedbackSound();
        }

        if (outcome.gameOver) {
            finishGame(outcome.won);
            return;
        }

        if (outcome.levelUp) {
            Toast.makeText(this, "¡Subiste de nivel!", Toast.LENGTH_SHORT).show();
            startPreRoundCountdown();
            return;
        }

        showCurrentStimulusAndStats();
        startRoundCountdown();
    }

    private void updateInteractionMode() {
        if (gameEngine.isInverseMode()) {
            reactButton.setVisibility(View.VISIBLE);
            answerOptionsContainer.setVisibility(View.GONE);
        } else {
            reactButton.setVisibility(View.GONE);
            answerOptionsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setOptionsEnabled(boolean enabled) {
        reactButton.setEnabled(enabled);
        for (Button optionButton : optionButtons) {
            optionButton.setEnabled(enabled);
        }
    }

    private void showCurrentStimulusAndStats() {
        StimulusRound stimulus = gameEngine.getCurrentStimulus();
        stimulusText.setText(stimulus.displayText);
        stimulusText.setTextColor(stimulus.textColor);
        ruleText.setText(stimulus.ruleDescription);
        updateOptionButtons(stimulus);
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

    private void updateOptionButtons(StimulusRound stimulus) {
        if (gameEngine.isInverseMode()) {
            return;
        }
        for (int i = 0; i < optionButtons.size(); i++) {
            Button button = optionButtons.get(i);
            if (i < stimulus.options.size()) {
                button.setVisibility(View.VISIBLE);
                button.setText(stimulus.options.get(i));
            } else {
                button.setVisibility(View.GONE);
            }
        }
    }

    private void finishGame(boolean won) {
        setOptionsEnabled(false);
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