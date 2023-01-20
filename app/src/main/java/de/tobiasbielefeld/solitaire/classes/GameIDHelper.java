package de.tobiasbielefeld.solitaire.classes;

import static android.content.Context.MODE_PRIVATE;
import static de.tobiasbielefeld.solitaire.SharedData.GAME;
import static de.tobiasbielefeld.solitaire.SharedData.cards;
import static de.tobiasbielefeld.solitaire.SharedData.currentGame;
import static de.tobiasbielefeld.solitaire.SharedData.gameLogic;
import static de.tobiasbielefeld.solitaire.SharedData.lg;
import static de.tobiasbielefeld.solitaire.SharedData.prefs;
import static de.tobiasbielefeld.solitaire.SharedData.recordList;
import static de.tobiasbielefeld.solitaire.SharedData.stacks;
import static de.tobiasbielefeld.solitaire.SharedData.timer;
import static de.tobiasbielefeld.solitaire.classes.Card.STATE_FACED_DOWN;
import static de.tobiasbielefeld.solitaire.classes.Card.STATE_FACED_UP;
import static de.tobiasbielefeld.solitaire.classes.Card.STATE_INVISIBLE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;

import org.json.*;

import de.tobiasbielefeld.solitaire.helper.RecordList;
import de.tobiasbielefeld.solitaire.ui.GameManager;

public class GameIDHelper {
    private static final String TAG = "GameIDHelper";

    public static String[] gameNames = { "AcesUp", "Calculation",
            "Canfield", "FortyEight", "Freecell", "Golf", "GrandfathersClock", "Gypsy", "Klondike",
            "Maze", "Mod3", "NapoleonsTomb", "Pyramid", "SimpleSimon", "Spider", "Spiderette",
            "TriPeaks", "Vegas", "Yukon" };
    private static final HashMap<String, Integer> nameToIndex = new HashMap<>();
    static {
        for (int i = 0; i < gameNames.length; i++) {
            nameToIndex.put(gameNames[i], i);
        }
    }

    public static String getCurrentGameName() {
        String[] gameNameParts = currentGame.getClass().getName().split("\\.");
        return gameNameParts[gameNameParts.length-1];
    }

