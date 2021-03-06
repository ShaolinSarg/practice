(ns practices.ocr.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(def zero  " _ | ||_|")
(def one   "     |  |")
(def two   " _  _||_ ")
(def three " _  _| _|")
(def four  "   |_|  |")
(def five  " _ |_  _|")
(def six   " _ |_ |_|")
(def seven " _   |  |")
(def eight " _ |_||_|")
(def nine  " _ |_| _|")

(def valid-numbers {zero 0
                    one 1
                    two 2
                    three 3
                    four 4
                    five 5
                    six 6
                    seven 7
                    eight 8
                    nine 9})

(defn get-char
  "get the 3 row values that make one character"
  [index raw-data]
  (str/join (take 3 (map #(str/join (nth (partition 3 3 %) index))
                          raw-data))))

(defn get-chars
  [raw]
  (for [i (range 9)] (get-char i raw)))


(defn parse-input
  "convert a 4 line ocr image to a number"
  [ocr-image]
  (get valid-numbers ocr-image "?"))

(defn valid-checksum?
  "validate the checksum of an account number"
  [account-number]
  ;account number:  3  4  5  8  8  2  8  6  5
  ;position names:  d9 d8 d7 d6 d5 d4 d3 d2 d1
  ;(d1+2*d2+3*d3+...+9*d9) mod 11 = 0
  (let [weights (take 9 (iterate dec 9))
        account-numbers (map #(Integer/parseInt (str %))
                             account-number)
        weighted (map * account-numbers weights)
        combined (apply + weighted)]

    (zero? (mod combined 11))))

(defn read-result
  "takes a `number string` and returns one of `ILL` `ERR` or `OK`
  the status of validating the number converted from ocr image"
  [number-string]
  (cond
    (< (count (remove #(= \? %) number-string)) 9) :ILL
    (false? (valid-checksum? number-string)) :ERR
    :else :OK))

(defn read-results
  "indicates the result of parsing the account representation"
  [accounts]
  (map (fn [v]
         (str v " " (name (read-result v)))) accounts))

(defn pixel-variants
  "returns the alternative values of the given value"
  [c]
  (let [pixels #{\| \_ \space}]
    (disj pixels c)))

(defn char-variants
  [current]
  (let [orig (vec current)]
    (set (for [i (range 0 (count orig))
                    v (pixel-variants (nth orig i))]
                (str/join (assoc orig i v))))))

(defn alternative-numbers
  "given a representaton returns numbers that have one more or less char"
  [candidate]
  (let [combinations (char-variants candidate)]
    (set (remove #(= "?" %)
                      (map parse-input
                           combinations)))))

(defn fix-error
  [initial-read raw-input]
  (let [alternative-characters (map alternative-numbers
                                 (get-chars raw-input))
        alternative-accounts   (for [i (range 9)
                                     v (nth alternative-characters i)]
                                 (str/join (assoc (vec initial-read) i v)))]
    (->> alternative-accounts
         (remove #(str/includes? % "?"))
         (remove (complement valid-checksum?)))))

(defn ocr
  "read a single account number from the input"
  [raw-data]
  (let [initial-read (str/join (map parse-input
                                    (get-chars raw-data)))
        initial-read-status (read-result initial-read)
        alternatives (fix-error initial-read raw-data)
        status (condp = initial-read-status
                 :OK initial-read-status
                 (cond
                   (empty? alternatives)      :ILL
                   (< 1 (count alternatives)) :AMB
                   :else                      :OK))]
    {:raw raw-data
     :initial-read initial-read
     :status status
     :final-value (condp = initial-read-status
                    :OK initial-read
                    (first alternatives))}))


(defn -main [args]
  (let [input-file (io/file args)]
    (map ocr
         (partition 4 4 (slurp input-file)))))
