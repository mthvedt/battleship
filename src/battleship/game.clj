(ns battleship.game
  (:use battleship.core battleship.ai)
  (:gen-class))

; A board + some data
(defrecord DecoratedBoard [board pieces action])

; a board together with some info about the game state
(defn new-decorated-board []
  (DecoratedBoard. (place-all-pieces newboard) pieces-map nil))

; canonically, board1 is player's board (he fires upon board2)
(defrecord Game [board1 board2])
(defn newgame []
  (Game. (new-decorated-board)
         (new-decorated-board)))

; If all pieces have no HP, the game is over for that player
(defn board-lost? [dboard]
  (zero? (apply + (vals (:pieces dboard)))))

; true if someone won
(defn game-won? [game]
  (or (board-lost? (:board1 game)) (board-lost? (:board2 game))))

; 0 for nobody, 1 for player, 2 for computer
(defn get-winner [game]
  (if (board-lost? (:board1 game))
    2
    (if (board-lost? (:board2 game)) 1 0)))

; Helper fns for 'fire--this one causes a square to be missed
(defn miss [dboard x y target]
  (assoc dboard
         :board (set-square (:board dboard) x y
                            (assoc target :state :struck))
         :action [x y :missed]))

; Causes a square to be struck
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
