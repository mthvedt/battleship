(ns battleship.core)
; Very basic battleship stuff goes here.

; a utility fn--repeatedly tries a form until it yields not nil
(defmacro retrying [& forms]
  `(first (remove nil? (repeatedly (fn [] ~@forms)))))

; A square can be empty or contain a ship.
; A square can be in three states: unstruck, struck, or sunk.
(defrecord Square [piece state])

; Boards and squares. A board is a 2-d sequence of sequences of squares.
(def board-size 10)
(def newboard
  (vec (repeat board-size (vec (repeat board-size (Square. nil :unstruck))))))

(defn get-square [board x y]
  (nth (nth board y) x))

(defn set-square [board x y square]
  (let [row (nth board y)]
    (assoc board y (assoc row x square))))

; the validator function should take in the original board, x, and y
; and return true if the validator will allow a piece to place there
(defn set-valid-square [board x y square validator]
  (if (nil? board)
    nil
    (if (validator board x y)
      (set-square board x y square)
      nil)))

; The pieces in the canonical US version of Battleship.
(def pieces
  [["carrier" 5]
   ["battleship" 4]
   ["cruiser" 3]
   ["submarine" 3]
   ["destroyer" 2]])

; A hash map of the above.
(def pieces-map (reduce conj {} pieces))

; helper fn for place-piece
(defn get-range [coord0 step?]
  (if step?
    (range coord0 Double/POSITIVE_INFINITY)
    (repeat coord0)))

; places a piece on the board, or nil if it can't be placed according
; to the given validator fn
(defn place-piece [board0 [piecename piecelen] x0 y0 is-horizontal validator]
  (let [xrange (get-range x0 is-horizontal)
        yrange (get-range y0 (not is-horizontal))
        placer (fn [board [x y]] (set-valid-square board
                                                   x y
                                                   (Square. piecename
                                                            :unstruck)
                                                   validator))]
    (reduce placer board0 (take piecelen (map vector xrange yrange)))))

(defn occupied-validator [board x y]
  (nil? (get (get-square board x y) :piece)))

; return a random [x, y] coordinate, or nil
(defn try-random-piece-coords [board piecelen validator]
  (let [is-horizontal (= (rand-int 2) 0)
        coord-a (rand-int board-size)
        ; make sure the piece doesn't overflow off the board
        coord-b (rand-int (- board-size piecelen))]
    (let [x (if is-horizontal coord-b coord-a)
          y (if is-horizontal coord-a coord-b)]
      (if (place-piece board ["dummy" piecelen] x y is-horizontal validator)
        [x y is-horizontal]
        nil))))

; try (possibly forever!) to place a piece
; works by making an infinite sequence of randomly-try-place-piece calls
; and pulling the first one that passes the validator.
;
; boards with overlapping pieces are destroyed outright (returning nil),
; instead of trying again. this prevents "restricted choice"
; or "monty hall" biasing of the solution space. the full math behind it
; is too much to fit here
(defn try-randomly-place-piece [board [piecename piecelen :as piece]
                                validator]
  (if (nil? board)
    nil
    (let [[x y is-horizontal] (retrying (try-random-piece-coords
                                         board piecelen validator))]
      (place-piece board piece x y is-horizontal occupied-validator))))

; Can return nil.
(defn try-place-all-pieces
  ; Places all 5 default pieces on the given board
  ([board] (try-place-all-pieces board pieces (fn [& _] true)))
  ; Places all the given pieces on the given board according
  ; to a given validator.
  ([board mypieces validator] 
   (reduce
     #(try-randomly-place-piece % %2 validator)
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
