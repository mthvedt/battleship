(ns battleship.game
  (:use battleship.core)
  (:gen-class))

(defn get-square-str [{piece :piece, state :state}  is-friendly]
  (case state
    :sunk "#"
    :struck (if (nil? piece) "." "*")
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
                            (map #(get-square-str % is-friendly) row) "|")))
           (range (int \A) (int \K)) (:board dboard))
      [top-bottom-border])))

(defrecord DecoratedBoard [board pieces action])

(defn new-decorated-board []
  (DecoratedBoard. (place-all-pieces (newboard))
                   (apply hash-map (apply concat pieces)) nil))

(defn print-message [dboard is-friendly]
  nil)

(defn game-over? [dboard]
  (zero? (apply + (vals pieces))))

(defrecord Game [board1 board2])

(defn newgame []
  (Game. (new-decorated-board)
         (new-decorated-board)))

(defn printgame [{dboard1 :board1, dboard2 :board2}]
  (print-message dboard1 true) (print-message dboard2 false)
  (println (str "My board" "             " "Your board"))
  (dorun (map println
              (get-board-strs dboard2 false) (repeat "      ")
              (get-board-strs dboard1 true))))

(defn -main [& args]
  (printgame (newgame)))
