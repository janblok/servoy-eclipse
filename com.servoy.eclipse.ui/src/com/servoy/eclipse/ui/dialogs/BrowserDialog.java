/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.progress.IProgressService;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IMainConceptsPageAction;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.preferences.StartupPreferences;
import com.servoy.eclipse.ui.views.TutorialView;
import com.servoy.eclipse.ui.wizards.ImportSolutionWizard;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 * @since 2020.03
 *
 */
public class BrowserDialog extends Dialog
{

	private String url;
	private Browser browser;
	private org.eclipse.swt.chromium.Browser chromiumBrowser;
	private Shell shell;
	private boolean showSkipNextTime;
	private static final int MIN_WIDTH = 900;
	private static final int MIN_HEIGHT = 600;

	/**
	 * @param parentShell
	 */
	public BrowserDialog(Shell parentShell, String url, boolean modal, boolean showSkipNextTime)
	{
		super(parentShell, modal ? SWT.APPLICATION_MODAL : SWT.MODELESS);
		this.url = url;
		this.showSkipNextTime = showSkipNextTime;
	}


	public Object open()
	{
		return this.open(false);
	}

	public Object open(boolean useChromiumHint)
	{
		Rectangle size = getParent().getBounds();
		int newWidth = (size.width / 1.5) < MIN_WIDTH ? MIN_WIDTH : (int)(size.width / 1.5);
		int newHeight = (size.height / 1.4) < MIN_HEIGHT ? MIN_HEIGHT : (int)(size.height / 1.4);
		Dimension newSize = new Dimension(newWidth, newHeight);

		int locationX, locationY;
		locationX = (size.width - newWidth) / 2 + size.x;
		locationY = (size.height - newHeight) / 2 + size.y;

		return this.open(new Point(locationX, locationY), newSize, useChromiumHint);
	}

