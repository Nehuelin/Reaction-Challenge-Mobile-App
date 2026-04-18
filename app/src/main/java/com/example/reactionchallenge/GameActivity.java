package com.example.reactionchallenge;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.reactionchallenge.data.BestScoreRepository;
import com.example.reactionchallenge.game.Difficulty;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    private static final String EXTRA_PLAYER_NAME = "extra_player_name";
    private static final String EXTRA_DIFFICULTY = "extra_difficulty";
    private static final String EXTRA_ITERATIONS = "extra_iterations";
    private static final String EXTRA_REACTION_LIMIT_MS = "extra_reaction_limit_ms";
    private static final String EXTRA_INVERSE_MODE = "extra_inverse_mode";

    private TextView statusText;
    private TextView ruleText;
    private TextView stimulusText;
    private TextView countdownText;
    private TextView statsText;
    private Button reactButton;
    private LinearLayout answerOptionsContainer;
    private final List<Button> optionButtons = new ArrayList<>();
    private Button restartButton;

    private GameViewModel viewModel;
    private BestScoreRepository bestScoreRepository;
    private float defaultRuleTextSizeSp;
    private boolean finalScoreProcessed;

    public static Intent createIntent(
            Context context,
            String playerName,
            Difficulty difficulty,
            int iterationsPerLevel,
            long reactionLimitMs,
            boolean inverseMode
    ) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(EXTRA_PLAYER_NAME, playerName);
        intent.putExtra(EXTRA_DIFFICULTY, difficulty.name());
        intent.putExtra(EXTRA_ITERATIONS, iterationsPerLevel);
        intent.putExtra(EXTRA_REACTION_LIMIT_MS, reactionLimitMs);
        intent.putExtra(EXTRA_INVERSE_MODE, inverseMode);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        bestScoreRepository = new BestScoreRepository(this);
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        bindViews();
        setupActions();
        observeState();
        startGameFromIntent();
    }

    private void bindViews() {
        statusText = findViewById(R.id.statusText);
        ruleText = findViewById(R.id.ruleText);
        stimulusText = findViewById(R.id.stimulusText);
        countdownText = findViewById(R.id.countdownText);
        statsText = findViewById(R.id.statsText);
        reactButton = findViewById(R.id.reactButton);
        restartButton = findViewById(R.id.restartButton);
        answerOptionsContainer = findViewById(R.id.answerOptionsContainer);
        optionButtons.add(findViewById(R.id.optionButton1));
        optionButtons.add(findViewById(R.id.optionButton2));
        optionButtons.add(findViewById(R.id.optionButton3));
        optionButtons.add(findViewById(R.id.optionButton4));
        defaultRuleTextSizeSp = ruleText.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
    }

    private void setupActions() {
        reactButton.setOnClickListener(v -> viewModel.onReactPressed());

        for (Button optionButton : optionButtons) {
            optionButton.setOnClickListener(v -> viewModel.onChoiceSelected(optionButton.getText().toString()));
        }

        restartButton.setOnClickListener(v -> {
            restartButton.setVisibility(View.GONE);
            startGameFromIntent();
        });
    }

    private void observeState() {
        viewModel.getUiState().observe(this, state -> {
            if (state == null) {
                return;
            }

            statusText.setText(state.statusText);
            ruleText.setText(state.ruleText);
            stimulusText.setText(state.stimulusText);
            stimulusText.setTextColor(state.stimulusColor);
            countdownText.setText(state.countdownText);
            restartButton.setVisibility(state.restartVisible ? View.VISIBLE : View.GONE);

            if (state.phase == GameUiState.Phase.PRE_ROUND_COUNTDOWN) {
                ruleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                stimulusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f);
            } else {
                ruleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultRuleTextSizeSp);
                stimulusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 46f);
            }

            reactButton.setVisibility(state.showReactButton ? View.VISIBLE : View.GONE);
            answerOptionsContainer.setVisibility(state.showReactButton ? View.GONE : View.VISIBLE);
            reactButton.setEnabled(state.optionsEnabled);
            for (Button optionButton : optionButtons) {
                optionButton.setEnabled(state.optionsEnabled);
            }

            updateOptionButtons(state.options);
            renderStats(state);

            if (state.playSuccessSound) {
                playFeedbackSound();
            }
            if (state.phase == GameUiState.Phase.LEVEL_UP) {
                Toast.makeText(this, "¡Subiste de nivel!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startGameFromIntent() {
        String playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        String difficultyName = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        int iterations = getIntent().getIntExtra(EXTRA_ITERATIONS, 20);
        long reactionLimitMs = getIntent().getLongExtra(EXTRA_REACTION_LIMIT_MS, 20_000L);
        boolean inverseMode = getIntent().getBooleanExtra(EXTRA_INVERSE_MODE, false);

        Difficulty difficulty = Difficulty.EASY;
        if (difficultyName != null) {
            difficulty = Difficulty.valueOf(difficultyName);
        }

        finalScoreProcessed = false;
        viewModel.startGame(playerName, difficulty, iterations, reactionLimitMs, inverseMode);
        Toast.makeText(this, "Partida iniciada. ¡Atención!", Toast.LENGTH_SHORT).show();
    }

    private void renderStats(GameUiState state) {
        String bestText = bestScoreRepository.buildBestStatsText(
                viewModel.getPlayerName(),
                viewModel.getScore(),
                viewModel.getAverageReactionMs(),
                state.phase == GameUiState.Phase.GAME_FINISHED
        );


        if (state.phase == GameUiState.Phase.GAME_FINISHED && viewModel.shouldPersistScore() && !finalScoreProcessed) {
            bestScoreRepository.saveIfBetter(
                    viewModel.getPlayerName(),
                    viewModel.getScore(),
                    viewModel.getAverageReactionMs()
            );
            finalScoreProcessed = true;
            bestText = bestScoreRepository.buildBestStatsText(
                    viewModel.getPlayerName(),
                    viewModel.getScore(),
                    viewModel.getAverageReactionMs(),
                    true
            );
        }

        statsText.setText(viewModel.appendBestStats(state.statsText, bestText));
    }

    private void updateOptionButtons(List<String> options) {
        for (int i = 0; i < optionButtons.size(); i++) {
            Button button = optionButtons.get(i);
            if (i < options.size()) {
                button.setVisibility(View.VISIBLE);
                button.setText(options.get(i));
            } else {
                button.setVisibility(View.GONE);
            }
        }
    }

    private void playFeedbackSound() {
        MediaPlayer player = MediaPlayer.create(this, R.raw.entranceactivate);
        if (player == null) {
            return;
        }
        player.setOnCompletionListener(MediaPlayer::release);
        player.start();
    }

}