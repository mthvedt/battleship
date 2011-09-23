(ns battleship.ai
  (:use battleship.core))

; A Monte Carlo based AI for Battleship.

; An infinite sequence of random boards.
;
; In Clojure inifinte sequences are declared with defn.
; If it were a def, a reference to the beginning of infinite-boards
; would always exist. Since Clojure caches its lazy seqs,
; this is a recipe for OutOfMemoryErrors.
;
; By making this an fn, we can generate infinite-board seqs
; and immediately throw away the reference to the head of the seq,
; allowing garbage collection to clean up already-consumed boards.
(defn infinite-boards []
  (repeatedly #(place-all-pieces (newboard))))

; Returns only those boards for which the given square is
; ocean or occupied by a piece, depending on the value of is-occupied
(defn filter-boards [boardseq x y is-occupied]
  (filter #(= is-occupied (not (nil? (:piece (get-square x y %)))))
          boardseq))

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
          (repeat (repeat 0.0)) boardseq))

#_(defn get-normalized-probability-distribution [boardseq]
  (let [magnitude (apply #(apply + %) boardseq)]
    (map #(map / (float %) magnitude)
         (get-distribution boardseq))))

;  Gets the most valuable target to fire upon. Returns the coordinates.
(defn get-target [dist]
  (let [coordinate-value-tuples
             (mapcat (fn [row y]
                                  (map #(vector % %2 y) row (range)))
                                dist (range))]
    (apply max-key first coordinate-value-tuples)))
