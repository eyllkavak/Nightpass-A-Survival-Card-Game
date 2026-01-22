/**
 * Represents a game card that participates in battles.
 * Each card object is instantiated with initial health and attack values
 * provided by the input commands. It receives an {@code entryTimeID}
 * to track its entry order into the deck.
 * The card's current attack dynamically adjusts based on damage taken.
 * This class also handles the necessary logic for the Type 2 Healing Phase, including
 * tracking revival progress and applying permanent attack penalties upon revival.
 */
public class Card {
    /**
     * A global counter shared across all cards. It is incremented every time a card
     * is created or when a card's effective combat stats change. This value serves as the ultimate tie-breaker.
     */
    private static long globalEntryCounter = 0;

    private String name;
    private int Ainit;
    private int Abase;
    private int Acur;
    private int Hinit;
    private int Hbase;
    private int Hcur;
    /**
     * Stores the value of {@code globalEntryCounter} when the card
     * was last added to the deck or when its attack stat was last updated.
     */
    private long entryTimeID;

    /**
     * Tracks the accumulated health points applied to a dead (discarded) card.
     * A card is fully revived when {@code Hbase - revivalProgress} reaches zero.
     */
    private int revivalProgress;


    /**
     * Constructs a new Card object, initializing base and current stats.
     * @param name The unique name of the card.
     * @param attack_init The initial attack value.
     * @param health_init The initial health value.
     */
    public Card(String name, int attack_init, int health_init) {
        this.name = name;
        this.Ainit = attack_init;
        this.Hinit = health_init;

        this.Abase = attack_init;
        this.Hbase = health_init;

        this.Acur = attack_init;
        this.Hcur = health_init;

        this.revivalProgress = 0;
        this.updateEntryTimeID();
    }

    /**
     * Calculates the card's missing health, used as the primary key for the Discard Pile AVL (Type 3).
     * @return The amount of health required for the card to be fully revived.
     */
    public int getHmissing() {
        return Hbase - revivalProgress;
    }

    /**
     * Adds health points to the card's revival progress.
     * This method is used during the Partial Revival (P3) phase.
     * @param points The amount of health points from the pool to attempt to apply.
     * @return The actual number of points applied to the card (Min(points, H_missing)).
     */
    public int addRevivalProgress(int points) {
        int missing = getHmissing();
        // The applied points are limited by how much health the card is actually missing.
        int appliedPoints = Math.min(points, missing);
        this.revivalProgress += appliedPoints;
        return appliedPoints;
    }

    /**
     * Applies a permanent, multiplicative reduction to the card's base attack.
     * @param isFullRevive True if applying the 10% penalty for full revival; False for the 5% partial penalty.
     */
    public void applyRevivalPenalty(boolean isFullRevive) {
        double penalty = isFullRevive ? 0.10 : 0.05;
        this.Abase = (int) Math.floor(this.Abase * (1.0 - penalty));
        this.Abase = Math.max(1, this.Abase);
    }

    /**
     * Prepares a fully revived card to return to the active Deck.
     * Resets healing metrics and recalculates dynamic stats.
     */
    public void prepareForDeckReEntry() {
        this.setHcur(this.Hbase);
        this.revivalProgress = 0;
        this.calculateCurrentAttack();
    }

    /**
     * Prepares a card immediately after death (H_cur <= 0) for insertion into the Discard Pile.
     * Resets revival progress to 0 (so H_missing = H_base initially) and updates the EntryTimeID.
     * Updating the EntryTimeID ensures the card is treated as 'newly discarded' for the tie-breaker rule.
     */
    public void prepareForDiscardPile() {
        this.revivalProgress = 0;
        this.updateEntryTimeID();
    }
    /**
     * Updates the card's {@code entryTimeID} using the next value of the global counter.
     * This occurs whenever a card is drawn, its attack changes due to damage, or it is discarded/revived.
     */
    public void updateEntryTimeID() {
        this.entryTimeID = ++globalEntryCounter;
    }

    /**
     * Recalculates the card's current attack ({@code Acur}) using the formula.
     * If the calculated attack differs from the old {@code Acur},
     * the value is updated, and {@code entryTimeID} is reset.
     */
    public void calculateCurrentAttack() {
        if (this.Hcur <= 0) {
            return;
        }

        int oldAttack = this.Acur;

        int newAttack = (int) Math.floor((double) ( (long)this.Abase * (long)this.Hcur ) / (double)this.Hbase);

        // Attack must be at least 1.
        newAttack = Math.max(1, newAttack);

        // Only update Acur and EntryTimeID if the attack value actually changed.
        if (newAttack != oldAttack) {
            this.Acur = newAttack;
            this.updateEntryTimeID();
        }
    }

    /**
     * @return The card's name.
     */
    public String getName() { return name; }

    /**
     * @return The card's current (dynamically calculated) attack value.
     */
    public int getAcur() { return Acur; }

    /**
     * @return The card's current health value.
     */
    public int getHcur() { return Hcur; }

    /**
     * @return The entry time ID for priority tracking.
     */
    public long getEntryTimeID() { return entryTimeID; }

    /**
     * @return The card's base health value.
     */
    public int getHbase() { return Hbase; }

    /**
     * Sets the card's current health, ensuring it does not drop below 0.
     * @param currentHealth The new current health value.
     */
    public void setHcur(int currentHealth) {
        this.Hcur = Math.max(0, currentHealth);
    }
}