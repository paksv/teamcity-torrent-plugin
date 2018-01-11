package jetbrains.buildServer.torrent;

import com.turn.ttorrent.Constants;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.torrent.settings.LeechSettings;
import jetbrains.buildServer.torrent.settings.SeedSettings;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlrpc.XmlRpcFactory;
import jetbrains.buildServer.xmlrpc.XmlRpcTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * User: Victory.Bedrosova
 * Date: 10/12/12
 * Time: 4:06 PM
 */
public class AgentConfiguration implements TorrentConfiguration, SeedSettings, LeechSettings {
  private XmlRpcTarget myXmlRpcTarget;
  @NotNull
  final private BuildAgentConfiguration myBuildAgentConfiguration;
  @NotNull
  private final CurrentBuildTracker myCurrentBuildTracker;

  public AgentConfiguration(@NotNull final EventDispatcher<AgentLifeCycleListener> dispatcher,
                            @NotNull final BuildAgentConfiguration buildAgentConfiguration,
                            @NotNull CurrentBuildTracker currentBuildTracker) {
    this.myBuildAgentConfiguration = buildAgentConfiguration;
    myCurrentBuildTracker = currentBuildTracker;
    dispatcher.addListener(new AgentLifeCycleAdapter() {
      @Override
      public void afterAgentConfigurationLoaded(@NotNull BuildAgent agent) {
        if (StringUtil.isNotEmpty(agent.getConfiguration().getServerUrl())) {
          myXmlRpcTarget = XmlRpcFactory.getInstance().create(agent.getConfiguration().getServerUrl(), "TeamCity Agent", 30000, false);
        }
      }
    });
  }

  @Nullable
  public String getAnnounceUrl() {
    return call("getAnnounceUrl", "http://localhost:8111/trackerAnnounce.html");
  }

  @Override
  public long getFileSizeThresholdBytes() {
    final String fileSizeThresholdBytes = getPropertyFromBuildOrDefault(FILE_SIZE_THRESHOLD, DEFAULT_FILE_SIZE_THRESHOLD);
    try {
      return StringUtil.parseFileSize(fileSizeThresholdBytes);
    } catch (NumberFormatException e) {
      return StringUtil.parseFileSize(DEFAULT_FILE_SIZE_THRESHOLD);
    }
  }

  @Override
  public boolean isDownloadEnabled() {
    String value = getPropertyFromBuildOrDefault(LeechSettings.DOWNLOAD_ENABLED, String.valueOf(LeechSettings.DEFAULT_DOWNLOAD_ENABLED));
    return Boolean.parseBoolean(value);
  }

  @Override
  public boolean isSeedingEnabled() {
    String value = getPropertyFromBuildOrDefault(SeedSettings.SEEDING_ENABLED, String.valueOf(SeedSettings.DEFAULT_SEEDING_ENABLED));
    return Boolean.parseBoolean(value);
  }

  @Override
  public int getMaxNumberOfSeededTorrents() {
    return getFromBuildOrDefault(SeedSettings.MAX_NUMBER_OF_SEEDED_TORRENTS, 500);
  }

  @Override
  public int getMinSeedersForDownload() {
    return getFromBuildOrDefault(LeechSettings.MIN_SEEDERS_FOR_DOWNLOAD, LeechSettings.DEFAULT_MIN_SEEDERS_FOR_DOWNLOAD);
  }

  @Override
  public int getMaxPieceDownloadTime() {
    return getFromBuildOrDefault(LeechSettings.MAX_PIECE_DOWNLOAD_TIME, LeechSettings.DEFAULT_MAX_PIECE_DOWNLOAD_TIME);
  }

  @Override public int getSocketTimeout() {
    int defaultTimeout = (int) TimeUnit.MILLISECONDS.toSeconds(Constants.DEFAULT_SOCKET_CONNECTION_TIMEOUT_MILLIS);
    return call("getSocketTimeout", defaultTimeout);
  }

  @Override public int getCleanupTimeout() {
    int defaultTimeout = (int) TimeUnit.MILLISECONDS.toSeconds(Constants.DEFAULT_CLEANUP_RUN_TIMEOUT_MILLIS);
    return call("getCleanupTimeout", defaultTimeout);
  }

  @Override public int getMaxConnectionsCount() {
    return call("getMaxConnectionsCount", TorrentConfiguration.DEFAULT_MAX_CONNECTIONS);
  }

  private int getFromBuildOrDefault(String key, int defaultValue) {
    String value = getPropertyFromBuildOrDefault(key, String.valueOf(defaultValue));
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @NotNull
  private String getPropertyFromBuildOrDefault(String key, String defaultValue) {
    AgentRunningBuild currentBuild;
    try {
      currentBuild = myCurrentBuildTracker.getCurrentBuild();
    } catch (NoRunningBuildException e) {
      return defaultValue;
    }
    final String value = currentBuild.getSharedConfigParameters().get(key);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  @NotNull
  private <T> T call(@NotNull String methodName, @NotNull final T defaultValue) {
    if (myXmlRpcTarget == null) {
      return defaultValue;
    }
    try {
      final Object retval = myXmlRpcTarget.call(XmlRpcConstants.TORRENT_CONFIGURATION + "." + methodName, new Object[0]);
      if (retval != null)
        return (T) retval;
      else
        return defaultValue;
    } catch (Exception e) {
      return defaultValue;
    }
  }
}