import java.util.ArrayList;
/**
 * Implements a self-balancing binary search tree (AVL Tree) for the card game.
 * This class supports three primary comparison modes, each optimized for different game mechanics:
 * Type 1 (Deck AVL 1):Prioritizes finding cards with minimum attack and minimum health (for Priorities 1, 3, and Stealing).
 * Type 2 (Deck AVL 2):Prioritizes finding cards with maximum attack and minimum health (for Priorities 2 and 4).
 * Type 3 (Discard AVL):Prioritizes cards for the Healing Phase, focusing on maximum missing health (`H_missing`) and the oldest entry time (tie-breaker).
 */
public class AVLTree {
    private AVLNode root;
    /**
     * Determines the tree's sorting logic:
     * 1: Min A / Min H priority (Deck)
     * 2: Max A / Min H priority (Deck)
     * 3: Max H_missing / Min TimeID priority (Discard Pile)
     */
    private final int comparisonType;
    private int size;
    /**Card with the maximum attack, used for O(1) Priority 4 searches (AVL-2 only). */
    private Card maxAttackCard = null;

    // Flags to track success of last insert/delete operation
    private boolean lastInsertSuccess = false;
    private boolean lastDeleteSuccess = false;

    //Stack used for traversing the AVL tree.
    private final ArrayList<AVLNode> stack = new ArrayList<>();

    /**
     * Constructs an AVL tree with specified comparison type.
     * @param comparisonType 1 for Deck Type 1, 2 for Deck Type 2, 3 for Discard Pile Type 3.
     */
    public AVLTree(int comparisonType) {
        this.root = null;
        this.comparisonType = comparisonType;
        this.size = 0;
    }

    /**
     * Returns the number of cards in the tree.
     * @return The size of the AVL tree.
     */
    public int getSize() { return size; }

    /**
     * Helper method to safely retrieve a node's height (returns 0 if the node is null).
     * @param node The node whose height is requested.
     * @return The height of the node, or 0 if it's null.
     */
    private int height(AVLNode node) {
        return (node == null) ? 0 : node.height;
    }

    /**
     * Updates a node's height and refreshes all stored subtree statistics (min/max A/H, and H_missing for Type 3).
     * This method is called after every insertion, deletion, or rotation. It ensures that
     * each node accurately reflects the statistical extremes (min/max values) within its subtree.
     *  For Type 3 trees (Discard Pile), it specifically tracks the minimum and maximum `H_missing`.
     * @param node The node whose fields need to be updated.
     */
    private void updateNodeFields(AVLNode node) {
        if (node == null) return;
        // 1. Update standard AVL height
        node.height = 1 + Math.max(height(node.left), height(node.right));

        // 2. Update Attack and Health statistics (used by Type 1 and Type 2)
        int mina = node.card.getAcur();
        int maxa = node.card.getAcur();
        int minh = node.card.getHcur();
        int maxh = node.card.getHcur();

        // Aggregate from left subtree
        if (node.left != null) {
            mina = Math.min(mina, node.left.minA);
            maxa = Math.max(maxa, node.left.maxA);
            minh = Math.min(minh, node.left.minH);
            maxh = Math.max(maxh, node.left.maxH);
        }
        // Aggregate from right subtree
        if (node.right != null) {
            mina = Math.min(mina, node.right.minA);
            maxa = Math.max(maxa, node.right.maxA);
            minh = Math.min(minh, node.right.minH);
            maxh = Math.max(maxh, node.right.maxH);
        }

        node.minA = mina;
        node.maxA = maxa;
        node.minH = minh;
        node.maxH = maxh;
        // 3. Update H_missing statistics (used only by Type 3 - Discard Pile)
        if (comparisonType == 3) {
            int minhm = node.card.getHmissing();
            int maxhm = node.card.getHmissing();

            if (node.left != null) {
                minhm = Math.min(minhm, node.left.minHmissing);
                maxhm = Math.max(maxhm, node.left.maxHmissing);
            }
            if (node.right != null) {
                minhm = Math.min(minhm, node.right.minHmissing);
                maxhm = Math.max(maxhm, node.right.maxHmissing);
            }
            node.minHmissing = minhm;
            node.maxHmissing = maxhm;
        }
    }

