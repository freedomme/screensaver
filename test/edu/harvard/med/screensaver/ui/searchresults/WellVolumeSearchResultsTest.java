// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/branches/schema-upgrade-2007/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.searchresults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.faces.model.DataModel;

import edu.harvard.med.screensaver.AbstractSpringPersistenceTest;
import edu.harvard.med.screensaver.db.DAOTransaction;
import edu.harvard.med.screensaver.db.LibrariesDAO;
import edu.harvard.med.screensaver.model.cherrypicks.LabCherryPick;
import edu.harvard.med.screensaver.model.cherrypicks.RNAiCherryPickRequest;
import edu.harvard.med.screensaver.model.cherrypicks.ScreenerCherryPick;
import edu.harvard.med.screensaver.model.libraries.Copy;
import edu.harvard.med.screensaver.model.libraries.CopyUsageType;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.PlateType;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.libraries.WellVolumeAdjustment;
import edu.harvard.med.screensaver.service.cherrypicks.CherryPickRequestAllocatorTest;
import edu.harvard.med.screensaver.ui.libraries.WellCopy;
import edu.harvard.med.screensaver.ui.libraries.WellCopyVolumeSearchResults;
import edu.harvard.med.screensaver.ui.libraries.WellVolume;
import edu.harvard.med.screensaver.ui.libraries.WellVolumeSearchResults;
import edu.harvard.med.screensaver.ui.table.column.TableColumn;
import edu.harvard.med.screensaver.util.Pair;
import edu.harvard.med.screensaver.util.Pair.PairComparator;

import org.apache.log4j.Logger;

public class WellVolumeSearchResultsTest extends AbstractSpringPersistenceTest
{
  // static members

  private static Logger log = Logger.getLogger(WellVolumeSearchResultsTest.class);


  // instance data members
  
  protected LibrariesDAO librariesDao;
  private WellVolumeSearchResults _wellVolumeSearchResults;
  private WellCopyVolumeSearchResults _wellCopyVolumeSearchResults;
  private Library _library;
  private RNAiCherryPickRequest _cherryPickRequest;
  private Map<Pair<WellKey,String>,BigDecimal> _expectedRemainingWellCopyVolume = new HashMap<Pair<WellKey,String>,BigDecimal>();
  private Map<WellKey,BigDecimal> _expectedRemainingWellVolume = new HashMap<WellKey,BigDecimal>();


  // public constructors and methods
  
  @Override
  protected void onSetUp() throws Exception
  {
    super.onSetUp();
    
    initializeWellCopyVolumes();
    _wellVolumeSearchResults = new WellVolumeSearchResults(genericEntityDao, null, null, null, null);
    _wellCopyVolumeSearchResults = new WellCopyVolumeSearchResults(genericEntityDao, null, null, _wellVolumeSearchResults, null);
  }
  
  public void testWellVolumeSearchResults()
  {
    _wellCopyVolumeSearchResults.searchWellsForLibrary(_library);
    doTestWellCopyVolumeSearchResult(makeWellCopyKeys(_library));
    doTestWellVolumeSearchResult(makeWellVolumeKeys(makeWellCopyKeys(_library)));

    List<Well> wells = new ArrayList<Well>(_library.getWells()).subList(24, 96);
    _wellCopyVolumeSearchResults.searchWells(makeWellKeys(wells));
    doTestWellCopyVolumeSearchResult(makeWellCopyKeys(wells));
    doTestWellVolumeSearchResult(makeWellVolumeKeys(makeWellCopyKeys(wells)));
    
    _wellCopyVolumeSearchResults.searchWellsForCherryPickRequest(_cherryPickRequest, false);
    doTestWellCopyVolumeSearchResult(makeWellCopyKeys(_cherryPickRequest));
    doTestWellVolumeSearchResult(makeWellVolumeKeys(makeWellCopyKeys(_cherryPickRequest)));
  }

