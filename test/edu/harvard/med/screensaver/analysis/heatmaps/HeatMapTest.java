// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.analysis.heatmaps;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;

import edu.harvard.med.screensaver.AbstractSpringTest;
import edu.harvard.med.screensaver.analysis.ChainedFilter;
import edu.harvard.med.screensaver.analysis.IdentityNormalizationFunction;
import edu.harvard.med.screensaver.analysis.ZScoreNormalizationFunction;
import edu.harvard.med.screensaver.io.screenresults.MockDaoForScreenResultParserTest;
import edu.harvard.med.screensaver.io.screenresults.ScreenResultParser;
import edu.harvard.med.screensaver.io.screenresults.ScreenResultParserTest;
import edu.harvard.med.screensaver.model.screenresults.ResultValue;
import edu.harvard.med.screensaver.model.screenresults.ScreenResult;
import edu.harvard.med.screensaver.model.screens.Screen;

import org.apache.log4j.Logger;

public class HeatMapTest extends AbstractSpringTest
{
  private static Logger log = Logger.getLogger(HeatMapTest.class);

  private ScreenResult _screenResult;
  private ScreenResultParser _parser;
  
  @Override
  protected void onSetUp() throws Exception
  {
    Screen screen = ScreenResultParser.makeDummyScreen(107);
    _parser = new ScreenResultParser(new MockDaoForScreenResultParserTest());
    _screenResult = _parser.parse(screen,
                                  new File(ScreenResultParserTest.TEST_INPUT_FILE_DIR, "107_heatmap.xls"));
    if (_parser.getHasErrors()) {
      System.err.println("Parser errors:\n" + _parser.getErrors());
      fail("could not parse screen results");
    }
  }
  
  
  public void testIdentityHeatMap()
  {
    assertFalse(_parser.getHasErrors());
    HeatMap heatMap = new HeatMap(_screenResult.getResultValueTypes().first(),
                                  1,
                                  new NoOpFilter(),
                                  new IdentityNormalizationFunction(),
                                  new MultiGradientColorFunction(Color.BLACK,
                                                                 Color.WHITE));
    assertEquals("min", 1625.0, heatMap.getMin(), 0.01);
    assertEquals("max", 19247.0, heatMap.getMax(), 0.01);
    assertEquals("median", 9451.5, heatMap.getMedian());
    assertEquals("A1 value", 8632.0, heatMap.getNormalizedValue(0, 0), 0.01);
    assertEquals("P20 value", 19247.0, heatMap.getNormalizedValue(15, 19), 0.01);

    int interpolatedColorComponent = (int) (255 * ((8632.0 - 1625.0) / (19247.0 - 1625.0)));
    Color expectedA2Color = new Color(interpolatedColorComponent,
                                      interpolatedColorComponent,
                                      interpolatedColorComponent);
    assertEquals("A1 color", expectedA2Color, heatMap.getColor(0, 0));
    
  }
  
  public void testZScoreHeatMap()
  {
    HeatMap heatMap = new HeatMap(_screenResult.getResultValueTypes().first(),
                                  1,
                                  new NoOpFilter(),
                                  new ZScoreNormalizationFunction(),
                                  new MultiGradientColorFunction(Color.BLACK,
                                                                 Color.WHITE));
    assertEquals("min", -2.82, heatMap.getMin(), 0.01);
    assertEquals("max", 3.27, heatMap.getMax(), 0.01);
    assertEquals("A1 z-score value", -0.4, heatMap.getNormalizedValue(0, 0), 0.01);
    assertEquals("B2 z-score value",  0.89, heatMap.getNormalizedValue(1, 1), 0.01);

    int interpolatedColorComponent = (int) (255 * (-0.4 - -2.82) / (3.27 - -2.82));
    Color expectedA2Color = new Color(interpolatedColorComponent,
                                      interpolatedColorComponent,
                                      interpolatedColorComponent);
    assertEquals("A1 color", expectedA2Color, heatMap.getColor(0, 0));
  }

  public void testFilters()
  {
    HeatMap heatMap = new HeatMap(_screenResult.getResultValueTypes().first(),
                                  1,
                                  new ChainedFilter<ResultValue>(new ExcludedWellsFilter(), 
                                    new ChainedFilter<ResultValue>(new ControlWellsFilter())),
                                  new IdentityNormalizationFunction(),
                                  new MultiGradientColorFunction(Color.BLACK,
                                                                 Color.WHITE));
    assertEquals("max", 2156.0, heatMap.getMin(), 0.01);  
    assertEquals("min", 19205.0, heatMap.getMax(), 0.01);
    
  }
  
  public void testMakeHtmlHeatMap() throws IOException
  {
    HeatMap heatMap = new HeatMap(_screenResult.getResultValueTypes()
                                               .first(),
                                  1,
                                  new ChainedFilter<ResultValue>(new ExcludedWellsFilter(),
                                                                 new ChainedFilter<ResultValue>(new ControlWellsFilter())),
                                  new ZScoreNormalizationFunction(),
                                  new DefaultMultiColorGradient());
    File file = File.createTempFile("heatmap", ".html");
    PrintWriter writer = new PrintWriter(new FileWriter(file));
    NumberFormat format = NumberFormat.getNumberInstance();
    format.setMaximumFractionDigits(3);
    writer.println("<html><body><table>");
    for (int row = 0; row < 16; row++) {
      writer.println("<tr>");
      for (int col = 0; col < 23; col++) {
        double normalizedValue = heatMap.getNormalizedValue(row, col);
        Color color = heatMap.getColor(row, col);
        writer.print("<td style=\"background-color: rgb(" + color.getRed()
                     + "," + color.getGreen() + "," + color.getBlue() + ")\">"
                     + format.format(normalizedValue) + "</td>");
      }
      writer.println("</tr>");
    }
    writer.print("</table></body></html>");
    writer.close();
  }
  

}