    /**
     * Calculates the balance factor of a node: height(left) - height(right).
     * @param node The node to check.
     * @return The balance factor.
     */
    private int getBalance(AVLNode node) {
        return (node == null) ? 0 : height(node.left) - height(node.right);
    }

    /**
     * Compares two cards based on the tree's specific comparison type.
     * @param card1 The first card to compare.
     * @param card2 The second card to compare.
     * @return Negative if card1 < card2, positive if card1 > card2, zero if equal (should not happen with TimeID).
     */
    public int compareCards(Card card1, Card card2) {
        // Type 3: Discard Pile (Max H_missing, then Min TimeID)
        if (comparisonType == 3) {
            // Primary: H_missing (Descending -> card with greater H_missing goes first)
            int result = Integer.compare(card2.getHmissing(), card1.getHmissing());
            if (result != 0) return result;

            // Secondary: Entry Time (Ascending -> oldest card goes first as tie-breaker)
            return Long.compare(card1.getEntryTimeID(), card2.getEntryTimeID());
        }
        // Types 1 and 2 Comparison Logic
        int a1 = card1.getAcur();
        int a2 = card2.getAcur();

        // Primary: Attack comparison
        // Type 1: Ascending Attack (min A first) | Type 2: Descending Attack (max A first)
        int result = (comparisonType == 1) ?
                Integer.compare(a1, a2) : Integer.compare(a2, a1);
        if (result != 0) return result;

        // Secondary: health comparison (always ascending)
        result = Integer.compare(card1.getHcur(), card2.getHcur());
        if (result != 0) return result;

        // Tertiary: entry time (always ascending for stability)
        return Long.compare(card1.getEntryTimeID(), card2.getEntryTimeID());
    }

    /**
     * Performs a classic right rotation around node 'y' to restore AVL balance after an insertion/deletion.
     * @param y The root of the unbalanced subtree (right node).
     * @return The new root of the balanced subtree (left node, x).
     */
    private AVLNode rotateRight(AVLNode y) {
        AVLNode x = y.left;
        AVLNode z = x.right;

        x.right = y;
        y.left = z;

        updateNodeFields(y);
        updateNodeFields(x);

        return x;
    }

    /**
     * Performs a classic left rotation around node 'x' to restore AVL balance after an insertion/deletion.
     * @param x The root of the unbalanced subtree (left node).
     * @return The new root of the balanced subtree (right node, y).
     */
    private AVLNode rotateLeft(AVLNode x) {
        AVLNode y = x.right;
        AVLNode z = y.left;

        y.left = x;
        x.right = z;

        updateNodeFields(x);
        updateNodeFields(y);

        return y;
    }

    /**
     * Inserts a card into the tree.
     * Updates max attack card for AVL-2 trees.
     * @param card The card to be inserted.
     */
    public void insert(Card card) {
        lastInsertSuccess = false;
        this.root = insertRecursive(this.root, card);
        if (lastInsertSuccess) {
            this.size++;
        }
        // Update max attack card for AVL-2
        if (comparisonType == 2) {
            if (maxAttackCard == null || compareCards(card, maxAttackCard) < 0) {
                maxAttackCard = card;
            }
        }
    }

