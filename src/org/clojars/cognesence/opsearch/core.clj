(ns ^{:doc "A simple, partially optimised implementation of a breadth-first search mechanism for applying simple STRIPS-style operators."
     :author "Simon Lynch"}
 org.clojars.cognesence.opsearch.core)
 
(require '[clojure.set :refer :all])
(require '[clojure.pprint :refer :all])
(require '[org.clojars.cognesence.core :refer :all])

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


;======================
; testing
;======================

;(def ops
;  '{pickup {:pre ((agent ?agent)
;                   (manipulable ?obj)
;                   (at ?agent ?place)
;                   (on ?obj   ?place)
;                   (holds ?agent nil)
;                   )
;            :add ((holds ?agent ?obj))
;            :del ((on ?obj   ?place)
;                   (holds ?agent nil))
;            :txt (pickup ?obj from ?place)
;            :cmd [grasp ?obj]
;            }
;    drop    {:pre ((at ?agent ?place)
;                    (holds ?agent ?obj)
;                    (:guard (? obj))
;                    )
;             :add ((holds ?agent nil)
;                    (on ?obj   ?place))
;             :del ((holds ?agent ?obj))
;             :txt (drop ?obj at ?place)
;             :cmd [drop ?obj]
;             }
;    move    {:pre ((agent ?agent)
;                    (at ?agent ?p1)
;                    (connects ?p1 ?p2)
;                    )
;             :add ((at ?agent ?p2))
;             :del ((at ?agent ?p1))
;             :txt (move ?p1 to ?p2)
;             :cmd [move ?p2]
;             }
;    })
;
;
;(def state1
;  '#{(at R table)
;     (on book table)
;     (on spud table)
;     (holds R nil)
;     (connects table bench)
;     (manipulable book)
;     (manipulable spud)
;     (agent R)
;     })
;
;
;user=> (ops-search state1 '((on book bench)) ops)
;{:state #{(agent R) (manipulable book) (on spud table) (on book bench) (holds R nil)
;          (at R bench) (manipulable spud) (connects table bench)},
; :path (#{(agent R) (manipulable book) (on spud table) (holds R nil) (manipulable spud)
;          (connects table bench) (on book table) (at R table)}
;        #{(agent R) (holds R book) (manipulable book) (on spud table) (manipulable spud)
;          (connects table bench) (at R table)}
;        #{(agent R) (holds R book) (manipulable book) (on spud table) (at R bench)
;          (manipulable spud) (connects table bench)}),
; :cmds ([grasp book] [move bench] [drop book]),
; :txt ((pickup book from table) (move table to bench) (drop book at bench))}
;
;
;(def world
;  '#{(connects table bench)
;     (manipulable book)
;     (manipulable spud)
;     (agent R)
;     })
;
;(def state2
;  '#{(at R table)
;     (on book table)
;     (on spud table)
;     (holds R nil)
;     })
;
;
;user=> (ops-search state2 '((on book bench)) ops :world world)
;{:state #{(on spud table) (on book bench) (holds R nil) (at R bench)},
; :path (#{(on spud table) (holds R nil) (on book table) (at R table)}
;        #{(holds R book) (on spud table) (at R table)}
;        #{(holds R book) (on spud table) (at R bench)}),
; :cmds ([grasp book] [move bench] [drop book]),
; :txt ((pickup book from table) (move table to bench) (drop book at bench))}
;
;
;(def world2
;  '#{(connects table bench)
;     (connects bench table)
;     (connects bench sink)
;     (connects sink bench)
;     (manipulable book)
;     (manipulable spud)
;     (agent R)
;     })
;
;(def state3
;  '#{(at R table)
;     (on book table)
;     (on spud table)
;     (holds R nil)
;     })
;
;user=> (ops-search state3 '((on book bench)(on spud sink)) ops :world world2)
;{:state #{(at R sink) (on book bench) (holds R nil) (on spud sink)},
; :path (#{(on spud table) (holds R nil) (on book table) (at R table)}
;        #{(holds R book) (on spud table) (at R table)}
;        #{(holds R book) (on spud table) (at R bench)}
;        #{(on spud table) (on book bench) (holds R nil) (at R bench)}
;        #{(on spud table) (on book bench) (holds R nil) (at R table)}
;        #{(on book bench) (holds R spud) (at R table)}
;        #{(on book bench) (at R bench) (holds R spud)}
;        #{(at R sink) (on book bench) (holds R spud)}),
; :cmds ([grasp book] [move bench] [drop book]
;        [move table] [grasp spud] [move bench] [move sink] [drop spud]),
; :txt ((pickup book from table) (move table to bench) (drop book at bench)
;       (move bench to table) (pickup spud from table) (move table to bench)
;       (move bench to sink) (drop spud at sink))}
