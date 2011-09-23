(ns battleship.ai
  (:use battleship.core))

; A Monte Carlo based AI for Battleship.

; Helper for infinite-seq. Given a square on a board, tells what the AI
; is "allowed to know" about it. A square may have one of the given pieces,
; not have a ship (blocked), or be unknown.
(defn get-knowledge [square mypieces]
  (if (= :struck (:state square))
    (if (nil? (get mypieces (:piece square)))
      :blocked ; There's a sunk ship or no ship here
      :has-ship) ; We know there's an unsunk ship here
    :unknown)) ; We don't know what's here

(def all-coordinates (for [x (range 10) y (range 10)] [x y]))

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
  (let [rval (loop [coordinate (first all-coordinates)
                    coordinates (rest all-coordinates)]
               (if (nil? coordinate)
                 true ; loop over
                 (let [[x y] coordinate]
                   (if (= :has-ship (get kmap coordinate))
                     (if (nil? (:piece (get-square candidate-board x y)))
                       false ; square should have a ship, but it didn't
                       (recur (first coordinates) (rest coordinates)))
                     (recur (first coordinates) (rest coordinates))))))]
    rval))

; Given a known-board, containing struck and unstruck squares,
; and pieces, containing unsunk ships;
; generates an infinite sequence of possible boards
; that match these criteria.
(defn infinite-boards [known-board mypieces]
  (let [kmap (knowledge-map known-board mypieces)]
    (filter #(struck-square-checker % kmap)
            (repeatedly #(place-all-pieces newboard mypieces
                                           (blocked-square-validator kmap))))))

; 1 if we might want to shoot that square, 0 otherwise
(defn is-target [square]
  (if (and (not (nil? (:piece square)))
           (= :unstruck (:state square)))
    1 0))

; Gets the (not normalized) distribution of targetable squares
; in a (finite) boardseq.
;
; There are a number of doalls here. Two reasons for this.
; The first is that reducing a lazy seq can produce a tower of calls.
; While most functional languages will handle the case where these are
; tailcalls, Clojure does not.
; The second is that this is the most performance-critical fn.
; in performance-critical areas, the JVM/JIT seems
; to play much better with eager seqs than lazy ones.
(defn get-distribution [boardseq]
  (reduce (fn [running-count board]
            (doall (map (fn [running-count-row row]
                          (doall (map + running-count-row
                                      (map is-target row))))
                        running-count board)))
          (repeat (repeat 0)) boardseq))

;  Gets the most valuable target to fire upon. Returns the coordinates.
(defn get-target-from-dist [theboard dist]
  (let [coordinate-value-tuples ; tuples of [value, x, y]
        (mapcat (fn [row y]
                  (map #(vector % %2 y) row (range)))
                dist (range))
        filtered-cvt (filter (fn [[_ x y]] ; remove all struck squares
                               (= :unstruck (:state (get-square theboard x y))))
                             coordinate-value-tuples)]
    (rest (apply max-key first filtered-cvt)))) ; return (x, y)

(defn get-target [theboard theseq search-size]
  (let [dist (get-distribution (take search-size theseq))]
    (get-target-from-dist theboard dist)))