	public Object open(Point location, Dimension size, boolean useChromiumHint)
	{
		Shell parent = getParent();
		shell = new Shell(parent, SWT.DIALOG_TRIM | getStyle());
		shell.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		if (!ApplicationServerRegistry.get().hasDeveloperLicense())
		{
			this.showSkipNextTime = false;
		}

		if (showSkipNextTime)
		{
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 1;
			shell.setLayout(gridLayout);
		}
		else
		{
			shell.setLayout(new FillLayout());
		}
		final Button[] showNextTime = new Button[1];
		//load html file in textReader
		if (useChromiumHint && new DesignerPreferences().useChromiumBrowser())
		{
			chromiumBrowser = new org.eclipse.swt.chromium.Browser(shell, SWT.NONE);
		}
		else
		{
			browser = new Browser(shell, SWT.NONE);
		}
		LocationListener locationListener = new LocationListener()
		{
			@Override
			public void changing(LocationEvent event)
			{
				String loc = event.location;
				if (loc == null) return;
				if (loc.equals(url)) return;

				IntroURLParser parser = new IntroURLParser(loc);
				if (parser.hasIntroUrl())
				{
					// stop URL first.
					event.doit = false;
					// execute the action embedded in the IntroURL
					final IntroURL introURL = parser.getIntroURL();
					if (IntroURL.RUN_ACTION.equals(introURL.getAction()))
					{
						String pluginId = introURL.getParameter(IntroURL.KEY_PLUGIN_ID);
						String className = introURL.getParameter(IntroURL.KEY_CLASS);

						final Object actionObject = ModelLoaderUtil.createClassInstance(pluginId, className);

						if (actionObject instanceof IMainConceptsPageAction)
						{
							Display display = Display.getCurrent();
							BusyIndicator.showWhile(display, new Runnable()
							{
								public void run()
								{
									((IMainConceptsPageAction)actionObject).runAction(introURL);
								}
							});
							if (!shell.isDisposed()) shell.close();
							return;
						}
					}

					String importSample = introURL.getParameter("importSample");
					final String[] showTutorial = new String[] { null };
					if (importSample != null)
					{
						try (InputStream is = new URL(importSample.startsWith("https://") ? importSample
							: "https://" + importSample).openStream())
						{
							String[] urlParts = importSample.split("/");
							if (urlParts.length >= 1)
							{
								final String solutionName = urlParts[urlParts.length - 1].replace(".servoy", "");
								ServoyProject sp = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
								IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
								if (sp == null)
								{
									if (Arrays.stream(ApplicationServerRegistry.get().getServerManager().getServerConfigs())
										.filter(
											s -> s.isEnabled() &&
												ApplicationServerRegistry.get().getServerManager().getServer(s.getServerName()) != null &&
												((IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(s.getServerName()))
													.isValid())
										.count() == 0)
									{
										// no valid servers
										UIUtils.reportError("No valid server",
											"There is no valid server defined in Servoy Developer, you must define servers / install PostgreSQL before importing the sample solution.");
										return;
									}

									if (!shell.isDisposed()) shell.close();

									final File importSolutionFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(),
										solutionName + ".servoy");
									if (importSolutionFile.exists())
									{
										importSolutionFile.delete();
									}
									try (FileOutputStream fos = new FileOutputStream(importSolutionFile))
									{
										Utils.streamCopy(is, fos);
									}

									//TODO import packages if (importPackagesRunnable != null) progressService.run(true, false, importPackagesRunnable);
									progressService.run(true, false, (IProgressMonitor monitor) -> {
										ImportSolutionWizard importSolutionWizard = new ImportSolutionWizard();
										importSolutionWizard.setSolutionFilePath(importSolutionFile.getAbsolutePath());
										importSolutionWizard.setAllowSolutionFilePathSelection(false);
										importSolutionWizard.init(PlatformUI.getWorkbench(), null);
										importSolutionWizard.setReportImportFail(true);
										importSolutionWizard.setSkipModulesImport(false);
										importSolutionWizard.setAllowDataModelChanges(true);
										importSolutionWizard.setImportSampleData(true);
										importSolutionWizard.shouldAllowSQLKeywords(true);
										importSolutionWizard.shouldCreateMissingServer(true);

										ServoyResourcesProject project = ServoyModelManager.getServoyModelManager().getServoyModel()
											.getActiveResourcesProject();
										String resourceProjectName = project == null ? getNewResourceProjectName() : null;

										importSolutionWizard.doImport(importSolutionFile, resourceProjectName, project, false, false, true, null, null,
											monitor, false, false);
										if (importSolutionWizard.isMissingServer() != null)
										{
											showTutorial[0] = introURL.getParameter("createDBConn");
										}

										try
										{
											importSolutionFile.delete();
										}
										catch (RuntimeException e)
										{
											Debug.error(e);
										}
									});
								}
								else
								{
									if (!shell.isDisposed()) shell.close();
									progressService.run(true, false, (IProgressMonitor monitor) -> {
										ServoyModelManager.getServoyModelManager()
											.getServoyModel()
											.setActiveProject(ServoyModelManager.getServoyModelManager()
												.getServoyModel()
												.getServoyProject(solutionName), true);
									});
								}
								ServoyModelManager.getServoyModelManager()
									.getServoyModel()
									.addActiveProjectListener(new IActiveProjectListener()
									{

										@Override
										public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
										{
											return true;
										}

										@Override
										public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
										{
										}

										@Override
										public void activeProjectChanged(ServoyProject activeProject)
										{
											Display.getDefault().asyncExec(() -> {
												if (introURL.getParameter("showTinyTutorial") != null)
												{
													showTinyTutorial(introURL);
													if (!shell.isDisposed()) shell.close();
												}
											});
											ServoyModelManager.getServoyModelManager()
												.getServoyModel()
												.removeActiveProjectListener(this);
										}
									});
							}
						}
						catch (Exception e)
						{
							Debug.error(e);
						}
					}
					if (showTutorial[0] != null)
					{
						showTinyTutorial(showTutorial[0]);
						return;
					}
					else if (introURL.getParameter("showTinyTutorial") != null)
					{
						showTinyTutorial(introURL);
						if (!shell.isDisposed()) shell.close();
						return;
					}
					if (introURL.getParameter("maximize") != null)
					{
						if (showNextTime != null && showNextTime[0] != null)
						{
							showNextTime[0].setVisible(false);
						}
						Rectangle bounds = parent.getBounds();
						if (browser != null)
						{
							browser.setSize(bounds.width, bounds.height);
						}
						else
						{
							chromiumBrowser.setSize(bounds.width, bounds.height);
						}
						shell.setBounds(bounds);
						shell.layout(true, true);
						return;
					}

					if (introURL.getParameter("normalize") != null)
					{
						Rectangle size = getParent().getBounds();
						Rectangle bounds = new Rectangle((size.width - (int)(size.width / 1.5)) / 2 + size.x,
							(size.height - (int)(size.height / 1.4)) / 2 + size.y, (int)(size.width / 1.5),
							(int)(size.height / 1.4));
						if (browser != null)
						{
							browser.setSize(bounds.width, bounds.height);
						}
						else
						{
							chromiumBrowser.setSize(bounds.width, bounds.height);
						}
						shell.setBounds(bounds);
						shell.layout(true, true);
						return;
					}

					try
					{
						introURL.execute();
					}
					catch (Exception e)
					{
						Debug.error(e);
					}

				}
			}

			private String getNewResourceProjectName()
			{
				String newResourceProjectName = "resources";
				int counter = 1;
				while (ServoyModel.getWorkspace().getRoot().getProject(newResourceProjectName).exists())
				{
					newResourceProjectName = "resources" + counter++;
				}
				return newResourceProjectName;
			}

			protected void showTinyTutorial(final String tutorialUrl)
			{
				try
				{
					TutorialView view = (TutorialView)PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow()
						.getActivePage()
						.showView(TutorialView.PART_ID);
					view.open(tutorialUrl.startsWith("https://") ? tutorialUrl : "https://" + tutorialUrl);
				}
				catch (PartInitException e)
				{
					Debug.error(e);
				}
			}

			protected void showTinyTutorial(final IntroURL introURL)
			{
				try
				{
					TutorialView view = (TutorialView)PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow()
						.getActivePage()
						.showView(TutorialView.PART_ID);
					view.open(introURL.getParameter("showTinyTutorial").startsWith("https://") ? introURL.getParameter("showTinyTutorial")
						: "https://" + introURL.getParameter("showTinyTutorial"));
				}
				catch (PartInitException e)
				{
					Debug.error(e);
				}
			}

			@Override
			public void changed(LocationEvent event)
			{
			}
		};
		if (browser != null)
		{
			browser.addLocationListener(locationListener);
			browser.setUrl(url);
			browser.setSize(size.width, size.height);
		}
		else
		{
			chromiumBrowser.addLocationListener(locationListener);
			chromiumBrowser.setUrl(url);
			chromiumBrowser.setSize(size.width, size.height);
		}
		if (showSkipNextTime)
		{
			if (browser != null)
			{
				browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			}
			else
			{
				chromiumBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			}
			showNextTime[0] = new Button(shell, SWT.CHECK);
			showNextTime[0].setText("Do not show this dialog anymore");
			showNextTime[0].setSelection(!Utils.getAsBoolean(Settings.getInstance().getProperty(StartupPreferences.STARTUP_SHOW_START_PAGE, "true")));
			showNextTime[0].addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					Settings.getInstance().setProperty(StartupPreferences.STARTUP_SHOW_START_PAGE,
						new Boolean(!showNextTime[0].getSelection()).toString());
				}
			});
			showNextTime[0].setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		}
		shell.setLocation(location);
		// in chromium i have to set size, else it shows very small
		if (Util.isMac() || chromiumBrowser != null)
		{
			Rectangle rect = shell.computeTrim(location.x, location.y, size.width, size.height);
			shell.setSize(rect.width, rect.height);
		}
		else
		{
			shell.pack();
		}
		shell.open();
		if (getStyle() == SWT.APPLICATION_MODAL)
		{
			Display display = parent.getDisplay();
			while (!shell.isDisposed())
			{
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		return null;
	}

	public boolean isDisposed()
	{
		return shell == null || shell.isDisposed();
	}


	/**
	 * @param optString
	 */
	public void setUrl(String url)
	{
		this.url = url;
		if (browser != null)
		{
			browser.setUrl(url);
		}
		else
		{
			chromiumBrowser.setUrl(url);
		}
	}

	public void setLocationAndSize(Point location, Dimension size)
	{
		if (browser != null)
		{
			browser.setSize(size.width, size.height);
		}
		else
		{
			chromiumBrowser.setSize(size.width, size.height);
		}
		shell.setLocation(location);
		shell.setSize(size.width, size.height);
	}
}