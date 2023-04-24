(ns dispatch
  (:require [net.eighttrigrams.defn-over-http.core :refer [defdispatch]]
            [controller :refer [list-resources]]))

(defdispatch handler list-resources)
