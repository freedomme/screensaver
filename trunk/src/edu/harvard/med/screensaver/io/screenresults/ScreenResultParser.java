// $HeadURL:
// svn+ssh://ant4@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/src/edu/harvard/med/screensaver/io/screenresults/ScreenResultParser.java
// $
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.io.screenresults;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.CellType;
import jxl.Sheet;

import edu.harvard.med.screensaver.db.LibrariesDAO;
import edu.harvard.med.screensaver.io.workbook2.Cell;
import edu.harvard.med.screensaver.io.workbook2.CellValueParser;
import edu.harvard.med.screensaver.io.workbook2.CellVocabularyParser;
import edu.harvard.med.screensaver.io.workbook2.ParseErrorManager;
import edu.harvard.med.screensaver.io.workbook2.PlateNumberParser;
import edu.harvard.med.screensaver.io.workbook2.WellNameParser;
import edu.harvard.med.screensaver.io.workbook2.Workbook;
import edu.harvard.med.screensaver.io.workbook2.WorkbookParseError;
import edu.harvard.med.screensaver.io.workbook2.Cell.Factory;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.screenresults.AssayWellType;
import edu.harvard.med.screensaver.model.screenresults.PartitionedValue;
import edu.harvard.med.screensaver.model.screenresults.PositiveIndicatorDirection;
import edu.harvard.med.screensaver.model.screenresults.PositiveIndicatorType;
import edu.harvard.med.screensaver.model.screenresults.ResultValue;
import edu.harvard.med.screensaver.model.screenresults.ResultValueType;
import edu.harvard.med.screensaver.model.screenresults.ResultValueTypeNumericalnessException;
import edu.harvard.med.screensaver.model.screenresults.ScreenResult;
import edu.harvard.med.screensaver.model.screens.AssayReadoutType;
import edu.harvard.med.screensaver.model.screens.Screen;
import edu.harvard.med.screensaver.model.screens.ScreeningRoomActivity;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.math.Range;
import org.apache.log4j.Logger;

/**
 * Parses data from a workbook files (a.k.a. Excel spreadsheets) necessary for
 * instantiating a
 * {@link edu.harvard.med.screensaver.model.screenresults.ScreenResult}.
 * ScreenResult data consists of both "data headers" and "raw" data. By
 * convention, each worksheet contains the raw data for a single plate, but the
 * parser is indifferent to how data may be arranged across worksheets.
 * <p>
 * The data header info is used to instantiate
 * {@link edu.harvard.med.screensaver.model.screenresults.ResultValueType}
 * objects, while the raw data is used to instantiate each of the
 * <code>ResultValueType</code>s'
 * {@link edu.harvard.med.screensaver.model.screenresults.ResultValue} objects.
 * Altogether these objects are used instantiate a {@link ScreenResult} object,
 * which is the returned result of the {@link #parse} method.
 * <p>
 * The class attempts to parse the file(s) as fully as possible, carrying on in
 * the face of errors, in order to catch as many errors as possible, as this
 * will aid the manual effort of correcting the files' format and content
 * between import attempts. By calling {@link #getErrorAnnotatedWorkbook()}, a
 * new error-annotated workbook will be generated (in memory only), containing
 * errors messages in each cell that encountered an error during parsing. Error
 * messages that are not cell-specific will be written to a new "Parse Errors"
 * sheet in the error-annotated workbook.
 * <p>
 * Each call to {@link #parse} will clear the errors accumulated from the
 * previous call, and so the result of calling {@link #getErrors()} will change
 * after each call to {@link #parse}.
 *
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 */
public class ScreenResultParser implements ScreenResultWorkbookSpecification
{

  // static data members

  private static final Logger log = Logger.getLogger(ScreenResultParser.class);
  private static final Logger memoryDebugLog = Logger.getLogger(ScreenResultParser.class + "memoryDebug");

  private static final String NO_SCREEN_ID_FOUND_ERROR = "Screen ID not found";
  private static final String DATA_HEADER_SHEET_NOT_FOUND_ERROR = "\"Data Headers\" sheet not found";
  private static final String UNKNOWN_ERROR = "unknown error";
  private static final String NO_DATA_SHEETS_FOUND_ERROR = "no data worksheets were found; no result data was imported";
  private static final String NO_SUCH_WELL = "library well does not exist";
  private static final String NO_SUCH_LIBRARY_WITH_PLATE = "no library with given plate number";

  private static final int RELOAD_WORKBOOK_AFTER_SHEET_COUNT = 32;

