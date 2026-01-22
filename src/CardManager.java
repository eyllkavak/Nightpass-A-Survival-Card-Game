import java.util.ArrayList;

/**
 * This class primarily manages the card deck and overall game state for the duel.
 * It maintains three AVL trees: two for the active deck (optimized for battle priorities) and one
 * for the discard pile (optimized for healing priorities). It handles scoring, card status changes
 * (damage, death, revival), and executing commands like draw, battle, and steal.
 */
public class CardManager {
    private int survivorPoints = 0; //The current score for The Survivor.
    private int strangerPoints = 0; //The current score for The Stranger.

    /** The first AVL Tree (deckAVL1), prioritized for:
     * Priority 1 (Min A / Min H for battle)
     * Priority 3 (Min A / Min H for battle)
     * Steal Card (Min A / Min H / Min TimeID)
     */
    private final AVLTree deckAVL1 = new AVLTree(1);

    /** The second AVL Tree (deckAVL2), prioritized for:
     * Priority 2 (Max A / Max H for battle)
     * Priority 4 (Min A / Max H for battle - uses a cached value for O(1) access)
     */
    private final AVLTree deckAVL2 = new AVLTree(2);

    /** The third AVL Tree (discardAVL),prioritized for:
     * Maximum H_missing (H_base - revival_progress)
     * Minimum TimeID (oldest discarded card, for tie-breaker)
     */
    private final AVLTree discardAVL = new AVLTree(3);
    /**
     * Constructs a new CardManager, initializing the game state.
     */
    public CardManager() {}


    /**
     * Creates a new Card and adds it to both AVL Trees.
     * @param name The name of the new card.
     * @param attackInit The initial base attack value.
     * @param healthInit The initial base health value.
     * @return A message indicating the card has been added.
     */
    public String handleDrawCard(String name, int attackInit, int healthInit) {
        Card newCard = new Card(name, attackInit, healthInit);

        deckAVL1.insert(newCard);
        deckAVL2.insert(newCard);

        return "Added " + name + " to the deck";
    }

    /**
     * Returns the current number of cards in the deck.
     * Since both AVL trees should hold the same set of cards, the size of one is returned.
     * @return A message containing the number of cards.
     */
    public String handleDeckCount() {
        int count = deckAVL1.getSize();
        return "Number of cards in the deck: " + count;
    }

    /**
     * Returns the current number of cards in the discard pile.
     * @return A message containing the number of cards in the discard pile.
     */
    public String handleDiscardPileCount() {
        int count = discardAVL.getSize();
        return "Number of cards in the discard pile: " + count;
    }

    /**
     * Executes the healing phase immediately after a battle is resolved.
     * The method attempts to fully or partially revive cards.
     * The process prioritizes full revivals (P1/P2) first,if points remain, it moves to partial revival (P3).
     * @param healPoolAmount The total points available to spend on revival.
     * @return The total number of cards successfully revived (fully revived only) during this phase.
     */
    private int performHealingPhase(int healPoolAmount) {
        if (healPoolAmount <= 0 || discardAVL.getSize() == 0) {
            return 0; // No healing needed or no cards available in the pile.
        }

        int kRevived = 0;
        Card cardToHeal;
        // Temporary list to hold cards fully revived before mass re-insertion into the Deck AVLs.
        ArrayList<Card> fullyRevivedCards = new ArrayList<>();

        // P1 & P2: Full Revival Priority (Max H_missing <= healPool)
        while (true) {
            // Find the card with the highest H_missing that the current pool can fully restore.
            cardToHeal = discardAVL.findMaxH_missing(healPoolAmount);

            if (cardToHeal == null) {
                break; // No more cards can be fully revived with the remaining pool.
            }

            int hMissing = cardToHeal.getHmissing();

            // Perform full revival: deduct the exact required amount.
            healPoolAmount -= hMissing;

            // Remove the card from the Discard AVL.
            discardAVL.delete(cardToHeal);

            // Apply permanent penalty (10% for Full Revive) and prepare for Deck re-entry.
            cardToHeal.applyRevivalPenalty(true);
            cardToHeal.prepareForDeckReEntry();

            fullyRevivedCards.add(cardToHeal);
            kRevived++;
        }

        // P3: Partial Revival Priority (Min H_missing, Oldest TimeID)
        // Runs if points remain after attempting all full revivals.
        if (healPoolAmount > 0) {
            // Find the card with the minimum H_missing (and oldest TimeID tie-breaker).
            cardToHeal = discardAVL.findMinH_missing();

            if (cardToHeal != null) {
                // Remove the card from the Discard AVL before modifying its status.
                discardAVL.delete(cardToHeal);

                // Apply partial healing. This method caps the points applied to H_missing.
                int appliedPoints = cardToHeal.addRevivalProgress(healPoolAmount);
                healPoolAmount -= appliedPoints;

                // Apply permanent penalty (5% for Partial Revive).
                cardToHeal.applyRevivalPenalty(false);

                // Re-insert the card into the Discard AVL. This forces the tree to update its position
                // based on the new H_missing value and the new entryTimeID.
                discardAVL.insert(cardToHeal);
            }
        }

        //Final Step: Return Fully Revived Cards to the Deck
        for (Card revivedCard : fullyRevivedCards) {
            deckAVL1.insert(revivedCard);
            deckAVL2.insert(revivedCard);
        }

        return kRevived;
    }


