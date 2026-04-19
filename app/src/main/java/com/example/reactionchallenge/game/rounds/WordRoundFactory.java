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

    private static final List<String> FIXED_WORD_OPTIONS = Arrays.asList("CORTA (2-3)", "MEDIA (4-5)", "LARGA (6-7)", "MUY LARGA (8-9)");
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

        if (inverseMode) {
            String targetBucket = pickInverseTargetBucket(random);
            return createInverseRound(selected, targetBucket);
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

    public String pickInverseTargetBucket(Random random) {
        return FIXED_WORD_OPTIONS.get(random.nextInt(FIXED_WORD_OPTIONS.size()));
    }

    public StimulusRound createInverseWithTarget(Difficulty difficulty, Random random, String targetBucket) {
        List<WordStimulus> words = buildWordPool(difficulty);
        WordStimulus selected = words.get(random.nextInt(words.size()));
        return createInverseRound(selected, targetBucket);
    }

    private List<WordStimulus> buildWordPool(Difficulty difficulty) {
        List<WordStimulus> words = new ArrayList<>();

        words.add(new WordStimulus("SOL", "CORTA (2-3)"));
        words.add(new WordStimulus("MAR", "CORTA (2-3)"));
        words.add(new WordStimulus("LAGO", "MEDIA (4-5)"));
        words.add(new WordStimulus("FLOR", "MEDIA (4-5)"));
        words.add(new WordStimulus("PLANETA", "LARGA (6-7)"));
        words.add(new WordStimulus("MONTAÑA", "LARGA (6-7)"));
        words.add(new WordStimulus("LUZ", "CORTA (2-3)"));
        words.add(new WordStimulus("PAN", "CORTA (2-3)"));
        words.add(new WordStimulus("RIO", "CORTA (2-3)"));
        words.add(new WordStimulus("HOJA", "MEDIA (4-5)"));
        words.add(new WordStimulus("ESTRELLA", "MUY LARGA (8-9)"));
        words.add(new WordStimulus("BOSQUE", "LARGA (6-7)"));

        if (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) {
            words.add(new WordStimulus("GALAXIA", "LARGA (6-7)"));
            words.add(new WordStimulus("RELÁMPAGO", "MUY LARGA (8-9)"));
            words.add(new WordStimulus("ECLIPSE", "LARGA (6-7)"));
            words.add(new WordStimulus("HORIZONTE", "MUY LARGA (8-9)"));
            words.add(new WordStimulus("NUBE", "MEDIA (4-5)"));
            words.add(new WordStimulus("TRUENO", "LARGA (6-7)"));
            words.add(new WordStimulus("COMETA", "LARGA (6-7)"));
            words.add(new WordStimulus("ROCÍO", "MEDIA (4-5)"));
            words.add(new WordStimulus("TORMENTA", "MUY LARGA (8-9)"));
            words.add(new WordStimulus("AURORA", "LARGA (6-7)"));
        }

        if (difficulty == Difficulty.HARD) {
            words.add(new WordStimulus("TRANSFORMACIÓN", "MUY LARGA (8-9)"));
            words.add(new WordStimulus("DESARROLLADOR", "MUY LARGA (8-9)"));
            words.add(new WordStimulus("ALGORITMO", "MUY LARGA (8-9)"));
            words.add(new WordStimulus("CÓDIGO", "LARGA (6-7)"));
            words.add(new WordStimulus("IA", "CORTA (2-3)"));
            words.add(new WordStimulus("ARQUITECTURA", "MUY LARGA (8-9)"));
            words.add(new WordStimulus("PROGRAMACIÓN", "MUY LARGA (8-9)"));
            words.add(new WordStimulus("DATOS", "MEDIA (4-5)"));
            words.add(new WordStimulus("SISTEMA", "LARGA (6-7)"));
            words.add(new WordStimulus("AUTOMATIZACIÓN", "MUY LARGA (8-9)"));
        }

        return words;
    }

    private StimulusRound createInverseRound(WordStimulus selected, String targetBucket) {
        boolean shouldReact = targetBucket.equals(selected.bucket);
        return new StimulusRound(
                StimulusRound.Category.WORD,
                StimulusRound.InputMode.REACTION,
                StimulusRound.RuleType.INVERSE_WORD_TARGET,
                selected.word,
                WORD_COLOR,
                shouldReact,
                Collections.emptyList(),
                null,
                "Modo inverso: reacciona SOLO ante palabras de longitud " + targetBucket
        );
    }
}