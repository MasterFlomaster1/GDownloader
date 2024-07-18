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
package net.brlns.gdownloader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import static net.brlns.gdownloader.Language.*;
import net.brlns.gdownloader.settings.QualitySettings;
import net.brlns.gdownloader.settings.enums.BrowserEnum;
import net.brlns.gdownloader.settings.enums.PlayListOptionEnum;
import net.brlns.gdownloader.settings.enums.SettingsEnum;
import net.brlns.gdownloader.settings.enums.VideoContainerEnum;
import net.brlns.gdownloader.settings.enums.WebFilterEnum;
import net.brlns.gdownloader.ui.GUIManager.ButtonFunction;
import net.brlns.gdownloader.ui.GUIManager.DialogButton;
import net.brlns.gdownloader.ui.MediaCard;

//TODO first clipboard copy tends to fail
//TODO max simultaneous downloads should be independent per website
//TODO we should only grab clipboard AFTER the button is clicked
//TODO expand thumbails a little when window is resized
//TODO implement CD Ripper
//TODO winamp icon for mp3's in disc
//TODO add button to convert individually
//TODO output filename settings
//TODO delete cache folder when its location changes
//TODO shutdown and startup hook to delete cache folder
//TODO add custom ytdlp filename modifiers to the settings
//TODO TEST - empty queue should delete directories too
//TODO check if empty spaces in filenames are ok
//TODO restart program on language change
//TODO dark and white themes
//TODO drag and drop
//TODO scale on resolution DPI
//TODO check if removing the border from the bottom fixes the scrolling down
//TODO should we move the window back up when a new card is added?
//TODO save last window size in config
//TODO setting do download channels as playlists
/**
 * @author Gabriel / hstr0100 / vertx010
 */
@Slf4j
public class YtDlpDownloader{

    private final GDownloader main;

    private final ExecutorService downloadScheduler;

    private final List<Consumer<YtDlpDownloader>> listeners = new ArrayList<>();

    private final Set<String> capturedLinks = Collections.synchronizedSet(new HashSet<>());

    private final Deque<QueueEntry> downloadDeque = new LinkedBlockingDeque<>();
    private final Queue<QueueEntry> completedDownloads = new ConcurrentLinkedQueue<>();
    private final Queue<QueueEntry> failedDownloads = new ConcurrentLinkedQueue<>();

    private final AtomicInteger runningDownloads = new AtomicInteger();

    private final AtomicInteger downloadCounter = new AtomicInteger();

    private final AtomicBoolean downloadsRunning = new AtomicBoolean(false);

