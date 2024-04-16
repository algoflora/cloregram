(ns cloregram.utils
  (:require [com.brunobonacci.mulog :as μ]))

(defn deep-merge
  
  "Recursively merges maps"

  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

(defn username

  "Helper to get human readable identificator of `user` even he don't have Telegram username"
  
  [user]
  (or (:user/username user) (str "id" (:user/id user))))

(defmacro get-project-info

  "This macro expands in map with keys `group`, `name` and `version` of current project by information from project.clj"
  
  []
  (let [[_ ga version] (read-string (try (slurp "project.clj") (catch Exception e "[]")))
        [ns name version] (try [(namespace ga) (name ga) version] (catch Exception e []))]
    {:group ns
     :name name
     :version version}))

(defn resolver

  "Resolves symbol to value if exists"

  [sym]
  (let [ns (-> sym namespace symbol)
        nm (-> sym name symbol)]
    (require ns)
    (if-let [resolved (ns-resolve ns nm)]
      resolved
      (μ/log ::symbol-not-resolved :symbol sym))))

(defn img-comparer
  [img1 img2]
  (let [h (.getHeight img1)
        w (.getWidth img1)]
    (loop [[x y] [0 0]]
      (cond
        (= y h) true
        (not= (.getRGB img1 x y) (.getRGB img2 x y)) false
        :else (let [x' (if (= (dec w) x) 0 (inc x))
                    y' (if (= 0 x) (inc y) y)]
                (recur [x' y']))))))

