/*
 * Copyright 2014 Lukas Benda <lbenda at lbenda.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.lbenda.rcp;

import cz.lbenda.rcp.action.Savable;
import cz.lbenda.rcp.action.SavableRegistry;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Class which help with creating dialog */
public class DialogHelper {

  private static DialogHelper instance;
  public static DialogHelper getInstance() {
    if (instance == null) { instance = new DialogHelper(); }
    return instance;
  }

  @Message
  public static final String msgCanOverwriteTitle = "File overwrite";
  @Message
  public static final String msgCanOverwriteContent = "The file '%s' already exist, should be overwrite? ";
  @Message
  public static final String msgFileNotExistTitle = "File not exist";
  @Message
  public static final String msgFileNotExistHeader = "The file '%s' not exist.";
  @Message
  public static final String msgFileNotExistContent = "For importing configuration from file, the file must exist.";
  @Message
  public static final String msgNotSavedObjectsTitle = "Some object wasn't saved.";
  @Message
  public static final String msgNotSavedObjectsHeader = "Some object aren't saved. You can choose if changes will be saved or not.";

  @Message
  public static final String button_cancel = "Cancel";
  @Message
  public static final String button_saveAndClose = "Save and close";
  @Message
  public static final String button_closeWithoutSave = "Close without save";

  @Message
  public static final String chooseSingleOption_title = "Choose one option";

  static { MessageFactory.initializeMessages(DialogHelper.class); }

  /** Ask user if file can be overwrite if file exist */
  @SuppressWarnings("unused")
  public boolean canBeOverwriteDialog(File file) {
    if (file == null) { return false; }
    if (!file.exists()) { return true; }
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(msgCanOverwriteTitle);
    alert.setContentText(String.format(msgCanOverwriteContent, file.getName()));
    Optional<ButtonType> result = alert.showAndWait();
    return (result.isPresent()) && (result.get() == ButtonType.OK);
  }

  /** Ask user if file can be overwrite if file exist
   * @param file file which is rewrite
   * @param defaultExtension if file haven't extension then default is add
   * @return file if user want rewrite it, or no file with this name exist
   * */
  public File canBeOverwriteDialog(File file, String defaultExtension) {
    if (file == null) { return null; }
    if ("".equals(FilenameUtils.getExtension(file.getName()))) {
      file = new File(file.getAbsoluteFile() + "." + defaultExtension);
    }
    if (!file.exists()) { return file; }
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(msgCanOverwriteTitle);
    alert.setContentText(String.format(msgCanOverwriteContent, file.getName()));
    Optional<ButtonType> result = alert.showAndWait();
    return (result.isPresent()) && (result.get() == ButtonType.OK) ? file : null;
  }

