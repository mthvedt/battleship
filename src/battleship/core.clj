(ns battleship.core)
; Very basic battleship stuff goes here.

; A square can be empty or contain a ship.
; A square can be in three states: unstruck, struck, or sunk.
(defrecord Square [piece state])

; Boards and squares.
(def board-size 10)
(def newboard
  (vec (repeat board-size (vec (repeat board-size (Square. nil :unstruck))))))

(defn get-square [board x y]
  (nth (nth board y) x))

(defn set-square [board x y square]
  (let [row (nth board y)]
    (assoc board y (assoc row x square))))

; The pieces in the canonical US version of Battleship.
(def pieces
  [["carrier" 5]
   ["battleship" 4]
   ["cruiser" 3]
   ["submarine" 3]
   ["destroyer" 2]])

; A hash map version.
(def pieces-map (reduce conj {} pieces))

; places a piece on the board, or nil if it can't be placed according
; to the given validator fn
; the validator function should take in the original board, x, and y
; and return true if the validator will allow a piece to place there
;
; this allows us to generate random boards (with randomly-try-place-piece
; below) according to certain constraints.
(defn place-piece [board0 [piecename piecelen] x0 y0 is-horizontal validator]
  (let [xstep (if is-horizontal 1 0)
        ystep (if is-horizontal 0 1)]
    (loop [board board0 x x0 y y0 i 0]
      (if (= i piecelen)
        board
        (if (validator board0 x y)
          (recur (set-square board x y (Square. piecename :unstruck))
                 (+ x xstep) (+ y ystep) (inc i))
          nil)))))

; try once to place a piece, return nil if failed
(defn randomly-try-place-piece [board [piecename piecelen] validator]
  (let [is-horizontal (= (rand-int 2) 0)
        coord-a (rand-int board-size)
        ; subtract piecelen; make sure the piece doesn't overflow off the board
        coord-b (rand-int (- board-size piecelen))]
    (let [x (if is-horizontal coord-b coord-a)
          y (if is-horizontal coord-a coord-b)]
      (place-piece board [piecename piecelen] x y is-horizontal validator))))

; try (possibly forever!) to place a piece
; works by making an infinite sequence of randomly-try-place-piece calls
; and pulling the first one
(defn randomly-place-piece [board piece validator]
  (first (remove nil? (repeatedly
                        #(randomly-try-place-piece board piece validator)))))

(defn place-all-pieces
  ; Places all 5 default pieces on the given board
  ([board] (place-all-pieces board pieces
                             #(nil? (:piece (get-square % %2 %3)))))
  ; Places all the given pieces on the given board according
  ; to a given validator.
  ([board mypieces validator] 
   (reduce
     #(randomly-place-piece % %2 validator)
     board mypieces)))

; Below are some methods for printing to console.
; Gets a string given a square.
(defn get-square-str [{piece :piece, state :state} pieces is-friendly]
  (case state
    :struck (if (nil? piece) "."
              (if (zero? (get pieces piece)) "#"
                "*"))
    :unstruck (if (and is-friendly (not (nil? piece))) "O" " ")))

; General purpose helper fn.
; Concatenate all the things then apply str. Not the same as C 'strcat'
(defn strcat [& things] (apply str (apply concat things)))

; A sequence of strs (lines) representing a board
; each str has the same length, making this a 13x13 char grid
(defn get-board-strs [board pieces is-friendly]
  (let [top-bottom-border (strcat " +" (repeat board-size "-") "+")]
    (concat
      [(strcat "  " (range 10) " ")]
      [top-bottom-border]
      (map (fn [char-num row]
             (strcat [(char char-num)] "|"
                     (map #(get-square-str %
                                           pieces
                                           is-friendly)
                          row) "|"))
           (range (int \A) (int \K)) board)
      [top-bottom-border])))

; good for debugging
(defn print-board [board]
  (dorun (map println (get-board-strs board pieces true))))
