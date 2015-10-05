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

import javafx.application.Platform;
import org.controlsfx.control.StatusBar;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 2.10.15.
 * System which help to show status messages and progress */
@SuppressWarnings("unused")
public class StatusHelper {

  private static final StatusHelper instance = new StatusHelper();
  public static StatusHelper getInstance() { return instance; }

  private StatusBar statusBar;
  public StatusBar getStatusBar() { return statusBar; }
  public void setStatusBar(StatusBar statusBar) { this.statusBar = statusBar; }

  public void setProgress(double progress) {
    if (statusBar != null) {
      Platform.runLater(() -> statusBar.setProgress(progress));
    }
  }

  /** Show message which is given */
  public void showMessage(String message) {
    if (statusBar != null) {
      Platform.runLater(() -> statusBar.setText(message));
    }
  }

  private final Map<Object, String> progressNames = new ConcurrentHashMap<>();
  private final Map<Object, Double> progressSteps = new ConcurrentHashMap<>();
  private final Map<Object, Double> progressStep = new ConcurrentHashMap<>();
  private final Map<Object, Double> progressStepRows = new ConcurrentHashMap<>();
  private final Map<Object, Double> progressStepRow = new ConcurrentHashMap<>();

  /** Start progress set name of this progress and steps which this progress have */
  public void progressStart(Object key, String name, double steps) {
    progressNames.put(key, name);
    progressSteps.put(key, steps);
    progressStep.put(key, 0.0);
    progressStepRows.put(key, 0.0);
    progressStepRow.put(key, 0.0);
  }
  /** Set next step and count of rows in the new step */
  public void progressNextStep(Object key, String name, double rows) {
    progressStep.put(key, progressStep.get(key) + 1);
    StatusHelper.getInstance().showMessage(progressNames.get(key) + ": " + name);
    StatusHelper.getInstance().setProgress((progressStep.get(key) - 1) / progressSteps.get(key));
    progressStepRows.put(key, rows);
    progressStepRow.put(key, 0.0);
  }
  /** Progress in current phase step increment by 1 */
  public void progress(Object key) {
    double row = progressStepRow.get(key);
    row++;
    double rows = progressStepRows.get(key);
    if (row > rows) { rows++; }
    StatusHelper.getInstance().setProgress((progressStep.get(key) - 1 + row / rows)
        / progressSteps.get(key));
    progressStepRows.put(key, rows);
    progressStepRow.put(key, row);
  }
  /** Finish the progress */
  public void progressFinish(Object key, String message) {
    StatusHelper.getInstance().setProgress(1);
    StatusHelper.getInstance().showMessage(progressNames.get(key) + ": " + message);
    progressNames.remove(key);
    progressSteps.remove(key);
    progressStep.remove(key);
    progressStepRows.remove(key);
    progressStepRow.remove(key);
  }
}
