/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IWindow;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.actions.CopyAction;
import com.servoy.eclipse.designer.editor.rfb.actions.CutAction;
import com.servoy.eclipse.designer.editor.rfb.actions.DeleteAction;
import com.servoy.eclipse.designer.editor.rfb.actions.FixedSelectAllAction;
import com.servoy.eclipse.designer.editor.rfb.actions.PasteAction;
import com.servoy.eclipse.designer.outline.FormOutlinePage;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.DefaultFieldPositioner;
import com.servoy.eclipse.ui.util.SelectionProviderAdapter;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Design page for browser based rfb editor.
 *
 * @author rgansevles
 *
 */
public class RfbVisualFormEditorDesignPage extends BaseVisualFormEditorDesignPage
{

	/**
	 * A unique editor content session id. Making sure we reuse the session across editors.
	 */
	public static final String CONTENT_SESSION_ID = "devsession";

	// for setting selection when clicked in editor
	private final ISelectionProvider selectionProvider = new SelectionProviderAdapter()
	{
		@Override
		public ISelection getSelection()
		{
			// select form if nothing else is selected
			return selection == null ? new StructuredSelection(editorPart.getForm()) : selection;
		}

		@Override
		public void setSelection(ISelection selection)
		{
			if (editorPart != PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart())
			{
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(editorPart);
			}
			super.setSelection(selection);
		}
	};

	private final IFieldPositioner fieldPositioner = new DefaultFieldPositioner(new Point(40, 50))
	{

		private boolean locationNeverSet = true;

		@Override
		public void setDefaultLocation(Point defaultLocation)
		{
			super.setDefaultLocation(defaultLocation);
			locationNeverSet = false;
		}

		@Override
		public Point getNextLocation(Point location)
		{
			if (location == null)
			{
				if (locationNeverSet)
				{
					defaultLocation.x += 20;
					defaultLocation.y += 20;
				}
				return defaultLocation;
			}
			return location;
		};
	};

	// for updating selection in editor when selection changes in IDE
	private final RfbSelectionListener selectionListener = new RfbSelectionListener();

	// for reloading palette when components change
	private final RfbWebResourceListener resourceChangedListener = new RfbWebResourceListener();

	private Browser browser;

	private EditorWebsocketSession editorWebsocketSession;

	private String layout = null;
	private String editorId = null;

	public RfbVisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	public void createPartControl(Composite parent)
	{
		// always reload the current spec so that always the latest stuff is shown.
		FormElementHelper.INSTANCE.reload(); // we can't reload just specs cause lately FormElement can add size/location/anchors to spec and we don't want to use old/cached/already initialized form elements while new specs were reloaded
		// Serve requests for rfb editor
		editorId = UUID.randomUUID().toString();
		WebsocketSessionManager.addSession(editorWebsocketSession = new EditorWebsocketSession(editorId));
		editorWebsocketSession.registerServerService("formeditor", new EditorServiceHandler(editorPart, selectionProvider, selectionListener, fieldPositioner));
		selectionListener.setEditorWebsocketSession(editorWebsocketSession);
		resourceChangedListener.setEditorWebsocketSession(editorWebsocketSession);
		try
		{
			browser = new Browser(parent, SWT.NONE);
		}
		catch (SWTError e)
		{
			ServoyLog.logError(e);
			return;
		}

		refreshBrowserUrl(false);
		try
		{
			// install fake WebSocket in case browser does not support it
			SwtWebsocket.installFakeWebSocket(browser);
			// install console
			new BrowserFunction(browser, "consoleLog")
			{
				@Override
				public Object function(Object[] arguments)
				{
					if (arguments.length > 1)
					{
						if ("log".equals(arguments[0]))
						{
							ServoyLog.logInfo((String)arguments[1]);
						}
						else if ("error".equals(arguments[0]))
						{
							ServoyLog.logError((String)arguments[1], null);
						}
					}
					return null;
				}
			};
		}
		catch (Exception e)
		{
			ServoyLog.logError("couldn't load the editor: ", e);
		}


	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class type)
	{
		Object adapter = super.getAdapter(type);

		if (type.equals(IContentOutlinePage.class))
		{
			adapter = new FormOutlinePage(editorPart.getForm(), null, getActionRegistry());
		}

		return adapter;
	}

	/**
	 * @param form
	 * @return
	 */
	private String computeLayout(Form form)
	{
		if (form.isResponsiveLayout()) return "flow";
		else return "absolute";
	}

	public void refreshBrowserUrl(boolean force)
	{
		Form form = editorPart.getForm();
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form);
		String newLayout = computeLayout(flattenedForm);
		if (!Utils.equalObjects(layout, newLayout))
		{
			layout = newLayout;
			Dimension formSize = flattenedForm.getSize();
			String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/rfb/angular/index.html?s=" +
				form.getSolution().getName() + "&l=" + layout + "&f=" + form.getName() + "&w=" + formSize.getWidth() + "&h=" + formSize.getHeight() +
				"&editorid=" + editorId + "&c_sessionid=" + CONTENT_SESSION_ID;
			try
			{
				ServoyLog.logInfo("Browser url for editor: " + url);
				browser.setUrl(url + "&replacewebsocket=true");
			}
			catch (Exception ex)
			{
				ServoyLog.logError("couldn't load the editor: " + url, ex);
			}
		}
		else if (force)
		{
			browser.refresh();
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		super.init(site, input);
		site.setSelectionProvider(selectionProvider);
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
	}

