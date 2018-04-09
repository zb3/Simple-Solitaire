/*
 * Copyright (C) 2016  Tobias Bielefeld
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you want to contact me, send me an e-mail at tobias.bielefeld@gmail.com
 */

package de.tobiasbielefeld.solitaire.games;

import android.content.Context;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import de.tobiasbielefeld.solitaire.classes.Card;
import de.tobiasbielefeld.solitaire.classes.CardAndStack;
import de.tobiasbielefeld.solitaire.classes.Stack;

import static de.tobiasbielefeld.solitaire.SharedData.OPTION_NO_RECORD;
import static de.tobiasbielefeld.solitaire.SharedData.OPTION_REVERSED_RECORD;
import static de.tobiasbielefeld.solitaire.SharedData.gameLogic;
import static de.tobiasbielefeld.solitaire.SharedData.hint;
import static de.tobiasbielefeld.solitaire.SharedData.moveToStack;
import static de.tobiasbielefeld.solitaire.SharedData.recordList;
import static de.tobiasbielefeld.solitaire.SharedData.stacks;
import static de.tobiasbielefeld.solitaire.games.Game.testMode.DOESNT_MATTER;
import static de.tobiasbielefeld.solitaire.games.Game.testMode3.ASCENDING;
import static de.tobiasbielefeld.solitaire.games.Game.testMode3.DESCENDING;

/**
 * Napoleon's tomb game! Follows the rules from here: http://www.pahnation.com/how-to-play-napoleons-tomb/
 * and is pretty hard to win. only ~ 10% win chance!
 */

public class NapoleonsTomb extends Game {

    public NapoleonsTomb() {
        setNumberOfDecks(1);
        setNumberOfStacks(11);

        setTableauStackIDs(0,1,2,3);
        setFoundationStackIDs(4,5,6,7,8);
        setDiscardStackIDs(9);
        setMainStackIDs(10);

        setDirections(0,0,0,0);

        setMixingCardsTestMode(testMode.ALTERNATING_COLOR);
    }

    public void setStacks(RelativeLayout layoutGame, boolean isLandscape, Context context) {

        // initialize the dimensions
        setUpCardDimensions(layoutGame, 8, 5);

        //calculate spacing and start position of cards
        int spacing = setUpHorizontalSpacing(layoutGame, 4, 4);
        int spacingVertical = setUpVerticalSpacing(layoutGame,3,2);

        int startPos = layoutGame.getWidth() / 2 - 2 * Card.width - 2 * spacing;

        //first row
        stacks[4].setX(startPos);
        stacks[4].view.setY((isLandscape ? Card.width / 4 : Card.width / 2) + 1);

        stacks[0].setX(startPos + spacing + Card.width );
        stacks[0].view.setY((isLandscape ? Card.width / 4 : Card.width / 2) + 1);

        stacks[5].setX(startPos + spacing * 2 + Card.width * 2);
        stacks[5].view.setY((isLandscape ? Card.width / 4 : Card.width / 2) + 1);

        //second row
        stacks[1].setX(stacks[4].getX());
        stacks[1].setY(stacks[4].getY() + Card.height + spacingVertical);

        stacks[8].setX(stacks[0].getX());
        stacks[8].setY(stacks[1].getY());

        stacks[2].setX(stacks[5].getX());
        stacks[2].setY(stacks[1].getY());

        //third row
        stacks[6].setX(stacks[4].getX());
        stacks[6].setY(stacks[1].getY() + Card.height + spacingVertical);

        stacks[3].setX(stacks[0].getX());
        stacks[3].setY(stacks[6].getY());

        stacks[7].setX(stacks[5].getX());
        stacks[7].setY(stacks[6].getY());

        //main + discard stack
        stacks[10].setX(stacks[5].getX() + spacing*2 + Card.width);
        stacks[10].setY(stacks[5].getY() + Card.height/2 + spacingVertical/2);

        stacks[9].setX(stacks[10].getX());
        stacks[9].setY(stacks[10].getY() + Card.height + spacingVertical);

        //also set backgrounds of the stacks
        for (Stack stack : stacks) {
            if (stack.getId() > 3 && stack.getId() <= 7)  {
                stack.view.setImageBitmap(Stack.background7);
            } else if (stack.getId() == 8) {
                stack.view.setImageBitmap(Stack.background6);
            }
            else if (stack.getId() == 10) {
                stack.view.setImageBitmap(Stack.backgroundTalon);
            }
        }
    }

    public boolean winTest() {
        //the first 4 foundation stacks have to contain 4 cards each
        for (int i = 4; i <= 7; i++) {
            if (stacks[i].getSize() != 7) {
                return false;
            }
        }

        //the last one has to contain 24 cards
        return stacks[8].getSize() == 24;
    }