  private static SortedMap<String,AssayReadoutType> assayReadoutTypeMap = new TreeMap<String,AssayReadoutType>();
  private static SortedMap<String,PositiveIndicatorDirection> indicatorDirectionMap = new TreeMap<String,PositiveIndicatorDirection>();
  private static SortedMap<String,PositiveIndicatorType> activityIndicatorTypeMap = new TreeMap<String,PositiveIndicatorType>();
  private static SortedMap<String,Boolean> rawOrDerivedMap = new TreeMap<String,Boolean>();
  private static SortedMap<String,Boolean> primaryOrFollowUpMap = new TreeMap<String,Boolean>();
  private static SortedMap<String,Boolean> booleanMap = new TreeMap<String,Boolean>();
  private static SortedMap<String,PartitionedValue> partitionedValueMap = new TreeMap<String,PartitionedValue>();
  private static SortedMap<String,AssayWellType> assayWellTypeMap = new TreeMap<String,AssayWellType>();
  static {
    for (AssayReadoutType assayReadoutType : AssayReadoutType.values()) {
      assayReadoutTypeMap.put(assayReadoutType.getValue(),
                              assayReadoutType);
    }

    indicatorDirectionMap.put(NUMERICAL_INDICATOR_DIRECTION_LOW_VALUES_INDICATE, PositiveIndicatorDirection.LOW_VALUES_INDICATE);
    indicatorDirectionMap.put(NUMERICAL_INDICATOR_DIRECTION_HIGH_VALUES_INDICATE, PositiveIndicatorDirection.HIGH_VALUES_INDICATE);

    activityIndicatorTypeMap.put("Numeric", PositiveIndicatorType.NUMERICAL);
    activityIndicatorTypeMap.put("Numerical", PositiveIndicatorType.NUMERICAL);
    activityIndicatorTypeMap.put("Boolean", PositiveIndicatorType.BOOLEAN);
    activityIndicatorTypeMap.put("Partitioned", PositiveIndicatorType.PARTITION);
    activityIndicatorTypeMap.put("Partition", PositiveIndicatorType.PARTITION);

    rawOrDerivedMap.put("", false);
    rawOrDerivedMap.put(RAW_VALUE, false);
    rawOrDerivedMap.put(DERIVED_VALUE, true);

    primaryOrFollowUpMap.put("", false);
    primaryOrFollowUpMap.put(PRIMARY_VALUE, false);
    primaryOrFollowUpMap.put(FOLLOWUP_VALUE, true);

    booleanMap.put("", false);
    booleanMap.put("false", false);
    booleanMap.put("no", false);
    booleanMap.put("n", false);
    booleanMap.put("0", false);
    booleanMap.put("true", true);
    booleanMap.put("yes", true);
    booleanMap.put("y", true);
    booleanMap.put("1", true);

    for (PartitionedValue pv : PartitionedValue.values()) {
      partitionedValueMap.put(pv.getDisplayValue().toLowerCase(), pv);
      partitionedValueMap.put(pv.getDisplayValue().toUpperCase(), pv);
      partitionedValueMap.put(pv.getValue(), pv);
    }

    assayWellTypeMap.put(AssayWellType.EXPERIMENTAL.getAbbreviation(), AssayWellType.EXPERIMENTAL);
    assayWellTypeMap.put(AssayWellType.EMPTY.getAbbreviation(), AssayWellType.EMPTY);
    assayWellTypeMap.put(AssayWellType.LIBRARY_CONTROL.getAbbreviation(), AssayWellType.LIBRARY_CONTROL);
    assayWellTypeMap.put(AssayWellType.ASSAY_POSITIVE_CONTROL.getAbbreviation(), AssayWellType.ASSAY_POSITIVE_CONTROL);
    assayWellTypeMap.put(AssayWellType.ASSAY_CONTROL.getAbbreviation(), AssayWellType.ASSAY_CONTROL);
    assayWellTypeMap.put(AssayWellType.BUFFER.getAbbreviation(), AssayWellType.BUFFER);
    assayWellTypeMap.put(AssayWellType.DMSO.getAbbreviation(), AssayWellType.DMSO);
    assayWellTypeMap.put(AssayWellType.OTHER.getAbbreviation(), AssayWellType.OTHER);
    assert assayWellTypeMap.size() == AssayWellType.values().length : "assayWellTypeMap not initialized properly";
  }


  // static methods


  /**
   * The ScreenResult object to be populated with data parsed from the spreadsheet.
   */
  private ScreenResult _screenResult;
  private ParseErrorManager _errors = new ParseErrorManager(); // init at instantiation, to avoid NPE from various method calls before parse() is called
  private SortedMap<String,ResultValueType> _dataTableColumnLabel2RvtMap;

  private ColumnLabelsParser _columnsDerivedFromParser;
  private ExcludeParser _excludeParser;
  private CellVocabularyParser<AssayReadoutType> _assayReadoutTypeParser;
  private CellVocabularyParser<PositiveIndicatorDirection> _indicatorDirectionParser;
  private CellVocabularyParser<PositiveIndicatorType> _activityIndicatorTypeParser;
  private CellVocabularyParser<Boolean> _rawOrDerivedParser;
  private CellVocabularyParser<Boolean> _primaryOrFollowUpParser;
  private CellVocabularyParser<Boolean> _booleanParser;
  private CellVocabularyParser<PartitionedValue> _partitionedValueParser;
  private CellVocabularyParser<AssayWellType> _assayWellTypeParser;
  private PlateNumberParser _plateNumberParser;
  private WellNameParser _wellNameParser;

  private LibrariesDAO _librariesDao;
  private Factory _dataHeadersCellParserFactory;
  private Factory _dataCellParserFactory;
  private Map<Integer,Short> _dataHeaderIndex2DataHeaderColumn;
  private Set<Library> _preloadedLibraries;
  /**
   * The library that was associated with the plate that was last accessed.
   * @motivation optimization for findLibraryWithPlate(); reduce db I/O
   */
  private Library _lastLibrary;



  // public methods and constructors

  public ScreenResultParser(LibrariesDAO librariesDao)
  {
    _librariesDao = librariesDao;
  }

