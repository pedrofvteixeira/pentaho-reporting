/*!
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
* Foundation.
*
* You should have received a copy of the GNU Lesser General Public License along with this
* program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
* or from the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU Lesser General Public License for more details.
*
* Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
*/

package org.pentaho.reporting.engine.classic.core.style;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.ItemBand;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.style.resolver.SimpleStyleResolver;

public class SimpleStyleResolverTest extends TestCase
{
  public SimpleStyleResolverTest()
  {
  }

  public SimpleStyleResolverTest(final String name)
  {
    super(name);
  }

  protected void setUp() throws Exception
  {
    ClassicEngineBoot.getInstance().start();
  }

  public void testStyleInheritance()
  {
    MasterReport report = new MasterReport();
    final ItemBand itemBand = report.getItemBand();

    report.getStyle().setStyleProperty(TextStyleKeys.FONT, "Dudadu");

    ResolverStyleSheet styleSheet = new ResolverStyleSheet();
    new SimpleStyleResolver(true).resolve(itemBand, styleSheet);
    Assert.assertEquals("Dudadu", styleSheet.getStyleProperty(TextStyleKeys.FONT));

  }
}
