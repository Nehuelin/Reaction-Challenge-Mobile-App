package com.example.reactionchallenge.game.rounds;

import com.example.reactionchallenge.game.Difficulty;
import com.example.reactionchallenge.game.DomainColor;
import com.example.reactionchallenge.game.StimulusRound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NumberRoundFactory implements RoundFactory {

    private static final List<String> FIXED_NUMBER_OPTIONS = Arrays.asList("PRIMO", "COMPUESTO", "PAR", "IMPAR");
    private static final DomainColor NUMBER_COLOR = new DomainColor(80, 50, 130);

    @Override
    public StimulusRound create(Difficulty difficulty, boolean inverseMode, Random random) {
        int lower = difficulty == Difficulty.HARD ? 200 : difficulty == Difficulty.MEDIUM ? 120 : 90;
        int range = difficulty == Difficulty.HARD ? 320 : difficulty == Difficulty.MEDIUM ? 220 : 120;

        int value = lower + random.nextInt(range);
        boolean prime = isPrime(value);

        if (inverseMode) {
            return new StimulusRound(
                    StimulusRound.Category.NUMBER,
                    StimulusRound.InputMode.REACTION,
                    StimulusRound.RuleType.INVERSE_PRIME_RULE,
                    String.valueOf(value),
                    NUMBER_COLOR,
                    !prime,
                    Collections.emptyList(),
                    null
            );
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
        if (difficulty == Difficulty.HARD) {
            Collections.shuffle(options, random);
        }

        return new StimulusRound(
                StimulusRound.Category.NUMBER,
                StimulusRound.InputMode.CHOICE,
                StimulusRound.RuleType.NUMBER_CLASSIFICATION,
                String.valueOf(value),
                NUMBER_COLOR,
                true,
                options,
                correct
        );
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
}