  /**
   * Parses the specified workbook file that contains Screen Result data in the
   * <a
   * href="https://wiki.med.harvard.edu/ICCBL/NewScreenResultFileFormat">"new"
   * format</a>. Errors encountered during parsing are stored with this object
   * until a parse() method is called again, and these errors can be retrieved
   * via {@link #getErrors}. The returned <code>ScreenResult</code> may only
   * be partially populated if errors are encountered, so always call
   * getErrors() to determine parsing success.
   * 
   * @param screen the parent Screen of the Screen Result being parsed
   * @param workbookFile the workbook file to be parsed
   * @param the range of plate numbers to be parsed, allowing for only a subset
   *          of the data to be imported. This may be required for resource
   *          utilization purposes, where the ScreenResult must be imported over
   *          multiple passes. If null, well data for all plate will be imported.
   * @return a ScreenResult object containing the data parsed from the workbook
   *         file; <code>null</code> if a fatal error occurs (e.g. file not
   *         found)
   * @throws FileNotFoundException
   * @see #getErrors()
   */
  public ScreenResult parse(Screen screen, File workbookFile, IntRange plateNumberRange)
  {
    try {
      return doParse(screen,
                     workbookFile,
                     new BufferedInputStream(new FileInputStream(workbookFile)),
                     plateNumberRange);
    }
    catch (FileNotFoundException e) {
      _errors.addError("input file not found: " + e.getMessage());
    }
    return null;
  }

  public ScreenResult parse(Screen screen, File workbookFile)
  {
    return parse(screen, workbookFile, null);
  }

  /**
   * Parses the specified workbook file that contains Screen Result data. Errors
   * encountered during parsing are stored with this object until a parse()
   * method is called again, and these errors can be retrieved via
   * {@link #getErrors}. The returned <code>ScreenResult</code> may only be
   * partially populated if errors are encountered, so always call getErrors()
   * to determine parsing success.
   *
   * @param screen the parent Screen of the Screen Result being parsed
   * @param inputSourceName the name of the workbook file to be parsed; if
   *          inputStream is null, will be used to obtain the workbook file,
   *          otherwise just used to hold a name for display/output purposes; if
   *          named file does not actually exist, inputStream must not be null
   * @param inputStream an InputStream that provides the workbook file via an
   *          InputStream
   * @return a ScreenResult object containing the data parsed from the workbook
   *         file; <code>null</code> if a fatal error occurs
   * @see #getErrors()
   * @motivation For use by the web application UI; the InputStream allows us to
   *             avoid making (another) temporary copy of the file.
   */
  public ScreenResult parse(Screen screen, 
                            String inputSourceName, 
                            InputStream inputStream)
  {
    return doParse(screen,
                   new File(inputSourceName),
                   inputStream,
                   null);
  }

