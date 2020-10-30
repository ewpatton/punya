// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.util.Log;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.*;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@DesignerComponent(version = YaVersion.DATA_FILE_COMPONENT_VERSION,
    description = "Component that allows reading CSV and JSON data." +
        "The DataFile contains functionality relevant to accessing " +
        "CSV or JSON parsed data in the form of rows or columns." +
        "Can be used together with the ChartData2D component to import " +
        "data directly from a file to the Chart. The component may also be " +
        "dragged and dropped on a Chart after a file has been selected and " +
        "parsed successfully to create ChartData components automatically from " +
        "the file onto the Chart.",
    category = ComponentCategory.STORAGE,
    nonVisible = true,
    iconName = "images/dataFile.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, android.permission.READ_EXTERNAL_STORAGE")
public class DataFile extends FileBase implements DataSource<YailList, Future<YailList>> {
  private String sourceFile;

  private YailList rows;
  private YailList columns;
  private YailList columnNames; // Elements of the first column

  private ExecutorService threadRunner; // Used to queue & execute asynchronous tasks

  /**
   * Creates a new DataFile component.
   *
   * @param container the Form that this component is contained in.
   */
  public DataFile(ComponentContainer container) {
    super(container);

    rows = new YailList();
    columns = new YailList();
    columnNames = new YailList();

    threadRunner = Executors.newSingleThreadExecutor();
  }

  /**
   * Retrieve a List of rows of the currently loaded Source file.
   *
   * @return a YailList representing the parsed rows of the Data file.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Returns a list of rows corresponding" +
      " to the Data File's content.")
  public YailList Rows() {
    return getYailListPropertyHelper(new Callable<YailList>() {
      @Override
      public YailList call() throws Exception {
        return rows;
      }
    });
  }


  /**
   * Retrieve a List of columns of the currently loaded Source file.
   *
   * @return a YailList representing the parsed columns of the Data file.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Returns a list of columns corresponding" +
      " to the Data File's content.")
  public YailList Columns() {
    return getYailListPropertyHelper(new Callable<YailList>() {
      @Override
      public YailList call() throws Exception {
        return columns;
      }
    });
  }

  /**
   * Retrieve the column names of the currently loaded Source file.
   * For CSV files, this will return a List of entries in the first row.
   * For JSON files, this will return a List of keys in the JSON object.
   *
   * @return a YailList containing the elements of the first row.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Returns the elements of the first row" +
      " of the Data File's contents.")
  public YailList ColumnNames() {
    return getYailListPropertyHelper(new Callable<YailList>() {
      @Override
      public YailList call() throws Exception {
        return columnNames;
      }
    });
  }

  /**
   * Helper method for retrieving YailList properties using blocking
   * calls. Used for Rows, Columns and ColumnNames properties.
   * <p>
   * The property to return has to be wrapped in a Callable and passed
   * to this method in order to return the updated variable (post-reading).
   * Passing in the property would not work as expected because the
   * variable would have to be final.
   *
   * @param propertyCallable Callable that returns the required YailList property
   * @return YailList property
   */
  private YailList getYailListPropertyHelper(Callable<YailList> propertyCallable) {
    // Since reading might be in progress, the task of
    // getting a DataFile property should be queued so that the
    // thread is blocked until the reading is finished.
    try {
      return threadRunner
          .submit(propertyCallable) // Run the callable async (and queued)
          .get(); // Get the property (blocks thread until previous threads finish)
    } catch (InterruptedException e) {
      Log.e(this.getClass().getName(), e.getMessage());
    } catch (ExecutionException e) {
      Log.e(this.getClass().getName(), e.getMessage());
    }

    return new YailList(); // Return empty list (default option)
  }

