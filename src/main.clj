(ns main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
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

(def input (atom (str/split-lines (slurp "input"))))

(defn read-line* []
  (if-let [line (first @input)]
    (do
      (println line)
      (swap! input rest)
      line)
    (read-line)))

(defn queue-input [vm]
  (let [line (read-line*)]
    (if (= \# (get line 0))
      (recur ((eval (read-string line)) vm))
      (assoc vm :input (map int (str line "\n"))))))

(defn in-op [vm a]
  (let [vm (if (seq (:input vm)) vm (queue-input vm))]
    (-> vm
        (update :registers assoc a (first (:input vm)))
        (update :input rest)
        (update :head + 2))))

(defn noop-op [vm]
  (update vm :head + 1))

(defn do-op [vm [op & args]]
  (let [vm* (apply op vm (map-indexed (fn [i arg] (arg vm (inc i))) args))]
    (when (:debug vm)
      (println)
      (Thread/sleep 5))
    vm*))

(defn rarg [vm offset]
  (when (:debug vm)
    (print (str "\tr" (- ((:memory vm) (+ offset (:head vm))) 32768))))
  (- ((:memory vm) (+ offset (:head vm))) 32768))

(defn varg [vm offset]
  (let [literal ((:memory vm) (+ offset (:head vm)))]
    (if (< literal 32768)
      (do
        (when (:debug vm)
          (print (str "\t" literal)))
        literal)
      (do
        (when (:debug vm)
          (print (str "\tr" (- literal 32768)
                      "(" ((:registers vm) (- literal 32768)) ")")))
        ((:registers vm) (- literal 32768))))))

(def debug-op-names
  {0  "halt" 1  "set"  2  "push" 3  "pop"
   4  "eq"   5  "gt"   6  "jmp"  7  "jt"
   8  "jf"   9  "add"  10 "mult" 11 "mod"
   12 "and"  13 "or"   14 "not"  15 "rmem"
   16 "wmem" 17 "call" 18 "ret"  19 "out"
   20 "in"   21 "noop"})

(def debug-op-args
  {0  0      1  2      2  1      3  1
   4  3      5  3      6  1      7  2
   8  2      9  3      10 3      11 3
   12 3      13 3      14 2      15 2
   16 2      17 1      18 0      19 1
   20 1      21 0})

(defn run [vm]
  (when (:debug vm)
    (print (str "[" (str/join "\t" (:registers vm)) "]" "\t"
                (:head vm) "\t"
                (debug-op-names ((:memory vm) (:head vm))))))
  (if (:halted vm)
    vm
    (recur (do-op vm (case ((:memory vm) (:head vm))
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

(defn darg [literal]
  (if (< literal 32768)
    literal
    (str "r" (- literal 32768))))

(defn decompile [memory]
  (loop [head 0]
    (if-let [op (debug-op-names (memory head))]
      (let [args (debug-op-args (memory head))
            head* (+ head args 1)]
        (println (str/join "\t" (into [head op]
                                      (map #(darg (memory (+ head 1 %)))
                                           (range args)))))
        (when (< head* (count memory))
          (recur head*)))
      (do
        (println head "\tdata\t" (memory head))
        (when (< (inc head) (count memory))
          (recur (inc head)))))))

;; 25734
;; It's the Ackermann function!
;; TODO this is still fairly slow. Can we make it faster?
(defn fast-check []
  (loop [r7 0]
    (def f6027
      (memoize
       (fn [r0 r1 r7]
         (if (zero? r0)
           (mod (inc r1) 32768)
           (if (zero? r1)
             (f6027 (dec r0) r7 r7)
             (f6027 (dec r0) (f6027 r0 (dec r1) r7) r7))))))
    (let [res (f6027 4 1 r7)]
      (println r7 res)
      (when (not= 6 res)
        (recur (inc r7))))))

(defn main [_]
  (run
   {:memory (->> (io/input-stream "challenge.bin")
                 .readAllBytes
                 (map #(Byte/toUnsignedInt %))
                 (partition 2)
                 (reduce (fn [[m i] [low high]]
                           [(assoc m i (+ (bit-shift-left high 8) low))
                            (inc i)])
                         [(vec (replicate 32768 0))
                          0])
                 first)
    :registers [0 0 0 0 0 0 0 0]
    :stack []
    :head 0}))
