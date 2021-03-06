// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.cherrypickrequests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import edu.harvard.med.screensaver.ScreensaverConstants;
import edu.harvard.med.screensaver.model.libraries.PlateSize;
import edu.harvard.med.screensaver.model.libraries.WellName;
import edu.harvard.med.screensaver.ui.arch.util.converter.EmptyWellsConverter;

public class EmptyWellsConverterTest extends TestCase
{
  private EmptyWellsConverter _converter;

  @Override
  protected void setUp() throws Exception
  {
    _converter = new EmptyWellsConverter();
  }

  public void testMakeFullColumn()
  {
    List<WellName> fullColumn = EmptyWellsConverter.makeFullColumn(8);
    if(ScreensaverConstants.DEFAULT_PLATE_SIZE == PlateSize.WELLS_384) {
      assertEquals(Arrays.asList(new WellName("A09"),
          new WellName("B09"),
          new WellName("C09"),
          new WellName("D09"),
          new WellName("E09"),
          new WellName("F09"),
          new WellName("G09"),
          new WellName("H09"),
          new WellName("I09"),
          new WellName("J09"),
          new WellName("K09"),
          new WellName("L09"),
          new WellName("M09"),
          new WellName("N09"),
          new WellName("O09"),
          new WellName("P09")),
          fullColumn);
      fullColumn = EmptyWellsConverter.makeFullColumn(9);
      assertEquals(Arrays.asList(new WellName("A10"),
           new WellName("B10"),
           new WellName("C10"),
           new WellName("D10"),
           new WellName("E10"),
           new WellName("F10"),
           new WellName("G10"),
           new WellName("H10"),
           new WellName("I10"),
           new WellName("J10"),
           new WellName("K10"),
           new WellName("L10"),
           new WellName("M10"),
           new WellName("N10"),
           new WellName("O10"),
           new WellName("P10")),
           fullColumn);
    	}else if(ScreensaverConstants.DEFAULT_PLATE_SIZE == PlateSize.WELLS_96) {
    		assertEquals(Arrays.asList(new WellName("A09"),
    				new WellName("B09"),
    				new WellName("C09"),
    				new WellName("D09"),
    				new WellName("E09"),
    				new WellName("F09"),
    				new WellName("G09"),
    				new WellName("H09")),
    				fullColumn);
    		fullColumn = EmptyWellsConverter.makeFullColumn(9);
    		assertEquals(Arrays.asList(new WellName("A10"),
    				new WellName("B10"),
    				new WellName("C10"),
    				new WellName("D10"),
    				new WellName("E10"),
    				new WellName("F10"),
    				new WellName("G10"),
    				new WellName("H10")),
    				fullColumn);    
    	}
  }

  public void testMakeFullRow()
  {
    if(ScreensaverConstants.DEFAULT_PLATE_SIZE == PlateSize.WELLS_384) {
    	List<WellName> fullRow = EmptyWellsConverter.makeFullRow('H' - 'A');
  		assertEquals(Arrays.asList(new WellName("H01"),
  				new WellName("H02"),
  				new WellName("H03"),
  				new WellName("H04"),
  				new WellName("H05"),
  				new WellName("H06"),
  				new WellName("H07"),
  				new WellName("H08"),
  				new WellName("H09"),
  				new WellName("H10"),
  				new WellName("H11"),
  				new WellName("H12"),
  				new WellName("H13"),
  				new WellName("H14"),
  				new WellName("H15"),
  				new WellName("H16"),
  				new WellName("H17"),
  				new WellName("H18"),
  				new WellName("H19"),
  				new WellName("H20"),
  				new WellName("H21"),
  				new WellName("H22"),
  				new WellName("H23"),
  				new WellName("H24")),
  				fullRow);
    	} else  if(ScreensaverConstants.DEFAULT_PLATE_SIZE == PlateSize.WELLS_384) {
      	List<WellName> fullRow = EmptyWellsConverter.makeFullRow('H' - 'A');
    		assertEquals(Arrays.asList(new WellName("H01"),
    				new WellName("H02"),
    				new WellName("H03"),
    				new WellName("H04"),
    				new WellName("H05"),
    				new WellName("H06"),
    				new WellName("H07"),
    				new WellName("H08"),
    				new WellName("H09"),
    				new WellName("H10"),
    				new WellName("H11"),
    				new WellName("H12")),
    				fullRow);
    	}
  }