  private ScreenResult doParse(Screen screen,
                               File workbookFile,
                               InputStream workbookInputStream,
                               IntRange plateNumberRange)
  {
    _screenResult = null;
    _errors = new ParseErrorManager();
    _preloadedLibraries = new HashSet<Library>();
    _lastLibrary = null;
    _assayReadoutTypeParser = new CellVocabularyParser<AssayReadoutType>(assayReadoutTypeMap, _errors);
    _dataTableColumnLabel2RvtMap = new TreeMap<String,ResultValueType>();
    _columnsDerivedFromParser = new ColumnLabelsParser(_dataTableColumnLabel2RvtMap, _errors);
    _excludeParser = new ExcludeParser(_dataTableColumnLabel2RvtMap, _errors);
    _indicatorDirectionParser = new CellVocabularyParser<PositiveIndicatorDirection>(indicatorDirectionMap, _errors);
    _activityIndicatorTypeParser = new CellVocabularyParser<PositiveIndicatorType>(activityIndicatorTypeMap, PositiveIndicatorType.NUMERICAL, _errors);
    _rawOrDerivedParser = new CellVocabularyParser<Boolean>(rawOrDerivedMap, Boolean.FALSE, _errors);
    _primaryOrFollowUpParser = new CellVocabularyParser<Boolean>(primaryOrFollowUpMap, Boolean.FALSE, _errors);
    _booleanParser = new CellVocabularyParser<Boolean>(booleanMap, Boolean.FALSE, _errors);
    _partitionedValueParser = new CellVocabularyParser<PartitionedValue>(partitionedValueMap, PartitionedValue.NONE, _errors);
    _assayWellTypeParser = new CellVocabularyParser<AssayWellType>(assayWellTypeMap, AssayWellType.EXPERIMENTAL, _errors);
    _plateNumberParser = new PlateNumberParser(_errors);
    _wellNameParser = new WellNameParser(_errors);
    try {
      Workbook workbook = new Workbook(workbookFile, workbookInputStream, _errors);
      log.info("parsing " + workbookFile.getAbsolutePath());
      if (screen.getScreenResult() == null) {
        DataHeadersParseResult dataHeadersParseResult = parseDataHeaders(screen, workbook);
        if (_errors.getHasErrors()) {
          log.info("errors found in data headers, will not attempt to parse data sheets");
        } else {
          _screenResult = dataHeadersParseResult.getScreenResult();
        }
      }
      else {
        _screenResult = screen.getScreenResult();
      }

      if (_screenResult != null) {
        initializeDataHeaders(_screenResult, workbook);
        log.info("parsing data sheets");
        parseData(workbook,
                  _screenResult,
                  plateNumberRange);
      }
    }
    catch (UnrecoverableScreenResultParseException e) {
      _errors.addError("serious parse error encountered (could not continue further parsing): " + e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
      String errorMsg = UNKNOWN_ERROR + " of type : " + e.getClass() + ": " + e.getMessage();
      _errors.addError(errorMsg);
    }
    finally {
      IOUtils.closeQuietly(workbookInputStream);
    }
    return _screenResult;
  }

  public boolean getHasErrors()
  {
    return _errors.getHasErrors();
  }

  /**
   * Return all errors the were detected during parsing. This class attempts to
   * parse as much of the workbook as possible, continuing on after finding an
   * error. The hope is that multiple errors will help a user/administrator
   * correct a workbook's errors in a batch fashion, rather than in a piecemeal
   * fashion.
   *
   * @return a <code>List&lt;String&gt;</code> of all errors generated during
   *         parsing
   */
  public List<WorkbookParseError> getErrors()
  {
    return _errors.getErrors();
  }

  /**
   * Get an annotated copy of the parsed workbook with parse errors. Cells with
   * errors will have a red background and error message will be contained in
   * the cell's comment field.
   *
   * @throws IOException
   */
  public jxl.write.WritableWorkbook getErrorAnnotatedWorkbook() throws IOException
  {
    return _errors.getErrorAnnotatedWorkbook();
  }

  public ScreenResult getParsedScreenResult()
  {
    return _screenResult;
  }


  // private methods

  /**
   * Initialize the 'Data Headers' worksheet and related data members.
   *
   * @throws UnrecoverableScreenResultParseException if 'Data Headers' worksheet could
   *           not be initialized or does not appear to be valid
   */
  private Sheet initializeDataHeadersSheet(Workbook workbook)
    throws UnrecoverableScreenResultParseException
  {
    // find the "Data Headers" sheet
    int dataHeadersSheetIndex;
    try {
      dataHeadersSheetIndex = workbook.findSheetIndex(DATA_HEADERS_SHEET_NAME);
    }
    catch (IllegalArgumentException e) {
      throw new UnrecoverableScreenResultParseException(
                                                        DATA_HEADER_SHEET_NOT_FOUND_ERROR,
                                                        workbook);
    }
    Sheet dataHeadersSheet = workbook.getWorkbook().getSheet(dataHeadersSheetIndex);

    // at this point, we know we have a valid workbook, to which we can
    // append errors
    _errors.setWorbook(workbook);
    _dataHeadersCellParserFactory = new Cell.Factory(workbook,
                                                     dataHeadersSheetIndex,
                                                     _errors);
    return dataHeadersSheet;
  }

  /**
   * Finds the total number of data header columns.
   *
   * @param dataHeadersSheet
   * @throws UnrecoverableScreenResultParseException
   */
  private int findDataHeaderColumnCount(Sheet dataHeadersSheet) throws UnrecoverableScreenResultParseException
  {
    int rows = dataHeadersSheet.getRows();
    if (DataHeaderRow.COLUMN_IN_DATA_WORKSHEET.getRowIndex() >= rows) {
      return 0;
    }
    int iCol = 1; // skip label column
    int n = 0;
    jxl.Cell[] row = dataHeadersSheet.getRow(DataHeaderRow.COLUMN_IN_DATA_WORKSHEET.getRowIndex());
    while (iCol < row.length && !row[iCol].getType().equals(CellType.EMPTY)) {
      ++n;
      ++iCol;
    }
    return n;
  }

  /**
   * Prepares the _dataCellParserFactory to return Cells from the specified sheet.
   * @param workbook
   * @param sheetIndex

   */
  private Sheet initializeDataSheet(final Workbook workbook, int sheetIndex)
  {
    Sheet dataSheet = workbook.getWorkbook().getSheet(sheetIndex);
    _dataCellParserFactory = new Cell.Factory(workbook,
                                              sheetIndex,
                                              _errors);
    return dataSheet;
  }


  private Cell dataHeadersCell(DataHeaderRow row, int dataHeader, boolean isRequired)
  {
    return _dataHeadersCellParserFactory.getCell((short) (DATA_HEADERS_FIRST_DATA_HEADER_COLUMN_INDEX + dataHeader),
                                                 row.getRowIndex(),
                                                 isRequired);
  }

  private Cell dataHeadersCell(DataHeaderRow row, int dataHeader)
  {
    return dataHeadersCell(row, dataHeader, /*required=*/false);
  }

  private Cell dataCell(int row, DataColumn column)
  {
    return dataCell(row, column, false);
  }

  private Cell dataCell(int row, DataColumn column, boolean isRequired)
  {
    return _dataCellParserFactory.getCell((short) column.ordinal(), row, isRequired);
  }

  private Cell dataCell(int row, int iDataHeader)
  {
    return _dataCellParserFactory.getCell((short) (_dataHeaderIndex2DataHeaderColumn.get(iDataHeader)),
                                          row);
  }


  private static class DataHeadersParseResult
  {
    private ScreenResult _screenResult;
    private ArrayList<Workbook> _rawDataWorkbooks;

    public ArrayList<Workbook> getRawDataWorkbooks()
    {
      return _rawDataWorkbooks;
    }

    public void setRawDataWorkbooks(ArrayList<Workbook> rawDataWorkbooks)
    {
      _rawDataWorkbooks = rawDataWorkbooks;
    }

    public ScreenResult getScreenResult()
    {
      return _screenResult;
    }

    public void setScreenResult(ScreenResult screenResult)
    {
      _screenResult = screenResult;
    }
  }

  /**
   * Parse the worksheet containing the ScreenResult data headers
   * @param workbook
   * @throws UnrecoverableScreenResultParseException
   */
  private DataHeadersParseResult parseDataHeaders(Screen screen,
                                                  Workbook workbook)
    throws UnrecoverableScreenResultParseException
  {
    DataHeadersParseResult dataHeadersParseResult = new DataHeadersParseResult();
    Sheet dataHeadersSheet = initializeDataHeadersSheet(workbook);
    ParsedScreenInfo parsedScreenInfo = parseScreenInfo(workbook, screen);

    ScreenResult screenResult = screen.createScreenResult(parsedScreenInfo.getDateCreated());
    dataHeadersParseResult.setScreenResult(screenResult);
    int dataHeaderCount = findDataHeaderColumnCount(dataHeadersSheet);
    for (int iDataHeader = 0; iDataHeader < dataHeaderCount; ++iDataHeader) {
      ResultValueType rvt = screenResult.createResultValueType(
        dataHeadersCell(DataHeaderRow.NAME, iDataHeader, true).getString(),
        dataHeadersCell(DataHeaderRow.REPLICATE, iDataHeader).getInteger(),
        _rawOrDerivedParser.parse(dataHeadersCell(DataHeaderRow.RAW_OR_DERIVED, iDataHeader)),
        _booleanParser.parse(dataHeadersCell(DataHeaderRow.IS_ASSAY_ACTIVITY_INDICATOR, iDataHeader)),
        _primaryOrFollowUpParser.parse(dataHeadersCell(DataHeaderRow.PRIMARY_OR_FOLLOWUP, iDataHeader)),
        dataHeadersCell(DataHeaderRow.ASSAY_PHENOTYPE, iDataHeader).getString());
      rvt.setDescription(dataHeadersCell(DataHeaderRow.DESCRIPTION, iDataHeader).getString());
      rvt.setTimePoint(dataHeadersCell(DataHeaderRow.TIME_POINT, iDataHeader).getString());
      if (rvt.isDerived()) {
        for (ResultValueType resultValueType : _columnsDerivedFromParser.parseList(dataHeadersCell(DataHeaderRow.COLUMNS_DERIVED_FROM,
                                                                                                   iDataHeader,
                                                                                                   true))) {
          if (resultValueType != null) { // can be null if unparsable value is encountered in list
            rvt.addTypeDerivedFrom(resultValueType);
          }
        }
        rvt.setHowDerived(dataHeadersCell(DataHeaderRow.HOW_DERIVED, iDataHeader, true).getString());
        // TODO: should warn if these values *are* defined and !isDerivedFrom()
      }
      else {
        rvt.setAssayReadoutType(_assayReadoutTypeParser.parse(dataHeadersCell(DataHeaderRow.ASSAY_READOUT_TYPE, iDataHeader, true)));
      }
      if (rvt.isPositiveIndicator()) {
        rvt.setPositiveIndicatorType(_activityIndicatorTypeParser.parse(dataHeadersCell(DataHeaderRow.ACTIVITY_INDICATOR_TYPE, iDataHeader, true)));
        if (rvt.getPositiveIndicatorType().equals(PositiveIndicatorType.NUMERICAL)) {
          rvt.setPositiveIndicatorDirection(_indicatorDirectionParser.parse(dataHeadersCell(DataHeaderRow.NUMERICAL_INDICATOR_DIRECTION, iDataHeader, true)));
          rvt.setPositiveIndicatorCutoff(dataHeadersCell(DataHeaderRow.NUMERICAL_INDICATOR_CUTOFF, iDataHeader, true).getDouble());
        }
        // TODO: should warn if these values *are* defined and !isActivityIndicator()
      }
      rvt.setComments(dataHeadersCell(DataHeaderRow.COMMENTS, iDataHeader).getString());
      _dataTableColumnLabel2RvtMap.put(dataHeadersCell(DataHeaderRow.COLUMN_IN_DATA_WORKSHEET, iDataHeader, true).getAsString(), rvt);
    }
    return dataHeadersParseResult;
  }

  private void initializeDataHeaders(ScreenResult screenResult,
                                     Workbook workbook)
    throws UnrecoverableScreenResultParseException
  {
    Sheet dataHeadersSheet = initializeDataHeadersSheet(workbook);
    _dataHeaderIndex2DataHeaderColumn = new HashMap<Integer,Short>();

    int dataHeaderCount = findDataHeaderColumnCount(dataHeadersSheet);
    for (int iDataHeader = 0; iDataHeader < dataHeaderCount; ++iDataHeader) {
      Cell cell = dataHeadersCell(DataHeaderRow.COLUMN_IN_DATA_WORKSHEET, iDataHeader, true);
      String forColumnInRawDataWorksheet = cell.getString().trim();
      try {
        if (forColumnInRawDataWorksheet != null) {
          _dataHeaderIndex2DataHeaderColumn.put(iDataHeader,
                                                (short) Cell.columnLabelToIndex(forColumnInRawDataWorksheet));
          ResultValueType rvt = screenResult.getResultValueTypesList().get(iDataHeader);
          _dataTableColumnLabel2RvtMap.put(forColumnInRawDataWorksheet, rvt);
        }
      }
      catch (IllegalArgumentException e) {
        _errors.addError(e.getMessage(), cell);
      }
    }
  }

  /**
   * Parse the workbook containing the ScreenResult data.
   *
   * @param workbook the workbook containing some or all of the raw data for a
   *          ScreenResult
   * @throws ExtantLibraryException if an existing Well entity cannot be found
   *           in the database
   * @throws IOException
   * @throws UnrecoverableScreenResultParseException
   */
  private void parseData(
    Workbook workbook,
    ScreenResult screenResult,
    IntRange plateNumberRange)
    throws ExtantLibraryException, IOException, UnrecoverableScreenResultParseException
  {
    int wellsWithDataLoaded = 0;
    int dataSheetsParsed = 0;
    int totalSheets = workbook.getWorkbook().getNumberOfSheets();
    int totalDataSheets = totalSheets - FIRST_DATA_SHEET_INDEX;
    for (int iSheet = FIRST_DATA_SHEET_INDEX; iSheet < totalSheets; ++iSheet) {
      String sheetName = workbook.getWorkbook().getSheet(iSheet).getName();
      log.info("parsing sheet " + (dataSheetsParsed + 1) + " of " + totalDataSheets + ", " + sheetName);
      initializeDataSheet(workbook, iSheet);
      DataRowIterator rowIter = new DataRowIterator(workbook, iSheet);

      int iDataHeader = 0;
      for (ResultValueType rvt : screenResult.getResultValueTypes()) {
        determineNumericalnessOfDataHeader(rvt, workbook, iSheet, iDataHeader++);
      }

      while (rowIter.hasNext()) {
        Integer iRow = rowIter.next();
        WellKey wellKey = rowIter.getWellKey();
        if (plateNumberRange == null || plateNumberRange.containsInteger(wellKey.getPlateNumber())) { 
          Well well = rowIter.getWell();
          AssayWellType assayWellType = _assayWellTypeParser.parse(dataCell(iRow, DataColumn.ASSAY_WELL_TYPE));
          List<ResultValueType> wellExcludes = _excludeParser.parseList(dataCell(iRow, DataColumn.EXCLUDE));
          iDataHeader = 0;
          for (ResultValueType rvt : screenResult.getResultValueTypes()) {
            Cell cell = dataCell(iRow, iDataHeader);
            boolean isExclude = (wellExcludes != null && wellExcludes.contains(rvt));
            try {
              ResultValue newResultValue = null;
              if (rvt.isPositiveIndicator()) {
                String value;
                if (rvt.getPositiveIndicatorType() == PositiveIndicatorType.BOOLEAN) {
                  if (cell.isBoolean()) {
                    value = cell.getBoolean().toString();
                  }
                  else {
                    value = _booleanParser.parse(cell).toString();
                  }
                  newResultValue =
                    rvt.createResultValue(well,
                                          assayWellType,
                                          value,
                                          isExclude);
                }
                else if (rvt.getPositiveIndicatorType() == PositiveIndicatorType.PARTITION) {
                  newResultValue =
                    rvt.createResultValue(well,
                                          assayWellType,
                                          _partitionedValueParser.parse(cell).toString(),
                                          isExclude);
                }
                else if (rvt.getPositiveIndicatorType() == PositiveIndicatorType.NUMERICAL) {
                  newResultValue =
                    rvt.createResultValue(well,
                                          assayWellType,
                                          cell.getDouble(),
                                          cell.getDoublePrecision(),
                                          isExclude);
                }
              }
              else { // not assay activity indicator
                if (rvt.isNumeric()) {
                  newResultValue =
                    rvt.createResultValue(well,
                                          assayWellType,
                                          cell.getDouble(),
                                          cell.getDoublePrecision(),
                                          isExclude);
                }
                else {
                  newResultValue =
                    rvt.createResultValue(well,
                                          assayWellType,
                                          cell.getString(),
                                          isExclude);
                }
              }
              if (newResultValue == null) {
                _errors.addError("duplicate well", cell);
              }
            }
            catch (ResultValueTypeNumericalnessException e) {
              // inconsistency in numeric or string types in RVT's result values
              _errors.addError(e.getMessage(), cell);
            }
            ++iDataHeader;
          }
          ++wellsWithDataLoaded;
        }
      }
      ++dataSheetsParsed;
    }
    if (dataSheetsParsed == 0) {
      _errors.addError(NO_DATA_SHEETS_FOUND_ERROR);
    } else {
      log.info("done parsing " + dataSheetsParsed + " data sheet(s) " + workbook.getWorkbookFile().getName());
      log.info("loaded data for " + wellsWithDataLoaded + " well(s) ");
    }
  }

  /**
   * Determines if a data header contains numeric or non-numeric data, by
   * reading ahead and making the determination based upon the first non-empty
   * cell in the column for the specified data header. Note that the test may be
   * inconclusive for a given worksheet (if the entire column contains empty
   * cells), but that a later worksheet may used to make the determination.
   */
  private void determineNumericalnessOfDataHeader(ResultValueType rvt,
                                                  Workbook workbook,
                                                  int iSheet,
                                                  int iDataHeader)
  {
    if (!rvt.isNumericalnessDetermined()) {
      if (rvt.getPositiveIndicatorType() == PositiveIndicatorType.NUMERICAL) {
        rvt.setNumeric(true);
      }
      else {
        DataRowIterator rowIter = new DataRowIterator(workbook, iSheet);
        while (rowIter.hasNext()) {
          Cell cell = dataCell(rowIter.next(), iDataHeader);
          if (cell.isEmpty()) {
            continue;
          }
          if (cell.isNumeric()) {
            rvt.setNumeric(true);
          }
          else if (cell.isBoolean()) {
            rvt.setNumeric(false);
          }
          else {
            rvt.setNumeric(false);
          }
          break;
        }
      }
    }
  }

  private class DataRowIterator implements Iterator<Integer>
  {
    private Sheet _sheet;
    private int _iRow;
    private WellKey _wellKey;
    private Well _well;
    private int _lastRowIndex;

    public DataRowIterator(Workbook workbook,
                           int sheetIndex)
    {
      _sheet = workbook.getWorkbook().getSheet(sheetIndex);
      _iRow = RAWDATA_FIRST_DATA_ROW_INDEX - 1;
      _lastRowIndex = _sheet.getRows() - 1;
    }

    public DataRowIterator(DataRowIterator rowIter)
    {
      _sheet = rowIter._sheet;
      _iRow = rowIter._iRow;
      _wellKey = rowIter._wellKey;
      _lastRowIndex = rowIter._lastRowIndex;
    }

    public boolean hasNext()
    {
      return findNextRow() != _iRow;
    }

    public Integer next()
    {
      int nextRow = findNextRow();
      if (nextRow != _iRow) {
        _iRow = nextRow;
        _wellKey = parseWellKey(_iRow);
        _well = null;
        return getRowIndex();
      }
      else {
        _wellKey = null;
        _well = null;
        return null;
      }
    }

    public Integer getRowIndex()
    {
      return new Integer(_iRow);
    }

    public WellKey getWellKey()
    {
      return _wellKey;
    }

    public Well getWell()
    {
      if (_wellKey != null && _well == null) {
        Library library = findLibraryWithPlate(_wellKey.getPlateNumber());
        if (library == null) {
          _errors.addError(NO_SUCH_LIBRARY_WITH_PLATE + ": " + _wellKey);
          return null;
        }
        preloadLibraryWells(library);

        _well = _librariesDao.findWell(_wellKey);
        if (_well == null) {
          _errors.addError(NO_SUCH_WELL + ": " + _wellKey);
        }
      }

      return _well;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    private int findNextRow()
    {
      int iRow = _iRow;
      while (++iRow <= _lastRowIndex) {
        if (!ignoreRow(_sheet, iRow)) {
          if (parseWellKey(iRow) != null) {
            return iRow;
          }
        }
      }
      return _iRow;
    }

    private WellKey parseWellKey(int iRow) 
    {
      // TODO: dataCell() call assumes initializeDataSheet() has been called before this iterator object is used! (bad!)
      Integer plateNumber = _plateNumberParser.parse(dataCell(iRow,
                                                              DataColumn.STOCK_PLATE_ID,
                                                              true));
      Cell wellNameCell = dataCell(iRow,
                                   DataColumn.WELL_NAME,
                                   true);
      String wellName = _wellNameParser.parse(wellNameCell);
      if (wellName.equals("")) {
        return null;
      }
      return new WellKey(plateNumber, wellName);
    }
  }

  /**
   * Determines if row at the specified 0-based index should be ignored,
   * which is the case if the row is undefined, has no cells, or the first cell
   * is blank or contains the empty string or only whitespace.
   *
   * @param sheet
   * @param rowIndex

   */
  private boolean ignoreRow(Sheet sheet, int rowIndex)
  {
    if (rowIndex >= sheet.getRows()) {
      return true;
    }
    jxl.Cell[] row = sheet.getRow(rowIndex);
    if (row.length == 0) {
      return true;
    }
    if (row[0].getType().equals(CellType.EMPTY)) {
      return true;
    }
    if (row[0].getContents().trim().length() == 0) {
      return true;
    }
    return false;
  }

  /**
   * @motivation database I/O optimization
   */
  private Library findLibraryWithPlate(Integer plateNumber)
  {
    if (_lastLibrary == null ||
      !_lastLibrary.containsPlate(plateNumber)) {
      _lastLibrary = _librariesDao.findLibraryWithPlate(plateNumber);
    }
    return _lastLibrary;
  }

  /**
   * @motivation database I/O optimization
   */
  private void preloadLibraryWells(Library library)
  {
    if (!_preloadedLibraries.contains(library)) {
      _librariesDao.loadOrCreateWellsForLibrary(library);
      _preloadedLibraries.add(library);
      //log.debug("flushing hibernate session after loading library");
      //releaseMemory(new Runnable() { public void run() { _dao.flush(); } });
    }
  }

  private static class ParsedScreenInfo {
    private Integer _screenId;
    private Date _date;

    public Date getDateCreated()
    {
      return _date;
    }

    public void setDate(Date date)
    {
      _date = date;
    }

    public Integer getScreenNumber()
    {
      return _screenId;
    }

    public void setScreenId(Integer screenId)
    {
      _screenId = screenId;
    }
  }

  /**
   * Parses the "Screen Info" worksheet.
   * <p>
   * For now, we just parse the ScreenResult date, but if we ever need to parse
   * more, we should return a composite object, rather than just a Date.
   *
   * @throws UnrecoverableScreenResultParseException if a screen ID is not found
   */
  private ParsedScreenInfo parseScreenInfo(Workbook workbook, Screen screen)
    throws UnrecoverableScreenResultParseException
  {
    ParsedScreenInfo parsedScreenInfo = new ParsedScreenInfo();
    int screenInfoSheetIndex;
    screenInfoSheetIndex = workbook.findSheetIndex(SCREEN_INFO_SHEET_NAME);
    Sheet screenInfoSheet = workbook.getWorkbook().getSheet(screenInfoSheetIndex);
    Cell.Factory factory = new Cell.Factory(workbook,
                                            screenInfoSheetIndex,
                                            _errors);
    if (screenInfoSheet != null) {
      for (int iRow = 0; iRow < screenInfoSheet.getRows(); iRow++) {
        Cell labelCell = factory.getCell((short) 0, iRow);
        String rowLabel = labelCell.getString();
        if (rowLabel != null) {
          if (rowLabel.equalsIgnoreCase(ScreenInfoRow.DATE_FIRST_LIBRARY_SCREENING.getDisplayText())) {
            Cell valueCell = factory.getCell((short) 1, iRow, false);
            parsedScreenInfo.setDate(valueCell.getDate());
          }
          else if (rowLabel.equalsIgnoreCase(ScreenInfoRow.ID.getDisplayText())) {
            Cell valueCell = factory.getCell((short) 1, iRow, true);
            parsedScreenInfo.setScreenId(valueCell.getInteger());
          }
        }
      }
    }
    if (parsedScreenInfo.getScreenNumber() == null) {
      _errors.addError(NO_SCREEN_ID_FOUND_ERROR);
    }
    else if (!parsedScreenInfo.getScreenNumber().equals(screen.getScreenNumber())) {
      _errors.addError("screen result data file is for screen number " +
                       parsedScreenInfo.getScreenNumber() +
                       ", expected " + screen.getScreenNumber());
    }
    if (parsedScreenInfo.getDateCreated() == null) {
      if (screen.getScreeningRoomActivities().size() > 0) {
        SortedSet<ScreeningRoomActivity> sortedScreeningRoomActivities =
          new TreeSet<ScreeningRoomActivity>(screen.getScreeningRoomActivities());
        parsedScreenInfo.setDate(sortedScreeningRoomActivities.first().getDateOfActivity());
      }
      else {
        log.warn("screen result's screen has no library screenings, so screen result's \"date created\" property will be set to today");
        parsedScreenInfo.setDate(new Date());
      }
    }
    return parsedScreenInfo;
  }

  public class ColumnLabelsParser implements CellValueParser<ResultValueType>
  {
    protected Map<String,ResultValueType> _columnLabel2RvtMap;
    private Pattern columnIdPattern = Pattern.compile("[A-Z]+");
    private ParseErrorManager _errors;


    // public methods

    public ColumnLabelsParser(Map<String,ResultValueType> columnLabel2RvtMap, ParseErrorManager errors)
    {
      _columnLabel2RvtMap =  columnLabel2RvtMap;
      _errors = errors;
    }

    public ResultValueType parse(Cell cell)
    {
      throw new UnsupportedOperationException();
    }

    public List<ResultValueType> parseList(Cell cell)
    {
      String textMultiValue = cell.getString();
      List<ResultValueType> result = new ArrayList<ResultValueType>();

      if (textMultiValue == null || textMultiValue.trim().length() == 0) {
        return result;
      }

      String[] textValues = textMultiValue.split(",");
      for (int i = 0; i < textValues.length; i++) {
        String text = textValues[i].trim();
        ResultValueType rvt = doParseSingleValue(text, cell);
        if (rvt != null) {
          result.add(rvt);
        }
        else {
          _errors.addError("invalid Data Header column reference '" + text +
            "' (expected one of " + _columnLabel2RvtMap.keySet() + ")",
            cell);
        }
      }
      return result;
    }


    // private methods

    private ResultValueType doParseSingleValue(String value, Cell cell)
    {
      Matcher matcher = columnIdPattern.matcher(value);
      if (!matcher.matches()) {
        return null;
      }
      String columnLabel = matcher.group(0);
      return _columnLabel2RvtMap.get(columnLabel);
    }
  }

  private class ExcludeParser extends ColumnLabelsParser
  {

    // public methods

    public ExcludeParser(Map<String,ResultValueType> columnLabel2RvtMap, ParseErrorManager errors)
    {
      super(columnLabel2RvtMap, errors);
    }

    public List<ResultValueType> parseList(Cell cell)
    {
      String textMultiValue = cell.getString();
      List<ResultValueType> result = new ArrayList<ResultValueType>();

      if (textMultiValue != null &&
        textMultiValue.equalsIgnoreCase(ScreenResultWorkbookSpecification.EXCLUDE_ALL_VALUE)) {
        return new ArrayList<ResultValueType>(_columnLabel2RvtMap.values());
      }

      if (textMultiValue == null) {
        return result;
      }

      return super.parseList(cell);
    }
  }
}