  /**
   * Sets the source file to parse data from, and then parses the
   * file asynchronously. The results are stored in the {@link #Columns()},
   * {@link #Rows()} and {@link #ColumnNames()} properties.
   * The expected formatting of the file is either the CSV or JSON format.
   *
   * @param source Source file name
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, userVisible = false)
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET,
      defaultValue = "")
  public void SourceFile(String source) {
    // The SourceFile property is only set from the Designer, so the
    // only possibility is a media file. Since media file paths are
    // distinguished by double slashes at the beginning, they need
    // to be added.
    ReadFile("//" + source);
  }

  @SimpleFunction(description = "Indicates source file to load data from." +
      "The expected format of the contents of the file are either CSV or JSON." +
      "Prefix the filename with / to read from a specific file on the SD card. " +
      "for instance /myFile.txt will read the file /sdcard/myFile.txt. To read " +
      "assets packaged with an application (also works for the Companion) start " +
      "the filename with // (two slashes). If a filename does not start with a " +
      "slash, it will be read from the applications private storage (for packaged " +
      "apps) and from /sdcard/AppInventor/data for the Companion." +
      "The results of the reading are stored in the Rows, Columns " +
      "and ColumnNames properties of the component.")
  public void ReadFile(String source) {
    this.sourceFile = source;

    readFromFile(sourceFile);
  }

  /**
   * Gets the specified column's elements as a YailList.
   *
   * @param column name of column
   * @return YailList of elements in the column
   */
  public YailList getColumn(String column) {
    // Get the index of the column (first row - column names)
    // 1 is subtracted from the index since YailList indexOf
    // returns an index that is 1-based.
    int index = columnNames.indexOf(column) - 1;

    // Column not found
    if (index < 0) {
      return new YailList();
    }

    return (YailList) columns.getObject(index);
  }

  @Override
  protected void AsyncRead(final InputStream inputStream, final String fileName) {
    // Add runnable to the Single Thread runner to read File asynchronously
    threadRunner.execute(new Runnable() {
      @Override
      public void run() {
        try {
          // Parse InputStream to String
          final String result = readFromInputStream(inputStream);

          // First character is a curly bracket; Assume JSON
          // TODO: When fetching columns and rows, in the case of
          // TODO: colums/rows being uneven lengths, the final rows and columns
          // TODO: objects will differ (the transpose will fill missing entries
          // TODO: with blank empty String entries, while the original List will
          // TODO: have uneven sized Lists. For consistency, this should be
          // TODO: handled, but currently there is a bit too much overhead in doing
          // TODO: so due to YailLists not supporting the add() operation)
          if (result.charAt(0) == '{') {
            try {
              // Parse columns from the result
              columns = JsonUtil.getColumnsFromJSON(result);

              // Construct row lists from columns
              rows = ChartDataSourceUtil.getTranspose(columns);
            } catch (JSONException e) {
              // JSON parsing failed; Fallback to CSV
              rows = CsvUtil.fromCsvTable(result);
              columns = ChartDataSourceUtil.getTranspose(rows);
            }
          } else { // Assume CSV otherwise
            // Parse rows from the result
            rows = CsvUtil.fromCsvTable(result);

            // Construct column lists from rows
            columns = ChartDataSourceUtil.getTranspose(rows);
          }

          // If rows size is non-zero, set column names to first row. Otherwise,
          // set it to an empty List.
          columnNames = (rows.size() > 0) ? ((YailList) rows.getObject(0)) : new YailList();
        } catch (IOException e) {
          Log.e(this.getClass().getName(), e.getMessage());
        } catch (Exception e) {
          Log.e(this.getClass().getName(), e.getMessage());
        }
      }
    });
  }

  /**
   * Returns a Future object which holds the DataFile columns at the point
   * of invoking the method.
   * <p>
   * If reading is in progress, the method blocks until reading is done
   * before returning the result.
   * <p>
   * The method should be called asynchronously to prevent freezing of
   * the main thread.
   * <p>
   * The row size is contained in the method to create default values for the
   * CharDataModel in case of an absence of columns.
   *
   * @param columns List of columns to retrieve (String object entries expected)
   * @return Future object containing YailList of format (rowCount, columns)
   */
  @Override
  public Future<YailList> getDataValue(final YailList columns) {
    // Submit a callable which constructs the results.
    // The callable is only executed after all the previous
    // tasks have been completed.
    return threadRunner.submit(new Callable<YailList>() {
      @Override
      public YailList call() {
        ArrayList<YailList> resultingColumns = new ArrayList<YailList>();

        // Iterate over the specified column names
        for (int i = 0; i < columns.size(); ++i) {
          // Get and add the specified column to the resulting columns list
          String columnName = columns.getString(i);
          YailList column = getColumn(columnName);
          resultingColumns.add(column);
        }

        // Convert result to a YailList and return it
        YailList csvColumns = YailList.makeList(resultingColumns);
        return csvColumns;
      }
    });
  }
}
