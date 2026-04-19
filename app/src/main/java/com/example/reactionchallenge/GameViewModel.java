package com.example.reactionchallenge;

import android.graphics.Color;
import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.reactionchallenge.game.Difficulty;
import com.example.reactionchallenge.game.DomainColor;
import com.example.reactionchallenge.game.GameConfig;
import com.example.reactionchallenge.game.GameEngine;
import com.example.reactionchallenge.game.StimulusRound;

import java.util.Collections;
import java.util.Locale;

public class GameViewModel extends ViewModel {

    private final MutableLiveData<GameUiState> uiState = new MutableLiveData<>();
    private final GameEngine gameEngine = new GameEngine();

    private CountDownTimer preRoundTimer;
    private CountDownTimer roundTimer;
    private long roundStartMs;
    private boolean responseRegistered;
    private int feedbackEventId;

    public LiveData<GameUiState> getUiState() {
        return uiState;
    }

    public void startGame(String playerName, Difficulty difficulty, int iterations, long reactionLimitMs, boolean inverseMode) {
        GameConfig config = new GameConfig(difficulty, iterations, reactionLimitMs, inverseMode);
        gameEngine.start(playerName, config);
        emitPreRoundState("Partida en curso", "Nueva ronda en: 4 s", 4);
        startPreRoundCountdown();
    }

    public void onReactPressed() {
        if (!gameEngine.isRunning() || responseRegistered) {
            return;
        }
        responseRegistered = true;
        resolveRound(gameEngine.resolveReaction(true, System.currentTimeMillis() - roundStartMs), true);
    }

    public void onChoiceSelected(String option) {
        if (!gameEngine.isRunning() || responseRegistered) {
            return;
        }
        responseRegistered = true;
        resolveRound(gameEngine.resolveChoice(option, System.currentTimeMillis() - roundStartMs), true);
    }

    public void onTimeExpired() {
        if (responseRegistered || !gameEngine.isRunning()) {
            return;
        }
        responseRegistered = true;
        long roundDuration = gameEngine.getEffectiveReactionMs();
        if (gameEngine.isInverseMode()) {
            resolveRound(gameEngine.resolveReaction(false, roundDuration), false);
        } else {
            resolveRound(gameEngine.resolveChoice(null, roundDuration), false);
        }
    }

    public void restart() {
        cancelTimers();
    }

