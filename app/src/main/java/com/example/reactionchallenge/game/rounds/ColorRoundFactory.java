package com.example.reactionchallenge.game.rounds;

import com.example.reactionchallenge.game.Difficulty;
import com.example.reactionchallenge.game.DomainColor;
import com.example.reactionchallenge.game.StimulusRound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ColorRoundFactory implements RoundFactory {

    private static final List<String> BASE_COLOR_OPTIONS = Arrays.asList("ROJO", "AZUL", "VERDE", "AMARILLO");

    private static class ColorStimulus {
        final String label;
        final DomainColor color;

        ColorStimulus(String label, DomainColor color) {
            this.label = label;
            this.color = color;
        }
    }

    @Override
    public StimulusRound create(Difficulty difficulty, boolean inverseMode, Random random) {
        List<ColorStimulus> palette = buildColorPalette(difficulty);
        ColorStimulus selected = palette.get(random.nextInt(palette.size()));

        String[] targetColors = difficulty == Difficulty.HARD
                ? new String[]{"ROJO", "AZUL", "MORADO", "CIAN"}
                : difficulty == Difficulty.MEDIUM
                ? new String[]{"ROJO", "AZUL"}
                : new String[]{"ROJO"};

        boolean isTargetColor = contains(selected.label, targetColors);

        if (inverseMode) {
            return new StimulusRound(
                    StimulusRound.Category.COLOR,
                    StimulusRound.InputMode.REACTION,
                    StimulusRound.RuleType.INVERSE_COLOR_TARGET,
                    "Color " + selected.label,
                    selected.color,
                    isTargetColor,
                    Collections.emptyList(),
                    null
            );
        }

        List<String> options;
        if (difficulty == Difficulty.EASY) {
            options = new ArrayList<>(BASE_COLOR_OPTIONS);
        } else {
            options = pickShuffledOptions(extractColorNames(palette), selected.label, 4, random);
        }


        return new StimulusRound(StimulusRound.Category.COLOR, StimulusRound.InputMode.CHOICE, StimulusRound.RuleType.COLOR_SELECTION, "COLOR", selected.color, true, options, selected.label);
    }

    private List<ColorStimulus> buildColorPalette(Difficulty difficulty) {
        List<ColorStimulus> palette = new ArrayList<>();
        palette.add(new ColorStimulus("ROJO", new DomainColor(220, 45, 45)));
        palette.add(new ColorStimulus("AZUL", new DomainColor(52, 116, 255)));
        palette.add(new ColorStimulus("VERDE", new DomainColor(40, 170, 90)));
        palette.add(new ColorStimulus("AMARILLO", new DomainColor(220, 185, 20)));

        if (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) {
            palette.add(new ColorStimulus("ROJO", new DomainColor(180, 30, 30)));
            palette.add(new ColorStimulus("AZUL", new DomainColor(24, 75, 180)));
            palette.add(new ColorStimulus("VERDE", new DomainColor(0, 120, 80)));
            palette.add(new ColorStimulus("AMARILLO", new DomainColor(240, 210, 80)));
            palette.add(new ColorStimulus("ROSA", new DomainColor(245, 120, 180)));
            palette.add(new ColorStimulus("MARRÓN", new DomainColor(120, 72, 35)));

        }

        if (difficulty == Difficulty.HARD) {
            palette.add(new ColorStimulus("MORADO", new DomainColor(130, 65, 180)));
            palette.add(new ColorStimulus("NARANJA", new DomainColor(240, 120, 20)));
            palette.add(new ColorStimulus("CIAN", new DomainColor(35, 170, 185)));
            palette.add(new ColorStimulus("MAGENTA", new DomainColor(220, 40, 155)));
            palette.add(new ColorStimulus("TURQUESA", new DomainColor(64, 224, 208)));
            palette.add(new ColorStimulus("LIMA", new DomainColor(140, 220, 60)));
            palette.add(new ColorStimulus("GRIS", new DomainColor(140, 140, 140)));
            palette.add(new ColorStimulus("NEGRO", new DomainColor(35, 35, 35)));
            palette.add(new ColorStimulus("BLANCO", new DomainColor(245, 245, 245)));
            palette.add(new ColorStimulus("DORADO", new DomainColor(212, 175, 55)));
            palette.add(new ColorStimulus("PLATEADO", new DomainColor(192, 192, 192)));
            palette.add(new ColorStimulus("BEIGE", new DomainColor(220, 200, 160)));
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

    private List<String> pickShuffledOptions(List<String> allOptions, String correct, int desiredCount, Random random) {
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
}