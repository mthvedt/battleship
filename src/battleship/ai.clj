(ns battleship.ai
  (:use battleship.core))

; A Monte Carlo solver for Battleship.

; An infinite sequence of random boards
(def infinite-boards
  (repeatedly #(place-all-pieces (newboard))))

; 1 if we might want to shoot that square, 0 otherwise
(defn is-target [square]
  (if (and (not (nil? (:piece square)))
       (= :unstruck (:state square)))
    1 0))

; Gets the (not normalized) distribution of targetable squares
; in a (finite) boardseq.
(defn get-distribution [boardseq]
  (reduce (fn [running-count board]
            (map (fn [running-count-row row]
                   (map + running-count-row
                        (map #(if (is-target %) 1 0) row)))
                 running-count board))
          (repeat (repeat 0.0)) boardseq))

#_(defn get-normalized-probability-distribution [boardseq]
  (let [magnitude (apply #(apply + %) boardseq)]
    (map #(map / (float %) magnitude)
         (get-distribution boardseq))))
