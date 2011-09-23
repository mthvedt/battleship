# battleship

A Monte Carlo Battleship AI, in Clojure.

## Motivation

I wrote this as a coding exercise to get the hang of functional programming techniques and probabilistic methods. Also, because I enjoy wasting my time playing games.

## Description

Battleship is a Clojure (JVM, Java-compatible, highly functional LISP) command-line app that allows you to play a game of the classic board game, Battleship, against the computer. The computer uses a Monte Carlo solver to guess where your ships are.

Currently, ship placement is random. You can't place your own ships. Maybe next release.

## How to play

Download the distribution, or the standalone jar, and run:
```
java -jar battleship.jar
```

and follow the very simple instructions. Enjoy!

### The solver

The computer generates an infinite sequence of possible boards, i.e., arrangements of ships the player might have, The sequence is lazy, boards not being generated until they are used.
It then filters this sequence, rejecting boards that it knows to be impossible--for instance, if a ship is someplace the computer fired upon and missed, or if the computer hit a ship and there's no ship there, that board is rejected as invalid. This generates another infinite sequence.
The computer then takes some fixed number of valid boards--100 in this release--and uses these to calculate where the player's ships most likely are.
It uses a greedy firing algorithm, firing on the square most likely to contain a ship. The computer does not plan ahead. Even with this greedy algorithm, it is still pretty good.

### Implementation

The first implementation used naive generation--any possible board was generated, and then rejected. As the board filled up with hits and misses, this became unacceptably slow. So the rejection criteria were divided into two: one set taken into account at board generation time, to efficiently generate boards that are more likely to be valid. The second set operates after board generation time, rejecting boards that turned out invalid anyway. This implementation is much faster; however, it still slows down in some edge cases--in particular, if the computer has struck several ships, but not sunk them yet.

Some impossible positions are not actually rejected. For instance, suppose the computer has fired at, and hit C5, D5, E5 and F5, without having sunk a ship. The computer may generate some speculative boards where these four squares are occupied by a battleship (which is four spaces long); even though that's impossible because it would have been sunk. These impossible positions seem to have a negligible effect on the computer's play, and so are not rejected because the programmer is a little lazy.

### Thoughts

* The random distribution is as follows: it places one ship randomly, then another, then another, until it's done. This looks uniformly random, but it actually isn't. For instance, positions where there are more options to place the last ship will be less likely than positions where there are fewer ways to place the last ship. Nevertheless, it is "random enough" for now.
* The computer doesn't make any short-circuit inferences. For instance, when it might be totally clear to a human player where his last carrier is located, the computer will still run a full simulation to find it. This can sometimes be noticeably slow.
* On that note, there is certainly a lot of room for performance improvements.
* The computer always starts off by firing at the center, which is where pieces are most likely to be placed in its random simulations. A human, given the option, can exploit this by preferring edge and corner positions.

## License

This work is public domain. This work comes distributed with Clojure, which is released under the Eclipse Public License.

## And, finally...

Please notify me if you find any issues!
