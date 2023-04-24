(ns repository.search
  (:require [xtdb.api :as xt]))

(defn- un-namespace-keys
  [m]
  (into {}
        (map (fn [[k v]]
               [(keyword (name k)) v])
             m)))

(defn find-all-plan-items
  [node]
  (map
   (fn [[plan-item bed-area]]
     (-> plan-item
         un-namespace-keys
         (assoc :bed-area (un-namespace-keys bed-area))
         (dissoc :bed-area-id)))

   (xt/q (xt/db node) '{:find  [(pull ?e [*]) (pull ?eb [*])]
                        :where [[?e :object/type :plan-item]
                                [?e :plan-item/bed-area-id bed-area-id]

                                [?eb :xt/id bed-area-id]
                                [?eb :bed-area/name name]]})))

(defn find-all-items [node]
  (xt/q (xt/db node)
        '{:find   [(pull ?e [*])]
          :where [[?e :xt/id _]]}))
