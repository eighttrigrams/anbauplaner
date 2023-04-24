(ns model
  (:require [clojure.spec.alpha :as s]))

(s/def :seed-instance/name string?)
(s/def :seed-instance/type string?)
(s/def :seed-instance/stability string?)
(s/def :seed-instance/manufacturer string?)

;; This entity describes a single specific instance
;; of a seed, as purchased from a manufacturer
(s/def :seed-instance/spec (s/keys :req [:seed-instance/name
                                         :seed-instance/type
                                         :seed-instance/stability
                                         :seed-instance/manufacturer]))

(s/def :relation/seed-instance-id string?)
(s/def :group-of-plants/amount int?)
(s/def :group-of-plants/seeding-date string?) ;; TODO convert to date

;; A group of plants grown from a specific seed instance
;; at a given seeding date
(s/def :group-of-plants/spec (s/keys :req
                                     [:relation/seed-instance-id
                                      :group-of-plants/seeding-date
                                      :group-of-plants/amount]))

(s/def :bed-area/name string?)
(s/def :bed-area/x-begin int?)
(s/def :bed-area/x-end int?)

(s/def :bed-area/spec (s/keys :req [:bed-area/name 
                                    :bed-area/x-begin 
                                    :bed-area/x-end]))

(s/def :relation/group-of-plants-id string?)
(s/def :plan-item/bed-area-id string?)
(s/def :plan-item/planned-seeding-date string?) ;; TODO convert to dates
(s/def :plan-item/planned-planting-date string?)
(s/def :plan-item/planned-harvesting-date string?)
(s/def :plan-item/succession-number int?)

;; per season
(s/def :plan-item/spec (s/keys :req [:relation/group-of-plants-id
                                     :plan-item/bed-area-id
                                     :plan-item/planned-seeding-date
                                     :plan-item/planned-planting-date
                                     :plan-item/planned-harvesting-date
                                     ;; can possibly be calculated based upon planting date
                                     :plan-item/succession-number
                                     ]))
