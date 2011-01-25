/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

package com.servoy.eclipse.designer.editor;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.RGB;

import com.servoy.eclipse.ui.resource.ColorResource;

/**
 * Figure for dragging part or form resizing.
 * 
 * @author rgansevles
 *
 */
public class DragFormBoundsFigure extends Figure
{
	public static final RGB PART_HANDLE_RGB = new RGB(0x41, 0x8F, 0xD4);

	private Polyline line;
	private PartFigure partFigure;

	public void addPartHandle(String text)
	{
		if (partFigure != null)
		{
			remove(partFigure);
		}
		partFigure = new PartFigure();
		partFigure.setText(text);

		add(partFigure);
	}

	protected Polyline getLine()
	{
		if (line == null)
		{
			// Use a line for feedback
			line = new Polyline();
			line.setForegroundColor(ColorResource.INSTANCE.getColor(PART_HANDLE_RGB)); // TODO: add preference 
			line.setLineStyle(Graphics.LINE_DOT);
			add(line);
		}
		return line;

	}

	/**
	 * @param y
	 * @param width
	 */
	public void setHorizontalLine(int y, int width)
	{
		getLine().removeAllPoints();
		getLine().addPoint(new Point(-FormBorderGraphicalEditPart.BORDER_MARGIN, y));
		getLine().addPoint(new Point(width, y));

		if (partFigure != null)
		{
			Dimension dim = partFigure.getMinimumSize(0, 0);
			partFigure.setBounds(new Rectangle(width, y, dim.width, dim.height));

			setBounds(line.getBounds().union(partFigure.getBounds()));
		}
		else
		{
			setBounds(line.getBounds());
		}
	}

	public void setVerticalLine(int x, int height)
	{
		getLine().removeAllPoints();
		getLine().addPoint(new Point(x, -FormBorderGraphicalEditPart.BORDER_MARGIN));
		getLine().addPoint(new Point(x, height));
		setBounds(line.getBounds());
	}

}
