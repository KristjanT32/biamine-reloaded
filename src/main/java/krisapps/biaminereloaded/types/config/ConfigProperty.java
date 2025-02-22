package krisapps.biaminereloaded.types.config;

public enum ConfigProperty {
    HEADER_CONTENT("defaults.header"), FOOTER_CONTENT("defaults.footer"), TIMER_FORMAT("options.timer-format"), DATE_FORMAT(
            "options.date-format"), DATE_FORMAT_EXTENDED("options.date-format-full"), TIME_FORMAT("options.time-format"), NOTIFY_STATUS_CHANGE(
            "options.notify-instance-status-change"), PAUSE_IF_PLAYER_DISCONNECT(
            "options.pause-if-player-disconnect.state"), EMERGENCY_PAUSE_DELAY(
            "options.pause-if-player-disconnect.delay"), AUTOREJOIN("options.autorejoin"), HALT_PLAYERS_WITH_POTIONEFFECT(
            "options.halt-players-with-effect"), SEND_ITEM_DISPENSER_MESSAGES("options.send-dispenser-messages"), SCOREBOARD_CYCLE_PERIOD(
            "options.scoreboard-cycle-period"), SHOOTING_RANGE_SCOREBOARD_ENABLED(
            "options.scoreboards.enable-shooting-range"), LEADERBOARD_ENABLED("options.scoreboards.enable-leaderboard"), AUTO_SHOW_SHOOTING_RANGE(
            "options.scoreboards.auto-show-shooting-range"), SKIP_EMPTY_SCOREBOARDS(
            "options.scoreboards.skip-empty-when-cycling"), KEEP_SHOOTING_RANGE_IF_ALL_PRESENT(
            "options.scoreboards.show-shooting-range-if-all-present"), INCLUDE_FINISHED_PLAYERS_IN_LEADERBOARD(
            "options.scoreboards.include-finished-players-in-leaderboard")
    ;

    final String configPath;

    ConfigProperty(String configPath) {
        this.configPath = configPath;
    }

    public String getConfigPath() {
        return configPath;
    }
}
