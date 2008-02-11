// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface DataExporter<T>
{
  public String getFormatName();
  public String getMimeType();
  public String getFileName();
  public InputStream export(Collection<T> data) throws IOException;
}
