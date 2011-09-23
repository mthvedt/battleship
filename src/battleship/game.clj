(ns battleship.game
  (:use battleship.core battleship.ai)
  (:gen-class))

; A board + some data
(defrecord DecoratedBoard [board pieces action])

(defn new-decorated-board []
  (DecoratedBoard. (place-all-pieces newboard) pieces-map nil))

; Returns a str representing a square
(defn print-message [dboard is-friendly]
  nil)

; If all pieces have no HP, the game is over for that player
(defn board-lost? [dboard]
  (zero? (apply + (vals (:pieces dboard)))))

(defn game-won? [game]
  (or (board-lost? (:board1 game)) (board-lost? (:board2 game))))

; 0 for nobody, 1 for player, 2 for computer
(defn get-winner [game]
  (if (board-lost? (:board1 game))
    2
    (if (board-lost? (:board2 game)) 1 0)))

; canonically, board1 is player's board (he fires upon board2)
(defrecord Game [board1 board2])
(defn newgame []
  (Game. (new-decorated-board)
         (new-decorated-board)))

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
                            "***You sunk my "
                            "***I sunk your ") (first more) "!***")))))

; Print a game to *out*
(defn printgame [{dboard1 :board1, dboard2 :board2}]
  (print-message dboard2 true) (print-message dboard1 false)
  (println (str "My board" "             " "Your board"))
  (dorun (map println
              (get-board-strs (:board dboard2) (:pieces dboard2) false)
              (repeat "      ")
              (get-board-strs (:board dboard1) (:pieces dboard1) true))))

; Helper fns for 'fire
(defn miss [dboard x y target]
  (assoc dboard
         :board (set-square (:board dboard) x y
                            (assoc target :state :struck))
         :action [x y :missed]))

(defn hit [dboard x y target]
  (let [{piece :piece, state :state} target]
    (if (= :unstruck state) ; hit something unstruck
      (let [pieces (:pieces dboard)
            hitpoints (dec (get pieces piece))] ; decrement piece's HP
        (DecoratedBoard. (set-square (:board dboard) x y
                                     (assoc target :state :struck))
                         (assoc pieces piece hitpoints)
                         (if (zero? hitpoints)
                           [x y :sunk piece]
                           [x y :struck])))
      (assoc dboard :action [x y :ineffective])))) ; already fired here

; Fires a missle at loc (x, y). Returns the modified dboard.
(defn fire [dboard x y]
  (let [target (get-square (:board dboard) x y)
        piece (:piece target)]
    (if (nil? piece) ; miss
      (miss dboard x y target)
      (hit dboard x y target))))

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
; All fns below interact with the in and out streamsh
(defn is-valid-coord [coordinate]
  (and (>= coordinate 0) (< coordinate 10)))

(defn print-endgame [game]
  (let [winner (get-winner game)]
    (if (= winner 1)
      (println "*** YOU WIN! ***")
      (println "*** YOU LOSE! ***"))))

; Parse input and fire
(defn do-player-turn [game]
  (do
    (print "Fire: ") (flush)
    (let [input-line (read-line) 
          letter (first (filter #(Character/isLetter %) input-line))
          number (first (filter #(Character/isDigit %) input-line))]
      (cond
        (or (= \q letter) (= \Q letter))
        (do
          (println "Be seeing you...")) ; return nil--game over
        (or (nil? letter) (nil? number))
        (do
          (println "Please input valid coordinates.")
          (recur game)) ; Go back to beginning, try again
        (not (nil? (nth (concat (filter #(not (Character/isWhitespace %))
                                        input-line)
                                (repeat nil)) ; prevent NPE
                        2))) ; more than 2 things were input
        (do
          (println "Please input just a letter and a number.")
          (recur game))
        true
        (let [uppercase-letter (if (>= (int letter) (int \a))
                                 (char (- (int letter) 32))
                                 letter)
              letter-coord (- (int uppercase-letter) (int \A))
              number-coord (Character/getNumericValue number)]
          (if (and (is-valid-coord letter-coord)
                   (is-valid-coord number-coord))
            (Game. (:board1 game)
                   (fire (:board2 game) number-coord letter-coord))
            (do
              (println "Please input valid coordinates.")
              (recur game))))))))

; And the mainloop. Phew!
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
          (nil? game) nil
          (game-won? game) (do (printgame game) (print-endgame game))
          true (recur (do-computer-turn game))))))
  (flush))