    /**
     * Recursive insert with AVL rebalancing.
     * Inserts a new card into the tree while maintaining AVL balance property.
     * The tree remains balanced by ensuring height difference between left and right
     * subtrees never exceeds 1.
     * @param node Current node being examined in recursion
     * @param card Card to be inserted
     * @return Root of the (possibly rebalanced) subtree after insertion
     */
    private AVLNode insertRecursive(AVLNode node, Card card) {
        // Base case: found insertion point (empty spot in tree)
        if (node == null) {
            lastInsertSuccess = true;
            return new AVLNode(card);
        }
        // Determine which subtree to insert into based on comparison
        int comparison = compareCards(card, node.card);

        if (comparison < 0) {
            // Card is "less than" current node, go left
            node.left = insertRecursive(node.left, card);
        } else if (comparison > 0) {
            // Card is "greater than" current node, go right
            node.right = insertRecursive(node.right, card);
        } else {
            // Card already exists, reject insertion
            return node;
        }
        // Update height and subtree statistics (min/max attack and health)
        updateNodeFields(node);
        // Check balance factor: positive means left-heavy, negative means right-heavy
        int balance = getBalance(node);
        // Left-heavy case (balance > 1): left subtree is too tall
        if (balance > 1) {
            if (getBalance(node.left) >= 0) {
                return rotateRight(node);
            } else {
                node.left = rotateLeft(node.left);
                return rotateRight(node);
            }
        }
        // Right-heavy case (balance < -1): right subtree is too tall
        if (balance < -1) {
            if (getBalance(node.right) <= 0) {
                return rotateLeft(node);
            } else {
                node.right = rotateRight(node.right);
                return rotateLeft(node);
            }
        }
        // Node is balanced, return it unchanged
        return node;
    }
    /**
     * Deletes a card from the tree.
     * Removes the specified card and maintains AVL balance property.
     * For AVL-2 trees, updates the maximum attack card if necessary.
     * @param card Card to be deleted
     */
    public void delete(Card card) {
        lastDeleteSuccess = false;
        this.root = deleteRecursive(this.root, card);
        if (lastDeleteSuccess) {
            this.size--;
        }
        // Special handling for AVL-2: maintain max attack card for O(1) Priority 4
        if (comparisonType == 2 && maxAttackCard == card) {
            maxAttackCard = null;
            AVLNode current = root;
            if (current != null) {
                // In AVL-2, minimum node (leftmost) has maximum attack
                // because tree is sorted by descending attack
                while (current.left != null) current = current.left;
                maxAttackCard = current.card;
            } else {
                // Tree is now empty, no max card exists
                maxAttackCard = null;
            }
        }
    }

    /**
     * Finds the minimum node in a given subtree (the leftmost node).
     * @param node The root of the subtree to search.
     * @return The node containing the minimum key in the subtree.
     */
    private AVLNode findMinNode(AVLNode node) {
        AVLNode current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }
    /**
     * Recursive delete with AVL rebalancing.
     * Removes a card from the tree while maintaining AVL balance property.
     * Handles three cases: leaf/single-child nodes and nodes with two children.
     * @param node Current node being examined in recursion
     * @param card Card to be deleted
     * @return Root of the (possibly rebalanced) subtree after deletion
     */
    private AVLNode deleteRecursive(AVLNode node, Card card) {
        // Base case: card not found in tree
        if (node == null) {
            return null;
        }

        int comparison = compareCards(card, node.card);

        if (comparison < 0) {
            // Card is in left subtree
            node.left = deleteRecursive(node.left, card);
        } else if (comparison > 0) {
            // Card is in right subtree
            node.right = deleteRecursive(node.right, card);
        } else {
            // Found the node to delete

            // Case 1 & 2: Node has at most one child
            if (node.left == null || node.right == null) {
                AVLNode temp;
                if (node.left != null){
                    temp = node.left;} // Has left child only
                else{
                    temp = node.right; // Has right child only (or no children)
                }
                if (temp == null) {
                    // Node is a leaf (no children) - simply remove it
                    lastDeleteSuccess = true;
                    node = null;
                } else {
                    // Node has one child - replace node with its child
                    lastDeleteSuccess = true;
                    node = temp;
                }
            } else {
                // Case 3: Node has two children
                // Strategy: Replace with inorder successor (smallest node in right subtree)
                AVLNode temp = findMinNode(node.right);
                node.card = temp.card; // Copy successor's data
                // Delete the successor (which has at most one child)
                node.right = deleteRecursive(node.right, temp.card);
            }
        }
        // If tree had only one node which was deleted, return null
        if (node == null) return null;
        // Update height and subtree statistics after deletion
        updateNodeFields(node);
        // Check balance and rebalance if necessary
        int balance = getBalance(node);

        // Left-heavy (balance > 1)
        if (balance > 1) {
            if (getBalance(node.left) >= 0) {
                return rotateRight(node);
            } else {
                node.left = rotateLeft(node.left);
                return rotateRight(node);
            }
        }

        // Right-heavy (balance < -1)
        if (balance < -1) {
            if (getBalance(node.right) <= 0) {
                return rotateLeft(node);
            } else {
                node.right = rotateRight(node.right);
                return rotateLeft(node);
            }
        }
        // Node is balanced, return it
        return node;
    }

