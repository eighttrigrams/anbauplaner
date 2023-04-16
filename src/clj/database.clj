(ns database
  (:require [clojure.spec.alpha :as s]
            [xtdb.api :as xt]))

(defonce node (xt/start-node {}))

(s/def ::name string?)
(s/def ::type string?)
(s/def ::stability string?)
(s/def ::manufacturer string?)

(s/def ::data (s/keys :req-un [::name ::type ::stability ::manufacturer]))

(defn put-seed-instance 
  "This entity describes a single specific instance
   of a seed, as purchased from a manufacturer"
  [data] 
  (when-not (s/valid? ::data data)
    (throw (Exception. "not valid"))) 
  (let [uuid (.toString (java.util.UUID/randomUUID))]
    (xt/submit-tx node 
                  [[::xt/put (merge {:xt/id uuid}
                                    data)]])
    (xt/sync node)
    uuid))

(defn get-seed-instance-by-id [id]
  (xt/entity (xt/db node) id))

