(ns ^{:doc "A simple, partially optimised implementation of a breadth-first search mechanism for applying simple STRIPS-style operators."
     :author "Simon Lynch"}
 org.clojars.cognesence.opsearch.core)
 
(require '[clojure.set :refer :all])
(require '[clojure.pprint :refer :all])
(require '[org.clojars.cognesence.matcher.core :refer :all])

(declare finalise-results update-state-map apply-op apply-all)


(defn ops-search
  [start goal ops & {:keys [world debug]
                     :or {debug  false
                          world  #{}}}]
  ; using sets for state tuples...
  (let [start {:state (set start) :path () :cmds () :txt ()}
        world (set world)
        goal? (fn [state] (mfind* [goal (into world (:state state))] state))
        ]
    (or (goal? start)
      (finalise-results
        (loop [waiting (list start)
               visited #{}
               ]
          (when debug (pprint (list 'waiting= waiting 'visited= visited)))
          (if (empty? waiting) nil
            (let [ [next & waiting] waiting
                   {:keys [state path cmds]} next
                   visited? (partial contains? visited)
                   ]
              (when debug (pprint (list 'next-state= state)))
              (if (visited? state)
                (recur waiting visited)
                (let [succs (remove visited? (apply-all ops state world))
                      g     (some goal? succs)
                      ]
                  (if g (update-state-map next g)
                    (recur
                      (concat waiting (map #(update-state-map next %) succs))
                      (conj visited state) ))
                  )))))
        ))))


(defn update-state-map
  [old successor]
  {:state (:state successor)
   :path  (cons (:state old) (:path old))
   :cmds  (cons (:cmd successor) (:cmds old))
   :txt   (cons (:txt successor) (:txt old))
   })


(defn finalise-results [state-map]
  "reverses relevant parts of results"
  (if-not (map? state-map)
    ;; leave it as it is
    state-map
    ;; else
    {:state (:state state-map)
     :path  (reverse (:path state-map))
     :cmds  (reverse (:cmds state-map))
     :txt   (reverse (:txt state-map))
     }
    ))


(defn apply-op [op state world]
  (mfor* [(:pre op) (seq (into world state))]
    {:state (union (set (mout (:add op)))
              (difference state (set (mout (:del op)))))
     :cmd   (mout (:cmd op))
     :txt   (mout (:txt op))
     }
    ))


(defn apply-all [ops state world]
  (reduce concat
    (map (fn [x] (apply-op (x ops) state world))
      (keys ops)
      )))