    /**
     * Determines if a subtree could contain a card better than the current best candidate.
     * Uses subtree statistics for efficient pruning during searches.
     * @param bestCandidate The best card found so far in the main DFS search.
     * @return True if the subtree has potential to contain a better card, false if it can be safely pruned.
     */
    private boolean subtreeSearch(AVLNode node, Card bestCandidate) {
        if (bestCandidate == null) return true; // No best yet, any card could qualify
        if (node == null) return false;

        if (comparisonType == 1) {
            // AVL-1: Looking for minimum attack and health
            int optA = node.minA;
            int optH = node.minH;
            int bestA = bestCandidate.getAcur();
            int bestH = bestCandidate.getHcur();
            if (optA > bestA) return false; // Subtree can't beat current best
            if (optA < bestA) return true; // Subtree has potential
            if (optH > bestH) return false;
            if (optH < bestH) return true;
            long bestTime = bestCandidate.getEntryTimeID();
            return Long.MIN_VALUE < bestTime;
        } else if (comparisonType == 2){
            // AVL-2: Looking for maximum attack, then minimum health
            int optA = node.maxA;
            int optH = node.minH;
            int bestA = bestCandidate.getAcur();
            int bestH = bestCandidate.getHcur();
            if (optA < bestA) return false;
            if (optA > bestA) return true;
            if (optH > bestH) return false;
            if (optH < bestH) return true;
            long bestTime = bestCandidate.getEntryTimeID();
            return Long.MIN_VALUE < bestTime;
        }
        else if (comparisonType == 3) {
            // AVL-3: (P1/P2 Healing) maximum H_missing, then minimum TimeID
            int optHmissing = node.maxHmissing;
            int bestHmissing = bestCandidate.getHmissing();

            if (optHmissing < bestHmissing) return false;

            return true;
        }
        return false;

    }


    /** MAIN LOGİC FOR SEARCHING
     * The search logic implemented in the findPriorityX,findStealCard,findMaxH_missing and findMinH_missing() methods is based on an optimized  DFS traversal of the AVL tree.
     * We use a custom Stack implementation to manage the search path iteratively.
     * To prioritize the search space, the right subtree is pushed onto the stack before the left subtree (LIFO order),
     * ensuring that the left branch—which statistically holds the majority of potential candidate cards—is explored first.
     * Furthermore, aggressive pruning is applied: each AVL node maintains the maximum and minimum key values within its subtrees.
     * If a subtree's stored value range definitively falls outside the criteria for a potential candidate,
     * the entire subtree is skipped (pruned) without traversal, significantly enhancing search efficiency.
     */



    /**
     * Finds best card for Priority 1 battle strategy (AVL-1 only).
     * Condition: H_cur > A_stranger AND A_cur >= H_stranger
     * Goal: Find the card with minimum attack and minimum health that satisfies conditions.
     * @param A_stranger Opponent's attack value
     * @param H_stranger Opponent's health value
     * @return Best card matching Priority 1 conditions, or null if none found
     */
    public Card findPriority1(int A_stranger, int H_stranger) {
        if (comparisonType != 1) return null;
        if (root == null) return null;

        stack.clear();
        stack.add(root);
        Card bestCandidate = null;

        while (!stack.isEmpty()) {
            int lastIdx = stack.size() - 1;
            AVLNode node = stack.remove(lastIdx);
            if (node == null) continue;

            if (node.maxH <= A_stranger || node.maxA < H_stranger) continue;
            if (!subtreeSearch(node, bestCandidate)) continue;

            if (node.right != null) stack.add(node.right);

            Card candidate = node.card;
            int cH = candidate.getHcur();
            int cA = candidate.getAcur();
            if (cH > A_stranger && cA >= H_stranger) {
                if (bestCandidate == null || compareCards(candidate, bestCandidate) < 0) {
                    bestCandidate = candidate;
                }
            }

            if (node.left != null) stack.add(node.left);
        }

        return bestCandidate;
    }
    /**
     * Finds best card for Priority 2 battle strategy (AVL-2 only).
     * Condition: H_cur > A_stranger AND A_cur < H_stranger
     * Goal: Find the card with maximum attack and minimum health that satisfies conditions.
     * @param A_stranger Opponent's attack value
     * @param H_stranger Opponent's health value
     * @return Best card matching Priority 2 conditions, or null if none found
     */
    public Card findPriority2(int A_stranger, int H_stranger) {
        if (comparisonType != 2) return null;
        if (root == null) return null;

        stack.clear();
        stack.add(root);
        Card bestCandidate = null;

        while (!stack.isEmpty()) {
            int lastIdx = stack.size() - 1;
            AVLNode node = stack.remove(lastIdx);
            if (node == null) continue;

            if (node.minA >= H_stranger || node.maxH <= A_stranger) continue;
            if (!subtreeSearch(node, bestCandidate)) continue;

            if (node.right != null) stack.add(node.right);

            Card candidate = node.card;
            int cA = candidate.getAcur();
            int cH = candidate.getHcur();
            if (cA < H_stranger && cH > A_stranger) {
                if (bestCandidate == null || compareCards(candidate, bestCandidate) < 0) {
                    bestCandidate = candidate;
                }
            }

            if (node.left != null) stack.add(node.left);
        }

        return bestCandidate;
    }

