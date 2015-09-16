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

import cz.lbenda.common.Tuple2;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 16.9.15.
 *
 */
public class ExceptionMessageFrmController {

  private static Logger LOG = LoggerFactory.getLogger(ExceptionMessageFrmController.class);

  private TextArea stackTrace;

  @Message
  public String msgAppErrorHeader = "Error in application";

  public ExceptionMessageFrmController() {
    MessageFactory.initializeMessages(this);
    stackTrace = new TextArea();
  }

  /** Create new instance return main node and controller of this node and subnodes */
  public static Tuple2<Parent, ExceptionMessageFrmController> createNewInstance() {
    ExceptionMessageFrmController controller = new ExceptionMessageFrmController();
    return new Tuple2<>(controller.stackTrace, controller);
  }

  public static void showException(final String message, Throwable e) {
    Dialog<Object> dialog = new Dialog<>();
    dialog.setResizable(true);
    final Tuple2<Parent, ExceptionMessageFrmController> tuple = createNewInstance();
    if (tuple == null) { return; }
    ExceptionMessageFrmController controller = tuple.get2();
    dialog.setTitle(controller.msgAppErrorHeader);
    dialog.setHeaderText(message);
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    controller.stackTrace.setText(sw.toString());

    dialog.getDialogPane().setContent(tuple.get1());
    ButtonType buttonTypeOk = ButtonType.OK;
    dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
    dialog.getDialogPane().setPadding(new Insets(0, 0, 0, 0));

    Optional<Object> result = dialog.showAndWait();
  }

  public static void showException(final Throwable exception) {
    Dialog<Object> dialog = new Dialog<>();
    dialog.setResizable(true);
    final Tuple2<Parent, ExceptionMessageFrmController> tuple = createNewInstance();
    if (tuple == null) { return; }
    ExceptionMessageFrmController controller = tuple.get2();
    dialog.setTitle(controller.msgAppErrorHeader);
    dialog.setHeaderText(exception.getMessage());
    StringWriter sw = new StringWriter();
    exception.printStackTrace(new PrintWriter(sw));
    controller.stackTrace.setText(sw.toString());

    dialog.getDialogPane().setContent(tuple.get1());
    ButtonType buttonTypeOk = ButtonType.OK;
    dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
    dialog.getDialogPane().setPadding(new Insets(0, 0, 0, 0));

    Optional<Object> result = dialog.showAndWait();
  }
}
