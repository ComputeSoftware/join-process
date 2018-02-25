(ns join-process.core
  (:require
    [clojure.java.io :as io]
    [me.raynes.conch.low-level :as sh]
    [primrose.core :as primrose]
    [clansi :as style])
  (:import (java.text SimpleDateFormat)
           (java.util Date)))

(def abort identity)

(def ^:private pallette (cycle [:green :blue :yellow :magenta :red]))

(def ^:private time-formatter (SimpleDateFormat. "HH:mm:ss"))

(def ^:private printing-lock (Object.))

(defn- synchronized-println [& args]
  (locking printing-lock
    (apply println args)))

(defn- current-time []
  (.format time-formatter (Date.)))

(defn- pretty-pipe [process-name process colour]
  (doseq [stream [(:out process) (:err process)]]
    (future
      (let [output (io/reader stream)]
        (loop [out (.readLine output)]
          (when-not (nil? out)
            (synchronized-println (str (style/style (str (current-time) " " process-name) colour) "| " out)))
          (recur (.readLine output)))))))

(defn- add-padding
  "Takes a list of process metadata and calculates the padded name. This is
   to ensure our logging output is aligned by finding the longest process out
   of the list and padding the other names enough so that
     07:46:33 web: ...
     07:46:34 db: ...
     07:46:34 scheduler: ...
   becomes,
     07:46:33 web       : ...
     07:46:34 db        : ...
     07:46:34 scheduler : ...
   Which is easier to skim when reading output."
  [procs]
  (let [characters-in-names (map #(count (:name %)) procs)
        longest (apply max characters-in-names)]
    (map (fn [proc]
           (let [format-string (str "%-" (inc longest) "s")
                 padded-name (format format-string (:name proc))]
             (assoc proc :padded-name padded-name))) procs)))

(defn- pipe-procs
  [procs]
  (let [names (map :padded-name procs)
        colours (take (count procs) pallette)]
    (doall (map pretty-pipe names procs colours))))

(defn init-procs
  [procs]
  (let [proc-maps (add-padding procs)]
    (doall (map (fn [{:keys [cmds] :as proc}]
                  (merge proc (apply sh/proc cmds))) proc-maps))))

(defn- fail [procs]
  (println (style/style "
One or more processes have stopped running. In the current
version of lein-cooper this could result in child processes
(i.e. ones spawned by a process that lein-cooper manages)
left hanging that may need killed manually
Sorry about the inconvenience." :red))
  (println)
  (doseq [proc procs]
    (sh/destroy proc))
  (abort))

(defn- wait-for-early-exit
  "Blocks until at least one of the running processes produces an exit code
   It is irrelevant what that code is.  If any of the stop running thats it."
  [procs]
  (deref
    (apply primrose/first
           (map #(future (sh/exit-code %)) procs))))

(defn join-process
  "Combine multiple long running processes and pipe their output and error
   streams to `stdout` in a distinctive manner.
    `procs` is a vector of maps where each map contains
      `:name` - the name of the process
      `:cmds` - a vector of commands to run
   **CAUTION**
   The JVM is super pants at managing external processes which means that when
   a processes dies and cooper attempts to kill the other processes there may
   be some processes left running.  This is due to the fact that when the JVM
   kills processes it wont kill child process of that process.  There is also
   no cross paltform way to get a handle on child processes and kill them.
   However this is only an issue when a process fails.  When you manually
   CTRL-C out of the lein cooper command everything will be shutdown as
   expected so this issue only happens in an error case."
  [procs]
  (doto (init-procs procs)
    (pipe-procs)
    (wait-for-early-exit)
    (fail)))