    /**
     * Finds best card for Priority 3 battle strategy (AVL-1 only).
     * Condition: H_cur =< A_stranger AND A_cur => H_stranger
     * Goal: Find the card with minimum attack and minimum health that satisfies conditions.
     * @param A_stranger Opponent's attack value
     * @param H_stranger Opponent's health value
     * @return Best card matching Priority 3 conditions, or null if none found
     */
    public Card findPriority3(int A_stranger, int H_stranger) {
        if (comparisonType != 1) return null;
        if (root == null) return null;

        stack.clear();
        stack.add(root);
        Card bestCandidate = null;

        while (!stack.isEmpty()) {
            int lastIdx = stack.size() - 1;
            AVLNode node = stack.remove(lastIdx);
            if (node == null) continue;

            if (node.minH > A_stranger || node.maxA < H_stranger) continue;
            if (!subtreeSearch(node, bestCandidate)) continue;

            if (node.right != null) stack.add(node.right);

            Card candidate = node.card;
            int cH = candidate.getHcur();
            int cA = candidate.getAcur();
            if (cH <= A_stranger && cA >= H_stranger) {
                if (bestCandidate == null || compareCards(candidate, bestCandidate) < 0) {
                    bestCandidate = candidate;
                }
            }

            if (node.left != null) stack.add(node.left);
        }

        return bestCandidate;
    }

    /**
     * This is O(1) constant time because maxAttackCard is cached.
     * Finds best card for Priority 4 battle strategy (AVL-2 only).
     * Condition: max A_cur
     * Goal: Find the card with maximum attack.
     * @return Best card matching Priority 4 conditions, or null if tree is empty
     */
    public Card findPriority4() {
        if (comparisonType != 2) return null;
        return maxAttackCard;
    }

    /**
     * Only AVL-1
     * Condition: H_cur =< A_stranger AND A_cur => H_stranger
     * Goal: Find the card with A_cur > attLimit and H_cur > hLimit.
     * @param attLimit Minimum attack threshold for stealing
     * @param hLimit Minimum health threshold for stealing
     * @return Best card to steal, or null if no card qualifies
     */
    public Card findStealCard(int attLimit, int hLimit) {
        if (comparisonType != 1) return null;
        if (root == null) return null;

        stack.clear();
        stack.add(root);
        Card bestCandidate = null;

        while (!stack.isEmpty()) {
            int lastIdx = stack.size() - 1;
            AVLNode node = stack.remove(lastIdx);
            if (node == null) continue;

            if (node.maxA <= attLimit || node.maxH <= hLimit) continue;
            if (!subtreeSearch(node, bestCandidate)) continue;

            if (node.right != null) stack.add(node.right);

            Card candidate = node.card;
            int cA = candidate.getAcur();
            int cH = candidate.getHcur();
            if (cA > attLimit && cH > hLimit) {
                if (bestCandidate == null || compareCards(candidate, bestCandidate) < 0) {
                    bestCandidate = candidate;
                }
            }

            if (node.left != null) stack.add(node.left);
        }

        return bestCandidate;
    }

