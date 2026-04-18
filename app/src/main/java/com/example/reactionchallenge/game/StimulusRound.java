package com.example.reactionchallenge.game;

import java.util.Collections;
import java.util.List;

public class StimulusRound {
    public final String displayText;
    public final int textColor;
    public final boolean shouldReact;
    public final String ruleDescription;
    public final List<String> options;
    public final String correctOption;

    public StimulusRound(String displayText, int textColor, boolean shouldReact, String ruleDescription) {
        this(displayText, textColor, shouldReact, ruleDescription, Collections.emptyList(), null);
    }

    public StimulusRound(String displayText, int textColor, boolean shouldReact, String ruleDescription, List<String> options, String correctOption) {
        this.displayText = displayText;
        this.textColor = textColor;
        this.shouldReact = shouldReact;
        this.ruleDescription = ruleDescription;
        this.options = options;
        this.correctOption = correctOption;
    }
}