    public static JSONObject getCurrentStateJSON(boolean withState, boolean withUndo) {
        String gameName = getCurrentGameName();

        JSONObject save = new JSONObject();

        JSONArray randomCardsToSave = new JSONArray();
        for (int i=0; i<gameLogic.randomCards.length; i++) {
            randomCardsToSave.put(gameLogic.randomCards[i].getId());
        }

        try {
            save.put("game", gameName);
            save.put("random", randomCardsToSave);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        // now settings
        JSONArray settingsArray = new JSONArray();

        switch(gameName) { // not all games here, only those that have settings
            case "Calculation":
                settingsArray.put(prefs.getSavedCalculationAlternativeModeOld());
                break;
            case "Canfield":
                settingsArray.put(prefs.getSavedCanfieldSizeOfReserve());
                settingsArray.put(prefs.getSavedCanfieldDrawModeOld());
                break;
            case "FortyEight":
                settingsArray.put(prefs.getSavedFortyEightLimitedRecycles());
                settingsArray.put(prefs.getSavedNumberOfRecycles(prefs.PREF_KEY_FORTYEIGHT_NUMBER_OF_RECYCLES,
                        prefs.DEFAULT_FORTYEIGHT_NUMBER_OF_RECYCLES));
                break;
            case "Golf":
                settingsArray.put(prefs.getSavedGoldCyclic());
                break;
            case "Klondike":
                settingsArray.put(prefs.getSavedKlondikeVegasDrawModeOld(1));
                settingsArray.put(prefs.getSavedKlondikeLimitedRecycles());
                settingsArray.put(prefs.getSavedNumberOfRecycles(prefs.PREF_KEY_KLONDIKE_NUMBER_OF_RECYCLES,
                        prefs.DEFAULT_KLONDIKE_NUMBER_OF_RECYCLES));
                break;
            case "Mod3":
                settingsArray.put(prefs.getSavedMod3AutoMove());
                break;
            case "NapoleonsTomb":
                settingsArray.put(prefs.getSavedNumberOfRecycles(prefs.PREF_KEY_NAPOLEONSTOMB_NUMBER_OF_RECYCLES,
                        prefs.DEFAULT_NAPOLEONSTOMB_NUMBER_OF_RECYCLES));
                break;
            case "Pyramid":
                settingsArray.put(prefs.getSavedPyramidLimitedRecycles());
                settingsArray.put(prefs.getSavedPyramidAutoMove());
                settingsArray.put(prefs.getSavedPyramidDifficulty());
                settingsArray.put(prefs.getSavedNumberOfRecycles(prefs.PREF_KEY_PYRAMID_NUMBER_OF_RECYCLES,
                        prefs.DEFAULT_PYRAMID_NUMBER_OF_RECYCLES));
                break;
            case "Spider":
                settingsArray.put(prefs.getSavedSpiderDifficultyOld());
                settingsArray.put(prefs.getSavedSpiderRelaxedMode());
                break;
            case "Spiderette":
                settingsArray.put(prefs.getSavedSpideretteDifficultyOld());
                settingsArray.put(prefs.getSavedSpideretteRelaxedMode());
                break;
            case "Vegas":
                settingsArray.put(prefs.getSavedKlondikeVegasDrawModeOld(2));
                settingsArray.put(prefs.getSavedNumberOfRecycles(prefs.PREF_KEY_PYRAMID_NUMBER_OF_RECYCLES,
                        prefs.DEFAULT_PYRAMID_NUMBER_OF_RECYCLES));
                break;
            case "Yukon":
                settingsArray.put(prefs.getSavedYukonRulesOld());
                break;
        }

        try {
            save.put("conf", settingsArray);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        if (withState && gameLogic.isGameInProgress()) {
            JSONArray stacksToSave = new JSONArray();
            for (Stack stack : stacks) {
                JSONArray curStack = new JSONArray();

                for (Card card : stack.currentCards) {
                    curStack.put(card.getId());
                }

                stacksToSave.put(curStack);
            }

            JSONArray cardStateToSave = new JSONArray();
            for (Card card : cards) {
                int state = card.isUp() ? STATE_FACED_UP : STATE_FACED_DOWN;

                if (card.isInvisible()) {
                    state = STATE_INVISIBLE;
                }
                cardStateToSave.put(state);
            }

            try {
                save.put("stacks", stacksToSave);
                save.put("cards", cardStateToSave);
            } catch (JSONException e) {
                throw new IllegalStateException();
            }

            if (withUndo) {
                JSONArray undoArray = new JSONArray();
                JSONArray cardsList = new JSONArray();
                JSONArray originsList = new JSONArray();
                JSONArray orderList = new JSONArray();
                JSONArray flipCardsList = new JSONArray();

                for (RecordList.Entry entry: recordList.entries) {
                   JSONArray cards = new JSONArray();
                   JSONArray origins = new JSONArray();
                   JSONArray order = new JSONArray();
                   JSONArray flipCards = new JSONArray();

                   ArrayList<Card> currentCards = entry.getCurrentCards();
                   ArrayList<Stack> currentOrigins = entry.getCurrentOrigins();

                    for (int i = 0; i < currentCards.size(); i++) {
                        cards.put(currentCards.get(i).getId());
                        origins.put(currentOrigins.get(i).getId());
                    }

                    for (int o: entry.getMoveOrder()) {
                        order.put(o);
                    }

                    for (Card card : entry.getFlipCards()) {
                        flipCards.put(card.getId());
                    }

                    cardsList.put(cards);
                    originsList.put(origins);
                    orderList.put(order);
                    flipCardsList.put(flipCards);
                }

                undoArray.put(cardsList).put(originsList).put(orderList).put(flipCardsList);

                try {
                    save.put("undo", undoArray);
                } catch (JSONException e) {
                    throw new IllegalStateException();
                }
            }
        }

        return save;
    }

    public static String getCurrentStateAsId() {
        JSONObject json = getCurrentStateJSON(false, false);
        String gameName;

        try {
           gameName = json.getString("game");
        } catch (JSONException je) {
            throw new IllegalStateException();
        }

        return gameName + ":" + Base64.encodeToString(encodeJSON(json), Base64.NO_PADDING |
                Base64.NO_WRAP | Base64.URL_SAFE);
    }

    public static byte[] encodeJSON(JSONObject json) {
        String jsonString = json.toString();
        byte[] jsonBytes = jsonString.getBytes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream def;
        try {
            def = new DeflaterOutputStream(out);
            def.write(jsonBytes);
            def.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public static JSONObject decodeJSON(byte[] encoded) throws IOException, JSONException {
        ByteArrayInputStream in = new ByteArrayInputStream(encoded);
        InflaterInputStream def;
        StringBuilder jsonString = new StringBuilder();

        def = new InflaterInputStream(in);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = def.read(buffer)) != -1) {
            jsonString.append(new String(buffer, 0, len));
        }
        def.close();

        return new JSONObject(jsonString.toString());
    }

    public static byte[] stateIdToBytes(String stateId) throws IOException {
        // we skip parts before ":" (these are optional so we do this via indexOf)
        String encoded = stateId.substring(stateId.lastIndexOf(":") + 1);
        byte[] decoded;
        try {
            decoded = Base64.decode(encoded, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (IllegalArgumentException e) {
            throw new IOException("Base64 error");
        }
        return decoded;
    }

    private static String jsonIntArrayToString(JSONArray arr) throws JSONException {
        StringBuilder s = new StringBuilder();
        for (int i=0; i<arr.length();i++) {
            s.append(arr.getInt(i)).append(",");
        }
        return s.toString();
    }

    public static void loadGameFromJSONState(JSONObject state, Activity activity) throws JSONException {
        if (activity instanceof GameManager) {
            // firstly we need to save the previous game
            // this might not run on onPause, because that might run after we restore the state
            // so then we need to prevent GameManager from saving state so we're not overridden
            if (((GameManager)activity).hasLoaded) {
                timer.save();
                gameLogic.save();
            }
        }

        int gameIndex = restoreJSONState(state, activity);

        if (activity instanceof GameManager) {
            // HACK: since we've saved the old state, now we need to prevent GameManager from
            // overwriting our changes.
            ((GameManager)activity).hasLoaded = false;
        }

        Intent intent = new Intent(activity.getApplicationContext(), GameManager.class);
        intent.putExtra(GAME, gameIndex);
        activity.startActivity(intent);

        if (activity instanceof GameManager) {
            activity.finish();
        }
    }

    /**
     * @return game index to start
     */
    public static int restoreJSONState(JSONObject state, Context context) throws JSONException {
        String gameName = state.getString("game");

        int index = nameToIndex.get(gameName);

        SharedPreferences.Editor gameData = context.getSharedPreferences(lg.getSharedPrefNameOfGame(index),
                MODE_PRIVATE).edit();

        gameData.putString(prefs.PREF_KEY_GAME_RANDOM_CARDS,
                jsonIntArrayToString(state.getJSONArray("random")));

        SharedPreferences.Editor sharedData = PreferenceManager.getDefaultSharedPreferences(context).edit();

        JSONArray settingsArray = state.getJSONArray("conf");
        int settingIdx = 0, totalSettings = settingsArray.length();

        // we need to restore both the old and the new settings
        // that's because if we load the game with saved state, then old settings are read
        // but when we load a game with no saved state, redeal is called which loads the new settings
        // while we could make a special state variable so that that redeal doesn't load new settings,
        // I'm not sure that behaviour would actually be expected

        switch(gameName) { // not all games here, only those that have settings
            case "Calculation":
                if (settingIdx < totalSettings) {
                    boolean val = settingsArray.getBoolean(settingIdx++);
                    sharedData.putBoolean(prefs.PREF_KEY_CALCULATION_ALTERNATIVE_OLD, val);
                    sharedData.putBoolean(prefs.PREF_KEY_CALCULATION_ALTERNATIVE, val);
                }
                break;
            case "Canfield":
                if (settingIdx < totalSettings) {
                    sharedData.putString(prefs.PREF_KEY_CANFIELD_SIZE_OF_RESERVE,
                            Integer.toString(settingsArray.getInt(settingIdx++)));
                }
                if (settingIdx < totalSettings) {
                    String val = settingsArray.getString(settingIdx++);
                    sharedData.putString(prefs.PREF_KEY_CANFIELD_DRAW_OLD, val);
                    sharedData.putString(prefs.PREF_KEY_CANFIELD_DRAW, val);
                }
                break;
            case "FortyEight":
                if (settingIdx < totalSettings) {
                    sharedData.putBoolean(prefs.PREF_KEY_FORTYEIGHT_LIMITED_RECYCLES,
                            settingsArray.getBoolean(settingIdx++));
                }
                if (settingIdx < totalSettings) {
                    sharedData.putString(prefs.PREF_KEY_FORTYEIGHT_NUMBER_OF_RECYCLES,
                            Integer.toString(settingsArray.getInt(settingIdx++)));
                }
                break;
            case "Golf":
                if (settingIdx < totalSettings) {
                    sharedData.putBoolean(prefs.PREF_KEY_GOLF_CYCLIC,
                            settingsArray.getBoolean(settingIdx++));
                }
                break;
            case "Klondike":
                if (settingIdx < totalSettings) {
                    String val = settingsArray.getString(settingIdx++);
                    sharedData.putString(prefs.PREF_KEY_KLONDIKE_DRAW_OLD, val);
                    sharedData.putString(prefs.PREF_KEY_KLONDIKE_DRAW, val);
                }
                if (settingIdx < totalSettings) {
                    sharedData.putBoolean(prefs.PREF_KEY_KLONDIKE_LIMITED_RECYCLES,
                            settingsArray.getBoolean(settingIdx++));
                }
                if (settingIdx < totalSettings) {
                    sharedData.putString(prefs.PREF_KEY_KLONDIKE_NUMBER_OF_RECYCLES,
                            Integer.toString(settingsArray.getInt(settingIdx++)));
                }
                break;
            case "Mod3":
                if (settingIdx < totalSettings) {
                    sharedData.putBoolean(prefs.PREF_KEY_MOD3_AUTO_MOVE,
                            settingsArray.getBoolean(settingIdx++));
                }
                settingsArray.put(prefs.getSavedMod3AutoMove());
                break;
            case "NapoleonsTomb":
                if (settingIdx < totalSettings) {
                    sharedData.putString(prefs.PREF_KEY_NAPOLEONSTOMB_NUMBER_OF_RECYCLES,
                            Integer.toString(settingsArray.getInt(settingIdx++)));
                }
                break;
            case "Pyramid":
                if (settingIdx < totalSettings) {
                    sharedData.putBoolean(prefs.PREF_KEY_PYRAMID_LIMITED_RECYCLES,
                            settingsArray.getBoolean(settingIdx++));
                }
                if (settingIdx < totalSettings) {
                    sharedData.putString(prefs.PREF_KEY_PYRAMID_NUMBER_OF_RECYCLES,
                            Integer.toString(settingsArray.getInt(settingIdx++)));
                }
                if (settingIdx < totalSettings) {
                    sharedData.putBoolean(prefs.PREF_KEY_PYRAMID_AUTO_MOVE,
                            settingsArray.getBoolean(settingIdx++));
                }
                if (settingIdx < totalSettings) {
                    sharedData.putString(prefs.PREF_KEY_PYRAMID_DIFFICULTY,
                            settingsArray.getString(settingIdx++));
                }
                break;
            case "Spider":
                if (settingIdx < totalSettings) {
                    String val = settingsArray.getString(settingIdx++);
                    sharedData.putString(prefs.PREF_KEY_SPIDER_DIFFICULTY_OLD, val);
                    sharedData.putString(prefs.PREF_KEY_SPIDER_DIFFICULTY, val);
                }
                if (settingIdx < totalSettings) {
                    boolean val = settingsArray.getBoolean(settingIdx++);
                    sharedData.putBoolean(prefs.PREF_KEY_SPIDER_RELAXED_MODE, val);
                }
                break;
            case "Spiderette":
                if (settingIdx < totalSettings) {
                    String val = settingsArray.getString(settingIdx++);
                    sharedData.putString(prefs.PREF_KEY_SPIDERETTE_DIFFICULTY_OLD, val);
                    sharedData.putString(prefs.PREF_KEY_SPIDERETTE_DIFFICULTY, val);
                }
                if (settingIdx < totalSettings) {
                    boolean val = settingsArray.getBoolean(settingIdx++);
                    sharedData.putBoolean(prefs.PREF_KEY_SPIDERETTE_RELAXED_MODE, val);
                }
                break;
            case "Vegas":
                if (settingIdx < totalSettings) {
                    String val = settingsArray.getString(settingIdx++);
                    sharedData.putString(prefs.PREF_KEY_VEGAS_DRAW_OLD, val);
                    sharedData.putString(prefs.PREF_KEY_VEGAS_DRAW, val);
                }
                if (settingIdx < totalSettings) {
                    sharedData.putString(prefs.PREF_KEY_VEGAS_NUMBER_OF_RECYCLES,
                            Integer.toString(settingsArray.getInt(settingIdx++)));
                }
                break;
            case "Yukon":
                if (settingIdx < totalSettings) {
                    String val = settingsArray.getString(settingIdx++);
                    sharedData.putString(prefs.PREF_KEY_YUKON_RULES_OLD, val);
                    sharedData.putString(prefs.PREF_KEY_YUKON_RULES, val);
                }
                break;
        }

        boolean hasState = state.has("cards") && state.has("stacks");

        if (hasState) {
            JSONArray stacks = state.getJSONArray("stacks");
            for (int s = 0; s < stacks.length(); s++) {
                gameData.putString(prefs.PREF_KEY_STACK + s,
                        jsonIntArrayToString(stacks.getJSONArray(s)));

            }

            gameData.putString(prefs.PREF_KEY_CARDS,
                        jsonIntArrayToString(state.getJSONArray("cards")));

            boolean hasUndo = state.has("undo");
            if (hasUndo) {
                JSONArray undoArray = state.getJSONArray("undo");

                JSONArray cardsList = undoArray.getJSONArray(0);
                JSONArray originsList = undoArray.getJSONArray(1);
                JSONArray orderList = undoArray.getJSONArray(2);
                JSONArray flipCardsList = undoArray.getJSONArray(3);

                int numEntries = cardsList.length();
                gameData.putInt(prefs.PREF_KEY_RECORD_LIST_ENTRIES_SIZE, numEntries);

                for (int i = 0; i < numEntries; i++) {
                    gameData.putString(prefs.PREF_KEY_RECORD_LIST_ENTRY + i + prefs.PREF_KEY_CARD,
                            jsonIntArrayToString(cardsList.getJSONArray(i)));
                    gameData.putString(prefs.PREF_KEY_RECORD_LIST_ENTRY + i + prefs.PREF_KEY_ORIGIN,
                            jsonIntArrayToString(originsList.getJSONArray(i)));
                    gameData.putString(prefs.PREF_KEY_RECORD_LIST_ENTRY + i + prefs.PREF_KEY_ORDER,
                            jsonIntArrayToString(orderList.getJSONArray(i)));
                    gameData.putString(prefs.PREF_KEY_RECORD_LIST_ENTRY + i + prefs.PREF_KEY_FLIP_CARD,
                            jsonIntArrayToString(flipCardsList.getJSONArray(i)));
                }
            }
        } else {
            // we need to set cards to an empty value so GameLogic knows when to do a redeal
            gameData.putString(prefs.PREF_KEY_CARDS, "");
        }

        gameData.putBoolean(prefs.PREF_KEY_GAME_FIRST_RUN, false);
        gameData.putBoolean(prefs.PREF_KEY_GAME_WON_AND_RELOADED, false);
        gameData.putBoolean(prefs.PREF_KEY_GAME_MOVED_FIRST_CARD, hasState);
        gameData.putBoolean(prefs.PREF_KEY_GAME_WON, false);

        gameData.apply();
        sharedData.apply();

        prefs.saveCurrentGame(index);
        return index;
    }
}
