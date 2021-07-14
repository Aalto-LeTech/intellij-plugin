package fi.aalto.cs.apluscourses.intellij.utils;

import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.Consumer;
import fi.aalto.cs.apluscourses.utils.BuildInfo;
import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class ErrorHandler extends ErrorReportSubmitter {
  private JSONObject serializeErrorData(@Nullable String errorInfo,
                                        @Nullable String lastActionId,
                                        @NotNull List<String> stackTraces) {
    var loadedPlugins = new ArrayList<JSONObject>();

    try {
      for (var p : PluginManager.getLoadedPlugins()) {
        var pluginInfo = new JSONObject()
            .put("id", p.getPluginId() == null ? "" : p.getPluginId().getIdString())
            .put("name", p.getName());

        loadedPlugins.add(pluginInfo);
      }
    } catch (Exception e) {
      // ignore all exceptions here
    }

    return new JSONObject()
        .put("ideVersion", ApplicationInfo.getInstance().getFullVersion())
        .put("ideProduct", ApplicationNamesInfo.getInstance().getFullProductNameWithEdition())
        .put("osName", System.getProperty("os.name"))
        .put("osVersion", System.getProperty("os.version"))
        .put("jvmName", System.getProperty("java.vm.name"))
        .put("jvmVersion", System.getProperty("java.vm.version"))
        .put("pluginVersion", BuildInfo.INSTANCE.pluginVersion.toString())
        .put("loadedPlugins", loadedPlugins)
        .put("errorInfo", errorInfo)
        .put("lastAction", lastActionId)
        .put("stackTraces", stackTraces);
  }

  @Override
  public @NotNull String getPrivacyNoticeText() {
    return "The privacy policy is <a href=\"\">available here</a>.";
  }

  @Override
  public @NotNull String getReportActionText() {
    return "Report to A+ Developers";
  }

  @Override
  public boolean submit(IdeaLoggingEvent @NotNull [] events,
                        @Nullable String additionalInfo,
                        @NotNull Component parentComponent,
                        @NotNull Consumer<? super SubmittedReportInfo> consumer) {
    DataContext context = DataManager.getInstance().getDataContext(parentComponent);
    Project project = CommonDataKeys.PROJECT.getData(context);

    new Task.Backgroundable(project, "Sending error report") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        ApplicationManager.getApplication().invokeLater(() -> {
          Messages.showInfoMessage(parentComponent,
              "Thank you for submitting the report.", "Error Report");
          consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
        });

        var serializedData = serializeErrorData(additionalInfo, IdeaLogger.ourLastActionId,
            Arrays.stream(events).map(IdeaLoggingEvent::getThrowableText)
                .collect(Collectors.toList()));

        HttpPost request = new HttpPost("http://127.0.0.1/report");
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Connection", "close");
        request.setEntity(new StringEntity(serializedData.toString(), StandardCharsets.UTF_8));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
          client.execute(request).close();
        } catch (Exception ex) {
          // silently ignore all errors
        }
      }
    }.queue();
    return true;
  }
}
