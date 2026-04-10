package com.example.reactionchallenge.game;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameEngine {

    public static final int MAX_LEVELS = 3;

    public static class RoundOutcome {
        public final boolean correct;
        public final boolean levelUp;
        public final boolean gameOver;
        public final boolean won;

        public RoundOutcome(boolean correct, boolean levelUp, boolean gameOver, boolean won) {
            this.correct = correct;
            this.levelUp = levelUp;
            this.gameOver = gameOver;
            this.won = won;
        }
    }

    private final Random random = new Random();
    private final List<Long> successfulReactionTimes = new ArrayList<>();

    private GameConfig config;
    private String playerName;
    private StimulusRound currentStimulus;

    private int level;
    private int lives;
    private int score;
    private int correctAnswers;
    private int totalRounds;
    private int levelProgress;
    private long effectiveReactionMs;
    private boolean running;

    public void start(String playerName, GameConfig config) {
        this.playerName = playerName;
        this.config = config;

        level = 1;
        lives = 3;
        score = 0;
        correctAnswers = 0;
        totalRounds = 0;
        levelProgress = 0;
        effectiveReactionMs = config.reactionLimitMs;
        running = true;
        successfulReactionTimes.clear();
        currentStimulus = generateStimulus();
    }

    public RoundOutcome resolveRound(boolean reacted, long reactionTimeMs) {
        totalRounds++;
        boolean correct = currentStimulus.shouldReact == reacted;

        if (correct) {
            correctAnswers++;
            levelProgress++;
            if (reacted) {
                successfulReactionTimes.add(reactionTimeMs);
            }

            if (config.difficulty != Difficulty.TRAINING) {
                int base = Math.max(10, 100 - (int) (reactionTimeMs / 100));
                score += base + level * 5;
            }

            if (config.difficulty != Difficulty.TRAINING && correctAnswers % 5 == 0) {
                effectiveReactionMs = Math.max(3000L, effectiveReactionMs - 500L);
            }
        } else {
            lives--;
            if (config.difficulty != Difficulty.TRAINING) {
                score = Math.max(0, score - 20);
            }
        }

        if (lives <= 0) {
            running = false;
            return new RoundOutcome(correct, false, true, false);
        }

        boolean levelUp = false;
        if (levelProgress >= config.iterationsPerLevel) {
            levelUp = true;
            if (level >= MAX_LEVELS) {
                running = false;
                return new RoundOutcome(correct, true, true, true);
            }
            level++;
            levelProgress = 0;
        }

        currentStimulus = generateStimulus();
        return new RoundOutcome(correct, levelUp, false, false);
    }

    private StimulusRound generateStimulus() {
        switch (level) {
            case 1:
                return generateColorStimulus();
            case 2:
                return generateWordStimulus();
            default:
                return generatePrimeNumberStimulus();
        }
    }

    private StimulusRound generateColorStimulus() {
        int pick = random.nextInt(4);
        String[] names = {"ROJO", "VERDE", "AZUL", "AMARILLO"};
        int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.rgb(205, 160, 0)};

        String selected = names[pick];
        boolean shouldReact = !"ROJO".equals(selected);
        String rule = config.inverseMode
                ? "Modo inverso: NO reacciones ante colores excepto ROJO"
                : "Regla: reacciona ante todos los colores excepto ROJO";
        if (config.inverseMode) {
            shouldReact = !shouldReact;
        }
        return new StimulusRound("Color " + selected, colors[pick], shouldReact, rule);
    }

    private StimulusRound generateWordStimulus() {
        String[] words = {"CASA", "SOL", "LUNA", "PERRO", "GATO"};
        String selected = words[random.nextInt(words.length)];
        boolean shouldReact = selected.length() <= 4;

        String rule = config.inverseMode
                ? "Modo inverso: NO reacciones ante palabras cortas (4 letras o menos)"
                : "Regla: reacciona ante palabras cortas (4 letras o menos)";
        if (config.inverseMode) {
            shouldReact = !shouldReact;
        }
        return new StimulusRound("Palabra " + selected, Color.DKGRAY, shouldReact, rule);
    }

    private StimulusRound generatePrimeNumberStimulus() {
        int value = 100 + random.nextInt(80);
        boolean prime = isPrime(value);
        boolean shouldReact = !prime;

        String rule = config.inverseMode
                ? "Modo inverso: NO reacciones ante números NO primos"
                : "Regla: no reacciones ante números primos";
        if (config.inverseMode) {
            shouldReact = !shouldReact;
        }
        return new StimulusRound(String.valueOf(value), Color.rgb(80, 50, 130), shouldReact, rule);
    }

    private boolean isPrime(int number) {
        if (number < 2) {
            return false;
        }
        for (int i = 2; i * i <= number; i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isRunning() {
        return running;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getLevel() {
        return level;
    }

    public int getLives() {
        return lives;
    }

    public int getScore() {
        return score;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public int getLevelProgress() {
        return levelProgress;
    }

    public int getIterationsPerLevel() {
        return config.iterationsPerLevel;
    }

    public long getConfiguredReactionMs() {
        return config.reactionLimitMs;
    }

    public long getEffectiveReactionMs() {
        return effectiveReactionMs;
    }

    public Difficulty getDifficulty() {
        return config.difficulty;
    }

    public StimulusRound getCurrentStimulus() {
        return currentStimulus;
    }

    public double getAverageReactionMs() {
        if (successfulReactionTimes.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (long t : successfulReactionTimes) {
            total += t;
        }
        return total / (double) successfulReactionTimes.size();
    }

    public String buildLiveStats(String bestStatsText) {
        return String.format(
                Locale.getDefault(),
                "Jugador: %s\nPuntaje: %d\nAciertos: %d/%d\nProgreso nivel: %d/%d\nTiempo configurado: %.1f s | Tiempo dinámico: %.1f s\n%s",
                playerName,
                score,
                correctAnswers,
                totalRounds,
                levelProgress,
                config.iterationsPerLevel,
                config.reactionLimitMs / 1000f,
                effectiveReactionMs / 1000f,
                bestStatsText
        );
    }
}