    public YtDlpDownloader(GDownloader mainIn){
        main = mainIn;

        //I know what era of computer this will be running on, so more than 10 threads would be insanity
        //But maybe add it as a setting later
        downloadScheduler = new ThreadPoolExecutor(0, 10, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    }

    public void registerListener(Consumer<YtDlpDownloader> consumer){
        listeners.add(consumer);
    }

    public boolean captureUrl(String inputUrl){
        return captureUrl(inputUrl, false);
    }

    public boolean captureUrl(String inputUrl, boolean skipDialog){
        for(WebFilterEnum webFilter : WebFilterEnum.values()){
            if(webFilter == WebFilterEnum.DEFAULT && main.getConfig().isCaptureAnyLinks() || webFilter.getPattern().apply(inputUrl)){
                if(inputUrl.contains("ytimg")){
                    return false;
                }

                if(!capturedLinks.contains(inputUrl)){
                    if(WebFilterEnum.isYoutubeChannel(inputUrl)){//Nope, maybe as a setting later
                        return false;
                    }

                    String filteredUrl = inputUrl;

                    if(webFilter == WebFilterEnum.YOUTUBE){
                        filteredUrl = stripQuery(inputUrl);
                    }

                    if(!skipDialog && webFilter == WebFilterEnum.YOUTUBE_PLAYLIST){
                        ButtonFunction playlist = (boolean setDefault) -> {
                            if(setDefault){
                                main.getConfig().setPlaylistDownloadOption(PlayListOptionEnum.DOWNLOAD_PLAYLIST);
                                main.updateConfig();
                            }

                            if(captureUrl(inputUrl, true)){
                                //TODO: increment a counter and call on the ticker
//                                main.getGuiManager().showMessage(
//                                    "URL CAPTURE",
//                                    "Captured Playlist\n" + inputUrl,
//                                    2500,
//                                    MessageType.INFO,
//                                    false
//                                );
                            }
                        };

                        ButtonFunction single = (boolean setDefault) -> {
                            if(setDefault){
                                main.getConfig().setPlaylistDownloadOption(PlayListOptionEnum.DOWNLOAD_SINGLE);
                                main.updateConfig();
                            }

                            String newUrl = stripQuery(inputUrl);

                            if(captureUrl(newUrl, true)){
//                                main.getGuiManager().showMessage(
//                                    "URL CAPTURE",
//                                    "Captured single video from playlist\n" + newUrl,
//                                    2500,
//                                    MessageType.INFO,
//                                    false
//                                );
                            }
                        };

                        switch(main.getConfig().getPlaylistDownloadOption()){
                            case DOWNLOAD_PLAYLIST:
                                playlist.accept(false);
                                break;
                            case DOWNLOAD_SINGLE:
                                single.accept(false);
                                break;
                            default:
                                main.getGuiManager().showConfirmDialog(
                                    get("dialog.confirm"),
                                    get("dialog.download_playlist") + "\n\n" + inputUrl,
                                    new DialogButton(PlayListOptionEnum.DOWNLOAD_PLAYLIST.getDisplayName(), playlist),
                                    new DialogButton(PlayListOptionEnum.DOWNLOAD_SINGLE.getDisplayName(), single));
                        }

                        return false;
                    }

                    if(capturedLinks.add(inputUrl)){
                        MediaCard mediaCard = main.getGuiManager().addMediaCard(!main.getConfig().isDownloadAudioOnly(), "");

                        int downloadId = downloadCounter.incrementAndGet();

                        QueueEntry queueEntry = new QueueEntry(mediaCard, webFilter, inputUrl, filteredUrl, downloadId);
                        queueEntry.updateStatus(DownloadStatus.QUERYING, get("gui.download_status.querying"));

                        mediaCard.setOnClose(() -> {
                            queueEntry.close();

                            capturedLinks.remove(inputUrl);

                            downloadDeque.remove(queueEntry);
                            failedDownloads.remove(queueEntry);
                            completedDownloads.remove(queueEntry);

                            fireListeners();
                        });

                        mediaCard.setOnClick(() -> {
                            queueEntry.launch(main);
                        });

                        queryVideo(queueEntry);

                        downloadDeque.offerLast(queueEntry);
                        fireListeners();

                        return true;
                    }

                    return false;
                }
            }
        }

        return false;
    }

    public boolean isRunning(){
        return downloadsRunning.get();
    }

    public void startDownloads(){
        downloadsRunning.set(true);

        fireListeners();
    }

    public void stopDownloads(){
        downloadsRunning.set(false);

        fireListeners();
    }

    private void fireListeners(){
        for(Consumer<YtDlpDownloader> listener : listeners){
            listener.accept(this);
        }
    }

    public int getQueueSize(){
        return downloadDeque.size();
    }

    public int getDownloadsRunning(){
        return runningDownloads.get();
    }

    public int getFailedDownloads(){
        return failedDownloads.size();
    }

    public int getCompletedDownloads(){
        return completedDownloads.size();
    }

    public void retryFailedDownloads(){
        QueueEntry next;
        while((next = failedDownloads.poll()) != null){
            next.updateStatus(DownloadStatus.QUEUED, get("gui.download_status.not_started"));
            next.reset();

            downloadDeque.offerLast(next);
        }

        startDownloads();

        fireListeners();
    }

    public void clearQueue(){
        capturedLinks.clear();

        QueueEntry next;
        while((next = downloadDeque.peek()) != null){
            main.getGuiManager().removeMediaCard(next.getMediaCard().getId());
            downloadDeque.remove(next);

            if(!next.isRunning()){
                next.clean();
            }
        }

        while((next = failedDownloads.poll()) != null){
            main.getGuiManager().removeMediaCard(next.getMediaCard().getId());

            if(!next.isRunning()){
                next.clean();
            }
        }

        while((next = completedDownloads.poll()) != null){
            main.getGuiManager().removeMediaCard(next.getMediaCard().getId());
        }

        //downloadDeque.clear();
        //We deliberately keep running downloads in the queue
        fireListeners();
    }

    private void queryVideo(QueueEntry queueEntry){
        if(queueEntry.getWebFilter() == WebFilterEnum.DEFAULT){
            queueEntry.updateStatus(DownloadStatus.QUEUED, get("gui.download_status.not_started"));
            return;
        }

        downloadScheduler.execute(() -> {
            try{
                List<String> list = readOutput(
                    main.getYtDlpUpdater().getYtDlpExecutablePath().toString(),
                    "--dump-json",
                    "--flat-playlist",
                    queueEntry.getUrl()
                );

                if(!list.isEmpty()){
                    VideoInfo info = GDownloader.OBJECT_MAPPER.readValue(list.get(0), VideoInfo.class);

                    queueEntry.setVideoInfo(info);
                }
            }catch(Exception e){
                log.error("Failed to parse json {}", e.getLocalizedMessage());
            }finally{
                queueEntry.updateStatus(DownloadStatus.QUEUED, get("gui.download_status.not_started"));
            }
        });

    }

    public void processQueue(){
        while(downloadsRunning.get() && !downloadDeque.isEmpty()){
            if(runningDownloads.get() >= main.getConfig().getMaxSimultaneousDownloads()){
                break;
            }

            QueueEntry next = downloadDeque.peek();

            if(next.getDownloadStatus() == DownloadStatus.QUERYING){
                break;
            }

            downloadDeque.remove(next);

            MediaCard mediaCard = next.getMediaCard();
            if(mediaCard.isClosed()){
                break;
            }

            runningDownloads.incrementAndGet();
            fireListeners();

            downloadScheduler.execute(() -> {
                if(!downloadsRunning.get()){
                    downloadDeque.offerFirst(next);
                    fireListeners();
                    return;
                }

                try{
                    next.getRunning().set(true);
                    next.updateStatus(DownloadStatus.STARTING, get("gui.download_status.starting"));

                    File finalPath = main.getOrCreateDownloadsDirectory();

                    File tmpPath = GDownloader.getOrCreate(finalPath, "cache", String.valueOf(next.getDownloadId()));
                    next.setTmpDirectory(tmpPath);

                    List<String> args = new ArrayList<>();

                    args.addAll(Arrays.asList(
                        main.getYtDlpUpdater().getYtDlpExecutablePath().toString(),
                        "-i"
                    ));

                    if(main.getYtDlpUpdater().getFfmpegExecutablePath() != null){
                        args.addAll(Arrays.asList(
                            "--ffmpeg-location",
                            main.getYtDlpUpdater().getFfmpegExecutablePath().toString()
                        ));
                    }

                    int audioQuality = 320;
                    QualitySettings quality = main.getConfig().getQualitySettings().get(next.getWebFilter());
                    if(quality != null){
                        audioQuality = quality.getAudioBitrate().getValue();

                        if(main.getConfig().isDownloadAudioOnly()){
                            args.addAll(Arrays.asList(
                                "-f",
                                "bestaudio"
                            ));
                        }else{
                            args.addAll(Arrays.asList(
                                "-f",
                                quality.getQualitySettings()
                            ));
                        }
                    }else{
                        args.addAll(Arrays.asList(
                            "-f",
                            "bestvideo*+bestaudio/best"
                        ));
                    }

                    if(!main.getConfig().isDownloadAudioOnly()){
                        args.addAll(Arrays.asList(
                            "--merge-output-format",
                            "mp4",
                            "--keep-video"//This is a hack, we should run two separate commands instead
                        ));
                    }

                    args.addAll(Arrays.asList(
                        "--extract-audio",
                        "--audio-format",
                        "mp3",
                        "--audio-quality",
                        audioQuality + "k"
                    ));

                    switch(next.getWebFilter()){
                        case YOUTUBE_PLAYLIST:
                            args.addAll(Arrays.asList(
                                "--yes-playlist"
                            ));

                        //Intentional fall-through
                        case YOUTUBE:
                            if(main.getConfig().isDownloadAudioOnly()){
                                args.addAll(Arrays.asList(
                                    "-o",
                                    tmpPath.getAbsolutePath() + "/%(title)s (" + audioQuality + "kbps).%(ext)s",
                                    "--embed-thumbnail",
                                    "--embed-metadata",
                                    "--sponsorblock-mark",
                                    "sponsor,intro,outro,selfpromo,interaction,music_offtopic"
                                ));
                            }else{
                                args.addAll(Arrays.asList(
                                    "-o",
                                    tmpPath.getAbsolutePath() + "/%(title)s (%(uploader_id)s %(upload_date)s %(resolution)s).%(ext)s",
                                    "--embed-thumbnail",
                                    "--embed-metadata",
                                    "--embed-subs",
                                    "--sub-langs",
                                    "all,-live_chat",
                                    "--parse-metadata",
                                    "description:(?s)(?P<meta_comment>.+)",
                                    "--embed-chapters",
                                    "--sponsorblock-mark",
                                    "sponsor,intro,outro,selfpromo,interaction,music_offtopic"
                                ));
                            }

                            if(next.getUrl().contains("liked") || next.getUrl().contains("list=LL") || next.getUrl().contains("list=WL")){
                                if(main.getConfig().isReadCookies()){
                                    args.addAll(Arrays.asList(
                                        "--cookies-from-browser",
                                        getBrowserForCookies().getName()
                                    ));
                                }
                            }

                            break;
                        case TWITCH:
                            args.addAll(Arrays.asList(
                                "-o",
                                tmpPath.getAbsolutePath() + "/%(title)s (%(uploader_id)s %(upload_date)s %(resolution)s).%(ext)s",
                                "--verbose",
                                "--continue",
                                "--hls-prefer-native"
                            ));

                            if(!main.getConfig().isDownloadAudioOnly()){
                                args.addAll(Arrays.asList(
                                    "--parse-metadata",
                                    ":%(?P<is_live>)"
                                ));
                            }

                            break;
                        case TWITTER:
                            args.addAll(Arrays.asList(
                                "-o",
                                tmpPath.getAbsolutePath() + "/%(title)s (%(uploader_id)s %(upload_date)s %(resolution)s).%(ext)s"
                            ));

                            if(main.getConfig().isReadCookies()){
                                args.addAll(Arrays.asList(
                                    "--cookies-from-browser",
                                    getBrowserForCookies().getName()
                                ));
                            }

                            break;
                        case FACEBOOK:
                            args.addAll(Arrays.asList(
                                "-o",
                                tmpPath.getAbsolutePath() + "/%(title)s (%(upload_date)s %(resolution)s).%(ext)s",
                                "--max-sleep-interval",
                                "30",
                                "--min-sleep-interval",
                                "15"
                            ));

                            break;
                        case CRUNCHYROLL:
                        case DROPOUT:
                        default:
                            args.addAll(Arrays.asList(
                                "-o",
                                tmpPath.getAbsolutePath() + "/%(title)s (%(resolution)s).%(ext)s"
                            ));

                            if(main.getConfig().isReadCookies()){
                                args.addAll(Arrays.asList(
                                    "--cookies-from-browser",
                                    getBrowserForCookies().getName()
                                ));
                            }

                            break;
                    }

                    args.add(next.getUrl());

                    log.info("exec {}", args);

                    Process process = Runtime.getRuntime().exec(args.stream().toArray(String[]::new));

                    next.setProcess(process);

                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    String s;
                    while(downloadsRunning.get() && !next.getCancelHook().get() && (s = stdInput.readLine()) != null){
                        log.info("[{}] - {}", next.getDownloadId(), s);

                        if(s.contains("[download]")){
                            next.updateStatus(DownloadStatus.DOWNLOADING, s.replace("[download] ", ""));

                            String[] parts = s.split("\\s+");
                            for(String part : parts){
                                if(part.endsWith("%")){
                                    mediaCard.setPercentage(Double.parseDouble(part.replace("%", "")));
                                }
                            }
                        }else{
                            next.updateStatus(DownloadStatus.PROCESSING, s);
                        }
                    }

                    String lastError = "- -";

                    while(downloadsRunning.get() && !next.getCancelHook().get() && (s = stdError.readLine()) != null){
                        log.error("[{}] - {}", next.getDownloadId(), s);

                        lastError = s;
                    }

                    if(!downloadsRunning.get()){
                        next.updateStatus(DownloadStatus.STOPPED, get("gui.download_status.not_started"));
                        next.reset();

                        downloadDeque.offerFirst(next);
                        fireListeners();
                    }else if(!next.getCancelHook().get()){
                        int exitCode = process.waitFor();

                        if(exitCode != 0){
                            next.updateStatus(DownloadStatus.FAILED, lastError);

                            failedDownloads.offer(next);
                        }else{
                            next.updateStatus(DownloadStatus.COMPLETE, get("gui.download_status.finished"));

                            try(Stream<Path> dirStream = Files.walk(tmpPath.toPath())){
                                dirStream.forEach(path -> {
                                    String fileName = path.getFileName().toString().toLowerCase();
                                    if(fileName.endsWith(").mp3") || fileName.endsWith(").mp4")){
                                        Path targetPath = finalPath.toPath().resolve(path.getFileName());

                                        try{
                                            Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                            next.getFinalMediaFiles().add(targetPath.toFile());
                                            log.info("Moved file: {}", path.getFileName());
                                        }catch(IOException e){
                                            log.error("Failed to move file: {} {}", path.getFileName(), e.getLocalizedMessage());
                                        }
                                    }
                                });
                            }catch(IOException e){
                                log.error("Failed to list files {}", e.getLocalizedMessage());
                            }

                            GDownloader.deleteRecursively(tmpPath.toPath());

                            completedDownloads.offer(next);
                        }

                        fireListeners();
                    }
                }catch(Exception e){
                    next.updateStatus(DownloadStatus.FAILED, e.getLocalizedMessage());
                    next.reset();

                    downloadDeque.offerLast(next);//Retry later
                    fireListeners();

                    main.handleException(e);
                }finally{
                    next.getRunning().set(false);

                    runningDownloads.decrementAndGet();
                    fireListeners();
                }
            });
        }

        if(downloadsRunning.get() && runningDownloads.get() == 0){
            stopDownloads();
        }
    }

    private static String stripQuery(String youtubeUrl){
        try{
            URL url = new URL(youtubeUrl);
            String host = url.getHost();

            if(host != null && host.contains("youtube.com")){
                String videoId = getParameter(url, "v");
                if(videoId != null){
                    return "https://www.youtube.com/watch?v=" + videoId;
                }
            }

            return youtubeUrl;
        }catch(MalformedURLException e){
            log.warn("Invalid url {} {}", youtubeUrl, e.getLocalizedMessage());
        }

        return youtubeUrl;
    }

    @Nullable
    private static String getParameter(URL url, String parameterName){
        String query = url.getQuery();

        if(query != null){
            String[] params = query.split("&");
            for(String param : params){
                String[] keyValue = param.split("=");
                if(keyValue.length == 2 && keyValue[0].equals(parameterName)){
                    return keyValue[1];
                }
            }
        }

        return null;
    }

    private static BrowserEnum _cachedBrowser;

    private BrowserEnum getBrowserForCookies(){
        if(_cachedBrowser != null){//I realize setting changes will have no effect until a restart
            return _cachedBrowser;
        }

        if(main.getConfig().getBrowser() == BrowserEnum.UNSET){
            String os = System.getProperty("os.name").toLowerCase();
            String browserName = null;

            try{
                if(os.contains("win")){
                    List<String> output = readOutput("reg query HKEY_CLASSES_ROOT\\http\\shell\\open\\command");

                    log.info("Default browser: {}", output);

                    for(String line : output){
                        if(line.contains(".exe")){
                            browserName = line.substring(0, line.indexOf(".exe") + 4);
                            break;
                        }
                    }
                }else if(os.contains("mac")){
                    browserName = "safari";//Why bother
                }else if(os.contains("nix") || os.contains("nux")){
                    List<String> output = readOutput("xdg-settings get default-web-browser");

                    log.info("Default browser: {}", output);

                    for(String line : output){
                        if(!line.isEmpty()){
                            browserName = line.trim();
                            break;
                        }
                    }
                }
            }catch(Exception e){
                log.error("{}", e.getCause());
            }

            BrowserEnum browser = BrowserEnum.getBrowserForName(browserName);

            if(browser == BrowserEnum.UNSET){
                _cachedBrowser = BrowserEnum.FIREFOX;
            }else{
                _cachedBrowser = browser;
            }
        }else{
            _cachedBrowser = main.getConfig().getBrowser();
        }

        return _cachedBrowser;
    }

    public static List<String> readOutput(String... command) throws IOException, InterruptedException{
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        List<String> list = new ArrayList<>();
        try(BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            while((line = in.readLine()) != null){
                list.add(line);
            }
        }

        int exitCode = process.waitFor();
        if(exitCode != 0){
            log.warn("Failed command for {}", Arrays.toString(command));
        }

        return list;
    }

    private static String truncate(String input, int length){
        if(input.length() > length){
            input = input.substring(0, length - 3) + "...";
        }

        return input;
    }

    @Getter
    private enum DownloadStatus implements SettingsEnum{
        QUERYING("enums.download_status.querying"),
        STOPPED("enums.download_status.stopped"),
        QUEUED("enums.download_status.queued"),
        STARTING("enums.download_status.starting"),
        PROCESSING("enums.download_status.processing"),
        DOWNLOADING("enums.download_status.downloading"),
        COMPLETE("enums.download_status.complete"),
        FAILED("enums.download_status.failed");

        @JsonIgnore
        private final String translationKey;

        private DownloadStatus(String translationKeyIn){
            translationKey = translationKeyIn;
        }
    }

    @Data
    private static class QueueEntry{

        private final MediaCard mediaCard;
        private final WebFilterEnum webFilter;
        private final String originalUrl;
        private final String url;
        private final int downloadId;

        @Setter(AccessLevel.NONE)
        private DownloadStatus downloadStatus;
        private VideoInfo videoInfo;

        private File tmpDirectory;//TODO
        private List<File> finalMediaFiles = new ArrayList<>();

        private AtomicBoolean cancelHook = new AtomicBoolean(false);
        private AtomicBoolean running = new AtomicBoolean(false);

        private Process process;

        public void launch(GDownloader main){
            if(!finalMediaFiles.isEmpty()){
                for(File file : finalMediaFiles){
                    if(!file.exists()){
                        continue;
                    }

                    String fileName = file.getAbsolutePath().toLowerCase();

                    //Video files get priority
                    for(VideoContainerEnum container : VideoContainerEnum.values()){
                        if(!main.getConfig().isDownloadAudioOnly() && fileName.endsWith(")." + container.getValue())
                            || main.getConfig().isDownloadAudioOnly() && fileName.endsWith(").mp3")){
                            main.open(file);
                            return;
                        }
                    }

                    main.open(file);
                    return;
                }
            }

            main.openUrlInBrowser(originalUrl);
        }

        public boolean isRunning(){
            return running.get();
        }

        public void clean(){
            if(tmpDirectory != null && tmpDirectory.exists()){
                GDownloader.deleteRecursively(tmpDirectory.toPath());
            }
        }

        public void close(){
            cancelHook.set(true);

            if(process != null){
                process.destroy();
            }

            clean();
        }

        public void reset(){
            cancelHook.set(false);
            process = null;
        }

        public void setVideoInfo(VideoInfo videoInfoIn){
            videoInfo = videoInfoIn;

            if(videoInfo.getThumbnail().startsWith("http")){
                mediaCard.setThumbnailAndDuration(videoInfo.getThumbnail(), videoInfoIn.getDuration());
            }
        }

        private String getTitle(){
            if(videoInfo != null && !videoInfo.getTitle().isEmpty()){
                return truncate(videoInfo.getTitle(), 40);
            }

            return truncate(url
                .replace("https://", "")
                .replace("www.", ""), 30);
        }

        public void updateStatus(DownloadStatus status, String text){
            mediaCard.setLabel(webFilter.getDisplayName(), getTitle(), truncate(text, 50));

            if(status != downloadStatus){
                downloadStatus = status;

                switch(status){
                    case QUERYING:
                        mediaCard.setPercentage(100);
                        mediaCard.setString(status.getDisplayName());
                        mediaCard.setColor(Color.MAGENTA);
                        break;
                    case QUEUED:
                    case STOPPED:
                        mediaCard.setPercentage(100);
                        mediaCard.setString(status.getDisplayName());
                        mediaCard.setColor(Color.GRAY);
                        break;
                    case DOWNLOADING:
                        mediaCard.setPercentage(0);
                        mediaCard.setString(status.getDisplayName() + ": " + mediaCard.getPercentage() + "%");
                        mediaCard.setColor(new Color(255, 214, 0));
                        break;
                    case STARTING:
                        mediaCard.setPercentage(0);
                        mediaCard.setString(status.getDisplayName());
                        mediaCard.setColor(new Color(255, 214, 0));
                        break;
                    case COMPLETE:
                        mediaCard.setPercentage(100);
                        mediaCard.setString(status.getDisplayName());
                        mediaCard.setTextColor(Color.WHITE);
                        mediaCard.setColor(new Color(0, 200, 83));
                        break;
                    case FAILED:
                        mediaCard.setPercentage(100);
                        mediaCard.setString(status.getDisplayName());
                        mediaCard.setColor(Color.RED);
                        break;
                    default:
                }
            }else if(status == DownloadStatus.DOWNLOADING){
                mediaCard.setString(status.getDisplayName() + ": " + mediaCard.getPercentage() + "%");
            }
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoInfo{

        @JsonProperty("id")
        private String id;

        @JsonProperty("title")
        private String title = "";

        @JsonProperty("thumbnail")
        private String thumbnail = "";

        @JsonProperty("description")
        private String description;

        @JsonProperty("channel_id")
        private String channelId;

        @JsonProperty("channel_url")
        private String channelUrl;

        @JsonProperty("duration")
        private long duration;

        @JsonProperty("view_count")
        private int viewCount;

        @JsonProperty("upload_date")
        private String uploadDate;

        @JsonIgnore
        @Nullable
        public LocalDate getUploadDateAsLocalDate(){
            if(uploadDate != null && !uploadDate.isEmpty()){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                return LocalDate.parse(uploadDate, formatter);
            }

            return null;
        }

        @JsonProperty("timestamp")
        private long timestamp;

        @JsonProperty("width")
        private int width;

        @JsonProperty("height")
        private int height;

        @JsonProperty("resolution")
        private String resolution;

        @JsonProperty("fps")
        private int fps;

    }

}
