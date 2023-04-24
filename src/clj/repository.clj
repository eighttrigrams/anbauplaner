(ns repository
  (:require [clojure.spec.alpha :as s]
            [xtdb.api :as xt]
            repository.model))

(defonce node (xt/start-node {}))

(defn dbg [%] (do (prn %) %))

;; from https://stackoverflow.com/a/43722784
(defn map->nsmap
  [m n]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str n) (name k))
                              k)]
                 (assoc acc new-kw v)))
             {} m))

(defn- convert-to-vec-of-vecs [symb args]
  (mapv #(do [symb % (% args)]) (keys args)))

(defn- find-existing'
  [symb m]
  (xt/q (xt/db node)
        {:find  [symb]
         :where (convert-to-vec-of-vecs symb m)}))

(defn- retain [ns' m]
  (into {} 
        (filter 
         (fn [[k _v]]
           (= (name ns') (namespace k))) m)))

(defn- find-existing 
  "Tries to find a record which has exactly the values
   as specified by the map `m`, but only for the keywords
   namespaced given by `ns'`"
  [ns' m]
  (find-existing' (symbol ns') (retain ns' m)))

(defn- put-data
  [data ns']
  (let [data (map->nsmap data (symbol ns'))
        spec (keyword (name ns') "spec")]
    (when-not (s/valid? spec data)
      (throw (ex-info "not valid" (s/explain-data spec data))))
    (let [existing-instance (find-existing ns' data)]
      (if (seq existing-instance)
        (ffirst existing-instance)
        (let [uuid (.toString (java.util.UUID/randomUUID))
              data (assoc data :object/type ns')]
          (xt/submit-tx node
                        [[::xt/put (merge {:xt/id uuid}
                                          data)]])
          (xt/sync node)
          uuid)))))

(defn get-data-by-id [id]
  (xt/entity (xt/db node) id))

(comment
  (search/find-all-items)

  ;; demo use case
  
  (do
    (require '[repository.search :as search])
    (def node (xt/start-node {}))
    (def seed-instance-1 {:name "Atlanta"
                          :type "Porree"
                          :stability ""
                          :manufacturer "Dürr"})
    (def seed-instance-1-id (put-data seed-instance-1 :seed-instance))
    (def group-of-plants-1 {:relation/seed-instance-id seed-instance-1-id
                            :seeding-date "2023-05-05"
                            :amount 5})
    (def group-of-plants-1-id (put-data group-of-plants-1 :group-of-plants))
    (def bed-area-1 {:name "Beet1"
                     :x-begin 0 
                     :x-end 1})
    (def bed-area-1-id (put-data bed-area-1 :bed-area))
    (def plan-item-1 {:relation/group-of-plants-id group-of-plants-1-id
                      :bed-area-id bed-area-1-id
                      :planned-seeding-date "2023-01-02"
                      :planned-planting-date "2023-05-08"
                      :planned-harvesting-date "2023-09-18"
                      :succession-number 1}) 
    (def plan-item-1-id (put-data plan-item-1 :plan-item))
    (get-data-by-id plan-item-1-id)
    (prn (first (search/find-all-plan-items node))))
    ;; => 
    ;; {:group-of-plants-id      "afd77f00-01e6-4c9d-aefa-51b18f7faa77"
    ;;  :type                    :plan-item
    ;;  :planned-planting-date   "2023-05-08"
    ;;  :planned-seeding-date    "2023-01-02"
    ;;  :bed-area                {:name    "Beet1"
                              ;;  :x-begin 0
                              ;;  :x-end   1
                              ;;  :type    :bed-area
                              ;;  :id      "0554e698-1192-46ab-9fee-8d5f28f003b1"}
    ;;  :id                      "2936b058-e9e6-4824-9c0c-5349226206c9"
    ;;  :succession-number       1
    ;;  :planned-harvesting-date "2023-09-18"}
  
  ;; demo use case end
)
