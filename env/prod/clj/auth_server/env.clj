(ns auth-server.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[auth-server started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[auth-server has shut down successfully]=-"))
   :middleware identity})
