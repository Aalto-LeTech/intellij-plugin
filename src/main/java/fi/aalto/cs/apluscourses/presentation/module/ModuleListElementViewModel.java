package fi.aalto.cs.apluscourses.presentation.module;

import fi.aalto.cs.apluscourses.model.Component;
import fi.aalto.cs.apluscourses.model.Module;
import fi.aalto.cs.apluscourses.presentation.base.BaseViewModel;
import fi.aalto.cs.apluscourses.presentation.base.ListElementViewModel;
import fi.aalto.cs.apluscourses.presentation.base.Searchable;
import fi.aalto.cs.apluscourses.utils.PluginResourceBundle;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;

public class ModuleListElementViewModel extends ListElementViewModel<Module>
        implements Searchable {

  public ModuleListElementViewModel(@NotNull Module module) {
    super(module);
    module.stateChanged.addListener(this, BaseViewModel::onChanged);
  }

  public String getName() {
    return getModel().getName();
  }

  public String getUrl() {
    return getModel().getUrl().toString();
  }

  @Override
  public @NotNull String getSearchableString() {
    return getName();
  }

  /**
   * Returns the timestamp of the module if it's defined, else the URL.
   * @return A {@link String} with info about the module.
   */
  public String getTooltip() {
    ZonedDateTime timestamp = getModel().getMetadata().getDownloadedAt();
    return timestamp != null
        ? PluginResourceBundle.getAndReplaceText(
                "presentation.moduleTooltip.timestamp",
                timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)))
        : PluginResourceBundle.getAndReplaceText(
                "presentation.moduleTooltip.moduleURL", getUrl());
  }

  public Boolean isUpdateAvailable() {
    return getModel().isUpdatable();
  }

  /**
   * Returns a textual representation of the status of the module.
   * @return A {@link String} describing the status.
   */
  public String getStatus() {
    Module model = getModel();
    switch (model.stateMonitor.get()) {
      case Component.UNRESOLVED:
        return getText("presentation.moduleStatuses.unknown");
      case Component.NOT_INSTALLED:
      case Component.FETCHED:
        return getText("presentation.moduleStatuses.doubleClickToInstall");
      case Component.FETCHING:
        return getText("presentation.moduleStatuses.downloading");
      case Component.LOADING:
        return getText("presentation.moduleStatuses.installing");
      case Component.LOADED:
        break;
      case Component.UNINSTALLING:
        return getText("presentation.moduleStatuses.uninstalling");
      case Component.UNINSTALLED:
        return getText("presentation.moduleStatuses.uninstalled");
      case Component.ACTION_ABORTED:
        return getText("presentation.moduleStatuses.cancelling");
      default:
        return getText("presentation.moduleStatuses.error");
    }
    switch (model.dependencyStateMonitor.get()) {
      case Component.DEP_INITIAL:
        return getText("presentation.dependencyStatus.installedDependenciesUnknown");
      case Component.DEP_WAITING:
        return getText("presentation.dependencyStatus.waitingForDependencies");
      case Component.DEP_LOADED:
        return getText("presentation.dependencyStatus.installed");
      default:
        return getText("presentation.dependencyStatus.errorInDependencies");
    }
  }

  /**
   * Indicates whether the module shown on a list should be displayed bold-faced.
   */
  public boolean isBoldface() {
    Module model = getModel();
    return !model.hasError() && model.stateMonitor.get() == Component.LOADED;
  }
}
