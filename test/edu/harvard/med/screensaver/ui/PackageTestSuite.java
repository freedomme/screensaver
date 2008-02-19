// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui;

import junit.framework.Test;
import junit.framework.TestSuite;

public class PackageTestSuite extends TestSuite
{
  public static void main(String[] args)
  {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite()
  {
    return new PackageTestSuite();
  }

  public PackageTestSuite()
  {
    addTest(edu.harvard.med.screensaver.ui.authentication.PackageTestSuite.suite());
    addTest(edu.harvard.med.screensaver.ui.util.PackageTestSuite.suite());
    addTest(edu.harvard.med.screensaver.ui.screenresults.PackageTestSuite.suite());
    addTest(edu.harvard.med.screensaver.ui.searchresults.PackageTestSuite.suite());
    addTest(edu.harvard.med.screensaver.ui.table.PackageTestSuite.suite());
  }
}
