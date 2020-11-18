(ns client.app
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [reitit.core :as reitit]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType])
  (:import goog.History))

(def state (r/atom {:page :home}))

(defn home-page []
  [:h1.title "Home"])

(defn redirect-page []
  [:h1.title "Redirect"])

(defn user-page []
  [:h1.title "User"])

(def pages
  {:home #'home-page
   :redirect #'redirect-page
   :user #'user-page})

(defn page []
  [(pages (:page @state))])

(def router
  (reitit/router
   [["/" :home]
    ["/redirect" :redirect]
    ["/user" :user]]))

(defn match-route [uri]
  (->> (or (not-empty (clojure.string/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)
       :data
       :name))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (swap! state assoc :page (match-route (.-token event)))))
    (.setEnabled true)))

(hook-browser-navigation!)
(rdom/render [page] (js/document.getElementById "app"))

;; routes
;;   home
;;   content
;;   redirect
