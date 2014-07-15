(ns instar.core
  (:require [instar.stack :as s]))

(def ^:private STAR *)

(defn ^:private split-at-exclusive [index path]
  (let [[a b] (split-at index path)]
    [(into [] a)
     (into [] (drop 1 b))]))

(defn expand-path-once [state path]
  (let [index-of-star (s/index-of path STAR)]
    (if (= index-of-star -1)
      [path]
      (let [[path-base path-rest] (split-at-exclusive index-of-star path)]
        (if (map? (get-in state path-base))
          (let [ks (keys (get-in state path-base))]
            (for [k ks]
              (into (into path-base [k]) path-rest)))
          [])))))

(defn expand-path [state path]
  (let [paths (s/stack [path])
        tmp (s/stack [])
        result (s/stack [])]
    (while (s/not-empty? paths)
      (let [path (s/pop! paths)]
        (if (= (s/index-of path STAR) -1)
          (s/push! result path)
          (s/push-all! paths (expand-path-once state path)))))
    (s/as-set result)))

(defn resolve-paths-for-transform [m args]
  (let [pairs (partition 2 args)
        result (s/stack [])]
    (doseq [[p f] pairs]
      (doseq [p (expand-path m p)]
        (s/push-all! result [p f])))
    (s/as-vector result)))

(defn transform-resolved-paths [m args]
  (let [pairs (partition 2 args)]
    (reduce
     (fn [state [path f]]
       (update-in state path f))
     m
     pairs
     )))

(defn transform [m & args]
  (let [x (resolve-paths-for-transform m args)]
    (transform-resolved-paths m x)))