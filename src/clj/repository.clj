(ns repository ;; data-driven logic
  (:require [mount.core :as mount]))

(mount/defstate repository
  :start (do
           (tap> [:resources :up 2])
           [{:id   1
             :name "one"}
            {:id   2
             :name "two"}
            {:id        3
             :name      "three"
             :protected true}])
  :stop (do 
          (tap> [:resources :down])
          nil))

;; TODO implement
(defn list-resources []
  (prn "list-resources")
  {:a :b})
