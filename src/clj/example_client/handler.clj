(ns example-client.handler
  (:require
   [example-client.middleware :as middleware]
   [example-client.routes.home :refer [home-routes]]
   [reitit.ring :as ring]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.webjars :refer [wrap-webjars]]
   [auth-server.env :refer [defaults]]
   [mount.core :as mount]))

(mount/defstate init-client
  :start ((or (:init defaults) (fn [])))
  :stop ((or (:init defaults) (fn []))))

(mount/defstate client-routes
  :start
  (ring/ring-handler
   (ring/router
    [(home-routes)])
   (ring/routes
    (ring/create-resource-handler
     {:path "/"})
    (wrap-content-type (wrap-webjars (constantly nil)))
    (ring/create-default-handler))))

(defn client []
  (middleware/wrap-base #'client-routes))
