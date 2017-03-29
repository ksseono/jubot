(defproject ksseono/jubot "0.1.1"
  :description "Chatbot framework in Clojure"
  :url "https://github.com/liquidz/jubot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure         "1.8.0"]
                 [com.stuartsierra/component  "0.3.2"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/tools.cli       "0.3.5"]
                 [compojure                   "1.5.2"]
                 [ring/ring-jetty-adapter     "1.5.1"]
                 [ring/ring-defaults          "0.2.3"]
                 [org.clojure/data.json       "0.2.6"]
                 [com.taoensso/carmine        "2.16.0"]
                 [com.taoensso/timbre         "4.8.0"]
                 [com.taoensso/encore         "2.90.1"]
                 [clj-http-lite               "0.3.0"]
                 [im.chit/cronj               "1.4.4"]
                 [environ                     "1.1.0"]]

  :profiles {:dev {:dependencies
                   [[org.clojars.runa/conjure "2.2.0"]
                    [ring/ring-mock           "0.3.0"]]
                   :source-paths ["dev"]}}

  :codox {:exclude [user dev]
          :src-dir-uri "http://github.com/liquidz/jubot/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "doc/api"}

  :aliases {"dev" ["run" "-m" "dev/-main"]})