    public void dealCards() {
        //deal cards to discard
        moveToStack(getMainStack().getTopCard(), stacks[9], OPTION_NO_RECORD);
        stacks[9].getCard(0).flipUp();

        //and move cards to the tableau
        for (int i = 0; i <= 3; i++) {
            moveToStack(getMainStack().getTopCard(), stacks[i], OPTION_NO_RECORD);
            stacks[i].getCard(0).flipUp();
        }
    }

    public int onMainStackTouch() {

        //if there are cards on the main stack
        if (getMainStack().getSize() > 0) {
            moveToStack(getMainStack().getTopCard(), stacks[9]);

            return 1;
        }
        //if there are NO cards on the main stack, but cards on the discard stacks, move them all to main
        else if (stacks[9].getSize() != 0) {
            ArrayList<Card> cards = new ArrayList<>();

            for (int i = 0; i < stacks[9].getSize(); i++) {
                cards.add(stacks[9].getCard(i));
            }

            ArrayList<Card> cardsReversed = new ArrayList<>();
            for (int i = 0; i < cards.size(); i++) {
                cardsReversed.add(cards.get(cards.size() - 1 - i));
            }

            moveToStack(cardsReversed, stacks[10], OPTION_REVERSED_RECORD);

            return 2;
        }

        return 0;
    }

    public boolean cardTest(Stack stack, Card card) {
        //move cards according to the rules
        if (stack.getId() < 4) {
            return stack.isEmpty();

        } else if (stack.getId() < 8) {
            if (stack.isEmpty()) {
                return card.getValue() == 7;
            } else {
                return canCardBePlaced(stack, card, DOESNT_MATTER, ASCENDING);
            }
        } else if (stack.getId() == 8) {
            if (stack.isEmpty() || stack.getTopCard().getValue() == 1) {
                return card.getValue() == 6;
            } else {
                return canCardBePlaced(stack, card, DOESNT_MATTER, DESCENDING);
            }
        }

        return false;
    }

    public boolean addCardToMovementGameTest(Card card) {
        return card.isTopCard();
    }

    public CardAndStack hintTest() {
        Card card;

        //from the cells to foundation
        for (int i = 0; i <= 3; i++) {

            Stack origin = stacks[i];

            if (origin.isEmpty()) {
                continue;
            }

            card = origin.getCard(0);

            if (!hint.hasVisited(card)) {
                for (int j = 4; j <= 8; j++) {
                    if (card.test(stacks[j])) {
                        return new CardAndStack(card, stacks[j]);
                    }
                }
            }

        }

        //discard stack to all other stacks
        if (stacks[9].getSize() > 0 && !hint.hasVisited(stacks[9].getTopCard())) {
            for (int j = 4; j<=8; j++) {
                if (stacks[9].getTopCard().test(stacks[j])) {
                    return new CardAndStack(stacks[9].getTopCard(), stacks[j]);
                }
            }
        }

        return null;
    }

    public Stack doubleTapTest(Card card) {

        //foundation stacks
        for (int j = 4; j <= 8; j++) {
            if (card.test(stacks[j])) {
                return stacks[j];
            }
        }

        //empty tableau stacks
        for (int j = 0; j <= 3; j++) {
            if (stacks[j].isEmpty() && card.test(stacks[j]))
                return stacks[j];
        }

        return null;
    }

    public int addPointsToScore(ArrayList<Card> cards, int[] originIDs, int[] destinationIDs, boolean isUndoMovement) {
        int originID = originIDs[0];
        int destinationID = destinationIDs[0];

        //tableau or discard stack to foundation
        if ((originID <=3 || originID == 9) && destinationID >= 4 && destinationID <= 8) {
            return 60;
        }

        //foundation to tableau
        if (destinationID <=3 && originID >= 4 && originID <= 8) {
            return -75;
        }

        //returning cards to stock
        if (originID == 9 &&  destinationID == 10) {
            return -200;
        }

        return 0;
    }

    public void testAfterMove() {
        /*
         *  after a card is moved from the discard stacks, it needs to update the order of the cards
         *  on the discard stacks. (But only in deal3 mode).
         *  This movement will be added to the last record list entry, so it will be also undone if
         *  the card will be moved back to the discard stacks
         */

        if (gameLogic.hasWon()){
            return;
        }

        if (stacks[9].isEmpty() && !stacks[10].isEmpty()) {
            recordList.addToLastEntry(stacks[10].getTopCard(), stacks[10]);
            moveToStack(stacks[10].getTopCard(), stacks[9], OPTION_NO_RECORD);
        }
    }
}
