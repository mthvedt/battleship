(ns battleship.ai
  (:use battleship.core))

; A Monte Carlo solver for Battleship.

; An infinite sequence of random boards.
;
; Why is this a defn, not a def?
; If it were a def, a reference to the beginning of infinite-boards
; would always exist. Since Clojure caches its lazy seqs,
; this would cause every single generated board to be immune to
; the garbage collector. In practice you get OutOfMemoryErrors.
;
; By making this an fn, we can generate infinite-board seqs
; and immediately throw away the reference to the head of the seq,
; allowing garbage collection to clean up already-consumed boards.
(defn infinite-boards []
  (repeatedly #(place-all-pieces (newboard))))

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
; The second is that in performance-critical areas, the JVM/JIT seems
; to play much better with eager seqs than lazy ones.
(defn get-distribution [boardseq]
  (reduce (fn [running-count board]
            ; The doall here prevents an esoteric stack overflow error
            ; wherein the lazy reduce + lazy maps creates
            ; a tower of calls to map.
            ; https://groups.google.com/group/clojure/browse_thread/thread/6f5064532546a852
            (doall (map (fn [running-count-row row]
                   (doall (map + running-count-row
                        (map is-target row))))
                 running-count board)))
          (repeat (repeat 0.0)) boardseq))

#_(defn get-normalized-probability-distribution [boardseq]
  (let [magnitude (apply #(apply + %) boardseq)]
    (map #(map / (float %) magnitude)
         (get-distribution boardseq))))