    /**
     * Finds the best card for the healing phase, corresponding to Priority 1 and 2:
     * This is an optimized traversal on the Type 3 AVL. Since the tree is ordered
     * by descending H_missing, the most promising nodes are often near the root/left side.
     * We apply two main pruning checks:
     * 1. Check against the current best candidate's H_missing.
     * 2. Check against the `healLimit` using the subtree's `minHmissing` (if minH_m > limit, no full revive possible).
     * @param healLimit The available healing pool points.
     * @return The best card for full revival, or null.
     */
    public Card findMaxH_missing(int healLimit) {
        if (comparisonType != 3) return null;
        //Pruning: If even the minimum H_missing in the tree is above the limit, nothing can be fully revived.
        if (root == null || root.minHmissing > healLimit) return null;

        stack.clear();
        stack.add(root);
        Card bestCandidate = null;

        while (!stack.isEmpty()) {
            int lastIdx = stack.size() - 1;
            AVLNode node = stack.remove(lastIdx);
            if (node == null) continue;

            // Pruning 1: If the largest H_missing in this subtree is worse than our current best, prune.
            if (bestCandidate != null && node.maxHmissing < bestCandidate.getHmissing()) continue;
            // Pruning 2: If the smallest H_missing in this subtree is already too high to be fully healed, prune.
            if (node.minHmissing > healLimit) continue;

            // Pushing order: Check the right subtree before the left one since LIFO.
            // Since Type 3 sorts by descending H_missing, the better (larger) H_missing values
            // are generally to the left. We push right first to prioritize searching left.
            if (node.right != null) stack.add(node.right);

            Card candidate = node.card;
            int canHM = candidate.getHmissing();

            // Check if current node is a valid full revival candidate
            if (canHM > 0 && canHM <= healLimit) {
                // Compare with best found for now
                if (bestCandidate == null || compareCards(candidate, bestCandidate) < 0) {
                    bestCandidate = candidate;
                }
            }

            if (node.left != null) stack.add(node.left);
        }

        return bestCandidate;
    }

    /**
     * Finds the best card for the Healing Phase, corresponding to Priority 3 (Partial Revival):
     * the card with the minimum H_missing, breaking ties by the oldest card (Minimum TimeID).
     * This search differs from P1/P2 as it seeks the minimum H_missing. The tree's
     * sorting (Max H_missing first) makes this an anti-order search. However, we use
     * the subtree's `minHmissing` statistic to prune branches that cannot possibly
     * hold a better (lower H_missing) candidate than the one currently found. We explore
     * both sides to account for the TimeID tie-breaker.
     * @return The best card for partial revival, or null if no discarded card exists.
     */
    public Card findMinH_missing() {
        if (comparisonType != 3) return null;
        if (root == null) return null;

        stack.clear();
        stack.add(root);
        Card bestCandidate = null;

        while (!stack.isEmpty()) {
            int lastIdx = stack.size() - 1;
            AVLNode node = stack.remove(lastIdx);
            if (node == null) continue;

            // Get current best stats (initialized to max possible values)
            int bestHM = bestCandidate != null ? bestCandidate.getHmissing() : Integer.MAX_VALUE;
            long bestTime = bestCandidate != null ? bestCandidate.getEntryTimeID() : Long.MAX_VALUE;

            // Pruning: If the smallest H_missing in this subtree is already worse (greater)
            // than our current best H_missing, we can prune the whole branch.
            if (node.minHmissing > bestHM) continue;

            // Check Left subtree first (Min TimeID tie-breaker is key for P3)
            if (node.left != null) stack.add(node.left);

            Card candidate = node.card;
            int cHM = candidate.getHmissing();

            // Check if current node is a valid candidate (H_missing must be > 0)
            if (cHM > 0) {
                // P3 Comparison: Min H_missing, or Min TimeID if H_missing is tied.
                if (cHM < bestHM || (cHM == bestHM && candidate.getEntryTimeID() < bestTime)) {
                    bestCandidate = candidate;
                }
            }

            // Check Right subtree (Min H_missing is favored by the tree order, but we must check it)
            if (node.right != null) stack.add(node.right);
        }
        return bestCandidate;
    }
}