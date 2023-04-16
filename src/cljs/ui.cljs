(ns ui
  (:require [reagent.core :as r]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            api))

(def original-state {})

(defn fetch-and-reset!
  [*state]
  (go (->>
       #_{:clj-kondo/ignore [:unresolved-var]}
       (api/list-resources)
       <p!
       (reset! *state))))

(defn component []
  (let [*state (r/atom original-state)]
    (fetch-and-reset! *state)
    (fn []
      (prn "..." @*state)
      [:div "hallo"])))
