(ns database
  (:require [xtdb.api :as xt]))

(defonce node (xt/start-node {}))

(defn put-seed-variety [data]
  (let [uuid (.toString (java.util.UUID/randomUUID))]
    (xt/submit-tx node 
                  [[::xt/put (merge {:xt/id uuid}
                                    data)]])
    (xt/sync node)
    uuid))