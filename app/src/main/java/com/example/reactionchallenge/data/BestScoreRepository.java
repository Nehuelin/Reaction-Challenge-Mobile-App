package com.example.reactionchallenge.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class BestScoreRepository {

    private static final String PREFS_NAME = "reaction_challenge_prefs";
    private static final String BEST_SCORE_PREFIX = "best_score_";
    private static final String BEST_AVG_PREFIX = "best_avg_";

    private final SharedPreferences preferences;

    public BestScoreRepository(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveIfBetter(String playerName, int score, double averageMs) {
        String scoreKey = BEST_SCORE_PREFIX + playerName;
        String avgKey = BEST_AVG_PREFIX + playerName;

        int bestScore = preferences.getInt(scoreKey, 0);
        float bestAvg = preferences.getFloat(avgKey, Float.MAX_VALUE);

        boolean shouldSave = score > bestScore || (score == bestScore && averageMs > 0 && averageMs < bestAvg);
        if (shouldSave) {
            preferences.edit()
                    .putInt(scoreKey, score)
                    .putFloat(avgKey, (float) averageMs)
                    .apply();
        }
    }

    public String buildBestStatsText(String playerName, int currentScore, double currentAverage, boolean allowPersistedOnly) {
        int bestScore = preferences.getInt(BEST_SCORE_PREFIX + playerName, 0);
        float bestAvg = preferences.getFloat(BEST_AVG_PREFIX + playerName, Float.MAX_VALUE);

        if (bestScore == 0 && allowPersistedOnly && currentScore == 0) {
            return "Datos Guardados\n• Jugador: " + playerName + "\n• Aún no hay registros guardados.";
        }

        String avgLabel = bestAvg == Float.MAX_VALUE ? "Sin dato" : String.format(Locale.getDefault(), "%.0f ms", bestAvg);
        String currentAvgLabel = currentAverage > 0 ? String.format(Locale.getDefault(), "%.0f ms", currentAverage) : "-";

        return String.format(
                Locale.getDefault(),
                "Datos persistidos\n• Jugador: %s\n• Mejor puntaje guardado: %d\n• Mejor reacción guardada: %s\n• Reacción promedio actual: %s",
                playerName,
                bestScore,
                avgLabel,
                currentAvgLabel
        );
    }
}