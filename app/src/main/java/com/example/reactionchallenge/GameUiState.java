package com.example.reactionchallenge;

import com.example.reactionchallenge.game.StimulusRound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameUiState {

    public enum Phase {
        PRE_ROUND_COUNTDOWN,
        ROUND_ACTIVE,
        LEVEL_UP,
        GAME_FINISHED
    }

    public enum FeedbackPulse {
        NONE,
        SUCCESS,
        FAILURE
    }

    public final Phase phase;
    public final String statusText;
    public final String ruleText;
    public final String stimulusText;
    public final int stimulusColor;
    public final String finalResultText;
    public final String countdownText;
    public final String statsText;
    public final boolean showReactButton;
    public final boolean optionsEnabled;
    public final List<String> options;
    public final boolean restartVisible;
    public final boolean playSuccessSound;
    public final boolean gameWon;
    public final FeedbackPulse feedbackPulse;
    public final int feedbackEventId;
    public final int preRoundSecondsLeft;

    public GameUiState(Phase phase, String statusText, String ruleText, String stimulusText, int stimulusColor, String finalResultText, String countdownText, String statsText, boolean showReactButton, boolean optionsEnabled, List<String> options, boolean restartVisible, boolean playSuccessSound, boolean gameWon, FeedbackPulse feedbackPulse, int feedbackEventId, int preRoundSecondsLeft){
        this.phase = phase;
        this.statusText = statusText;
        this.ruleText = ruleText;
        this.stimulusText = stimulusText;
        this.stimulusColor = stimulusColor;
        this.finalResultText = finalResultText;
        this.countdownText = countdownText;
        this.statsText = statsText;
        this.showReactButton = showReactButton;
        this.optionsEnabled = optionsEnabled;
        this.options = options == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(options));
        this.restartVisible = restartVisible;
        this.playSuccessSound = playSuccessSound;
        this.gameWon = gameWon;
        this.feedbackPulse = feedbackPulse;
        this.feedbackEventId = feedbackEventId;
        this.preRoundSecondsLeft = preRoundSecondsLeft;
    }

    public static String buildRuleText(StimulusRound.RuleType ruleType, boolean inverseMode) {
        switch (ruleType) {
            case INVERSE_COLOR_TARGET:
                return "Modo inverso: reacciona SOLO ante color objetivo";
            case COLOR_SELECTION:
                return "Selecciona el nombre del color principal mostrado";
            case INVERSE_WORD_TARGET:
                return "Modo inverso: reacciona SOLO ante palabras MUY LARGAS";
            case WORD_LENGTH_SELECTION:
                return "Selecciona la categoría de longitud de la palabra";
            case INVERSE_NUMBER_TARGET:
                return "Modo inverso: reacciona SOLO ante la categoría objetivo";
            case NUMBER_CLASSIFICATION:
                return "Selecciona la mejor clasificación para el número";
            default:
                return inverseMode ? "Modo inverso" : "Ronda activa";
        }

    }
}