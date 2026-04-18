package com.example.reactionchallenge.game;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameEngine {

    public static final int MAX_LEVELS = 3;

    private static final List<String> FIXED_COLOR_OPTIONS = Arrays.asList("ROJO", "AZUL", "VERDE", "AMARILLO");
    private static final List<String> FIXED_WORD_OPTIONS = Arrays.asList("CORTA", "MEDIA", "LARGA", "MUY LARGA");
    private static final List<String> FIXED_NUMBER_OPTIONS = Arrays.asList("PRIMO", "COMPUESTO", "PAR", "IMPAR");

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

    private static class ColorStimulus {
        final String label;
        final int color;

        ColorStimulus(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private static class WordStimulus {
        final String word;
        final String bucket;

        WordStimulus(String word, String bucket) {
            this.word = word;
            this.bucket = bucket;
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
        boolean correct = currentStimulus.shouldReact == reacted;
        return evaluateRound(correct, reactionTimeMs, reacted);
    }

    public RoundOutcome resolveChoice(String selectedOption, long reactionTimeMs) {
        boolean correct = selectedOption != null && selectedOption.equals(currentStimulus.correctOption);
        return evaluateRound(correct, reactionTimeMs, selectedOption != null);
    }

    private RoundOutcome evaluateRound(boolean correct, long reactionTimeMs, boolean hasUserInput) {
        totalRounds++;
        if (correct) {
            correctAnswers++;
            levelProgress++;
            if (hasUserInput) {
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
        List<ColorStimulus> palette = buildColorPalette();
        ColorStimulus selected = palette.get(random.nextInt(palette.size()));

        String[] targetColors = config.difficulty == Difficulty.HARD
                ? new String[]{"ROJO", "AZUL", "MORADO", "CIAN"}
                : config.difficulty == Difficulty.MEDIUM
                ? new String[]{"ROJO", "AZUL"}
                : new String[]{"ROJO"};

        boolean isTargetColor = contains(selected.label, targetColors);
        boolean shouldReact = !isTargetColor;
        String colorList = String.join(", ", targetColors);

        String rule = config.inverseMode
                ? "Modo inverso: reacciona SOLO ante " + colorList
                : "Selecciona el nombre del color principal mostrado";
        if (config.inverseMode) {
            return new StimulusRound("Color " + selected.label, selected.color, isTargetColor, rule);
        }

        List<String> options = config.difficulty == Difficulty.HARD
                ? pickShuffledOptions(extractColorNames(palette), selected.label, 4)
                : new ArrayList<>(FIXED_COLOR_OPTIONS);

        return new StimulusRound("COLOR", selected.color, true, rule, options, selected.label);
    }

    private List<ColorStimulus> buildColorPalette() {
        List<ColorStimulus> palette = new ArrayList<>();
        palette.add(new ColorStimulus("ROJO", Color.rgb(220, 45, 45)));
        palette.add(new ColorStimulus("AZUL", Color.rgb(52, 116, 255)));
        palette.add(new ColorStimulus("VERDE", Color.rgb(40, 170, 90)));
        palette.add(new ColorStimulus("AMARILLO", Color.rgb(220, 185, 20)));

        if (config.difficulty == Difficulty.MEDIUM || config.difficulty == Difficulty.HARD) {
            palette.add(new ColorStimulus("ROJO", Color.rgb(180, 30, 30)));
            palette.add(new ColorStimulus("AZUL", Color.rgb(24, 75, 180)));
            palette.add(new ColorStimulus("VERDE", Color.rgb(0, 120, 80)));
            palette.add(new ColorStimulus("AMARILLO", Color.rgb(240, 210, 80)));
        }

        if (config.difficulty == Difficulty.HARD) {
            palette.add(new ColorStimulus("MORADO", Color.rgb(130, 65, 180)));
            palette.add(new ColorStimulus("NARANJA", Color.rgb(240, 120, 20)));
            palette.add(new ColorStimulus("CIAN", Color.rgb(35, 170, 185)));
            palette.add(new ColorStimulus("MAGENTA", Color.rgb(220, 40, 155)));
        }

        return palette;
    }

    private List<String> extractColorNames(List<ColorStimulus> palette) {
        List<String> names = new ArrayList<>();
        for (ColorStimulus stimulus : palette) {
            if (!names.contains(stimulus.label)) {
                names.add(stimulus.label);
            }
        }
        return names;
    }

    private StimulusRound generateWordStimulus() {
        List<WordStimulus> words = buildWordPool();
        WordStimulus selected = words.get(random.nextInt(words.size()));

        boolean shouldReact = !"MUY LARGA".equals(selected.bucket);

        String rule = config.inverseMode
                ? "Modo inverso: reacciona SOLO ante palabras MUY LARGAS"
                : "Selecciona la categoría de longitud de la palabra";
        if (config.inverseMode) {
            return new StimulusRound(selected.word, Color.DKGRAY, !shouldReact, rule);
        }

        List<String> options = new ArrayList<>(FIXED_WORD_OPTIONS);
        if (config.difficulty == Difficulty.HARD) {
            Collections.shuffle(options, random);
        }

        return new StimulusRound(selected.word, Color.DKGRAY, true, rule, options, selected.bucket);
    }

    private List<WordStimulus> buildWordPool() {
        List<WordStimulus> words = new ArrayList<>();
        words.add(new WordStimulus("SOL", "CORTA"));
        words.add(new WordStimulus("MAR", "CORTA"));
        words.add(new WordStimulus("LAGO", "MEDIA"));
        words.add(new WordStimulus("FLOR", "MEDIA"));
        words.add(new WordStimulus("PLANETA", "LARGA"));
        words.add(new WordStimulus("MONTAÑA", "LARGA"));

        if (config.difficulty == Difficulty.MEDIUM || config.difficulty == Difficulty.HARD) {
            words.add(new WordStimulus("GALAXIA", "LARGA"));
            words.add(new WordStimulus("RELÁMPAGO", "MUY LARGA"));
            words.add(new WordStimulus("ECLIPSE", "LARGA"));
            words.add(new WordStimulus("HORIZONTE", "MUY LARGA"));
            words.add(new WordStimulus("NUBE", "MEDIA"));
        }
        if (config.difficulty == Difficulty.HARD) {
            words.add(new WordStimulus("TRANSFORMACIÓN", "MUY LARGA"));
            words.add(new WordStimulus("DESARROLLADOR", "MUY LARGA"));
            words.add(new WordStimulus("ALGORITMO", "MUY LARGA"));
            words.add(new WordStimulus("CÓDIGO", "LARGA"));
            words.add(new WordStimulus("IA", "CORTA"));
        }
        return words;
    }

    private StimulusRound generatePrimeNumberStimulus() {
        int lower = config.difficulty == Difficulty.HARD ? 200 : config.difficulty == Difficulty.MEDIUM ? 120 : 90;
        int range = config.difficulty == Difficulty.HARD ? 320 : config.difficulty == Difficulty.MEDIUM ? 220 : 120;

        int value = lower + random.nextInt(range);
        boolean prime = isPrime(value);
        String rule = config.inverseMode
                ? "Modo inverso: NO reacciones ante números primos"
                : "Selecciona la mejor clasificación para el número";


        if (config.inverseMode) {
            return new StimulusRound(String.valueOf(value), Color.rgb(80, 50, 130), !prime, rule);
        }

        String correct;
        if (prime) {
            correct = "PRIMO";
        } else if (value % 2 == 0) {
            correct = "PAR";
        } else {
            correct = "COMPUESTO";
        }

        List<String> options = new ArrayList<>(FIXED_NUMBER_OPTIONS);
        if (config.difficulty == Difficulty.HARD) {
            Collections.shuffle(options, random);
        }

        return new StimulusRound(String.valueOf(value), Color.rgb(80, 50, 130), true, rule, options, correct);
    }

    private List<String> pickShuffledOptions(List<String> allOptions, String correct, int desiredCount) {
        List<String> pool = new ArrayList<>();
        for (String option : allOptions) {
            if (!option.equals(correct)) {
                pool.add(option);
            }
        }
        Collections.shuffle(pool, random);

        List<String> result = new ArrayList<>();
        result.add(correct);
        for (int i = 0; i < pool.size() && result.size() < desiredCount; i++) {
            result.add(pool.get(i));
        }
        Collections.shuffle(result, random);
        return result;
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

    public boolean isInverseMode() {
        return config.inverseMode;
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