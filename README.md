Nightpass: Strategic Card Game Engine
Nightpass is a high-performance card battle engine developed in Java. The system is designed to simulate complex strategic duels between a 'Survivor' and 'The Stranger' while handling massive datasets under strict performance and memory constraints. The project demonstrates advanced software architecture, deterministic logic, and efficient resource management.

System Architecture
1. Card State Management

The engine tracks multiple layers of card statistics to manage temporary damage and permanent progression:

Initial Stats (A_init, H_init): The card's original values when first drawn.

Baseline Stats (A_base, H_base): The permanent current stats, which can be modified by revival penalties.

Current Stats (A_cur, H_cur): Real-time values affected by battle damage, used for priority selection and combat resolution.


2. Battle Priority and Decision Logic

The core engine utilizes a 4-tier deterministic priority system to select the optimal card for any given opponent encounter. If a priority search fails, the engine falls back to the next tier :

Priority 1: Survive and Kill – Selects a card that survives the encounter (H_cur > A_stranger) and defeats the opponent (A_cur >= H_stranger) .

Priority 2: Survive and Not Kill – Maximizes damage output while ensuring the card returns to the deck.

Priority 3: Kill and Don't Survive – Prioritizes eliminating high-health enemy cards even if it results in unit loss .

Priority 4: Maximum Damage – Default logic to ensure maximum impact when defeat is inevitable .

Tie-Breaker: In case of identical stats, the system uses a 'First-In-First-Out' (FIFO) policy based on deck entry time.



3. Dynamic Mechanics

Attack Scaling: Attack power is dynamically recomputed after every survived battle based on health ratios: A_cur = max(1, floor(A_base * H_cur / H_base)).

Revival Algorithm: Implements a sophisticated healing phase where a point pool is distributed to cards in the discard pile.

Permanent Penalties: Units suffer a 10% reduction in A_base for full revivals and sequential 5% reductions for partial revivals, ensuring long-term strategic weight to unit loss.



Performance and Scalability
Large-Scale Data Handling: Optimized to process up to 550,000 commands and 400,000 unique card entities .

Time Complexity: Engineered to complete execution within a 15-second limit for the largest test cases.

Strict Memory Constraints: To demonstrate custom data structure implementation, no collections other than ArrayList were utilized. All search and sort logic is custom-built.



Tech Stack
Language: Java.

Concepts: Object-Oriented Programming (OOP), Custom Algorithm Design, File I/O Stream Management.

Execution
javac *.java
java Main <input_file> <output_file>
