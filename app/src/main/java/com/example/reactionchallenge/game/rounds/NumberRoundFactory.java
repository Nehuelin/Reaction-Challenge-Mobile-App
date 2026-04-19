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

    private static final List<String> ALL_NUMBER_OPTIONS =
            Arrays.asList("PRIMO", "COMPUESTO", "PAR", "IMPAR");

    private static final DomainColor NUMBER_COLOR = new DomainColor(80, 50, 130);

    @Override
    public StimulusRound create(Difficulty difficulty, boolean inverseMode, Random random) {
        int value = generateValue(difficulty, random);
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

        String correct = classify(value, prime);
        List<String> options = buildOptions(difficulty, correct, random);

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

    private int generateValue(Difficulty difficulty, Random random) {
        int lower;
        int range;

        switch (difficulty) {
            case EASY:
                lower = 2;
                range = 28;   // 2..29
                break;
            case MEDIUM:
                lower = 10;
                range = 60;   // 10..69
                break;
            case HARD:
            default:
                lower = 40;
                range = 110;  // 40..149
                break;
        }

        return lower + random.nextInt(range);
    }

    private String classify(int value, boolean prime) {
        if (prime) {
            return "PRIMO";
        }
        if (value % 2 == 0) {
            return "PAR";
        }
        return "COMPUESTO";
    }

    private List<String> buildOptions(Difficulty difficulty, String correct, Random random) {
        List<String> options = new ArrayList<>();
        options.add(correct);

        switch (difficulty) {
            case EASY:
                options.add(getEasyDistractor(correct));
                break;

            case MEDIUM:
                options.addAll(getMediumDistractors(correct, random));
                Collections.shuffle(options, random);
                break;

            case HARD:
            default:
                for (String option : ALL_NUMBER_OPTIONS) {
                    if (!option.equals(correct)) {
                        options.add(option);
                    }
                }
                Collections.shuffle(options, random);
                break;
        }

        return options;
    }

    private String getEasyDistractor(String correct) {
        switch (correct) {
            case "PRIMO":
                return "COMPUESTO";
            case "PAR":
                return "COMPUESTO";
            case "COMPUESTO":
            default:
                return "PAR";
        }
    }

    private List<String> getMediumDistractors(String correct, Random random) {
        List<String> distractors = new ArrayList<>();

        switch (correct) {
            case "PRIMO":
                distractors.add("COMPUESTO");
                distractors.add(random.nextBoolean() ? "PAR" : "IMPAR");
                break;

            case "PAR":
                distractors.add("COMPUESTO");
                distractors.add("IMPAR");
                break;

            case "COMPUESTO":
            default:
                distractors.add("PRIMO");
                distractors.add("PAR");
                break;
        }

        return distractors;
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