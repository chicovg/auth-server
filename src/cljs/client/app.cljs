(ns client.app
  (:require [client.utils :refer [get-item remove-item! set-location! set-item!]]
            [clojure.string :as s]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [reitit.core :as reitit]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [ajax.core :refer [GET POST]]
            [cemerick.url :refer [url]])
  (:import goog.History))

(def client-id "dev-client")
(def client-secret "secret")
(def token-request-headers {"Authorization" (str "Basic "
                                                 (js/btoa (str client-id
                                                               ":"
                                                               client-secret)))})
(def authorize-url "http://localhost:3000/api/authorize")
(def token-url "http://localhost:3000/api/token")
(def user-details-url "http://localhost:3000/api/user")
(def redirect-with-code-url "http://localhost:4000/#/redirect-with-code")
(def redirect-with-token-url "http://localhost:4000/#/redirect-with-token")
(def user-url "http://localhost:4000/#/user")
(def token-key "token")

(def state (r/atom {:error nil
                    :page :home
                    :token nil}))

(defn create-token [form-params]
  (POST token-url {:body form-params
                   :headers token-request-headers
                   :response-format :json
                   :handler (fn [res]
                              (set-item! token-key (get res "access_token"))
                              (set-location! user-url))
                   :error-handler (fn [{:keys [error]}]
                                    (let [err (or error "Unable to get token")]
                                      (print err)
                                      (remove-item! token-key)
                                      (set-location! "/")))}))

(defn simple-auth-panel [{:keys [title link handler]}]
  [:div.box.tile.is-child
   [:p.title title]
   [:p "Demonstrates the " [:a {:href link} title] " grant type."]
   [:button.button.is-primary {:on-click handler} "Login"]])

(defn redirect-auth-panel [params]
  [simple-auth-panel (assoc params
                            :handler
                            #(let [location (-> (url authorize-url)
                                                (assoc :query {:client_id client-id
                                                               :redirect_uri (:redirect-uri params)
                                                               :response_type (:response-type params)}))]
                               (set! (.. js/document -location) location)))])

(defn credentials-auth-panel []
  [:div.box.tile.is-child
   [:p.title "Resource Owner Password Credentials"]
   [:p "Demonstrates the " [:a {:href "https://tools.ietf.org/html/rfc6749#section-4.3"}
                            "Resource Owner Password Credentials"] " grant type."]
   [:div {:role "form"}
    [:div.field
     [:label.label {:for "username"}]
     [:div.control.has-icons-left
      [:input#username.input {:type "text"
                              :name "username"
                              :placeholder "Username"
                              :required true}]
      [:span.icon.is-small.is-left
       [:i.fas.fa-user]]]]
    [:div.field
     [:label.label {:for "password"}]
     [:div.control.has-icons-left
      [:input#password.input {:type "password"
                              :name "password"
                              :placeholder "Password"
                              :required true}]
      [:span.icon.is-small.is-left
       [:i.fas.fa-key]]]]
    [:button.button.is-primary
     {:on-click #(create-token (doto
                                (js/URLSearchParams.)
                                 (.append "grant_type" "password")
                                 (.append "username" (.-value (. js/document getElementById "username")))
                                 (.append "password" (.-value (. js/document getElementById "password")))))}
     "Login"]]])

(defn home-page []
  [:div.content
   [:h1.title "Welcome!"]
   [:p "This is an example client to demo the auth-server functionality"]
   [:div.tile.is-ancestor
    [:div.tile.is-parent.is-vertical.is-6
     [redirect-auth-panel {:title "Authorization Code"
                           :href "https://tools.ietf.org/html/rfc6749#section-4.1"
                           :redirect-uri redirect-with-code-url
                           :response-type "code"}]
     [redirect-auth-panel {:title "Implicit"
                           :href "https://tools.ietf.org/html/rfc6749#section-4.2"
                           :redirect-uri redirect-with-token-url
                           :response-type "token"}]]
    [:div.tile.is-parent.is-vertical.is-6
     [credentials-auth-panel]
     [simple-auth-panel {:title "Client Credentials"
                         :href "https://tools.ietf.org/html/rfc6749#section-4.4"
                         :handler #(create-token (doto
                                                  (js/URLSearchParams.)
                                                   (.append "grant_type" "client_credentials")))}]]]])

(defn redirect-with-code-page []
  (let [code (-> (.. js/document -location)
                 url
                 :query
                 (get "code"))]
    (if code
      (do
        (create-token (doto
                       (js/URLSearchParams.)
                        (.append "client_id" client-id)
                        (.append "code" code)
                        (.append "grant_type" "authorization_code")))
        [:div.content
         [:p "Logging in..."]
         [:progress.progress.is-primary "20%"]])
      (swap! state #(-> %
                        (assoc :error "Couldn't get auth code")
                        (assoc :page :home))))))

(defn fetch-user-details [token]
  (print token)
  (GET user-details-url
    {:headers {"Authorization" (str "Token " token)}
     :format :json
     :response-format :json
     :handler #(swap! state assoc :user %)
     :error-handler #(swap! state assoc :error "Unable to fetch user")}))

(defn user-page []
  (let [s @state
        user (:user s)
        token (get-item token-key)
        log-out #(do
                   (remove-item! token-key)
                   (set-location! "/"))]
    (cond
      (get s :error)
      (do
        (remove-item! token-key)
        (set-location! "/"))

      (get user "id")
      [:div.box
       [:div.block
        [:p [:strong "Logged in as client: "] (get user "id")]
        [:p (get user "description")]]
       [:button.button.is-primary {:on-click log-out} "Logout"]]

      (get user "username")
      [:div.box
       [:div.block
        [:p [:strong "Logged in as user: "] (get user "username")]
        [:ul
         [:li (get user "first_name")]
         [:li (get user "last_name")]
         [:li (get user "email")]]]
       [:button.button.is-primary {:on-click log-out} "Logout"]]

      :else
      (do
        (print token)
        (fetch-user-details token)
        [:div "Loading..."]))))

(def pages
  {:home #'home-page
   :redirect-with-code #'redirect-with-code-page
   :user #'user-page})

(defn page []
  [:section.section
   [:div.container
    [(pages (:page @state))]]])

(def router
  (reitit/router
   [["/" :home]
    ["/redirect-with-code" :redirect-with-code]
    ["/redirect-with-token/token=:token" :redirect-with-token]
    ["/user" :user]]))

(defn match-route [uri]
  (->> (or (not-empty (s/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (let [route-match (match-route (.-token event))
             page (get-in route-match [:data :name])
             token (get-in route-match [:path-params :token])]
         (if (and (= page :redirect-with-token) token)
           (do
             (set-item! token-key token)
             (set-location! user-url))
           (swap! state assoc :page page)))))
    (.setEnabled true)))

(hook-browser-navigation!)
(rdom/render [page] (js/document.getElementById "app"))
