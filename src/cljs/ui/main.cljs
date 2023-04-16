(ns ui.main
  (:require [ui.actions :as actions]
            [ui.main.lhs :as lhs]
            [ui.main.rhs :as rhs]))

(defn component [*state]
  (actions/fetch! *state)
  (fn [*state]
    [:div "Hallo"]))
