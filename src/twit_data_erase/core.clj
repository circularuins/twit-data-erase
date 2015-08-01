(ns twit-data-erase.core
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :as mo]
            [clojure.string :as str]
            [clj-time
             [core :as t]
             [local :as tl]
             [coerce :as coerce]
             [format :as f]
             [periodic :as p]])
  (:gen-class))

(def db (mg/get-db (mg/connect) "matching-db"))

(defn fix-object [object]
  (let [id (:_id object)]
    (-> object
        (assoc :id (str id))
        (dissoc :_id))))

(def custom-formatter (f/formatter "yyyy/MM/dd HH:mm:ss"))

(defn ten-days-ago-from-now []
  (t/minus (tl/local-now) (t/days 10)))

(def db (mg/get-db (mg/connect) "matching-db"))

(defn get-by-screen-name
  [screen-name]
  (->> (mq/with-collection db "mach-ranking"
         (mq/find {:screen-name screen-name})
         (mq/sort (array-map :date 1)))
       (map fix-object)))

(defn delete-by-id
  [user-id collection]
  (let [coll collection]
    (mc/remove db coll {:user-id user-id})))

(defn get-all-matching-data []
  (->> (mq/with-collection db "mach-ranking")
       (map fix-object)))

(defn erase-data []
  (->>
   (get-all-matching-data)
   (filter #(< (:pv %) 3))
   (filter #(t/after? (ten-days-ago-from-now) (f/parse custom-formatter (:date %))))
   (map #(:user-id %))
   (map #(delete-by-id % "mach-ranking"))
   (map #(delete-by-id % "best-matching"))
   ))

(defn count-all [coll]
  (mc/count db coll))

;; main
(defn -main [& args]
  (erase-data))

