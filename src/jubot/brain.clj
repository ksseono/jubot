(ns jubot.brain
  "Jubot brain manager."
  (:require
    [jubot.system       :refer [system]]
    [jubot.brain.memory :refer [map->MemoryBrain]]
    [jubot.brain.redis  :refer [map->RedisBrain]])
  (:refer-clojure :exclude [set get]))

(defn create-brain
  "Create the specified brain.

  Params
    :brain - a brain's name
  Return
    brain component
  "
  [{:keys [brain] :as config-option}]
  (case brain
    "redis" (map->RedisBrain config-option)
    (map->MemoryBrain config-option)))

(defn set
  "Store data to system brain.
  Before using this function, jubot.system should be started.

  Params
    k - key
    v - value
  "
  [k v]
  (some-> system :brain :set (as-> f (f k v))))

(defn get
  "Get data from system brain.
  Before using this function, jubot.system should be started.

  Params
    k - key
  Return
    stored data
  "
  [k]
  (some-> system :brain :get (as-> f (f k))))