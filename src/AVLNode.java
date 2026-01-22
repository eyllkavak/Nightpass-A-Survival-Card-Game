/**
 * Represents a node in the AVL Tree structure used by the CardManager.
 * In addition to standard AVL properties (card, children, height), this node stores
 * statistical summaries for its entire subtree.
 */
public class AVLNode {
    /** The {@link Card} object held by this node. */
    Card card;

    /** The left child node. */
    AVLNode left, right;

    /** The height of the node in the AVL tree. */
    int height;

    // Subtree summaries:
    /** The minimum current Attack value in the subtree rooted at this node. */
    int minA;
    /** The maximum current Attack value in the subtree rooted at this node. */
    int maxA;
    /** The minimum current Health value in the subtree rooted at this node. */
    int minH;
    /** The maximum current Health value in the subtree rooted at this node. */
    int maxH;
    /** The minimum missing health (H_missing) value in the subtree rooted at this node. */
    int minHmissing;
    /** The maximum missing health (H_missing) value in the subtree rooted at this node. */
    int maxHmissing;
    /**
     * Constructs a new AVLNode with a given card, initializing its height
     * and subtree summaries based on the card's stats.
     * @param card The Card object to be stored in this node.
     */
    public AVLNode(Card card) {
        this.card = card;
        this.left = null;
        this.right = null;
        this.height = 1;

        int a = card.getAcur();
        int h = card.getHcur();

        // Initialize summaries with the card's own stats
        this.minA = this.maxA = a;
        this.minH = this.maxH = h;

        // Initialize Type 3 (Discard Pile) summaries with the card's H_missing
        int hmissing = card.getHmissing();
        this.minHmissing = this.maxHmissing = hmissing;
    }
}
