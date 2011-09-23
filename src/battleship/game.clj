(ns battleship.game
  (:use battleship.core)
  (:gen-class))

; A board + some data
(defrecord DecoratedBoard [board pieces action])

(defn new-decorated-board []
  (DecoratedBoard. (place-all-pieces (newboard))
                   (apply hash-map (apply concat pieces))
                   nil))

; Returns a str representing a square
(defn get-square-str [{piece :piece, state :state} pieces is-friendly]
  (case state
    :struck (if (nil? piece) "."
              (if (zero? (get pieces piece)) "#"
                "*"))
    :unstruck (if (and is-friendly (not (nil? piece))) "O" " ")))

; A sequence of strs (lines) representing a board
; each str has the same length, making this a 13x13 char grid
(defn get-board-strs [dboard is-friendly]
  (let [top-bottom-border (apply str
                                 (concat " +" (repeat board-size "-") "+"))]
    (concat
      [(apply str (concat "  " (range 10) " "))]
      [top-bottom-border]
      (map (fn [char-num row]
             (apply str
                    (concat [(char char-num)] "|"
                            (map #(get-square-str %
                                                  (:pieces dboard)
                                                  is-friendly)
                                 row) "|")))
           (range (int \A) (int \K)) (:board dboard))
      [top-bottom-border])))

; good for debugging
(defn print-board [board]
  (dorun (map println (get-board-strs (DecoratedBoard. board nil nil) true))))

(defn print-message [dboard is-friendly]
  nil)

; If all pieces have no HP, the game is over for that player
(defn board-won? [dboard]
  (zero? (apply + (vals (:pieces dboard)))))

(defn game-won? [game]
  (or (board-won? (:board1 game)) (board-won? (:board2 game))))

(defn get-winner [game]
  (if (board-won? {:board1 game})
    1
    (if (board-won? {:board2 game})
      2
      0)))

(defrecord Game [board1 board2])

(defn newgame []
  (Game. (new-decorated-board)
         (new-decorated-board)))

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
                            "You sunk my "
                            "I sunk your ") (first more) "!")))))

; Print a game to *out*
(defn printgame [{dboard1 :board1, dboard2 :board2}]
  (print-message dboard2 true) (print-message dboard1 false)
  (println (str "My board" "             " "Your board"))
  (dorun (map println
              (get-board-strs dboard2 false) (repeat "      ")
              (get-board-strs dboard1 true))))

; Fires a missle at loc (x, y). Returns the modified dboard.
(defn fire [dboard x y]
  (let [target (get-square (:board dboard) x y)
        {piece :piece, state :state} target]
    (if (nil? piece) ; miss
      (assoc dboard
             :board (set-square (:board dboard) x y
                                (assoc target :state :struck))
             :action [x y :missed])
      (if (= :unstruck state) ; hit something unstruck
        (let [pieces (:pieces dboard)
              hitpoints (dec (get pieces piece))] ; decrement piece's HP
          (DecoratedBoard. (set-square (:board dboard) x y
                                       (assoc target :state :struck))
                           (assoc pieces piece hitpoints)
                           (if (zero? hitpoints)
                             [x y :sunk piece]
                             [x y :struck])))
        (assoc dboard :action [x y :ineffective])))))

(defn is-valid-coord [coordinate]
  (or (>= coordinate 0) (< coordinate 10)))

(defn -main [& args]
  (println
    "Welcome to Battleship. Input some coordinates to fire, or 'Q' to quit.")
  (loop [game (newgame)]
    (printgame game)
    (if (game-won? game)
      (let [winner (get-winner game)]
        (if (= winner 1)
          (println "***YOU WIN!***")
          (println "***YOU LOSE!***"))
        (flush)
        nil) ; returning nil not neccesary, but clarifies the code
      (do
        (print "Fire: ")
        (flush)
        (let [input-line (read-line) 
              letter (first (filter #(Character/isLetter %) input-line))
              number (first (filter #(Character/isDigit %) input-line))]
          (cond
            (= \Q letter)
            (do
              (println "Be seeing you...")
              (flush)
              nil)
            (or (nil? letter) (nil? number))
            (do
              (println "Please input valid coordinates.")
              (recur game))
            true
            (let [uppercase-letter (if (>= (int letter) (int \a))
                                     (char (- (int letter) 32))
                                     letter)
                  letter-coord (- (int uppercase-letter) (int \A))
                  number-coord (Character/getNumericValue number)]
              (if (and (is-valid-coord letter-coord)
                       (is-valid-coord number-coord))
                (recur (Game. (:board1 game)
                              (fire (:board2 game) number-coord letter-coord)))
                (do
                  (println "Please input valid coordinates.")
                  (recur game))))))))))