    /**
     * Determines and returns the winner of the game based on the final scores.
     * The Survivor wins in case of a tie.
     * @return A string indicating the winner and their final score.
     */
    public String handleFindWinning() {
        String winner;
        int score;

        if (survivorPoints >= strangerPoints) {
            winner = "The Survivor";
            score = survivorPoints;
        } else {
            winner = "The Stranger";
            score = strangerPoints;
        }

        return winner + ", Score: " + score;
    }

    /**
     * Attempts to steal a card for "The Stranger" based on specific criteria:
     * 1. Current Attack (A_cur) > attackLimit AND Current Health (H_cur) > healthLimit.
     * 2. Among candidates, prioritize the card with Min A_cur, then Min H_cur, then Min TimeID.
     * @param attackLimit The minimum attack required to be eligible for stealing.
     * @param healthLimit The minimum health required to be eligible for stealing.
     * @return A message indicating the stolen card's name or that no card was stolen.
     */
    public String handleStealCard(int attackLimit, int healthLimit) {
        Card stolenCard = deckAVL1.findStealCard(attackLimit, healthLimit);

        if (stolenCard == null) {
            return "No card to steal";
        }

        deckAVL1.delete(stolenCard);
        deckAVL2.delete(stolenCard);

        return "The Stranger stole the card: " + stolenCard.getName();
    }

    /**
     * Resolves a battle round between The Stranger's card and a card from The Survivor's deck.
     * 1.Search through priorities 1 to 4 using the corresponding Deck AVL (Type 1 or Type 2)
     * 2.Damage & Scoring: Simultaneous damage is applied. Points (+1 or +2) are awarded based on
     * whether the opponent's card died or was damaged, and whether the played card died or was damaged.
     * 3.Card Management (Post-Battle):The played card is removed from both Deck AVLs. If it died,
     * it is moved to the Discard Pile (discardAVL), prepared for revival. If it survived, its stats
     * are updated and it is re-inserted into both Deck AVLs.
     * 4.Healing Phase is executed immediately after card management.
     * @param strangerAttack The attack of The Stranger's card.
     * @param strangerHealth The health of The Stranger's card.
     * @param healPoolAmount The amount of health to use for reviving cards (Type 2 feature).
     * @return A detailed message about the battle outcome, card status, and revived cards count.
     */
    public String handleBattle(int strangerAttack, int strangerHealth, int healPoolAmount) {

        Card playedCard = null;
        int priority = 0;

        // Priority 1 (Min A / Min H)
        playedCard = deckAVL1.findPriority1(strangerAttack, strangerHealth);
        if (playedCard != null) priority = 1;

        // Priority 2 (Max A / Max H)
        if (playedCard == null) {
            playedCard = deckAVL2.findPriority2(strangerAttack, strangerHealth);
            if (playedCard != null) priority = 2;
        }

        // Priority 3 (Min A / Min H)
        if (playedCard == null) {
            playedCard = deckAVL1.findPriority3(strangerAttack, strangerHealth);
            if (playedCard != null) priority = 3;
        }

        // Priority 4 (Max H)
        if (playedCard == null) {
            playedCard = deckAVL2.findPriority4(); //
            if (playedCard != null) priority = 4;
        }

        //If no card is found, the stranger automatically gains +2 points.
        if (playedCard == null) {
            strangerPoints += 2;
            int k_revived = performHealingPhase(healPoolAmount);
            return "No card to play, " + k_revived + " cards revived";
        }



        // Apply simultaneous damage (Health' = Health - Attack of opponent)
        int survivorInitialHealth = playedCard.getHcur();
        int survivorBaseHealth = playedCard.getHbase();


        int survivorNewHealth = survivorInitialHealth - strangerAttack;
        int strangerNewHealth = strangerHealth - playedCard.getAcur();

        int survivorDelta = 0;
        int strangerDelta = 0;

        // Scoring for The Stranger
        if (survivorNewHealth <= 0) {
            strangerDelta += 2;
        } else if (survivorNewHealth > 0 && survivorNewHealth < survivorBaseHealth) {
            strangerDelta += 1;
        }

        // Scoring for The Survivor
        if (strangerNewHealth <= 0) {
            survivorDelta += 2;
        } else if (strangerNewHealth > 0 && strangerNewHealth < strangerHealth) {
            survivorDelta += 1;
        }

        // Update total scores
        survivorPoints += survivorDelta;
        strangerPoints += strangerDelta;

        String cardStatus;

        // The card is removed from the active deck AVLs regardless of survival,
        // as its stats/location will change (death or stat update/re-insert).
        deckAVL1.delete(playedCard);
        deckAVL2.delete(playedCard);

        if (survivorNewHealth <= 0) {
            //Card dies
            cardStatus = "the played card is discarded";
            playedCard.setHcur(0);
            playedCard.prepareForDiscardPile();
            discardAVL.insert(playedCard);
        } else {
            //Card survives
            playedCard.setHcur(survivorNewHealth);
            playedCard.calculateCurrentAttack();

            //Re-insert the updated card into both Deck AVLs (it finds its new position).
            deckAVL1.insert(playedCard);
            deckAVL2.insert(playedCard);

            cardStatus = "the played card returned to deck";
        }
        // Healing phase execution
        int k_revived = performHealingPhase(healPoolAmount);
        return "Found with priority " + priority +
                ", Survivor plays " + playedCard.getName() +
                ", " + cardStatus +
                ", " + k_revived + " cards revived";
    }
}