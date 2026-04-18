package com.example.reactionchallenge.game.rounds;

import com.example.reactionchallenge.game.Difficulty;
import com.example.reactionchallenge.game.StimulusRound;

import java.util.Random;

public interface RoundFactory {
    StimulusRound create(Difficulty difficulty, boolean inverseMode, Random random);
}
