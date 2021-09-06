(ns vm
  (:require [clojure.java.io :as io])
  (:import java.lang.Byte))

(defn halt-op [vm]
  (assoc vm :halted true))

(defn set-op [vm a b]
  (-> vm
      (update :registers assoc a b)
      (update :head + 3)))

(defn push-op [vm a]
  (-> vm
      (update :stack conj a)
      (update :head + 2)))

(defn pop-op [vm a]
  (-> vm
      (update :registers assoc a (peek (:stack vm)))
      (update :stack pop)
      (update :head + 2)))

(defn eq-op [vm a b c]
  (-> vm
      (update :registers assoc a (if (= b c) 1 0))
      (update :head + 4)))

(defn gt-op [vm a b c]
  (-> vm
      (update :registers assoc a (if (> b c) 1 0))
      (update :head + 4)))

(defn jmp-op [vm a]
  (assoc vm :head a))

(defn jt-op [vm a b]
  (if (pos? a)
    (assoc vm :head b)
    (update vm :head + 3)))

(defn jf-op [vm a b]
  (if (zero? a)
    (assoc vm :head b)
    (update vm :head + 3)))

(defn add-op [vm a b c]
  (-> vm
      (update :registers assoc a (mod (+ b c) 32768))
      (update :head + 4)))

(defn mult-op [vm a b c]
  (-> vm
      (update :registers assoc a (mod (* b c) 32768))
      (update :head + 4)))

(defn mod-op [vm a b c]
  (-> vm
      (update :registers assoc a (mod b c))
      (update :head + 4)))

(defn and-op [vm a b c]
  (-> vm
      (update :registers assoc a (bit-and b c))
      (update :head + 4)))

(defn or-op [vm a b c]
  (-> vm
      (update :registers assoc a (bit-or b c))
      (update :head + 4)))

(defn not-op [vm a b]
  (-> vm
      (update :registers assoc a (bit-and 32767 (bit-not b)))
      (update :head + 3)))

(defn rmem-op [vm a b]
  (-> vm
      (update :registers assoc a ((:memory vm) b))
      (update :head + 3)))

(defn wmem-op [vm a b]
  (-> vm
      (update :memory assoc a b)
      (update :head + 3)))

(defn call-op [vm a]
  (-> vm
      (update :stack conj (+ (:head vm) 2))
      (assoc :head a)))

(defn ret-op [vm]
  (if (seq (:stack vm))
    (-> vm
        (update :stack pop)
        (assoc :head (peek (:stack vm))))
    (assoc vm :halted true)))

(defn out-op [vm a]
  (print (char a))
  (flush)
  (update vm :head + 2))

(defn in-op [vm a]
  (-> vm
      (update :registers assoc a (.read *in*))
      (update :head + 2)))

(defn noop-op [vm]
  (update vm :head + 1))

(defn do-op [vm [op & args]]
  (apply op vm (map-indexed (fn [i arg] (arg vm (inc i))) args)))

(defn rarg [vm offset]
  (- ((:memory vm) (+ offset (:head vm))) 32768))

(defn varg [vm offset]
  (let [literal ((:memory vm) (+ offset (:head vm)))]
    (if (< literal 32768)
      literal
      ((:registers vm) (rarg vm offset)))))

(defn run [vm]
  (if (:halted vm)
    vm
    (recur (do-op vm (case (varg vm 0)
                       0  [halt-op]                 1 [set-op  rarg varg]
                       2  [push-op varg]            3 [pop-op  rarg]
                       4  [eq-op   rarg varg varg]  5 [gt-op   rarg varg varg]
                       6  [jmp-op  varg]            7 [jt-op   varg varg]
                       8  [jf-op   varg varg]       9 [add-op  rarg varg varg]
                       10 [mult-op rarg varg varg] 11 [mod-op  rarg varg varg]
                       12 [and-op  rarg varg varg] 13 [or-op   rarg varg varg]
                       14 [not-op  rarg varg]      15 [rmem-op rarg varg]
                       16 [wmem-op varg varg]      17 [call-op varg]
                       18 [ret-op]                 19 [out-op  varg]
                       20 [in-op   rarg]           21 [noop-op])))))

(run
 {:memory (->> (io/input-stream "challenge.bin")
               .readAllBytes
               (map #(Byte/toUnsignedInt %))
               (partition 2)
               (reduce (fn [[m i] [low high]]
                         [(assoc m i (+ (bit-shift-left high 8) low)) (inc i)])
                       [(vec (replicate 32768 0)) 0])
               first)
  :registers [0 0 0 0 0 0 0 0]
  :stack []
  :head 0})
