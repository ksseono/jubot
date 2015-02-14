(ns jubot.core
  "jubot のコア"
  (:require
    [com.stuartsierra.component :as component]
    [clojure.tools.cli :refer [parse-opts]]
    [jubot.system      :as sys]
    [jubot.adapter     :as ja]
    [jubot.brain       :as jb]
    [jubot.scheduler   :as js]
    [jubot.handler     :as jh]
    [jubot.require     :as jr]))

(defn create-system-fn
  "システムを作成するための関数を生成して返す関数

  @params
    :name    - ボットの名前
    :handler - ハンドラー関数
    :entries - スケジュールのエントリーリスト

  @return
    function
  "
  [& {:keys [name handler entries] :or {entries []}}]
  (fn [{:keys [adapter brain] :as config-option}]
    (component/system-map
      :adapter   (ja/create-adapter   {:adapter adapter
                                       :name name
                                       :handler handler})
      :brain     (jb/create-brain     {:brain brain})
      :scheduler (js/create-scheduler {:entries entries}))))

(def ^{:const true :doc "デフォルトのアダプター名"} DEFAULT_ADAPTER "slack")
(def ^{:const true :doc "デフォルトのブレイン名"} DEFAULT_BRAIN   "memory")
(def ^{:const true :doc "デフォルトのボット名"} DEFAULT_BOTNAME "jubot")

(def ^:private cli-options
  [["-a" "--adapter ADAPTER_NAME" "Select adapter" :default DEFAULT_ADAPTER]
   ["-b" "--brain NAME"           "Select brain"   :default DEFAULT_BRAIN]
   ["-n" "--name NAME"            "Set bot name"   :default DEFAULT_BOTNAME]])

(defn jubot
  "jubot 起動時のメイン関数を生成して返す関数

  @params
    :handler   - ハンドラー関数
    :entries   - スケジュールのエントリーリスト
    :ns-regexp - ハンドラーやスケジュールリストを定義しているネームスペースを示す正規表現。handler, entries を省略した場合には、この正規表現に一致するネームスペースから handler, entries を自動的に集める

  @return
    function
  "
  [& {:keys [handler entries ns-regexp]}]
  (fn [& args]
    (when ns-regexp (jr/regexp-require ns-regexp))
    (let [{:keys [options _ _ errors]} (parse-opts args cli-options)
          {:keys [name adapter brain debug]} options
          handler (or handler (and ns-regexp (jh/collect ns-regexp)))
          entries (or entries (and ns-regexp (js/collect ns-regexp)))
          create-system (create-system-fn
                          :name    name
                          :handler handler
                          :entries entries)]
      (sys/init #(create-system options))
      (sys/start))))
