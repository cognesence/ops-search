# Operator Search

Provides a simple, partially optimised implementation of a breadth-first search mechanism for applying simple
STRIPS-style operators. For a more efficient engine, check out [the planner](https://github.com/cognesence/planner).

## Contents

+ [Installation](#installation)
+ [Overview](#overview)
+ [Operators](#operators)
+ [Examples](#examples)
    - [Without a World](#without-a-world)
    - [Using a World](#using-a-world)
    - [Using Compound Goals](#using-compound-goals)

## Installation

This library is hosted on clojars. Get it by adding `org.clojars.cognesence/ops-search` to your dependencies in your
Leiningen `project.clj` file.

```
(defproject com.example/myproject "1.0.0"
  :description "My Leiningen project!"
  :url "http://example.com/projects/myproject"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojars.cognesence/ops-search "1.0.1"]])
```

**Note:** This library used to be called `opsearch` but isn't anymore. The `opsearch` package on Clojars is deprecated
and not maintained.

## Overview

The `ops-search` method takes the following arguments:

+ `start` - a start state
+ `goal` - a minimally specified goal state (see below)
+ `operators` - a collection of operators
+ `:world` - a definition of unchanging world states

A map is returned showing:

+ the goal state it reached
+ the path it took
+ any commands to send to another subsystem
+ a textual description of the path

**Note:** It is possible to use ops-search without the pattern matcher but its use is then highly restricted. For this
guide we assume the pattern matcher will be used.

Goals are minimally specified so any state which is a superset of the goal is deemed a goal state.

## Operators

Operators for this search are specified in a map which should associate the operator name with its definition. Operator
definitions describe preconditions and effects. Effects are specified in terms of deletions and additions. The
operators used in the examples below are based on a tuple representation.

For example, a move-agent operator could be written as:

```clojure
move {:pre ((agent ?agent)
            (at ?agent ?p1)
            (connects ?p1 ?p2))
      :add ((at ?agent ?p2))
      :del ((at ?agent ?p1))}
```

Given a state description like:

```clojure
'#{(at Ralf table)
   (connects table bench)
   (agent Ralf)}
```

The move operator could produce a successor state:

```clojure
'#{(at Ralf bench)
   (connects table bench)
   (agent Ralf)}
```

By deleting `(at Ralf table)` and adding `(at Ralf bench)`.

Operators may (optionally) also specify:

+ `:txt` - a textual description of the operator application to aid readibility
+ `:cmd` - an encoded command for the operator, typically for the benefit of some other subsystem.

A more complete (but still slightly toy) set of operators could be:

```clojure
(def ops
  '{pickup {:pre ( (agent ?agent)
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
    drop    {:pre ( (at ?agent ?place)
                    (holds ?agent ?obj)
                    (:guard (? obj))
                    )
             :add ( (holds ?agent nil)
                    (on ?obj   ?place))
             :del ((holds ?agent ?obj))
             :txt (drop ?obj at ?place)
             :cmd [drop ?obj]
             }
    move    {:pre ( (agent ?agent)
                    (at ?agent ?p1)
                    (connects ?p1 ?p2)
                    )
             :add ((at ?agent ?p2))
             :del ((at ?agent ?p1))
             :txt (move ?p1 to ?p2)
             :cmd [move ?p2]
             }
    })
```

## Examples

### Without a World
 
All following examples use the pickup, drop and move operators defined above:
 
```clojure
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
```
 
Note (as stated above) goals are minimally specified so any state which is a superset of the goal is deemed a goal 
state.
 
```clojure
; user=> (ops-search state1 '((on book bench)) ops)
{:state #{(agent R) (manipulable book) (on spud table)
          (on book bench) (holds R nil) (at R bench)
          (manipulable spud) (connects table bench)},
:path (#{(agent R) (manipulable book) (on spud table)
         (holds R nil) (manipulable spud) (connects table bench)
         (on book table) (at R table)}
        #{(agent R) (holds R book) (manipulable book)
          (on spud table) (manipulable spud)
          (connects table bench) (at R table)}
        #{(agent R) (holds R book) (manipulable book)
          (on spud table) (at R bench)
          (manipulable spud) (connects table bench)}),
:cmds ([grasp book] [move bench] [drop book]),
:txt ((pickup book from table) (move table to bench) (drop book at bench))}
```

### Using a World
 
`:world` definitions contain unchanging state relations. Separating static/world relations from those that may change 
improves the efficiency of the search.
 
```clojure
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
 
; user=> (ops-search state2 '((on book bench)) ops :world world)
{:state #{(on spud table) (on book bench) (holds R nil) (at R bench)},
:path (#{(on spud table) (holds R nil) (on book table) (at R table)}
        #{(holds R book) (on spud table) (at R table)}
        #{(holds R book) (on spud table) (at R bench)}),
:cmds ([grasp book] [move bench] [drop book]),
:txt ((pickup book from table) (move table to bench) (drop book at bench))}
```

### Using Compound Goals

```clojure
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
{:state #{(at R sink) (on book bench) (holds R nil) (on spud sink)},
 :path (#{(on spud table) (holds R nil) (on book table) (at R table)}
        #{(holds R book) (on spud table) (at R table)}
        #{(holds R book) (on spud table) (at R bench)}
        #{(on spud table) (on book bench) (holds R nil) (at R bench)}
        #{(on spud table) (on book bench) (holds R nil) (at R table)}
        #{(on book bench) (holds R spud) (at R table)}
        #{(on book bench) (at R bench) (holds R spud)}
        #{(at R sink) (on book bench) (holds R spud)}),
 :cmds ([grasp book] [move bench] [drop book]
        [move table] [grasp spud] [move bench] [move sink] [drop spud]),
 :txt ((pickup book from table) (move table to bench) (drop book at bench)
       (move bench to table) (pickup spud from table) (move table to bench)
       (move bench to sink) (drop spud at sink))}
```
  
Note that goals can be specified as matcher patterns so the following is also a valid call:
 
```clojure
(ops-search state3 '((on ?x bench)(on ?y sink)) ops :world world2)
```
 
## License

Copyright © 2017 Simon Lynch

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