    private void startPreRoundCountdown() {
        cancelTimers();
        responseRegistered = true;

        preRoundTimer = new CountDownTimer(4_000L, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = (millisUntilFinished + 999L) / 1_000L;
                emitPreRoundState(
                        buildStatusText(),
                        String.format(Locale.getDefault(), "Nueva ronda en: %d s", secondsLeft),
                        (int) secondsLeft
                );
            }

            @Override
            public void onFinish() {
                emitRoundActiveState(String.format(Locale.getDefault(), "Tiempo restante: %.1f s", gameEngine.getEffectiveReactionMs() / 1000f), false);
                startRoundCountdown();
            }
        }.start();
    }

    private void startRoundCountdown() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        responseRegistered = false;
        roundStartMs = System.currentTimeMillis();
        long roundDuration = gameEngine.getEffectiveReactionMs();

        roundTimer = new CountDownTimer(roundDuration, 100L) {
            @Override
            public void onTick(long millisUntilFinished) {
                emitRoundActiveState(
                        String.format(Locale.getDefault(), "Tiempo restante: %.1f s", millisUntilFinished / 1000f),
                        false
                );
            }

            @Override
            public void onFinish() {
                onTimeExpired();
            }
        }.start();
    }

    private void resolveRound(GameEngine.RoundOutcome outcome, boolean hasExplicitInput) {
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        boolean shouldPlaySound = outcome.correct && hasExplicitInput;
        GameUiState.FeedbackPulse feedbackPulse = outcome.correct
                ? GameUiState.FeedbackPulse.SUCCESS
                : GameUiState.FeedbackPulse.FAILURE;
        int currentEventId = ++feedbackEventId;
        if (outcome.gameOver) {
            emitFinishedState(outcome.won, shouldPlaySound, feedbackPulse, currentEventId);
            return;
        }

        if (outcome.levelUp) {
            emitLevelUpState(shouldPlaySound, feedbackPulse, currentEventId);
            startPreRoundCountdown();
            return;
        }

        emitRoundActiveState(
                String.format(Locale.getDefault(), "Tiempo restante: %.1f s", gameEngine.getEffectiveReactionMs() / 1000f),
                shouldPlaySound,
                feedbackPulse,
                currentEventId
        );
        startRoundCountdown();
    }

    private void emitPreRoundState(String status, String countdown, int secondsLeft) {
        StimulusRound stimulus = gameEngine.getCurrentStimulus();
        uiState.setValue(new GameUiState(
                GameUiState.Phase.PRE_ROUND_COUNTDOWN,
                status,
                buildRuleText(stimulus),
                buildRuleText(stimulus),
                Color.WHITE,
                "",
                countdown,
                buildStatsText(),
                gameEngine.isInverseMode(),
                false,
                stimulus.getOptions(),
                false,
                false,
                false,
                GameUiState.FeedbackPulse.NONE,
                feedbackEventId,
                secondsLeft
        ));
    }

    private void emitRoundActiveState(String countdown, boolean playSound) {
        emitRoundActiveState(countdown, playSound, GameUiState.FeedbackPulse.NONE, feedbackEventId);
    }

    private void emitRoundActiveState(String countdown, boolean playSound, GameUiState.FeedbackPulse feedbackPulse, int eventId){
        StimulusRound stimulus = gameEngine.getCurrentStimulus();
        uiState.setValue(new GameUiState(
                GameUiState.Phase.ROUND_ACTIVE,
                buildStatusText(),
                buildRuleText(stimulus),
                stimulus.getDisplayText(),
                toAndroidColor(stimulus.getTextColor()),
                "",
                countdown,
                buildStatsText(),
                gameEngine.isInverseMode(),
                true,
                stimulus.getOptions(),
                false,
                playSound,
                false,
                feedbackPulse,
                eventId,
                -1
        ));
    }

    private void emitLevelUpState(boolean playSound, GameUiState.FeedbackPulse feedbackPulse, int eventId) {
        StimulusRound stimulus = gameEngine.getCurrentStimulus();
        uiState.setValue(new GameUiState(
                GameUiState.Phase.LEVEL_UP,
                "¡Subiste de nivel!",
                buildRuleText(stimulus),
                stimulus.getDisplayText(),
                toAndroidColor(stimulus.getTextColor()),
                "",
                "",
                buildStatsText(),
                gameEngine.isInverseMode(),
                false,
                stimulus.getOptions(),
                false,
                playSound,
                false,
                feedbackPulse,
                eventId,
                -1
        ));
    }

    private void emitFinishedState(boolean won, boolean playSound, GameUiState.FeedbackPulse feedbackPulse, int eventId) {
        String result = won ? "¡Ganaste todos los niveles!" : "Perdiste. Puedes reiniciar.";
        String finalStats = String.format(
                Locale.getDefault(),
                "%s\nAciertos: %d/%d\nPuntaje final: %d%s\nTiempo de reacción promedio: %.0f ms",
                result,
                gameEngine.getCorrectAnswers(),
                gameEngine.getTotalRounds(),
                gameEngine.getScore(),
                gameEngine.getDifficulty() == Difficulty.TRAINING ? " (modo entrenamiento)" : "",
                gameEngine.getAverageReactionMs()
        );

        uiState.setValue(new GameUiState(
                GameUiState.Phase.GAME_FINISHED,
                result,
                "",
                "",
                Color.WHITE,
                won ? "¡VICTORIA!" : "DERROTA",
                "",
                finalStats,
                gameEngine.isInverseMode(),
                false,
                Collections.emptyList(),
                true,
                playSound,
                won,
                feedbackPulse,
                eventId,
                -1
        ));
    }

    private String buildStatusText() {
        return String.format(
                Locale.getDefault(),
                "Nivel %d de %d | Vidas: %d",
                gameEngine.getLevel(),
                GameEngine.MAX_LEVELS,
                gameEngine.getLives()
        );
    }

    private String buildStatsText() {
        return String.format(
                Locale.getDefault(),
                "Jugador: %s\nPuntaje: %d\nAciertos: %d/%d\nProgreso nivel: %d/%d\nTiempo configurado: %.1f s | Tiempo dinámico: %.1f s",
                gameEngine.getPlayerName(),
                gameEngine.getScore(),
                gameEngine.getCorrectAnswers(),
                gameEngine.getTotalRounds(),
                gameEngine.getLevelProgress(),
                gameEngine.getRequiredIterationsForCurrentLevel(),
                gameEngine.getConfiguredReactionMs() / 1000f,
                gameEngine.getEffectiveReactionMs() / 1000f
        );
    }

    private String buildRuleText(StimulusRound stimulusRound) {
        if (stimulusRound.getRuleType() == StimulusRound.RuleType.INVERSE_COLOR_TARGET) {
            String targets = gameEngine.getDifficulty() == Difficulty.HARD
                    ? "ROJO, AZUL, MORADO, CIAN"
                    : gameEngine.getDifficulty() == Difficulty.MEDIUM
                    ? "ROJO, AZUL"
                    : "ROJO";
            return "Modo inverso: reacciona SOLO ante " + targets;
        }
        return GameUiState.buildRuleText(stimulusRound.getRuleType(), gameEngine.isInverseMode());
    }

    public boolean shouldPersistScore() {
        return gameEngine.getDifficulty() != Difficulty.TRAINING;
    }

    public String getPlayerName() {
        return gameEngine.getPlayerName();
    }

    public int getScore() {
        return gameEngine.getScore();
    }

    public double getAverageReactionMs() {
        return gameEngine.getAverageReactionMs();
    }

    public String appendBestStats(String baseStats, String bestStatsText) {
        if (bestStatsText == null || bestStatsText.isEmpty()) {
            return baseStats;
        }
        return baseStats + "\n\n" + bestStatsText;
    }

    private int toAndroidColor(DomainColor color) {
        return Color.rgb(color.red, color.green, color.blue);
    }

    private void cancelTimers() {
        if (preRoundTimer != null) {
            preRoundTimer.cancel();
            preRoundTimer = null;
        }
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    @Override
    protected void onCleared() {
        cancelTimers();
    }

}