  public void testEmptyWellsConverter()
  {
  	if(ScreensaverConstants.DEFAULT_PLATE_SIZE == PlateSize.WELLS_384)
  	{
      List<WellName> wellNames = Arrays.asList(new WellName("A01"),
          new WellName("F10"),
          new WellName("P24"));
			doTest("single well", "A01", wellNames.subList(0, 1));
			doTest("multiple wells", "A01, F10, P24", wellNames);
			List<WellName> rowB = EmptyWellsConverter.makeFullRow('B' - 'A');
			List<WellName> rowH = EmptyWellsConverter.makeFullRow('H' - 'A');
			List<WellName> rows = new ArrayList<WellName>();
			rows.addAll(rowB);
			rows.addAll(rowH);
			doTest("full row", "Row:B", rowB);
			doTest("full rows", "Row:B, Row:H", rows);
			
			List<WellName> col2 = EmptyWellsConverter.makeFullColumn(1);
			List<WellName> col11 = EmptyWellsConverter.makeFullColumn(10);
			List<WellName> cols = new ArrayList<WellName>();
			cols.addAll(col2);
			cols.addAll(col11);
			doTest("full col", "Col:02", col2);
			doTest("full cols", "Col:02, Col:11", cols);
			
			List<WellName> all = new ArrayList<WellName>();
			all.addAll(wellNames);
			all.addAll(rows);
			all.addAll(cols);
			doTest("mix", "Col:02, Col:11, Row:B, Row:H, A01, F10, P24", all);
  	}
  	else 	if(ScreensaverConstants.DEFAULT_PLATE_SIZE == PlateSize.WELLS_96)
  	{
      List<WellName> wellNames = Arrays.asList(new WellName("A01"),
          new WellName("F10"),
          new WellName("H12"));
			doTest("single well", "A01", wellNames.subList(0, 1));
			doTest("multiple wells", "A01, F10, H12", wellNames);
			List<WellName> rowB = EmptyWellsConverter.makeFullRow('B' - 'A');
			List<WellName> rowH = EmptyWellsConverter.makeFullRow('H' - 'A');
			List<WellName> rows = new ArrayList<WellName>();
			rows.addAll(rowB);
			rows.addAll(rowH);
			doTest("full row", "Row:B", rowB);
			doTest("full rows", "Row:B, Row:H", rows);
			
			List<WellName> col2 = EmptyWellsConverter.makeFullColumn(1);
			List<WellName> col10 = EmptyWellsConverter.makeFullColumn(10);
			List<WellName> cols = new ArrayList<WellName>();
			cols.addAll(col2);
			cols.addAll(col10);
			doTest("full col", "Col:02", col2);
			doTest("full cols", "Col:02, Col:11", cols);
			
			List<WellName> all = new ArrayList<WellName>();
			all.addAll(wellNames);
			all.addAll(rows);
			all.addAll(cols);
			doTest("mix", "Col:02, Col:11, Row:B, Row:H, A01, F10", all);
  	}
  }

  private void doTest(String test, String asString, List<WellName> asList)
  {
    Set<WellName> asSet = new HashSet<WellName>(asList);
    assertEquals(test + ":getAsObject()",
                 asSet,
                 (Set<WellName>) _converter.getAsObject(null, null, asString));
    assertEquals(test + ":getAsString()",
                 asString,
                 _converter.getAsString(null, null, asSet));
  }
}
