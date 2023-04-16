(ns database
  (:require [clojure.spec.alpha :as s]
            [xtdb.api :as xt]))

(defonce node (xt/start-node {}))

(defn- put-data
  [data spec]
  (when-not (s/valid? spec (dissoc data :object/type))
    (throw (ex-info "not valid" (s/explain-data spec (dissoc data :object/type)))))
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
      (put-data (assoc data :object/type :seed-instance) ::seed-instance))))

(defn get-data-by-id [id]
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
  (put-data (assoc data :object/type :group-of-plants) ::group-of-plants))

(s/def ::name string?)
(s/def ::x-begin int?)
(s/def ::x-end int?)

(s/def ::bed-area (s/keys :req-un [::name ::x-begin ::x-end]))

(defn put-bed-area
  [data]
  (put-data (assoc data :object/type :bed-area) ::bed-area))

(s/def ::group-of-plants-id string?)
(s/def ::bed-area-id string?)
(s/def ::planned-seeding-date string?) ;; TODO convert to dates
(s/def ::planned-planting-date string?)
(s/def ::planned-harvesting-date string?)
(s/def ::succession-number int?) 

;; per season
(s/def ::plan-item (s/keys :req-un [::group-of-plants-id
                                    ::bed-area-id
                                    ::planned-seeding-date
                                    ::planned-planting-date
                                    ::planned-harvesting-date
                                    ;; can possibly be calculated based upon planting date
                                    ::succession-number]))
(defn put-plan-item
  [data]
  (put-data (assoc data :object/type :plan-item) ::plan-item))

(defn find-all-plan-items
  []
  (map
   
   (fn [[id
         planned-seeding-date
         bed-area-id
         name]]
     {:planned-seeding-date planned-seeding-date
      :id id
      :bed-area/id bed-area-id
      :bed-area/name name})
   
   (xt/q (xt/db node) '{:find  [?e planned-seeding-date ?eb name]
                        :where [[?e :object/type :plan-item]
                                [?e :planned-seeding-date planned-seeding-date]
                                [?e :bed-area-id bed-area-id]

                                [?eb :xt/id bed-area-id]
                                [?eb :name name]]})))

(comment
  
  (find-all-plan-items)

  ;; demo use case

  (do
    (def node (xt/start-node {}))
    (def seed-instance-1 {:name "Atlanta"
                          :type "Porree"
                          :stability ""
                          :manufacturer "Dürr"})
    (def seed-instance-1-id (put-seed-instance seed-instance-1))
    (def group-of-plants-1 {:seed-instance-id seed-instance-1-id
                            :seeding-date "2023-05-05"
                            :amount 5})
    (def group-of-plants-1-id (put-group-of-plants group-of-plants-1))
    (def bed-area-1 {:name "Beet1" :x-begin 0 :x-end 1})
    (def bed-area-1-id (put-bed-area bed-area-1))
    (def plan-item-1 {:group-of-plants-id group-of-plants-1-id
                      :bed-area-id bed-area-1-id
                      :planned-seeding-date "2023-01-02"
                      :planned-planting-date "2023-05-08"
                      :planned-harvesting-date "2023-09-18"
                      :succession-number 1}) 
    (def plan-item-1-id (put-plan-item plan-item-1))
    (get-data-by-id plan-item-1-id))

  ;; demo use case end

  (get-data-by-id seed-instance-id)

  )
