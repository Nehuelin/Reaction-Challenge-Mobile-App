package com.example.reactionchallenge.game;

public class GameConfig {
    public final Difficulty difficulty;
    public final int iterationsPerLevel;
    public final long reactionLimitMs;
    public final boolean inverseMode;

    public GameConfig(Difficulty difficulty, int iterationsPerLevel, long reactionLimitMs, boolean inverseMode) {
        this.difficulty = difficulty;
        this.iterationsPerLevel = iterationsPerLevel;
        this.reactionLimitMs = reactionLimitMs;
        this.inverseMode = inverseMode;
    }
}