  private void doTestWellCopyVolumeSearchResult(SortedSet<Pair<WellKey,String>> expectedWellCopyKeys)
  {
    DataModel model = _wellCopyVolumeSearchResults.getDataTableModel();
    assertEquals("row count", expectedWellCopyKeys.size(), model.getRowCount());
    int j = 0;
    for (Pair<WellKey,String> expectedKey : expectedWellCopyKeys) {
      model.setRowIndex(j++);
      assertEquals("row data " + j,
                   expectedKey,
                   ((WellCopy) model.getRowData()).getKey());
      int columnsTested = 0;
      WellCopy rowData = (WellCopy) model.getRowData();
      for (TableColumn<WellCopy,?> column : _wellCopyVolumeSearchResults.getColumnManager().getAllColumns()) {
        if (column.isVisible()) {
          Object cellValue = column.getCellValue(rowData);
          if (column.getName().equals("Library")) {
            assertEquals("row " + j + ", " + expectedKey + ":Library",
                         librariesDao.findLibraryWithPlate(expectedKey.getFirst().getPlateNumber()).getLibraryName(),
                         (String) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Plate")) {
            assertEquals("row " + j + ", " + expectedKey + ":Plate",
                         (Integer) expectedKey.getFirst().getPlateNumber(),
                         (Integer) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Well")) {
            assertEquals("row " + j + ", " + expectedKey  + ":Well",
                         expectedKey.getFirst().getWellName(),
                         (String) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Initial Volume")) {
            assertEquals("row " + j + ", " + expectedKey  + ":Initial Volume",
                         expectedKey.getSecond().equals("C") ? new BigDecimal("10.00") : new BigDecimal("20.00"),
                         (BigDecimal) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Remaining Volume")) {
            // this tests aggregation of WVAs
            assertEquals("row " + j + ", " + expectedKey  + ":Remaining Volume",
                         _expectedRemainingWellCopyVolume.get(expectedKey),
                         (BigDecimal) cellValue);
            ++columnsTested;
          }
          // TODO: test all aggregation columns!
        }
      }
      assertEquals("tested all columns for row " + j, 5, columnsTested);
    }
  }

  private void doTestWellVolumeSearchResult(SortedSet<WellKey> expectedWellKeys)
  {
    DataModel model = _wellVolumeSearchResults.getDataTableModel();
    assertEquals("row count", expectedWellKeys.size(), model.getRowCount());
    int j = 0;
    for (WellKey expectedKey : expectedWellKeys) {
      model.setRowIndex(j++);
      assertEquals("row data " + j,
                   expectedKey,
                   ((WellVolume) model.getRowData()).getWell().getWellKey());
      int columnsTested = 0;
      WellVolume rowData = (WellVolume) model.getRowData();
      for (TableColumn<WellVolume,?> column : _wellVolumeSearchResults.getColumnManager().getAllColumns()) {
        if (column.isVisible()) {
          Object cellValue = column.getCellValue(rowData);
          if (column.getName().equals("Library")) {
            assertEquals("row " + j + ", " + expectedKey + ":Library",
                         librariesDao.findLibraryWithPlate(expectedKey.getPlateNumber()).getLibraryName(),
                         (String) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Plate")) {
            assertEquals("row " + j + ", " + expectedKey + ":Plate",
                         (Integer) expectedKey.getPlateNumber(),
                         (Integer) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Well")) {
            assertEquals("row " + j + ", " + expectedKey  + ":Well",
                         expectedKey.getWellName(),
                         (String) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Total Initial Copy Volume")) {
            assertEquals("row " + j + ", " + expectedKey  + ":Total Initial Copy Volume",
                         new BigDecimal("30.00"),
                         (BigDecimal) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Consumed Volume")) {
            // this tests aggregation of WVAs
            assertEquals("row " + j + ", " + expectedKey  + ":Consumed Volume",
                         new BigDecimal("30.00").subtract(_expectedRemainingWellVolume.get(expectedKey)),
                         (BigDecimal) cellValue);
            ++columnsTested;
          }
          else if (column.getName().equals("Copies")) {
            // this tests aggregation of WVAs
            assertEquals("row " + j + ", " + expectedKey  + ":Copies",
                         (List<String>) Arrays.asList("C", "D"),
                         (List<String>) cellValue);
            ++columnsTested;
          }

          // TODO: test all aggregation columns!
        }
      }
      assertEquals("tested all columns for row " + j, 6, columnsTested);
    }
  }
  
  /**
   * 00001:A01:C: 1 lcp
   * 00001:A01:D: 1 lcp
   * 00001:A02:C: -
   * 00001:A02:D: -
   * 00001:B01:C: 2 wvas
   * 00001:B01:D: -
   * 00001:B02:C: 1 wva
   * 00001:B02:D: 1 wva 
   * 00002:A01:C: 1 lcp
   * 00002:A01:D: -
   * 00002:B01:C: -
   * 00002:B01:D: 1 lcp, 1 wva
   */
  private void initializeWellCopyVolumes()
  {
    genericEntityDao.doInTransaction(new DAOTransaction() {
      public void runTransaction() {
        _library = CherryPickRequestAllocatorTest.makeRNAiDuplexLibrary("library", 1, 2, 384);
        Copy copyC = _library.createCopy(CopyUsageType.FOR_CHERRY_PICK_SCREENING, "C");
        Copy copyD = _library.createCopy(CopyUsageType.FOR_CHERRY_PICK_SCREENING, "D");
        copyC.createCopyInfo(1, "loc1", PlateType.EPPENDORF, new BigDecimal("10.00"));
        copyC.createCopyInfo(2, "loc1", PlateType.EPPENDORF, new BigDecimal("10.00"));
        copyD.createCopyInfo(1, "loc1", PlateType.EPPENDORF, new BigDecimal("20.00"));
        copyD.createCopyInfo(2, "loc1", PlateType.EPPENDORF, new BigDecimal("20.00"));
        genericEntityDao.saveOrUpdateEntity(_library);

        Well plate1WellA01 = genericEntityDao.findEntityById(Well.class, "00001:A01");
        Well plate1WellB01 = genericEntityDao.findEntityById(Well.class, "00001:B01");
        Well plate1WellB02 = genericEntityDao.findEntityById(Well.class, "00001:B02");
        Well plate2WellA01 = genericEntityDao.findEntityById(Well.class, "00002:A01");
        Well plate2WellB01 = genericEntityDao.findEntityById(Well.class, "00002:B01");

        _cherryPickRequest = CherryPickRequestAllocatorTest.createRNAiCherryPickRequest(1, 3);
        ScreenerCherryPick dummyScreenerCherryPick = _cherryPickRequest.createScreenerCherryPick(plate1WellA01);
        // note: you cannot normally have 2 LCP for the same well in a CPR, 
        // but we do that here anyway to test aggregation of 2 LCPs in the same well;
        // to do this correctly, we should create 2 CPRs
        _cherryPickRequest.createLabCherryPick(dummyScreenerCherryPick, plate1WellA01).setAllocated(copyC);
        _cherryPickRequest.createLabCherryPick(dummyScreenerCherryPick, plate1WellA01).setAllocated(copyD);
        _cherryPickRequest.createLabCherryPick(dummyScreenerCherryPick, plate2WellA01).setAllocated(copyC);
        _cherryPickRequest.createLabCherryPick(dummyScreenerCherryPick, plate2WellB01).setAllocated(copyD);
        genericEntityDao.saveOrUpdateEntity(_cherryPickRequest.getScreen());
        
        genericEntityDao.persistEntity(new WellVolumeAdjustment(copyC, plate1WellB01, new BigDecimal("-1.00"), null));
        genericEntityDao.persistEntity(new WellVolumeAdjustment(copyC, plate1WellB01, new BigDecimal("-1.00"), null));
        genericEntityDao.persistEntity(new WellVolumeAdjustment(copyC, plate1WellB02, new BigDecimal("-1.00"), null));
        genericEntityDao.persistEntity(new WellVolumeAdjustment(copyD, plate1WellB02, new BigDecimal("-1.00"), null));
        genericEntityDao.persistEntity(new WellVolumeAdjustment(copyD, plate2WellB01, new BigDecimal("-1.00"), null));
        
        _expectedRemainingWellCopyVolume.put(new Pair<WellKey,String>(plate1WellA01.getWellKey(), "C"), new BigDecimal("7.00"));
        _expectedRemainingWellCopyVolume.put(new Pair<WellKey,String>(plate1WellA01.getWellKey(), "D"), new BigDecimal("17.00"));
        _expectedRemainingWellCopyVolume.put(new Pair<WellKey,String>(plate1WellB01.getWellKey(), "C"), new BigDecimal("8.00"));
        _expectedRemainingWellCopyVolume.put(new Pair<WellKey,String>(plate1WellB02.getWellKey(), "C"), new BigDecimal("9.00"));
        _expectedRemainingWellCopyVolume.put(new Pair<WellKey,String>(plate1WellB02.getWellKey(), "D"), new BigDecimal("19.00"));
        _expectedRemainingWellCopyVolume.put(new Pair<WellKey,String>(plate2WellA01.getWellKey(), "C"), new BigDecimal("7.00"));
        _expectedRemainingWellCopyVolume.put(new Pair<WellKey,String>(plate2WellB01.getWellKey(), "D"), new BigDecimal("16.00"));
        
        for (Well well : _library.getWells()) {
          BigDecimal expectedRemainingWellVolume = new BigDecimal("0.00");
          for (Copy copy : _library.getCopies()) {
            Pair<WellKey,String> wellCopyKey = new Pair<WellKey,String>(well.getWellKey(), copy.getName());
            if (!_expectedRemainingWellCopyVolume.containsKey(wellCopyKey)) {
              BigDecimal expectedRemainingWellCopyVolume = copy.getCopyInfos().iterator().next().getMicroliterWellVolume();
              _expectedRemainingWellCopyVolume.put(wellCopyKey, expectedRemainingWellCopyVolume);
              expectedRemainingWellVolume = expectedRemainingWellVolume.add(expectedRemainingWellCopyVolume);
            }
            else {
              expectedRemainingWellVolume = expectedRemainingWellVolume.add(_expectedRemainingWellCopyVolume.get(wellCopyKey));
            }
          }
          _expectedRemainingWellVolume.put(well.getWellKey(), expectedRemainingWellVolume);
        }
      }
    });
  }

  private static Set<WellKey> makeWellKeys(List<Well> wells)
  {
    Set<WellKey> wellKeys = new HashSet<WellKey>();
    for (Well well : wells) {
      wellKeys.add(well.getWellKey());
    }
    return wellKeys;
  }

  private static SortedSet<Pair<WellKey,String>> makeWellCopyKeys(Library library)
  {
    SortedSet<Pair<WellKey,String>> expectedKeys = new TreeSet<Pair<WellKey,String>>(new PairComparator<WellKey,String>());
    for (Well well : library.getWells()) {
      for (Copy copy : library.getCopies()) {
        expectedKeys.add(new Pair<WellKey,String>(well.getWellKey(), copy.getName()));
      }
    }
    return expectedKeys;
  }
  
  private static SortedSet<Pair<WellKey,String>> makeWellCopyKeys(List<Well> wells)
  {
    SortedSet<Pair<WellKey,String>> wellCopyKeys = new TreeSet<Pair<WellKey,String>>(new Pair.PairComparator<WellKey,String>());
    for (Well well : wells) {
      for (Copy copy : well.getLibrary().getCopies()) {
        wellCopyKeys.add(new Pair<WellKey,String>(well.getWellKey(), copy.getName()));
      }
    }
    return wellCopyKeys;
  }
  
  private static SortedSet<Pair<WellKey,String>> makeWellCopyKeys(RNAiCherryPickRequest cherryPickRequest)
  {
    SortedSet<Pair<WellKey,String>> expectedKeys = new TreeSet<Pair<WellKey,String>>(new PairComparator<WellKey,String>());
    for (LabCherryPick labCherryPick : cherryPickRequest.getLabCherryPicks()) {
      Well well = labCherryPick.getSourceWell();
        //if (labCherryPick.getWellVolumeAdjustments().size() > 0) {
        //Copy copy = labCherryPick.getWellVolumeAdjustments().iterator().next().getCopy()
      for (Copy copy : well.getLibrary().getCopies()) {
        expectedKeys.add(new Pair<WellKey,String>(well.getWellKey(), copy.getName()));
      }
    }
    return expectedKeys;
  }
  
  private SortedSet<WellKey> makeWellVolumeKeys(SortedSet<Pair<WellKey,String>> wellCopyKeys)
  {
    SortedSet<WellKey> wellKeys = new TreeSet<WellKey>();
    for (Pair<WellKey,String> wellCopyKey : wellCopyKeys) {
      wellKeys.add(wellCopyKey.getFirst());
    } 
    return wellKeys;
  }
}