package com.example.reactionchallenge.game.rounds;

import com.example.reactionchallenge.game.Difficulty;
import com.example.reactionchallenge.game.DomainColor;
import com.example.reactionchallenge.game.StimulusRound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WordRoundFactory implements RoundFactory {

    private static final List<String> FIXED_WORD_OPTIONS = Arrays.asList("CORTA", "MEDIA", "LARGA", "MUY LARGA");
    private static final DomainColor WORD_COLOR = new DomainColor(55, 55, 55);

    private static class WordStimulus {
        final String word;
        final String bucket;

        WordStimulus(String word, String bucket) {
            this.word = word;
            this.bucket = bucket;
        }
    }

    @Override
    public StimulusRound create(Difficulty difficulty, boolean inverseMode, Random random) {
        List<WordStimulus> words = buildWordPool(difficulty);
        WordStimulus selected = words.get(random.nextInt(words.size()));

        boolean shouldReact = "MUY LARGA".equals(selected.bucket);
        if (inverseMode) {
            return new StimulusRound(
                    StimulusRound.Category.WORD,
                    StimulusRound.InputMode.REACTION,
                    StimulusRound.RuleType.INVERSE_WORD_TARGET,
                    selected.word,
                    WORD_COLOR,
                    shouldReact,
                    Collections.emptyList(),
                    null
            );
        }

        List<String> options = new ArrayList<>(FIXED_WORD_OPTIONS);
        if (difficulty == Difficulty.HARD) {
            Collections.shuffle(options, random);
        }

        return new StimulusRound(
                StimulusRound.Category.WORD,
                StimulusRound.InputMode.CHOICE,
                StimulusRound.RuleType.WORD_LENGTH_SELECTION,
                selected.word,
                WORD_COLOR,
                true,
                options,
                selected.bucket
        );
    }

    private List<WordStimulus> buildWordPool(Difficulty difficulty) {
        List<WordStimulus> words = new ArrayList<>();
        words.add(new WordStimulus("SOL", "CORTA"));
        words.add(new WordStimulus("MAR", "CORTA"));
        words.add(new WordStimulus("LAGO", "MEDIA"));
        words.add(new WordStimulus("FLOR", "MEDIA"));
        words.add(new WordStimulus("PLANETA", "LARGA"));
        words.add(new WordStimulus("MONTAÑA", "LARGA"));

        if (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) {
            words.add(new WordStimulus("GALAXIA", "LARGA"));
            words.add(new WordStimulus("RELÁMPAGO", "MUY LARGA"));
            words.add(new WordStimulus("ECLIPSE", "LARGA"));
            words.add(new WordStimulus("HORIZONTE", "MUY LARGA"));
            words.add(new WordStimulus("NUBE", "MEDIA"));
        }
        if (difficulty == Difficulty.HARD) {
            words.add(new WordStimulus("TRANSFORMACIÓN", "MUY LARGA"));
            words.add(new WordStimulus("DESARROLLADOR", "MUY LARGA"));
            words.add(new WordStimulus("ALGORITMO", "MUY LARGA"));
            words.add(new WordStimulus("CÓDIGO", "LARGA"));
            words.add(new WordStimulus("IA", "CORTA"));
        }
        return words;
    }
}