  /** Inform user about not existing file */
  public void fileNotExist(File file) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(msgFileNotExistTitle);
    alert.setHeaderText(String.format(msgFileNotExistHeader, file.getName()));
    alert.setContentText(msgFileNotExistContent);
    alert.show();
  }

  public void openWindowInCenterOfStage(Stage parentStage, Pane pane, String title) {
    Stage stage = new Stage();
    stage.setTitle(title);
    stage.setScene(new Scene(pane, pane.getPrefWidth(), pane.getPrefHeight()));
    stage.getIcons().addAll(parentStage.getIcons());
    stage.show();
    stage.setX(parentStage.getX() + (parentStage.getWidth() - stage.getWidth()) / 2);
    stage.setY(parentStage.getY() + (parentStage.getHeight() - stage.getHeight()) / 2);
  }

  /** Show unsaved object if aren't saved. if user want cancel the closing then return false, elserwhere return true
   * @param savableRegistry register which hold unsaved data
   * @return true if window/object can be closed */
  public boolean showUnsavedObjectDialog(SavableRegistry savableRegistry) {
    Set<Savable> savables = savableRegistry.dirtySavables();
    if (savables.size() == 0) { return true; }
    Dialog<?> dialog = new Dialog<>();
    dialog.setResizable(false);
    dialog.setTitle(msgNotSavedObjectsTitle);
    dialog.setHeaderText(msgNotSavedObjectsHeader);

    BorderPane pane = new BorderPane();
    pane.setPrefHeight(400);
    pane.setPrefWidth(300);
    ListView<DialogHelper.Item> listView = new ListView<>();
    listView.getItems().addAll(
        savables.stream().map(savable -> new Item(savable, true)).collect(Collectors.toList()));
    listView.setCellFactory(CheckBoxListCell.forListView(DialogHelper.Item::onProperty));
    pane.setCenter(listView);

    dialog.getDialogPane().setContent(pane);

    ButtonType btCancel = new ButtonType(button_cancel, ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType btSaveClose = new ButtonType(button_saveAndClose, ButtonBar.ButtonData.OK_DONE);
    ButtonType btClose = new ButtonType(button_closeWithoutSave);

    dialog.getDialogPane().getButtonTypes().addAll(btClose, btSaveClose, btCancel);

    Optional<?> result = dialog.showAndWait();
    if (result.isPresent()) {
      if (btCancel == result.get()) { return false; }
      if (btSaveClose == result.get()) {
        listView.getItems().stream().filter(Item::isOn).forEach(item -> item.getSavable().save());
      }
    } else { return false; }
    return true;
  }

  public static class Item implements Comparable<Item> {
    private final Savable savable;
    private final BooleanProperty onProperty = new SimpleBooleanProperty();

    public Item(@Nonnull Savable savable, boolean on) {
      this.savable = savable;
      setOn(on);
    }

    public final @Nonnull Savable getSavable() { return savable; }
    public final BooleanProperty onProperty() { return this.onProperty; }
    public final boolean isOn() { return this.onProperty().get(); }
    public final void setOn(final boolean on) { this.onProperty().set(on); }

    @Override
    public String toString() {
      return savable.displayName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Item)) return false;
      Item item = (Item) o;
      return getSavable().equals(item.getSavable());
    }

    @Override
    public int hashCode() {
      return getSavable().hashCode();
    }

    @Override
    public int compareTo(@Nonnull Item o) {
      if (!this.getClass().equals(o.getClass())) { throw new ClassCastException("The compared object must be same. Expected: "
          + getClass().getName() + " but compared object is: " + o.getClass().getName()); }
      return getSavable().displayName().compareTo(o.getSavable().displayName());
    }
  }


  /** Open dialog with chooser when user can choose single option
   * @param question question which is show to user
   * @param items list of items which user can choose
   * @param <T> type of item
   * @return null if user click on cancel or don't choose anything, elsewhere choosed item */
  public static <T> T chooseSingOption(String question, List<T> items) {
    //noinspection unchecked
    return chooseSingOption(question, (T[]) items.toArray());
  }

  /** Open dialog with chooser when user can choose single option
   * @param question question which is show to user
   * @param items list of items which user can choose
   * @param <T> type of item
   * @return null if user click on cancel or don't choose anything, elsewhere choosed item */
  @SuppressWarnings("unchecked")
  public static <T> T chooseSingOption(String question, T... items) {
    if (items.length == 0) { return null; }
    Dialog<T> dialog = new Dialog<>();
    dialog.setResizable(false);
    dialog.setTitle(chooseSingleOption_title);
    dialog.setHeaderText(question);

    ComboBox<T> comboBox = new ComboBox<>();
    comboBox.getItems().addAll(items);
    dialog.getDialogPane().setContent(comboBox);

    ButtonType btCancel = ButtonType.CANCEL;
    ButtonType btOk = ButtonType.OK;
    dialog.getDialogPane().getButtonTypes().addAll(btCancel, btOk);

    Optional<T> result = dialog.showAndWait();
    if (result.isPresent()) {
      if (btCancel == result.get()) { return null; }
      if (btOk == result.get()) {
        return comboBox.getSelectionModel().getSelectedItem();
      }
    } else { return null; }
    return result.get();
  }
}
