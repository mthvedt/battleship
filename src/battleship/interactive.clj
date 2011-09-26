(ns battleship.interactive
  (:use (battleship core ai game))
  (:import battleship.game.Game)
  (:gen-class))
; Text interactive game vs the computer.

; How many boards the AI can search. 100 is just flawed enough to be interesting
(def ai-search 100)

; Turn for the AI. Returns [modified game, modified ai-dist]
(defn do-computer-turn [game]
  (let [dboard1 (:board1 game)
        board1 (:board dboard1)
        board-samples (infinite-boards
                        board1
                        ; remove all dead pieces from the 'pieces set'
                        ; before passing to infinite-boards
                        (select-keys pieces-map
                                     (map first
                                          (remove #(= 0 (second %))
                                                  (:pieces dboard1)))))
        [x y] (get-target board1 board-samples ai-search)
        was-occupied (not (nil? (:piece (get-square board1 x y))))
        newdboard1 (fire dboard1 x y)]
    (assoc game :board1 newdboard1)))

; Helper fns for the battleship main loop
(defn is-valid-coord [coordinate]
  (and (>= coordinate 0) (< coordinate 10)))

(def QUIT (Object.))

; Tries to parse the input line and make a player move
; reutrns the game, error message, or QUIT
(defn parse-player-turn [game input-line]
  ; cast to char below is anti-reflection so applets can use this code
  (let [letter (first (filter #(Character/isLetter (char %)) input-line))
        number (first (filter #(Character/isDigit (char %)) input-line))]
    (cond
      (or (= \q letter) (= \Q letter)) QUIT ; game over

      (or (nil? letter) (nil? number)) "Please input valid coordinates."

      (not (nil? (nth (concat (filter #(not (Character/isWhitespace (char %)))
                                      input-line)
                              (repeat nil)) ; prevent NPE
                      2))) ; means more than 2 things were input
      "Please input just a letter and a number."

      true
      (let [uppercase-letter (if (>= (int letter) (int \a))
                               (char (- (int letter) 32))
                               letter)
            letter-coord (- (int uppercase-letter) (int \A))
            number-coord (Character/getNumericValue (char number))]
        (if (and (is-valid-coord letter-coord)
                 (is-valid-coord number-coord))
          (Game. (:board1 game)
                 (fire (:board2 game) number-coord letter-coord))
          "Please input valid coordinates."))))) 

; All fns below interact with the *out* stream

; Helper for printgame
(defn print-message [dboard is-player]
  (when-let [[x y result & more] (:action dboard)]
    (if is-player
      (print "You fire at ")
      (print "I fire at "))
    (print (str (char (+ y (int \A))) x ". "))
    (println (case result
               :ineffective "But that area has already been fired upon."
               :missed "It's a miss."
               :struck "It's a hit!"
               :sunk (str (if is-player
                            " *** You sunk my "
                            " *** I sunk your ") (first more) "! *** ")))))

(defn printgame [{dboard1 :board1, dboard2 :board2}]
  (print-message dboard2 true) (print-message dboard1 false)
  (println (str "My board" "             " "Your board"))
  (dorun (map println
              (get-board-strs (:board dboard2) (:pieces dboard2) false)
              (repeat "      ")
              (get-board-strs (:board dboard1) (:pieces dboard1) true))))

(defn print-endgame [game]
  (let [winner (get-winner game)]
    (if (= winner 1)
      (println "*** YOU WIN! ***")
      (println "*** YOU LOSE! ***"))))

; Parse input and fire, talks to *in* and *out*
; returns the modified game or the object QUIT
(defn do-player-turn [game]
  (do
    (print "Fire: ") (flush)
    (let [input-line (read-line)
          result (parse-player-turn game input-line)]
      (if (string? result)
        (do
          (println result) ; we got an error message... print and try again
          (recur game))
        result))))

; And the mainloop. After this we're done!
(defn -main [& args]
  (println
    "Welcome to Battleship. Input some coordinates to fire, or 'Q' to quit.")
  (loop [game (newgame)]
    (dotimes [i 8] (println))
    (printgame game)
    (if (game-won? game)
      (print-endgame game) ; do not recur, terminate
      (let [game (do-player-turn game)]
        (cond
          (= QUIT game) (do (println "Be seeing you...") nil) ; quit
          (game-won? game) (do (printgame game) (print-endgame game))
          true (recur (do-computer-turn game))))))
  (flush))
