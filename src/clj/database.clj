(ns database
  (:require [clojure.spec.alpha :as s]
            [xtdb.api :as xt]))

(defonce node (xt/start-node {}))

(defn- put-data
  [data spec]
  (when-not (s/valid? spec data)
    (throw (ex-info "not valid" (s/explain-data spec data))))
  (let [uuid (.toString (java.util.UUID/randomUUID))]
    (xt/submit-tx node
                  [[::xt/put (merge {:xt/id uuid}
                                    data)]])
    (xt/sync node)
    uuid))

(s/def ::name string?)
(s/def ::type string?)
(s/def ::stability string?)
(s/def ::manufacturer string?)

(s/def ::seed-instance (s/keys :req-un [::name ::type ::stability ::manufacturer]))

(defn find-existing-seed-instance
  [{:keys [name type stability manufacturer]}]
  (xt/q (xt/db node)
        '{:find  [seed-instance]
          :where [[seed-instance :name name]
                  [seed-instance :type type]
                  [seed-instance :stability stability]
                  [seed-instance :manufacturer manufacturer]]
          :in [name type stability manufacturer]}
        name type stability manufacturer))

(defn put-seed-instance 
  "This entity describes a single specific instance
   of a seed, as purchased from a manufacturer"
  [data] 
  (let [existing-seed-instance (find-existing-seed-instance data)]
    (if (seq existing-seed-instance)
      (ffirst existing-seed-instance)
      (put-data data ::seed-instance))))

(defn get-seed-instance-by-id [id]
  (xt/entity (xt/db node) id))

(s/def ::seed-instance-id string?)
(s/def ::amount int?)
(s/def ::seeding-date string?) ;; TODO convert to date

(s/def ::group-of-plants (s/keys :req-un 
                                 [::seed-instance-id
                                  ::seeding-date
                                  ::amount]))

(defn put-group-of-plants
  "A group of plants grown from a specific seed instance
   at a given seeding date"
  [data]
  (put-data data ::group-of-plants))

(defn get-group-of-plants-by-id [id]
  (xt/entity (xt/db node) id))

(comment
  (def seed-instance-1 {:name "Sweet million"
                        :type "Tomate"
                        :stability "F1"
                        :manufacturer "abc"})
  (def seed-instance-id (put-seed-instance seed-instance-1))
  (def group-of-plants-1 {:seed-instance-id seed-instance-id
                          :seeding-date "2023-05-05"
                          :amount 5})
  (def group-of-plants-1-id (put-group-of-plants group-of-plants-1)))
