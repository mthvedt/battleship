(ns battleship.ai
  (:use battleship.core))

; A Monte Carlo based AI for Battleship.

; Given a square on a board, tells what the AI knows about it.
; It may know there's a live ship there, no ship there,
; or the state may be wholly unknown.
(defn get-knowledge [square mypieces]
  (if (= :struck (:state square))
    (if (nil? (get mypieces (:piece square)))
      :blocked ; There's a sunk ship or no ship here
      :has-ship) ; We know there's an unsunk ship here
    :unknown)) ; We don't know what's here (haven't shot here yet)

; [x, y] running thru [10, 10]
(def all-coordinates (for [x (range 10) y (range 10)] [x y]))

; A map [x, y] -> what is known about it
(defn knowledge-map [known-board mypieces]
  (zipmap all-coordinates
          (for [[x y] all-coordinates]
            (get-knowledge (get-square known-board x y) mypieces))))

; Square validator (see 'place-all-pieces) that rejects any square
; which is known to be blocked.
(defn blocked-square-validator [kmap]
  (fn [_ x y]
    (not (= :blocked (get kmap [x y])))))

; Makes sure that, for some board, all squares known to have a ship
; do in fact have a ship.
(defn struck-square-checker [candidate-board kmap]
  (empty? (filter (fn [[x y]]
                    (and (= :has-ship (get kmap [x y]))
                         (nil? (:piece (get-square candidate-board x y)))))
                  ; The filter will find a square that is 'known'
                  ; to have a ship but doesn't have one. Any such square
                  ; causes the board to be rejected.
                  all-coordinates)))

; Given a known-board, containing struck and unstruck squares,
; and pieces, containing unsunk ships;
; generates an infinite sequence of possible boards
; that match these criteria.
(defn infinite-boards [known-board mypieces]
  (let [kmap (knowledge-map known-board mypieces)]
    (filter
      #(and (not (nil? %)) (struck-square-checker % kmap))
      (repeatedly #(try-place-all-pieces newboard mypieces
                                         (blocked-square-validator kmap))))))

; 1 if we might want to shoot that square, 0 otherwise
(defn is-target [square]
  (if (and (not (nil? (:piece square)))
           (= :unstruck (:state square)))
    1 0))

; Gets the (not normalized) distribution of targetable squares
; in a (finite) boardseq.
;
; Doalls are used here to prevent lazy reduction.
; In some cases, reducing a lazy seq with a lazy fn
; can produce a large tower of nested fn calls and cause a stack overflow.
; Most LISP-like languages take care of this
; with tail-call optimization; the JVM can't.
(defn get-distribution [boardseq]
  (reduce (fn [running-count board]
            (doall (map (fn [running-count-row row]
                          (doall (map + running-count-row
                                      (map is-target row))))
                        running-count board)))
          (repeat (repeat 0)) boardseq))

;  Guesses the most valuable target to fire upon. Returns the coordinates.
(defn get-target-from-dist [theboard dist]
  (let [coordinate-value-tuples ; tuples of [value, x, y]
        (mapcat (fn [row y]
                  (map #(vector % %2 y) row (range)))
                dist (range))
        filtered-cvt (filter (fn [[_ x y]] ; remove all struck squares
                               (= :unstruck
                                  (:state (get-square theboard x y))))
                             coordinate-value-tuples)]
    (rest (apply max-key first filtered-cvt)))) ; return (x, y)

; The punch line. Guesses the most valuable target from a sequence of boards. 
(defn get-target [theboard theseq search-size]
  (let [dist (get-distribution (take search-size theseq))]
    (get-target-from-dist theboard dist)))
