(def ops
  '{pickup {:pre ((agent ?agent)
                   (manipulable ?obj)
                   (at ?agent ?place)
                   (on ?obj   ?place)
                   (holds ?agent nil)
                   )
            :add ((holds ?agent ?obj))
            :del ((on ?obj   ?place)
                   (holds ?agent nil))
            :txt (pickup ?obj from ?place)
            :cmd [grasp ?obj]
            }
    drop    {:pre ((at ?agent ?place)
                    (holds ?agent ?obj)
                    (:guard (? obj))
                    )
             :add ((holds ?agent nil)
                    (on ?obj   ?place))
             :del ((holds ?agent ?obj))
             :txt (drop ?obj at ?place)
             :cmd [drop ?obj]
             }
    move    {:pre ((agent ?agent)
                    (at ?agent ?p1)
                    (connects ?p1 ?p2)
                    )
             :add ((at ?agent ?p2))
             :del ((at ?agent ?p1))
             :txt (move ?p1 to ?p2)
             :cmd [move ?p2]
             }
    })

(def state1
  '#{(at R table)
     (on book table)
     (on spud table)
     (holds R nil)
     (connects table bench)
     (manipulable book)
     (manipulable spud)
     (agent R)
     })

; Move the book to the bench.
; user=>(ops-search state1 '((on book bench)) ops)
; {:state #{(agent R) (manipulable book) (on spud table) (on book bench) (holds R nil)
;           (at R bench) (manipulable spud) (connects table bench)},
;  :path (#{(agent R) (manipulable book) (on spud table) (holds R nil) (manipulable spud)
;           (connects table bench) (on book table) (at R table)}
;         #{(agent R) (holds R book) (manipulable book) (on spud table) (manipulable spud)
;           (connects table bench) (at R table)}
;         #{(agent R) (holds R book) (manipulable book) (on spud table) (at R bench)
;           (manipulable spud) (connects table bench)}),
;  :cmds ([grasp book] [move bench] [drop book]),
;  :txt ((pickup book from table) (move table to bench) (drop book at bench))}
     
(def world
  '#{(connects table bench)
     (manipulable book)
     (manipulable spud)
     (agent R)
     })

(def state2
  '#{(at R table)
     (on book table)
     (on spud table)
     (holds R nil)
     })

; Move the book to the bench, from a world and initial state.
; user=> (ops-search state2 '((on book bench)) ops :world world)
; {:state #{(on spud table) (on book bench) (holds R nil) (at R bench)},
;  :path (#{(on spud table) (holds R nil) (on book table) (at R table)}
;         #{(holds R book) (on spud table) (at R table)}
;         #{(holds R book) (on spud table) (at R bench)}),
;  :cmds ([grasp book] [move bench] [drop book]),
;  :txt ((pickup book from table) (move table to bench) (drop book at bench))}

(def world2
  '#{(connects table bench)
     (connects bench table)
     (connects bench sink)
     (connects sink bench)
     (manipulable book)
     (manipulable spud)
     (agent R)
     })

(def state3
  '#{(at R table)
     (on book table)
     (on spud table)
     (holds R nil)
     })

; user=> (ops-search state3 '((on book bench)(on spud sink)) ops :world world2)
; {:state #{(at R sink) (on book bench) (holds R nil) (on spud sink)},
;  :path (#{(on spud table) (holds R nil) (on book table) (at R table)}
;         #{(holds R book) (on spud table) (at R table)}
;         #{(holds R book) (on spud table) (at R bench)}
;         #{(on spud table) (on book bench) (holds R nil) (at R bench)}
;         #{(on spud table) (on book bench) (holds R nil) (at R table)}
;         #{(on book bench) (holds R spud) (at R table)}
;         #{(on book bench) (at R bench) (holds R spud)}
;         #{(at R sink) (on book bench) (holds R spud)}),
;  :cmds ([grasp book] [move bench] [drop book]
;         [move table] [grasp spud] [move bench] [move sink] [drop spud]),
;  :txt ((pickup book from table) (move table to bench) (drop book at bench)
;        (move bench to table) (pickup spud from table) (move table to bench)
;        (move bench to sink) (drop spud at sink))}
