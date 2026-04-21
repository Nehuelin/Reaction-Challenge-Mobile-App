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
    private static final String CATEGORY_LESS_THAN_50 = "MENOR A 50";
    private static final String CATEGORY_50_TO_99 = "ENTRE 50 Y 99";
    private static final String CATEGORY_100_OR_MORE = "100 O MÁS";
    private static final String CATEGORY_REPEATED_DIGITS = "REPITE DÍGITOS";

    private static final List<String> ALL_NUMBER_OPTIONS =
            Arrays.asList(
                    CATEGORY_LESS_THAN_50,
                    CATEGORY_50_TO_99,
                    CATEGORY_100_OR_MORE,
                    CATEGORY_REPEATED_DIGITS
            );

    private static final List<Integer> HARD_TRAP_VALUES = Arrays.asList(49, 50, 99, 100, 101, 111, 121, 222);
    private static final DomainColor NUMBER_COLOR = new DomainColor(80, 50, 130);

    @Override
    public StimulusRound create(Difficulty difficulty, boolean inverseMode, Random random) {
        int value = generateValue(difficulty, random);

        if (inverseMode) {
            String targetType = pickInverseTargetType(random);
            return createInverseRound(value, targetType);
        }

        String correct = classify(value);
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

    public String pickInverseTargetType(Random random) {
        return ALL_NUMBER_OPTIONS.get(random.nextInt(ALL_NUMBER_OPTIONS.size()));
    }

    public StimulusRound createInverseWithTarget(Difficulty difficulty, Random random, String targetType) {
        int value = generateValue(difficulty, random);
        return createInverseRound(value, targetType);
    }

    private int generateValue(Difficulty difficulty, Random random) {
        switch (difficulty) {
            case EASY:
                return 10 + random.nextInt(111); // 10..120
            case MEDIUM:
                return 10 + random.nextInt(190); // 10..199
            case HARD:
            default:
                if (random.nextInt(100) < 35) {
                    return HARD_TRAP_VALUES.get(random.nextInt(HARD_TRAP_VALUES.size()));
                }
                return 10 + random.nextInt(390); // 10..399
        }
    }

    public String classify(int value) {
        if (hasRepeatedDigits(value)) {
            return CATEGORY_REPEATED_DIGITS;
        }
        if (value < 50) {
            return CATEGORY_LESS_THAN_50;
        }
        if (value <= 99) {
            return CATEGORY_50_TO_99;
        }
        return CATEGORY_100_OR_MORE;
    }

    public boolean hasRepeatedDigits(int value) {
        String digits = String.valueOf(Math.abs(value));
        boolean[] seen = new boolean[10];

        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(i) - '0';
            if (seen[digit]) {
                return true;
            }
            seen[digit] = true;
        }
        return false;
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
        if (CATEGORY_REPEATED_DIGITS.equals(correct)) {
            return CATEGORY_100_OR_MORE;
        }
        if (CATEGORY_LESS_THAN_50.equals(correct)) {
            return CATEGORY_50_TO_99;
        }
        if (CATEGORY_50_TO_99.equals(correct)) {
            return CATEGORY_LESS_THAN_50;
        }
        return CATEGORY_50_TO_99;
    }

    private List<String> getMediumDistractors(String correct, Random random) {
        List<String> distractorPool = new ArrayList<>(ALL_NUMBER_OPTIONS);
        distractorPool.remove(correct);
        Collections.shuffle(distractorPool, random);
        return new ArrayList<>(distractorPool.subList(0, 2));
    }

    private boolean matchesInverseTarget(int value, String targetType) {
        return classify(value).equals(targetType);
    }

    private StimulusRound createInverseRound(int value, String targetType) {
        return new StimulusRound(
                StimulusRound.Category.NUMBER,
                StimulusRound.InputMode.REACTION,
                StimulusRound.RuleType.INVERSE_NUMBER_TARGET,
                String.valueOf(value),
                NUMBER_COLOR,
                matchesInverseTarget(value, targetType),
                Collections.emptyList(),
                null,
                "Modo inverso: reacciona SOLO ante números " + targetType
        );
    }
}