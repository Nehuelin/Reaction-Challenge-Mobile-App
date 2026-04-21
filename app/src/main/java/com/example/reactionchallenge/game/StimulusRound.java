package com.example.reactionchallenge.game;

import java.util.Collections;
import java.util.List;

public class StimulusRound {
    public enum Category {
        COLOR,
        WORD,
        NUMBER
    }
    public enum InputMode {
        REACTION,
        CHOICE
    }

    public enum RuleType {
        INVERSE_COLOR_TARGET,
        COLOR_SELECTION,
        INVERSE_WORD_TARGET,
        WORD_LENGTH_SELECTION,
        INVERSE_NUMBER_TARGET,
        NUMBER_CLASSIFICATION
    }

    private final Category category;
    private final InputMode inputMode;
    private final RuleType ruleType;
    private final String displayText;
    private final DomainColor textColor;
    private final boolean shouldReact;
    private final List<String> options;
    private final String correctOption;
    private final String inverseRuleText;

    public StimulusRound(Category category, InputMode inputMode, RuleType ruleType, String displayText, DomainColor textColor, boolean shouldReact, List<String> options, String correctOption){
        this(category, inputMode, ruleType, displayText, textColor, shouldReact, options, correctOption, null);
    }

    public StimulusRound(Category category, InputMode inputMode, RuleType ruleType, String displayText, DomainColor textColor, boolean shouldReact, List<String> options, String correctOption, String inverseRuleText){
        this.category = category;
        this.inputMode = inputMode;
        this.ruleType = ruleType;
        this.displayText = displayText;
        this.textColor = textColor;
        this.shouldReact = shouldReact;
        this.options = options == null ? Collections.emptyList() : Collections.unmodifiableList(options);
        this.correctOption = correctOption;
        this.inverseRuleText = inverseRuleText;
    }

    public Category getCategory() {
        return category;
    }

    public InputMode getInputMode() {
        return inputMode;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public String getDisplayText() {
        return displayText;
    }

    public DomainColor getTextColor() {
        return textColor;
    }

    public boolean shouldReact() {
        return shouldReact;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public String getInverseRuleText() {
        return inverseRuleText;
    }
}
