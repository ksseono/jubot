(ns jubot.adapter.slack
  "Jubot adapter for Slack.
  https://slack.com/
  "
  (:require
    [jubot.adapter.util :refer :all]
    [jubot.redef :refer :all]
    [jubot.config :refer :all]
    [com.stuartsierra.component :as component]
    [ring.adapter.jetty       :refer [run-jetty]]
    [ring.middleware.defaults :refer :all]
    [compojure.core           :refer [defroutes GET POST]]
    [compojure.route          :refer [files not-found]]
    [clojure.data.json        :as    json]
    [clj-http.lite.client     :as    client]))

(def ^:private DEFAULT_PORT 8008)
(def ^:private OUTGOING_TOKEN_KEY "SLACK_OUTGOING_TOKEN")
(def ^:private INCOMING_URL_KEY   "SLACK_INCOMING_URL")

(defn not-from-slackbot
  "If the user name of inputted message is \"slackbot\", return text as it is.
  If that's not the case, return nil.

  Params
    username - The user name of inputted message.
    text     - Message from user.
  Return
    Text string or nil.
  "
  [username text]
  (if (not= "slackbot" username)
    text))

(defn valid-outgoing-token
  "If the outgoing token is valid, return text as it is.
  If that's not the case, return nil.

  Params
    token - Outgoing token passed from slack.
    text  - Message from user.
  Return
    Text string or nil.
  "
  [token text]
  (if (= token (or (-> (read-config) :adapter :slack-outgoing-token) (getenv* OUTGOING_TOKEN_KEY)))
    text))

(defn process-output
  "Process output to Slack.

  Params
    this        - Slack adapter.
      :name     - Bot's name.
    text        - Output text to Slack.
    option
      :as       - Overwrite bot's name if you specify this option.
      :icon-url - Customized icon URL.
  "
  [this text & {:keys [as icon-url]}]
  (let [url     (getenv* INCOMING_URL_KEY)
        name    (or as (:name this))
        payload {:text text :username name}
        payload (merge payload (if icon-url {:icon_url icon-url} {}))]
    (when url
      (client/post url {:form-params {:payload (json/write-str payload)}}))))

(defn process-input
  "Process input from Slack.

  Params
    this       - REPL adapter.
    handler-fn - A handler function.
    params     - POST parameters.
      {:channel_id   \"xxxxxxxxx\",
       :token        \"xxxxxxxxx\",
       :channel_name \"test\",
       :user_id      \"xxxxxxxxx\",
       :team_id      \"xxxxxxxxx\",
       :service_id   \"xxxxxxxxx\",
       :user_name    \"slackbot\",
       :team_domain  \"uochan\",
       :timestamp    \"1422058599.000004\",
       :text         \"foo bar\"}
  Return
    JSON string or empty string.
  "
  [this handler-fn params]
  (let [{:keys [token user_name channel_name text]} params
        botname (:name this)
        option  {:user user_name :channel channel_name}]
    (or (some->> text
                 (not-from-slackbot user_name)
                 (valid-outgoing-token token)
                 (parse-text botname)
                 (merge option)
                 handler-fn
                 (str "@" user_name " ")
                 (hash-map :username botname :text)
                 json/write-str)
        "")))

(defroutes app
  (GET "/" {:keys [adapter]}
    (str "this is jubot slack adapter."
         " bot's name is \"" (:name adapter) "\"."))
  (POST "/" {:keys [adapter handler-fn params]}
    (process-input adapter handler-fn params))
  (files "/static" {:root "static"})
  (not-found "page not found"))

(defn wrap-adapter
  "Ring middleware to wrap jubot adapter on request.

  Params
    handler     - A ring handler.
    adapter     - A jubot adapter.
    bot-handler - A jubot handler function.
  Return
    A ring handler.
  "
  [handler adapter bot-handler]
  #(handler
     (assoc % :adapter    adapter
              :handler-fn bot-handler)))


(defrecord SlackAdapter [name server handler]
  component/Lifecycle
  (start [this]
    (if server
      this
      (do (println ";; start slack adapter. bot name is" name)
          (let [server (run-jetty
                         (-> app
                             (wrap-defaults api-defaults)
                             (wrap-adapter this handler))
                         {:port (Integer. (or (getenv* "PORT") DEFAULT_PORT))
                          :join? false })]
            (assoc this
                   :server server
                   :out    (partial process-output this))))))
  (stop [this]
    (if-not server
      this
      (do (println ";; stop slack adapter")
          (try (.stop server)
               (catch Exception e
                 (println ";; error occured when stopping server:" e)))
          (assoc this :server nil)))))
