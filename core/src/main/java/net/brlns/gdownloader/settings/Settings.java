/*
 * Copyright (C) 2024 hstr0100
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.brlns.gdownloader.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.brlns.gdownloader.settings.enums.BrowserEnum;
import net.brlns.gdownloader.settings.enums.LanguageEnum;
import net.brlns.gdownloader.settings.enums.PlayListOptionEnum;
import net.brlns.gdownloader.settings.enums.ThemeEnum;
import net.brlns.gdownloader.settings.enums.WebFilterEnum;
import net.brlns.gdownloader.settings.filters.AbstractUrlFilter;

/**
 * @author Gabriel / hstr0100 / vertx010
 */
@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class Settings{

    @JsonProperty("ConfigVersion")
    private int configVersion = 20;

    @JsonProperty("MonitorClipboardForLinks")
    private boolean monitorClipboardForLinks = true;

    @JsonProperty("AutomaticUpdates")
    private boolean automaticUpdates = true;

    @JsonProperty("LanguageDefined")
    private boolean languageDefined = false;

    @JsonProperty("Language")
    private LanguageEnum language = LanguageEnum.ENGLISH;

    @Deprecated
    @JsonProperty("ReadCookies")
    private boolean readCookies = false;

    @JsonProperty("ReadCookiesFromBrowser")
    private boolean readCookiesFromBrowser = false;

    @JsonProperty("BrowserForCookies")
    private BrowserEnum browser = BrowserEnum.UNSET;

    @JsonProperty("DownloadYoutubeChannels")
    private boolean downloadYoutubeChannels = false;

    @JsonProperty("RespectYtDlpConfigFile")
    private boolean respectYtDlpConfigFile = false;

    @JsonProperty("DownloadsPath")
    private String downloadsPath = "";

    // TODO implement
    @JsonProperty("UIScale")
    private double uiScale = 1.0;

    @JsonProperty("FontSize")
    private int fontSize = 14;

    @JsonProperty("UseSystemFont")
    private boolean useSystemFont = false;

    @JsonProperty("LogMagnetLinks")
    private boolean logMagnetLinks = false;

    @JsonProperty("Theme")
    private ThemeEnum theme = ThemeEnum.DARK;

    @JsonProperty("CaptureAnyLinks")
    private boolean captureAnyLinks = false;

    @JsonProperty("ExtraYtDlpArguments")
    private String extraYtDlpArguments = "";

    @JsonProperty("DownloadAudio")
    private boolean downloadAudio = true;

    @JsonProperty("DownloadVideo")
    private boolean downloadVideo = true;

    @JsonProperty("DownloadSubtitles")
    private boolean downloadSubtitles = false;

    @JsonProperty("DownloadAutoGeneratedSubtitles")
    private boolean downloadAutoGeneratedSubtitles = false;

    @JsonProperty("DownloadThumbnails")
    private boolean downloadThumbnails = false;

    @JsonProperty("AutoDownloadStart")
    private boolean autoDownloadStart = false;

    @JsonProperty("RandomIntervalBetweenDownloads")
    private boolean randomIntervalBetweenDownloads = false;

    @JsonProperty("DisplayLinkCaptureNotifications")
    private boolean displayLinkCaptureNotifications = true;

    @JsonProperty("UseSponsorBlock")
    private boolean useSponsorBlock = false;

    @JsonProperty("KeepWindowAlwaysOnTop")
    private boolean keepWindowAlwaysOnTop = false;

    @JsonProperty("MaximumSimultaneousDownloads")
    private int maxSimultaneousDownloads = 3;

    @JsonProperty("PlaylistDownloadOption")
    private PlayListOptionEnum playlistDownloadOption = PlayListOptionEnum.ALWAYS_ASK;

    @JsonProperty("DebugMode")
    private boolean debugMode = false;

    @JsonProperty("AutoStart")
    private boolean autoStart = false;

    @JsonProperty("ExitOnClose")
    private boolean exitOnClose = false;

    @JsonProperty("TranscodeAudioToAAC")
    private boolean transcodeAudioToAAC = true;

    // TODO add more sounds
    @JsonProperty("PlaySounds")
    private boolean playSounds = false;

    @JsonProperty("AutoDownloadRetry")
    private boolean autoDownloadRetry = true;

    @Deprecated
    @JsonProperty("QualitySettings")
    private Map<WebFilterEnum, QualitySettings> qualitySettings = new TreeMap<>();

    @JsonProperty("UrlFilters")
    private List<AbstractUrlFilter> urlFilters = new ArrayList<>();

    public Settings(){
        urlFilters.addAll(AbstractUrlFilter.getDefaultUrlFilters());
    }

    @JsonIgnore
    @SuppressWarnings("deprecation")
    public void doMigration(){
        List<AbstractUrlFilter> defaultFilters = AbstractUrlFilter.getDefaultUrlFilters();

        if(urlFilters.isEmpty()){
            urlFilters.addAll(defaultFilters);
        }else{
            defaultFilters.stream()
                .filter(
                    filter -> urlFilters.stream()
                        .noneMatch(savedFilter -> savedFilter.getId().equals(filter.getId()))
                )
                .forEach(urlFilters::add);
        }

        for(Map.Entry<WebFilterEnum, QualitySettings> entry : qualitySettings.entrySet()){
            WebFilterEnum key = entry.getKey();
            QualitySettings value = entry.getValue();

            urlFilters.stream()
                .filter(filter -> filter.getId().equals(key.getId()))
                .forEach(filter -> {
                    filter.setQualitySettings(value);
                    log.info("Migrated {} -> {}", key, value);
                });
        }

        qualitySettings.clear();

        // Broken in Chromium
        // https://github.com/yt-dlp/yt-dlp/issues/7271
        // https://github.com/yt-dlp/yt-dlp/issues/10927
        if(isReadCookies() && getBrowser() == BrowserEnum.FIREFOX){
            setReadCookiesFromBrowser(true);
        }
    }
}
