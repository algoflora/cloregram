(ns cloregram.logging)

(defn transform-json
  [_ event]
  (let [p (-> (str (System/getProperty "user.dir") "/project.clj") slurp read-string)
        p-name (nth p 1)
        p-version (nth p 2)]
    (-> event
        (dissoc :line)
        (assoc :project p-name)
        (assoc :version p-version))))