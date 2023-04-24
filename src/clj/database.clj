(ns database
  (:require [clojure.spec.alpha :as s]
            [xtdb.api :as xt]
            model))

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
  [data spec ns']
  (let [data (map->nsmap data (symbol ns'))]
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

(defn- un-namespace-keys
  [m]
  (into {}
        (map (fn [[k v]]
               [(keyword (name k)) v])
             m)))

(defn find-all-plan-items
  []
  (map
   
   (fn [[plan-item bed-area]] 
     (-> plan-item 
         un-namespace-keys
         (assoc :bed-area (un-namespace-keys bed-area))
         (dissoc :bed-area-id)))
   
   (xt/q (xt/db node) '{:find  [(pull ?e [*]) (pull ?eb [*])]
                        :where [[?e :object/type :plan-item]
                                [?e :plan-item/planned-seeding-date planned-seeding-date]
                                [?e :plan-item/bed-area-id bed-area-id]

                                [?eb :xt/id bed-area-id]
                                [?eb :bed-area/name name]]})))

(defn find-all-items []
  (xt/q (xt/db node)
        '{:find   [(pull ?e [*])]
           :where [[?e :xt/id _]]}))

(comment
  (find-all-items)

  ;; demo use case
  
  (do
    (def node (xt/start-node {}))
    (def seed-instance-1 {:name "Atlanta"
                          :type "Porree"
                          :stability ""
                          :manufacturer "Dürr"})
    (def seed-instance-1-id (put-data seed-instance-1 
                                      :seed-instance/spec 
                                      :seed-instance))
    (def group-of-plants-1 {:relation/seed-instance-id seed-instance-1-id
                            :seeding-date "2023-05-05"
                            :amount 5})
    (def group-of-plants-1-id (put-data group-of-plants-1
                                        :group-of-plants/spec
                                        :group-of-plants))
    (def bed-area-1 {:name "Beet1"
                     :x-begin 0 
                     :x-end 1})
    (def bed-area-1-id (put-data bed-area-1
                                 :bed-area/spec
                                 :bed-area))
    (def plan-item-1 {:relation/group-of-plants-id group-of-plants-1-id
                      :bed-area-id bed-area-1-id
                      :planned-seeding-date "2023-01-02"
                      :planned-planting-date "2023-05-08"
                      :planned-harvesting-date "2023-09-18"
                      :succession-number 1}) 
    (def plan-item-1-id (put-data plan-item-1
                                  :plan-item/spec
                                  :plan-item))
    (get-data-by-id plan-item-1-id)
    (prn (first (find-all-plan-items))))
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
