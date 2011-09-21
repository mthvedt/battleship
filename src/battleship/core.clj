(ns battleship.core)

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
(defn randomly-try-place-piece [board [piecename piecelen]]
  (let [is-horizontal (= (rand-int 2) 0)
        coord-a (rand-int board-size)
        ; subtract piecelen; make sure the piece doesn't overflow off the board
        coord-b (rand-int (- board-size piecelen))]
    (let [x (if is-horizontal coord-b coord-a)
          y (if is-horizontal coord-a coord-b)]
      (place-piece board [piecename piecelen] x y is-horizontal))))

; try (possibly forever!) to place a piece
; works by making an infinite sequence of randomly-try-place-piece calls
; and pulling the first one
(defn randomly-place-piece [board piece]
  (first (remove nil? (repeat-expr
                        (randomly-try-place-piece board piece)))))

(defn place-all-pieces [board]
  (reduce randomly-place-piece board pieces))
