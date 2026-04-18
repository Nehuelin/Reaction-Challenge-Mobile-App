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
        String[] names;
        int[] colors;
        String[] targetColors;
        if (config.difficulty == Difficulty.HARD) {
            names = new String[]{"ROJO", "VERDE", "AZUL", "AMARILLO", "MORADO", "NARANJA", "CIAN", "MAGENTA"};
            colors = new int[]{
                    Color.RED,
                    Color.GREEN,
                    Color.BLUE,
                    Color.rgb(205, 160, 0),
                    Color.rgb(128, 0, 128),
                    Color.rgb(255, 140, 0),
                    Color.CYAN,
                    Color.MAGENTA
            };
            targetColors = new String[]{"ROJO", "AZUL", "MORADO"};
        } else if (config.difficulty == Difficulty.MEDIUM) {
            names = new String[]{"ROJO", "VERDE", "AZUL", "AMARILLO", "NARANJA", "MORADO"};
            colors = new int[]{
                    Color.RED,
                    Color.GREEN,
                    Color.BLUE,
                    Color.rgb(205, 160, 0),
                    Color.rgb(255, 140, 0),
                    Color.rgb(128, 0, 128)
            };
            targetColors = new String[]{"ROJO", "AZUL"};
        } else {
            names = new String[]{"ROJO", "VERDE", "AZUL", "AMARILLO"};
            colors = new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.rgb(205, 160, 0)};
            targetColors = new String[]{"ROJO"};
        }

        int pick = random.nextInt(names.length);

        String selected = names[pick];

        boolean isTargetColor = contains(selected, targetColors);
        boolean shouldReact = !isTargetColor;

        String colorList = String.join(", ", targetColors);

        String rule = config.inverseMode
                ? "Modo inverso: reacciona SOLO ante " + colorList
                : "Regla: reacciona ante todos los colores excepto " + colorList;
        if (config.inverseMode) {
            shouldReact = !shouldReact;
        }
        return new StimulusRound("Color " + selected, colors[pick], shouldReact, rule);
    }

    private StimulusRound generateWordStimulus() {
        String[] words;
        int maxLength;
        if (config.difficulty == Difficulty.HARD) {
            words = new String[]{"SOL", "GATO", "MONTAÑA", "PLANETA", "ALGORITMO", "DESARROLLO", "NUBE", "RAYO"};
            maxLength = 6;
        } else if (config.difficulty == Difficulty.MEDIUM) {
            words = new String[]{"CASA", "SOL", "LUNA", "PERRO", "GATO", "LLUVIA", "ARBOL", "MONTE"};
            maxLength = 5;
        } else {
            words = new String[]{"CASA", "SOL", "LUNA", "PERRO", "GATO"};
            maxLength = 4;
        }

        String selected = words[random.nextInt(words.length)];

        boolean shouldReact = selected.length() <= maxLength;

        String rule = config.inverseMode
                ? "Modo inverso: NO reacciones ante palabras de " + maxLength + " letras o menos"
                : "Regla: reacciona ante palabras de " + maxLength + " letras o menos";
        if (config.inverseMode) {
            shouldReact = !shouldReact;
        }
        return new StimulusRound("Palabra " + selected, Color.DKGRAY, shouldReact, rule);
    }

    private StimulusRound generatePrimeNumberStimulus() {
        int lower;
        int range;
        if (config.difficulty == Difficulty.HARD) {
            lower = 200;
            range = 300;
        } else if (config.difficulty == Difficulty.MEDIUM) {
            lower = 120;
            range = 180;
        } else {
            lower = 100;
            range = 80;
        }

        int value = lower + random.nextInt(range);
        boolean prime = isPrime(value);
        boolean shouldReact;
        String rule;
        if (config.difficulty == Difficulty.HARD) {
            boolean divisibleByThree = value % 3 == 0;
            boolean target = prime || divisibleByThree;
            shouldReact = !target;
            rule = config.inverseMode
                    ? "Modo inverso: reacciona SOLO ante primos o múltiplos de 3"
                    : "Regla: no reacciones ante primos o múltiplos de 3";
        } else {
            shouldReact = !prime;
            rule = config.inverseMode
                    ? "Modo inverso: NO reacciones ante números NO primos"
                    : "Regla: no reacciones ante números primos";
        }


        if (config.inverseMode) {
            shouldReact = !shouldReact;
        }
        return new StimulusRound(String.valueOf(value), Color.rgb(80, 50, 130), shouldReact, rule);
    }

    private boolean contains(String value, String[] candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
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