	@Override
	public void dispose()
	{
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
	}

	protected INGClientWebsocketSession getContentWebsocketSession()
	{
		return (INGClientWebsocketSession)WebsocketSessionManager.getSession(WebsocketSessionFactory.DESIGN_ENDPOINT, CONTENT_SESSION_ID);
	}

	protected IWindow getContentWindow()
	{
		INGClientWebsocketSession editorContentWebsocketSession = getContentWebsocketSession();
		if (editorContentWebsocketSession != null)
		{
			return editorContentWebsocketSession.getActiveWindow(getEditorPart().getForm().getName());
		}

		return null;
	}

	@Override
	public void refreshAllParts()
	{
		IWindow window = getContentWindow();
		if (window != null)
		{
			CurrentWindow.runForWindow(window, new Runnable()
			{
				@Override
				public void run()
				{
					INGClientWebsocketSession editorContentWebsocketSession = getContentWebsocketSession();
					editorContentWebsocketSession.getEventDispatcher().addEvent(new FormUpdater(editorContentWebsocketSession, null, getEditorPart().getForm()));
				}
			});
		}
	}

	@Override
	public void refreshPersists(List<IPersist> persists)
	{
		IWindow window = null;
		final INGClientWebsocketSession editorContentWebsocketSession = (INGClientWebsocketSession)WebsocketSessionManager.getSession(
			WebsocketSessionFactory.DESIGN_ENDPOINT, CONTENT_SESSION_ID);
		if (editorContentWebsocketSession != null)
		{
			window = editorContentWebsocketSession.getActiveWindow(getEditorPart().getForm().getName());
		}

		if (window == null)
		{
			Debug.warn("Receiving changes for editor without active window");
			return;
		}

		final Map<Form, List<IFormElement>> frms = new HashMap<Form, List<IFormElement>>();
		Form changedForm = null;
		Media cssFile = null;
		for (IPersist persist : persists)
		{
			if (persist instanceof IFormElement || persist instanceof Tab)
			{
				IPersist parent = persist;
				if (persist instanceof Tab)
				{
					parent = ((Tab)persist).getParent();
					persist = parent;
				}
				while (parent != null)
				{
					if (parent instanceof Form)
					{
						List<IFormElement> list = frms.get(parent);
						if (list == null)
						{
							frms.put((Form)parent, list = new ArrayList<IFormElement>());
						}
						list.add((IFormElement)persist);
						break;
					}
					parent = parent.getParent();
				}
			}
			else if (persist instanceof Form)
			{
				changedForm = (Form)persist;
			}
			else if (persist instanceof Part)
			{
				changedForm = (Form)persist.getParent();
			}
			else if (persist instanceof LayoutContainer)
			{
				changedForm = (Form)persist.getAncestor(IRepository.FORMS);
			}
			else if (persist instanceof Media)
			{
				if (((Media)persist).getName().endsWith(".css"))
				{
					cssFile = (Media)persist;
				}
			}
		}

		final Media fcssFile = cssFile;
		final Form fchangedForm = changedForm;

		CurrentWindow.runForWindow(window, new Runnable()
		{
			@Override
			public void run()
			{
				if (frms.size() > 0 || fchangedForm != null)
				{
					editorContentWebsocketSession.getEventDispatcher().addEvent(new FormUpdater(editorContentWebsocketSession, frms, fchangedForm));
				}
				if (fcssFile != null)
				{
					ISupportChilds parent = fcssFile.getParent();
					if (parent instanceof Solution)
					{
						Solution theSolution = (Solution)parent;
						//TODO change to commented code once IPersistChangeListener is notified only the modified file, not all media files: case SVY-7581
						/*
						 * Object property = theSolution.getProperty(StaticContentSpecLoader.PROPERTY_STYLESHEET.getPropertyName()); if
						 * (property.equals(cssFile.getID()) || (Integer)property == 0) websocketSession.getEventDispatcher().addEvent( new
						 * SendCSSFile(theSolution));
						 */
						editorContentWebsocketSession.getEventDispatcher().addEvent(new SendCSSFile(editorContentWebsocketSession, theSolution));
					}
				}
			}
		});
	}

	@Override
	public boolean showPersist(IPersist persist)
	{
		selectionProvider.setSelection(new StructuredSelection(persist));
		return true;
	}

	@Override
	public void setFocus()
	{
	}

	@Override
	protected DeleteAction createDeleteAction()
	{
		return new DeleteAction(editorPart);
	}

	@Override
	protected IAction createCopyAction()
	{
		return new CopyAction(editorPart);
	}

	@Override
	protected IAction createCutAction()
	{
		return new CutAction(editorPart);
	}

	@Override
	protected IAction createPasteAction()
	{
		return new PasteAction(Activator.getDefault().getDesignClient(), selectionProvider, editorPart, fieldPositioner);
	}

	@Override
	protected SelectAllAction createSelectAllAction()
	{
		return new FixedSelectAllAction(editorPart, selectionProvider);
	}

}