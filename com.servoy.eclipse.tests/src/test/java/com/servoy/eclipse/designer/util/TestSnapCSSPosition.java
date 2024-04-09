/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.designer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;

/**
 * Unit tests for css position when snapping to a component edge.
 * @author emera
 */
public class TestSnapCSSPosition
{
	@Test
	public void testSnapToRight1() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: right }");

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "345", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "-1", "-1", "260", "180", "70"); //right property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the computed value", "340", newPosition.left);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}

	@Test
	public void testSnapToRight2() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: right }");

		CSSPosition old = new CSSPosition("250", "460", "-1", "80", "-1", "30"); //component is anchored left-right, no width
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "340", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "-1", "-1", "260", "180", "70"); //right property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos right should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos left should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the computed value", "340", newPosition.left);
		assertEquals("css pos width should be set to the computed value", "100", newPosition.width);
	}

	@Test
	public void testSnapToRight3() throws Exception
	{
		String property = "right";
		JSONObject json = new JSONObject("{prop: left }"); //align the right side of the component to the left of the target

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "160", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "-1", "-1", "260", "180", "70");

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos right should be set to the left value copied from the target", "380", newPosition.right);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}

	@Test
	public void testSnapToLeft1() throws Exception
	{
		String property = "left";
		JSONObject json = new JSONObject("{prop: right }"); //align the left side of the component to the right of the target

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "440", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "-1", "-1", "260", "180", "70");

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the right value copied from the target", "440", newPosition.left);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}

	@Test
	public void testSnapToLeft2() throws Exception
	{
		String property = "left";
		JSONObject json = new JSONObject("{prop: right }"); //align the left side of the component to the right of the target

		CSSPosition old = new CSSPosition("250", "-1", "-1", "150", "100", "30"); //component is anchored top, left
		CSSPosition newPosition = new CSSPosition("280", "-1", "-1", "440", "100", "30");
		CSSPosition targetPosition = new CSSPosition("80", "200", "-1", "-1", "180", "70");

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos left should be set to the right value copied from the target", "440", newPosition.left);
		assertEquals("css pos width should not be changed", "100", newPosition.width);
	}

	@Test
	public void testSnapToTop1() throws Exception
	{
		String property = "top";
		JSONObject json = new JSONObject("{prop: top }");

		CSSPosition old = new CSSPosition("-1", "250", "150", "-1", "100", "30"); //component is anchored right, bottom
		CSSPosition newPosition = new CSSPosition("150", "280", "160", "236", "100", "30");
		CSSPosition targetPosition = new CSSPosition("-1", "80", "260", "-1", "180", "70"); //top property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos top should NOT be set", CSSPositionUtils.isSet(newPosition.top));
		assertTrue("css pos bottom should be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("css pos bottom should be set to the computed value", "300", newPosition.bottom);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}

	@Test
	public void testSnapToBottom1() throws Exception
	{
		String property = "bottom";
		JSONObject json = new JSONObject("{prop: bottom }");

		CSSPosition old = new CSSPosition("150", "250", "-1", "-1", "100", "30"); //component is anchored right, top
		CSSPosition newPosition = new CSSPosition("160", "280", "-1", "-1", "100", "30");
		CSSPosition targetPosition = new CSSPosition("260", "80", "-1", "-1", "180", "70"); //bottom property not set

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos bottom should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos top should be set", CSSPositionUtils.isSet(newPosition.top));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("css pos top should be set to the computed value", "300", newPosition.top);
		assertEquals("css pos height should not be changed", "30", newPosition.height);
	}

	@Test
	public void testSnapToLeftBottom() throws Exception
	{
		String property = "left";
		JSONObject json = new JSONObject("{prop: left }");

		CSSPosition old = null; //the component is new
		CSSPosition newPosition = new CSSPosition("239", "-1", "-1", "74", "180", "30");
		CSSPosition targetPosition = new CSSPosition("110", "386", "-1", "-1", "180", "70");

		java.awt.Dimension containerSize = new java.awt.Dimension(640, 480);
		java.awt.Dimension targetContainerSize = new java.awt.Dimension(640, 480);
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		property = "bottom";
		json = new JSONObject("{prop: bottom }");
		targetPosition = new CSSPosition("269", "160", "-1", "340", "0", "30");
		DesignerUtil.setCssValue(json, property, newPosition, old, targetPosition, containerSize, targetContainerSize, false);

		assertFalse("css pos left should NOT be set", CSSPositionUtils.isSet(newPosition.left));
		assertTrue("css pos right should be set", CSSPositionUtils.isSet(newPosition.right));
		assertTrue("css pos width should be set", CSSPositionUtils.isSet(newPosition.width));

		assertEquals("css pos right should be set to the right value copied from the target", "386", newPosition.right);

		assertFalse("css pos top should NOT be set", CSSPositionUtils.isSet(newPosition.bottom));
		assertTrue("css pos bottom should be set", CSSPositionUtils.isSet(newPosition.top));
		assertTrue("css pos height should be set", CSSPositionUtils.isSet(newPosition.height));

		assertEquals("css pos top should be set to the computed value", "269", newPosition.top);
		assertEquals("css pos height should be set", "30", newPosition.height);
	}
}