package jetbrains.buildServer.artifactsMirror.torrent;

import com.intellij.openapi.diagnostic.Logger;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import jetbrains.buildServer.NetworkUtil;
import jetbrains.buildServer.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

public class TorrentTracker {
  private final static Logger LOG = Logger.getInstance(TorrentTracker.class.getName());

  private Tracker myTracker;

  public TorrentTracker() {
  }

  public void start(@NotNull String rootUrl) {
    int freePort = NetworkUtil.getFreePort(6969);

    try {
      if (rootUrl.endsWith("/")) rootUrl = rootUrl.substring(0, rootUrl.length()-1);
      URI serverUrl = new URI(rootUrl);
      InetAddress serverAddress = InetAddress.getByName(serverUrl.getHost());
      myTracker = new Tracker(new InetSocketAddress(serverAddress, freePort));
      myTracker.setAcceptForeignTorrents(true);
      myTracker.start();
      LOG.info("Torrent tracker started on url: " + myTracker.getAnnounceUrl().toString());
    } catch (Exception e) {
      LOG.error("Failed to start torrent tracker, server URL is invalid: " + e.toString());
    }
  }

  public void stop() {
    if (myTracker != null) {
      LOG.info("Stopping torrent tracker");
      myTracker.stop();
    }
  }

  @NotNull
  public URI getAnnounceURI() {
    try {
      return myTracker.getAnnounceUrl().toURI();
    } catch (URISyntaxException e) {
      ExceptionUtil.rethrowAsRuntimeException(e);
    }
    return null;
  }

  @NotNull
  public Collection<TrackedTorrent> getTrackedTorrents() {
    return myTracker.getTrackedTorrents();
  }

  public void removeTrackedTorrent(@NotNull TrackedTorrent torrent) {
    myTracker.remove(torrent.getInfoHash());
  }
}