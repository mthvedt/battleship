(ns battleship.core
  (:import java.util.Random))

; This macro will help us construct lazy sequences.
(defmacro repeat-expr [expr]
  `(repeatedly (fn [] ~expr)))

; A square can be empty or contain a ship.
; A square can be in three states: unstruck, struck, or sunk.
(defrecord Square [piece state])

(def board-size 10)
(defn newboard []
  (vec (repeat board-size (vec (repeat board-size (Square. nil :unstruck))))))

(defn get-square [board x y]
  (nth (nth board x) y))

(defn set-square [board x y square]
  (let [row (nth board x)]
    (assoc board x (assoc row y square))))

(def pieces
  [["carrier" 5]
   ["battleship" 4]
   ["cruiser" 3]
   ["submarine" 3]
   ["destroyer" 2]])

; places a piece on the board, or nil if it can't be placed
(defn place-piece [board0 [piecename piecelen] x0 y0 is-horizontal]
  (let [xstep (if is-horizontal 1 0)
        ystep (if is-horizontal 0 1)]
    (loop [board board0 x x0 y y0 i 0]
      (if (= i piecelen)
        board
        (if (nil? (:piece (get-square board0 x y)))
          (recur (set-square board x y (Square. piecename :unstruck))
                 (+ x xstep) (+ y ystep) (inc i))
          nil)))))

; try once to place a piece, return nil if failed
(defn randomly-try-place-piece [board [piecename piecelen] random]
  (let [is-horizontal (= (.nextInt random 2) 0)
        coord-a (.nextInt random board-size)
        ; subtract piecelen; make sure the piece doesn't overflow off the board
        coord-b (.nextInt random (- board-size piecelen))]
    (let [x (if is-horizontal coord-b coord-a)
          y (if is-horizontal coord-a coord-b)]
      (place-piece board [piecename piecelen] x y is-horizontal))))

; try (possibly forever!) to place a piece
(defn randomly-place-piece [board piece random]
  (first (remove nil? (repeat-expr
                        (randomly-try-place-piece board piece random)))))

(defn place-all-pieces [board random]
  (reduce #(randomly-place-piece % %2 random) board pieces))

(defn get-square-str [{piece :piece, state :state}  is-friendly]
  (case state
    :sunk "#"
    :struck (if (nil? piece) "." "*")
    :unstruck (if (and is-friendly (not (nil? piece))) "O" " ")))

(defn print-board [board is-friendly]
  (println (apply str (concat "  " (range 10))))
  (let [top-bottom-border (apply str
                              (concat " +" (repeat board-size "-") "+"))]
    (println top-bottom-border)
    (reduce 
      (fn [row-char-num row]
        (println
          (apply str
                 (concat [(char row-char-num)] "|"
                         (map #(get-square-str % is-friendly) row) "|")))
        (inc row-char-num))
      (int \A) board)
    (println top-bottom-border)))
