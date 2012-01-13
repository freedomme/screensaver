// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.arch.util.converter;

import java.util.SortedSet;
import java.util.TreeSet;


public class SortedSetConverter extends CollectionConverter<SortedSet<String>,String>
{
  public SortedSetConverter()
  {
    super(new NoOpStringConverter());
  }

  @Override
  protected SortedSet<String> makeCollection()
  {
    return new TreeSet<String>();
  }
}
