(ns auth-server.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [auth-server.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[auth-server started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[auth-server has shut down successfully]=-"))
   :middleware wrap-dev})
