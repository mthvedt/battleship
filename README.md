# battleship

A Monte Carlo Battleship AI, in Clojure.

## Motivation

A coding exercise/code sample to work with functional programming techniques of calculation. In particular, probabilistic Monte Carlo methods.

## Description

Battleship is a Java command line app that allows you to play a game of the classic board game, Battleship, against the computer. The computer uses a Monte Carlo solver to guess where your ships are.

Currently, ship placement is random. You can't place your own ships. Maybe next release.

Battleship is written in Clojure, a LISP-like functional programming language that compiles down to Java bytecode. It will run anywhere Java can run.

## How to play

Download the distribution, or the standalone jar, and run:
```
java -jar battleship.jar
```

and follow the instructions. Enjoy!

### The solver

The AI uses a greedy firing algorithm. Given some known facts about the players pieces are--which squares have hits, which squares have misses, which squares have sunken ships, and which ships are sunk--the AI attempts to guess where the player's ships are most likely to be, and then fires upon the square most likely to contain a ship.
To do this, the computer generates a sequence of random ship positions (boards) the player might have. It then reduces some number of positions into a random probability distribution.
Clojure was chosen primarily because the author wanted to learn Clojure. This problem was chosen because the author wanted to play with practical Monte Carlo probability simulations, and Clojure is near ideal for quick algorithmic coding and experimentation. The implementation is concise, though not as concise as a master of this language might come up with. The entire AI module, for example, is about 90 lines of code, including comments. As Clojure is a functional language that encourages modular composition of higher-order functions, instead of control structures and imperative steps, the elements of an algorithmic idea can quickly be coded, mixed, matched, debugged, and rewritten simply by writing and rearranging functions.

### Thoughts

* The computer generates random positions as follows: it places one ship randomly, then another, then another, until it's done. Any one ship misplacement--for instance, a ship being placed in a 'miss' square, or two squares overlapping--causes the entire position to be rejected. This prevents sample space bias--if you imagine piece placements as a probability tree, with each branch being a piece placement and the leaves being fully placed boards, it's important not to misweight the leaves by equal weighting the branch subdivisions further up when one branch has more leaves, yielding nonuniform probability weights to the leaves.
* The computer doesn't make any short-circuit inferences. For instance, when it might be totally clear to a human player where his last carrier is located, the computer will still run a full simulation to find it. This can be slow in edge cases.
* The code is not heavily optimized, so there is room for other performance improvements as well.
* The computer always starts off by firing at the center, which is where pieces are most likely to be placed in its random simulations. A human, given the option, can exploit this by preferring edge and corner positions. Fortunately for the computer, the human is not given the option in this version!

## License

This work is public domain. This work's binary comes distributed with Clojure, which is released under the Eclipse Public License.

## And, finally...

Please notify me if you find any issues!
