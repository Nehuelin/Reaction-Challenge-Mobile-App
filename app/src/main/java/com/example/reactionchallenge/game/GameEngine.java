package com.example.reactionchallenge.game;

import com.example.reactionchallenge.game.rounds.ColorRoundFactory;
import com.example.reactionchallenge.game.rounds.NumberRoundFactory;
import com.example.reactionchallenge.game.rounds.RoundFactory;
import com.example.reactionchallenge.game.rounds.WordRoundFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {

    public static final int MAX_LEVELS = 3;
    private static final long MIN_DYNAMIC_REACTION_MS = 5_000L;

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
    private final ColorRoundFactory colorFactory = new ColorRoundFactory();
    private final WordRoundFactory wordFactory = new WordRoundFactory();
    private final NumberRoundFactory numberFactory = new NumberRoundFactory();

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
    private int correctStreak;
    private Difficulty currentDifficulty;
    private boolean running;
    private int targetRoundsForMinimumReaction;
    private int firstDifficultyThreshold;
    private int secondDifficultyThreshold;
    private List<String> inverseColorTargetsForLevel;
    private String inverseWordTargetForLevel;
    private String inverseNumberTargetForLevel;

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
        correctStreak = 0;
        currentDifficulty = config.difficulty;
        running = true;
        targetRoundsForMinimumReaction = calculateBestCaseRoundsToWin();
        firstDifficultyThreshold = Math.max(1, (int) Math.ceil(targetRoundsForMinimumReaction * 0.25d));
        secondDifficultyThreshold = Math.max(firstDifficultyThreshold + 1, (int) Math.ceil(targetRoundsForMinimumReaction * 0.65d));
        successfulReactionTimes.clear();
        currentStimulus = generateStimulus();
        inverseColorTargetsForLevel = null;
        inverseWordTargetForLevel = null;
        inverseNumberTargetForLevel = null;
    }

    public RoundOutcome resolveRoundInput(boolean correct, long reactionTimeMs, boolean hasUserInput) {
        return evaluateRound(correct, reactionTimeMs, hasUserInput);
    }

    public RoundOutcome resolveReaction(boolean reacted, long reactionTimeMs) {
        boolean correct = currentStimulus.shouldReact() == reacted;
        return resolveRoundInput(correct, reactionTimeMs, reacted);
    }

    public RoundOutcome resolveChoice(String selectedOption, long reactionTimeMs) {
        boolean correct = selectedOption != null && selectedOption.equals(currentStimulus.getCorrectOption());
        return resolveRoundInput(correct, reactionTimeMs, selectedOption != null);
    }

    private RoundOutcome evaluateRound(boolean correct, long reactionTimeMs, boolean hasUserInput) {
        totalRounds++;
        if (correct) {
            correctAnswers++;
            levelProgress++;
            correctStreak++;
            if (hasUserInput) {
                successfulReactionTimes.add(reactionTimeMs);
            }

            updateDynamicDifficultyForCorrectStreak();
            updateDynamicReactionLimit(true);

            if (config.difficulty != Difficulty.TRAINING) {
                int base = Math.max(10, 100 - (int) (reactionTimeMs / 100));
                score += base + level * 5;
            }

        } else {
            lives--;
            updateDynamicDifficultyForFailure(correctStreak);
            correctStreak = 0;
            updateDynamicReactionLimit(false);
            if (config.difficulty != Difficulty.TRAINING) {
                score = Math.max(0, score - 20);
            }
        }

        if (lives <= 0) {
            running = false;
            return new RoundOutcome(correct, false, true, false);
        }

        boolean levelUp = false;
        if (levelProgress >= getRequiredIterationsForCurrentLevel()) {
            levelUp = true;
            if (level >= MAX_LEVELS) {
                running = false;
                return new RoundOutcome(correct, true, true, true);
            }
            level++;
            levelProgress = 0;
            initializeInverseRuleForCurrentLevel();
        }

        currentStimulus = generateStimulus();
        return new RoundOutcome(correct, levelUp, false, false);
    }

    private StimulusRound generateStimulus() {
        if (level == 1) {
            if (config.inverseMode) {
                initializeInverseRuleForCurrentLevel();
                return colorFactory.createInverseWithTargets(currentDifficulty, random, inverseColorTargetsForLevel);
            }
            return colorFactory.create(currentDifficulty, config.inverseMode, random);
        }

        if (level == 2) {
            if (config.inverseMode) {
                initializeInverseRuleForCurrentLevel();
                return wordFactory.createInverseWithTarget(currentDifficulty, random, inverseWordTargetForLevel);
            }
            return wordFactory.create(currentDifficulty, config.inverseMode, random);
        }

        if (config.inverseMode) {
            initializeInverseRuleForCurrentLevel();
            return numberFactory.createInverseWithTarget(currentDifficulty, random, inverseNumberTargetForLevel);
        }
        return numberFactory.create(currentDifficulty, config.inverseMode, random);
    }

    private void initializeInverseRuleForCurrentLevel() {
        if (!config.inverseMode) {
            return;
        }

        if (level == 1 && inverseColorTargetsForLevel == null) {
            inverseColorTargetsForLevel = colorFactory.pickInverseTargetColors(currentDifficulty, random);
        } else if (level == 2 && inverseWordTargetForLevel == null) {
            inverseWordTargetForLevel = wordFactory.pickInverseTargetBucket(random);
        } else if (level == 3 && inverseNumberTargetForLevel == null) {
            inverseNumberTargetForLevel = numberFactory.pickInverseTargetType(random);
        }
    }


    private void updateDynamicDifficultyForCorrectStreak() {
        if (!config.dynamicDifficultyEnabled || config.difficulty == Difficulty.TRAINING) {
            return;
        }

        if (correctStreak >= secondDifficultyThreshold) {
            currentDifficulty = increaseDifficulty(increaseDifficulty(config.difficulty));
            return;
        }

        if (correctStreak >= firstDifficultyThreshold) {
            currentDifficulty = increaseDifficulty(config.difficulty);
        }
    }

    private void updateDynamicDifficultyForFailure(int streakBeforeFailure) {
        if (!config.dynamicDifficultyEnabled || config.difficulty == Difficulty.TRAINING) {
            return;
        }

        Difficulty streakDifficulty = calculateDifficultyForStreak(streakBeforeFailure);
        if (currentDifficulty.ordinal() > streakDifficulty.ordinal()) {
            currentDifficulty = decreaseDifficulty(currentDifficulty);
        }
    }

    private Difficulty calculateDifficultyForStreak(int streak) {
        if (streak >= secondDifficultyThreshold) {
            return increaseDifficulty(increaseDifficulty(config.difficulty));
        }
        if (streak >= firstDifficultyThreshold) {
            return increaseDifficulty(config.difficulty);
        }
        return config.difficulty;
    }

    private void updateDynamicReactionLimit(boolean correct) {
        if (!config.dynamicDifficultyEnabled || config.difficulty == Difficulty.TRAINING) {
            effectiveReactionMs = config.reactionLimitMs;
            return;
        }

        long step = calculateReactionAdjustmentStep();
        if (correct) {
            effectiveReactionMs = Math.max(MIN_DYNAMIC_REACTION_MS, effectiveReactionMs - step);
        } else {
            effectiveReactionMs = Math.min(config.reactionLimitMs, effectiveReactionMs + step);
        }
    }

    private long calculateReactionAdjustmentStep() {

        long configurableRange = Math.max(0L, config.reactionLimitMs - MIN_DYNAMIC_REACTION_MS);
        if (configurableRange == 0L || targetRoundsForMinimumReaction <= 1) {
            effectiveReactionMs = MIN_DYNAMIC_REACTION_MS;
            return 0L;
        }

        int progressionSteps = targetRoundsForMinimumReaction - 1;
        return Math.max(1L, configurableRange / progressionSteps);
    }

    private int calculateBestCaseRoundsToWin() {
        int base = config.iterationsPerLevel;
        int finalLevelIterations = Math.max(2, base - 1);
        return Math.max(1, base + base + finalLevelIterations);
    }

    private Difficulty increaseDifficulty(Difficulty difficulty) {
        if (difficulty == Difficulty.EASY) {
            return Difficulty.MEDIUM;
        }
        if (difficulty == Difficulty.MEDIUM) {
            return Difficulty.HARD;
        }
        return difficulty;
    }

    private Difficulty decreaseDifficulty(Difficulty difficulty) {
        if (difficulty == Difficulty.HARD) {
            return Difficulty.MEDIUM;
        }
        if (difficulty == Difficulty.MEDIUM) {
            return Difficulty.EASY;
        }
        return difficulty;
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

    public int getRequiredIterationsForCurrentLevel() {
        int base = config.iterationsPerLevel;
        if (level < 3) {
            return base;
        }
        switch (currentDifficulty) {
            case TRAINING:
            case EASY:
                return Math.max(2, base - 4);
            case MEDIUM:
                return Math.max(2, base - 2);
            case HARD:
            default:
                return Math.max(2, base - 1);
        }
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

    public Difficulty getCurrentDifficulty(){
        return currentDifficulty;
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

}