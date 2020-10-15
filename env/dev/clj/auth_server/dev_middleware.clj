(ns auth-server.dev-middleware
  (:require
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.reload :refer [wrap-reload]]
    [selmer.middleware :refer [wrap-error-page]]
    [prone.middleware :refer [wrap-exceptions]]))

(defn wrap-request-log [handler]
  (fn
    ([request]
     (do (prn request)
         (handler request)))
    ([request respond raise]
     (do (prn request)
         (handler request respond raise)))))

(defn wrap-dev [handler]
  (-> handler
      wrap-reload
      wrap-error-page
      wrap-request-log
      (wrap-cors :access-control-allow-origin #"http://localhost.*"
                 :access-control-allow-methods [:get :post])
      (wrap-exceptions {:app-namespaces ['auth-server]})))
