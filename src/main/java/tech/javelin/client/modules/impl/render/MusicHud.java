package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
        name = "MusicHud",
        category = Category.RENDER,
        description = "Показывает текущую музыку (Spotify, YouTube, браузер)"
)
public final class MusicHud extends Module {
    public static final MusicHud INSTANCE = new MusicHud();

    public static final class TrackInfo {
        public final String title;
        public final String artist;
        public final String platform;
        public final boolean playing;
        public final long progressMs;
        public final long durationMs;

        TrackInfo(String title, String artist, String platform, boolean playing, long progressMs, long durationMs) {
            this.title      = title;
            this.artist     = artist;
            this.platform   = platform;
            this.playing    = playing;
            this.progressMs = progressMs;
            this.durationMs = durationMs;
        }
    }

    private final AtomicReference<TrackInfo> currentTrack = new AtomicReference<>(null);
    private ScheduledExecutorService executor;

    public TrackInfo getCurrentTrack() { return currentTrack.get(); }

    private MusicHud() {}

    @Override
    public void onEnable() {
        super.onEnable();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MusicHud-Poller");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::pollMedia, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        if (executor != null) { executor.shutdownNow(); executor = null; }
        currentTrack.set(null);
        super.onDisable();
    }

    @Native
    private void pollMedia() {
        try {
            TrackInfo info = null;
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                info = pollWindows();
            }
            currentTrack.set(info);
        } catch (Exception ignored) {}
    }

    private TrackInfo pollWindows() {
        try {
            String[] cmd = {
                "powershell", "-NonInteractive", "-Command",
                "try {" +
                "  $sessions = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]::RequestAsync().GetAwaiter().GetResult();" +
                "  $s = $sessions.GetCurrentSession();" +
                "  if ($s -eq $null) { exit 1 }" +
                "  $info = $s.TryGetMediaPropertiesAsync().GetAwaiter().GetResult();" +
                "  $tl = $s.GetTimelineProperties();" +
                "  $playing = ($s.GetPlaybackInfo().PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing);" +
                "  $src = $s.SourceAppUserModelId;" +
                "  Write-Output ($info.Title + '|' + $info.Artist + '|' + $src + '|' + $playing + '|' + $tl.Position.TotalMilliseconds + '|' + $tl.EndTime.TotalMilliseconds)" +
                "} catch { exit 2 }"
            };
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String line = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
            p.waitFor(3, TimeUnit.SECONDS);
            if (line == null || line.isEmpty()) return null;
            String[] parts = line.split("\\|", -1);
            if (parts.length < 6) return null;
            String title    = parts[0].trim();
            String artist   = parts[1].trim();
            String srcApp   = parts[2].trim().toLowerCase();
            boolean playing = Boolean.parseBoolean(parts[3].trim());
            long progress   = (long) Double.parseDouble(parts[4].trim());
            long duration   = (long) Double.parseDouble(parts[5].trim());
            String platform = resolvePlatform(srcApp);
            return new TrackInfo(title, artist, platform, playing, progress, duration);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolvePlatform(String appId) {
        if (appId.contains("spotify"))    return "Spotify";
        if (appId.contains("youtube"))    return "YouTube";
        if (appId.contains("soundcloud")) return "SoundCloud";
        if (appId.contains("chrome"))     return "Chrome";
        if (appId.contains("firefox"))    return "Firefox";
        if (appId.contains("msedge") || appId.contains("edge")) return "Edge";
        if (appId.contains("vlc"))        return "VLC";
        if (appId.contains("foobar"))     return "foobar2000";
        return "Music";
    }

}
