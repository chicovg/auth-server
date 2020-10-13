(ns auth-server.routes.services
  (:require
   [auth-server.middleware.formats :as formats]
   [auth-server.middleware.exception :as exception]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.string :as st]
   [reitit.core :as r]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.util.codec :refer [url-decode url-encode]]
   [ring.util.response :refer [redirect]]
   [ring.util.http-response :refer [bad-request ok]]
   [reitit.ring :as ring]))

(s/def ::response_type #(= % "code"))
(s/def ::client_id string?)
(s/def ::redirect_uri (s/and string?
                             #(try
                                (io/as-url %)
                                (catch Throwable t false))))
(s/def ::scope string?)
(s/def ::state string?)

(s/def ::authorize-query-params (s/keys :req-un [::response_type ::client_id ::redirect_uri]
                                        :opt-un [::scope ::state]))

(defn param [key value]
  (str key "=" value))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]

   ;; authorization
   ["/authorize" {:get {:summary "An endpoint used to request authorization of a user on behalf of a client"
                        :parameters {:query ::authorize-query-params}
                        :responses {302 nil
                                    400 {:body {:error string?}}}
                        :handler (fn [{{{:keys [response_type
                                                client_id
                                                redirect_uri
                                                scope
                                                state]} :query} :parameters}]
                                   (let [url (-> redirect_uri url-decode io/as-url)
                                         query (.getQuery url)
                                         updated-query (as-> (or query "") $
                                                         (st/split $ #"&")
                                                         (conj $ (param "code" "test"))
                                                         (filter not-empty $)
                                                         (st/join "&" $))
                                         updated-url (str (.getProtocol url) "://"
                                                          (.getHost url)
                                                          (when (.getPort url) (str ":" (.getPort url)))
                                                          (when (.getPath url) (.getPath url))
                                                          "?" updated-query)]
                                     (redirect (str "/login?next=" (url-encode updated-url)))))}}]

   ;; ["/math"
   ;;  {:swagger {:tags ["math"]}}

   ;;  ["/plus"
   ;;   {:get {:summary "plus with spec query parameters"
   ;;          :parameters {:query {:x int?, :y int?}}
   ;;          :responses {200 {:body {:total pos-int?}}}
   ;;          :handler (fn [{{{:keys [x y]} :query} :parameters}]
   ;;                     {:status 200
   ;;                      :body {:total (+ x y)}})}
   ;;    :post {:summary "plus with spec body parameters"
   ;;           :parameters {:body {:x int?, :y int?}}
   ;;           :responses {200 {:body {:total pos-int?}}}
   ;;           :handler (fn [{{{:keys [x y]} :body} :parameters}]
   ;;                      {:status 200
   ;;                       :body {:total (+ x y)}})}}]]

   ;; ["/files"
   ;;  {:swagger {:tags ["files"]}}

   ;;  ["/upload"
   ;;   {:post {:summary "upload a file"
   ;;           :parameters {:multipart {:file multipart/temp-file-part}}
   ;;           :responses {200 {:body {:name string?, :size int?}}}
   ;;           :handler (fn [{{{:keys [file]} :multipart} :parameters}]
   ;;                      {:status 200
   ;;                       :body {:name (:filename file)
   ;;                              :size (:size file)}})}}]

   ;;  ["/download"
   ;;   {:get {:summary "downloads a file"
   ;;          :swagger {:produces ["image/png"]}
   ;;          :handler (fn [_]
   ;;                     {:status 200
   ;;                      :headers {"Content-Type" "image/png"}
   ;;                      :body (-> "public/img/warning_clojure.png"
   ;;                                (io/resource)
   ;;                                (io/input-stream))})}}]]
   ])
