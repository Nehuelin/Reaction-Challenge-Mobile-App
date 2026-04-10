package com.example.reactionchallenge.game;

public class StimulusRound {
    public final String displayText;
    public final int textColor;
    public final boolean shouldReact;
    public final String ruleDescription;

    public StimulusRound(String displayText, int textColor, boolean shouldReact, String ruleDescription) {
        this.displayText = displayText;
        this.textColor = textColor;
        this.shouldReact = shouldReact;
        this.ruleDescription = ruleDescription;
    }
}