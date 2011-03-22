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
package com.servoy.eclipse.ui.views.solutionexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.TextActionHandler;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.servoy.eclipse.core.I18NChangeListener;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ISolutionMetaDataChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyUpdatingProject;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeComparer;
import com.servoy.eclipse.ui.node.UserNodeListDragSourceListener;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.preferences.SolutionExplorerPreferences;
import com.servoy.eclipse.ui.search.SearchAction;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.FilterDelayJob;
import com.servoy.eclipse.ui.util.FilteredEntity;
import com.servoy.eclipse.ui.views.ModifiedPropertySheetEntry;
import com.servoy.eclipse.ui.views.ModifiedPropertySheetPage;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ActivateSolutionAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.AddAsModuleAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ChangeResourcesProjectAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.CollapseTreeAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ContextAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.CopyAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.CopyTableAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.CutAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DebugMethodAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeleteI18NAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeletePersistAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeleteScriptAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeleteServerAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeleteSolutionAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeleteTableAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DuplicatePersistAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DuplicateServerAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.EditI18nAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.EditSecurityAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.EditVariableAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.EnableServerAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ExpandNodeAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.I18NCreateFromDBAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.I18NExternalizeAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.I18NWriteToDBAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ImportMediaAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ImportMediaFolderAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.LinkWithEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.LoadRelationsAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.MovePersistAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.MoveTextAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMethodAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewPostgresDbAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewRelationAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewServerAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewSybaseDbAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewTableAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewValueListAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewVariableAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenFormEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenI18NAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenMediaAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenNewFormWizardAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenRelationAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenScriptAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenServerAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenSqlEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenStyleAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenTableAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenValueListAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenWizardAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OrientationAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OverrideMethodAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.PasteAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RefreshAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ReloadTablesAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RemoveModuleAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RemoveSolutionProtectionAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RenameMediaAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RenamePersistAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RenameSolutionAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ReplaceServerAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ReplaceTableAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.SelectAllAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.SuggestForeignTypesAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.SynchronizeTablesAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ToggleFormCommandsAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.UpdateServoySequencesAction;
import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;
import com.servoy.eclipse.ui.wizards.ImportSolutionWizard;
import com.servoy.eclipse.ui.wizards.NewFormWizard;
import com.servoy.eclipse.ui.wizards.NewSolutionWizard;
import com.servoy.eclipse.ui.wizards.NewStyleWizard;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * This view is meant to be similar to the old designer's tree (in editor) in looks and in functionality. It will show a logical presentation of the eclipse
 * workspace's Servoy related solutions/styles.
 */
public class SolutionExplorerView extends ViewPart implements ISelectionChangedListener, FilteredEntity, IShowInSource, IShowInTarget
{
	private final Color yellow = new Color(null, 255, 255, 0);
	private final Color light_grey = new Color(null, 120, 120, 120);

	public static final String PART_ID = "com.servoy.eclipse.ui.views.SolutionExplorerView"; //$NON-NLS-1$

	private static final long TREE_FILTER_DELAY_TIME = 500;

	private static final int DEFAULT_SEARCH_FIELD_WIDTH = 64; // Widget.DEFAULT_WIDTH
	/**
	 * Constant used to point to a method.
	 */
	public static final int METHOD = 0;

	/**
	 * Constant used to point to a variable.
	 */
	public static final int VARIABLE = 1;

	/**
	 * The key to be used is <code>DIALOGSTORE_RATIO + fSplitter.getOrientation()</code>.
	 */
	private static final String DIALOGSTORE_RATIO = "SolutionExplorerView.ratio"; //$NON-NLS-1$

	public static final String DIALOGSTORE_VIEWORIENTATION = "SolutionExplorerView.orientation";

	public static final String USE_OPEN_AS_DEFAULT = "SolutionExplorerView.useOpenAsDefaultAction";

	public static final String INCLUDE_ENTRIES_FROM_MODULES = "SolutionExplorerView.includeEntriedFromModules";

	/**
	 * This orientation tells the view to put the list (outline) part of the view under the tree.
	 */
	public static final int VIEW_ORIENTATION_VERTICAL = 0;

	/**
	 * This orientation tells the view to put the list (outline) part of the view on the right side of the tree.
	 */
	public static final int VIEW_ORIENTATION_HORIZONTAL = 1;

	/**
	 * This orientation tells the view to decide based on it's size which of the other 2 orientations it should use.
	 */
	public static final int VIEW_ORIENTATION_AUTOMATIC = 2;

	private TreeViewer tree;

	private DrillDownAdapter drillDownAdapter;

	private ServoyProject[] treeRoots; // because of the drilldownadapter, roots in the tree might change

	private ContextAction newActionInListPrimary;

	private ContextAction newActionInTreePrimary;

	private ContextAction newActionInTreeSecondary;

	private ReplaceTableAction replaceActionInTree;
	private ReplaceServerAction replaceServerAction;

	private OpenSqlEditorAction openSqlEditorAction;

	private ContextAction deleteActionInList;

	private ContextAction openAction;

	private ContextAction openActionInTree;

	private ContextAction deleteActionInTree;

	private AddAsModuleAction addAsModuleAction;

	private RenameSolutionAction renameSolutionAction;

	private RemoveModuleAction removeModuleAction;

	private MoveTextAction moveCode;

	private SearchAction searchTreeAction;
	private SearchAction searchListAction;

	private OverrideMethodAction overrideMethod;

	private MoveTextAction moveSample;

	private ActivateSolutionAction setActive;

	private CutAction cutAction;

	private CopyAction copyAction;

	private PasteAction pasteAction;

	private TextActionHandler textActionHandler;

	private ExpandNodeAction expandNodeAction;

	private Action collapseTreeAction;

	private Action linkWithEditorAction;

	private ChangeResourcesProjectAction changeResourcesProjectAction;
	private RemoveSolutionProtectionAction removeSolutionProtectionAction;

	private DuplicatePersistAction duplicateFormAction;
	private MovePersistAction moveFormAction;
	private RenamePersistAction renameFormAction;

	private NewPostgresDbAction newPostgresqlDatabase;
	private NewSybaseDbAction newSybaseDatabase;

	private DuplicateServerAction duplicateServer;
	private EnableServerAction enableServer;

	private RefreshAction fRefreshAction;

	private EditVariableAction editVariableAction;

	private DebugMethodAction debugMethodAction;

	private ContextAction newActionInListSecondary;

	private SynchronizeTablesAction synchronizeTablesWithDBAction;

	private LoadRelationsAction loadRelationsAction;

	private ToggleFormCommandsAction toggleFormCommandsActions;

	private TextFilter treeFilter;

	private final Map<Bean, Object> beanCache = new HashMap<Bean, Object>();

	private TableViewer list;

	private SelectionProviderMediator fSelectionProviderMediator;

	private SashForm fSplitter;

	private Composite fParent;

	private ActiveEditorTracker fPartListener;

	private final IDialogSettings fDialogSettings;

	private OrientationAction[] fToggleOrientationActions;

	private int fCurrentOrientation = VIEW_ORIENTATION_AUTOMATIC;

	private ToolBar listToolBar;

	private IActiveProjectListener activeProjectListener;

//	private IPersistListener persistListener;
	private IPersistChangeListener persistChangeListener;

	private ISolutionMetaDataChangeListener solutionMetaDataChangeListener;

	private ControlListener resizeListener;

	private ViewLabelProvider labelProvider;

	private MenuItem openModeToggleButton;

	private MenuItem includeModulesToggleButton;

	private Menu listDropDownMenu;

	private StatusBarUpdater statusBarUpdater;

	private IResourceChangeListener resourceChangeListener;

	private IElementComparer nodeContentComparer;

	private ITableListener tableListener;

	private IServerListener serverListener;

	private ImportMediaFolderAction importMediaFolder;

	private RenameMediaAction renameMediaAction;

	private MovePersistAction movePersistAction;
	private DuplicatePersistAction duplicatePersistAction;

	private IAction selectAllActionInTree;

	private IAction selectAllActionInlist;

	private IAction exportActiveSolutionAction;

	private IAction importSolutionAction;

	private IAction suggestForeignTypes;

	private OpenFormEditorAction openFormEditorAction;

	private IAction i18nExternalizeAction;
	private IAction i18nCreateFromDBAction;
	private IAction i18nWriteToDBAction;
	private I18NChangeListener i18nChangeListener;

	//copy table action
	private CopyTableAction copyTable;

	private ReloadTablesAction reloadTablesOfServerAction;
	private UpdateServoySequencesAction updateServoySequencesAction;

	private DecoratingColumnLabelProvider decoratingLabelProvider;

	private ILabelProviderListener labelProviderListener;

	class ViewLabelProvider extends ColumnLabelProvider
	{
		private Image null_image = null;
		private final Point tooltipShift = new Point(10, 10);

		ViewLabelProvider()
		{
			null_image = Activator.getDefault().loadImageFromBundle("gray_dot.gif"); //$NON-NLS-1$
		}

		@Override
		public Image getImage(Object obj)
		{
			Image retval = null;
			if (obj instanceof SimpleUserNode)
			{
				retval = (Image)((SimpleUserNode)obj).getIcon();
			}
			if (retval == null)
			{
				retval = null_image;
			}
			return retval;
		}

		@Override
		public Font getFont(Object element)
		{
			if (element instanceof SimpleUserNode &&
				(((SimpleUserNode)element).getType() == UserNodeType.GRAYED_OUT || (((SimpleUserNode)element).getType() == UserNodeType.I18N_FILE_ITEM && !((SimpleUserNode)element).isEnabled())))
			{
				return JFaceResources.getFontRegistry().getItalic((JFaceResources.DIALOG_FONT));
			}
			if (treeFilter != null)
			{
				Set<Object> tmp = treeFilter.getMatchingNodes();
				if (tmp.contains(element))
				{
					return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
				}
				else if (treeFilter.getParentsOfMatchingNodes().contains(element))
				{
					return JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);
				}
			}
			return JFaceResources.getFontRegistry().get(JFaceResources.DIALOG_FONT);
		}

		@Override
		public String getToolTipText(Object element)
		{
			String result = null;
			if (element instanceof SimpleUserNode)
			{
				result = ((SimpleUserNode)element).getToolTipText();
				// nicely remove html markup as SWT does not support it
				result = Utils.stringReplaceCaseInsensitiveSearch(result, "<br>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				result = Utils.stringRemoveTags(result);
			}
			return result;
		}

		@Override
		public Point getToolTipShift(Object object)
		{
			return tooltipShift;
		}

		@Override
		public Color getForeground(Object element)
		{
			if (element instanceof SimpleUserNode &&
				(((SimpleUserNode)element).getType() == UserNodeType.GRAYED_OUT || (((SimpleUserNode)element).getType() == UserNodeType.I18N_FILE_ITEM && !((SimpleUserNode)element).isEnabled())))
			{
				return light_grey;
			}
			else return super.getForeground(element);
		}

		@Override
		public Color getBackground(Object element)
		{
			if (element instanceof SimpleUserNode)
			{
				SimpleUserNode sun = (SimpleUserNode)element;
				if (sun.getType() == UserNodeType.FORM) // should we add more
				// types to highlight?
				// (where highlight is as
				// in old developer)
				{
					Object real = sun.getRealObject();
					if (real instanceof IPersist)
					{
						IEditorPart ep = fPartListener.getActiveEditor();
						if (ep != null)
						{
							IPersist persist = (IPersist)ep.getAdapter(IPersist.class);
							if (persist != null && ((IPersist)real).getUUID().equals(persist.getUUID()))
							{
								return yellow;// Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);
							}
						}
					}
				}
			}
			return super.getBackground(element);
		}
	}

	/**
	 * The constructor.
	 */
	public SolutionExplorerView()
	{
		fDialogSettings = Activator.getDefault().getDialogSettings();
	}

	public void selectionChanged(SelectionChangedEvent e)
	{
		if (e.getSelectionProvider() == tree)
		{
			IStructuredSelection sel = (IStructuredSelection)e.getSelection();
			if (sel.size() == 1)
			{
				list.setInput(sel.getFirstElement());
			}
			else
			{
				list.setInput(null);
			}
		}
	}

	// This is a call-back that will allow us
	// to create the viewer and initialize it.
	@Override
	public void createPartControl(Composite parent)
	{
		fParent = parent;

		labelProvider = new ViewLabelProvider();
		createSplitter(parent);
//		createPersistListener();
		createTreeViewer(fSplitter);
		createListViewer(fSplitter);

		// initDragAndDrop();

		// PlatformUI.getWorkbench().getHelpSystem().setHelp(fPagebook,
		// IJavaHelpContextIds.CALL_HIERARCHY_VIEW);

		fSelectionProviderMediator = new SelectionProviderMediator(new StructuredViewer[] { tree, list }, null);

		IStatusLineManager slManager = getViewSite().getActionBars().getStatusLineManager();
		statusBarUpdater = new StatusBarUpdater(slManager);
		list.addSelectionChangedListener(statusBarUpdater);
		getSite().setSelectionProvider(fSelectionProviderMediator);

		initializeEditmenuActions();
		createActions();

		hookContextMenu();
		hookTreeDoubleClickAction();
		contributeToActionBars();

		initOrientation();

		restoreSplitterRatio();
		addPartListener();

		KeyListener refreshListener = createKeyListener();
		tree.getControl().addKeyListener(refreshListener);
		list.getControl().addKeyListener(refreshListener);
		addResizeListener(parent);

		IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService)this.getSite().getAdapter(IWorkbenchSiteProgressService.class);
		Job job = new WorkbenchJob("Setting up solution explorer tree") //$NON-NLS-1$
		{
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor)
			{
				if (statusBarUpdater == null) return Status.OK_STATUS; // the workbench is already closed/closing
				initTreeViewer();
				addActiveProjectListener();
				addResourceListener();
				addServerAndTableListeners();
				addI18NChangeListener();

				SolutionExplorerTreeContentProvider treeContentProvider = (SolutionExplorerTreeContentProvider)tree.getContentProvider();
				// expand the resources node after startup
				Object[] rootNodes = treeContentProvider.getElements(tree.getInput());
				if (rootNodes.length > 0)
				{
					tree.setExpandedState(rootNodes[0], true);
				}


				//expand the solution node after startup
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
				if (servoyProject != null)
				{
					String activeProjectName = servoyProject.getProject().getName();
					Object activeProjectNode = (treeContentProvider).getSolutionNode(activeProjectName);
					if (activeProjectNode != null)
					{
						tree.setExpandedState(activeProjectNode, true);
					}
				}
				((SolutionExplorerTreeContentProvider)tree.getContentProvider()).setScriptingNodesEnabled(servoyProject != null);
				((SolutionExplorerTreeContentProvider)tree.getContentProvider()).setResourceNodesEnabled(servoyProject != null);

				//expand the servers node if we have invalid (but enabled) servers (expansion will occur after startup)
				IServerManagerInternal serverManager = ServoyModel.getServerManager();
				String[] array = serverManager.getServerNames(true, false, true, true);
				for (String server_name : array)
				{
					IServerInternal server = (IServerInternal)serverManager.getServer(server_name, false, false);
					if (!server.isValid())
					{
						Object serverNode = treeContentProvider.getServers();
						tree.setExpandedState(serverNode, true);
						break;
					}
				}

				return Status.OK_STATUS;
			}
		};
		siteService.schedule(job);
	}

	private void initializeEditmenuActions()
	{
		// cut, copy, paste actions
		// TODO implement CUT, drag and drop in tree and cut/copy/paste in list
		final IActionBars bars = getViewSite().getActionBars();
		cutAction = new CutAction(getSite().getShell().getDisplay());
		copyAction = new CopyAction(getSite().getShell().getDisplay());
		pasteAction = new PasteAction(getSite().getShell().getDisplay());

		textActionHandler = new TextActionHandler(bars);
		textActionHandler.setCutAction(cutAction);
		textActionHandler.setCopyAction(copyAction);
		textActionHandler.setPasteAction(pasteAction);
		textActionHandler.setDeleteAction(deleteActionInTree);
		textActionHandler.setSelectAllAction(selectAllActionInTree);

		tree.addSelectionChangedListener(cutAction);
		tree.addSelectionChangedListener(copyAction);
		tree.addSelectionChangedListener(pasteAction);

		cutAction.setActionDefinitionId("org.eclipse.ui.edit.cut"); //$NON-NLS-1$
		copyAction.setActionDefinitionId("org.eclipse.ui.edit.copy"); //$NON-NLS-1$
		pasteAction.setActionDefinitionId("org.eclipse.ui.edit.paste"); //$NON-NLS-1$

		bars.updateActionBars();

		// delete action
		fSelectionProviderMediator.addSelectionChangedListener(new ISelectionChangedListener()
		{
			boolean treeSelection = false;
			boolean listSelection = false;

			public void selectionChanged(SelectionChangedEvent event)
			{
				if (fSelectionProviderMediator.getViewerInFocus() == tree)
				{
					if (!treeSelection)
					{
						textActionHandler.setDeleteAction(deleteActionInTree);
						textActionHandler.setSelectAllAction(selectAllActionInTree);
						treeSelection = true;
						listSelection = false;
					}
				}
				else if (fSelectionProviderMediator.getViewerInFocus() == list)
				{
					if (!listSelection)
					{
						textActionHandler.setDeleteAction(deleteActionInList);
						textActionHandler.setSelectAllAction(selectAllActionInlist);
						treeSelection = false;
						listSelection = true;
					}
				}
			}
		});
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException
	{
		super.init(site, memento);
	}

	/**
	 * Refreshes the whole tree.
	 */
	public void refreshTreeCompletely()
	{
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				initTreeViewer();
			}
		};
		runAndKeepTreePaths(runnable);
	}

	/**
	 * Refreshes the specified node from the tree. If the node is null, it refreshes the whole tree.
	 * 
	 * @param node the node to be refreshed in the tree or null to refresh the whole tree.
	 */
	public void refreshTreeNodeFromModel(final Object node)
	{
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				if (treeFilter != null && treeFilter.getText() != null && treeFilter.getText().length() > 0)
				{
					filter(treeFilter.getText());
				}
				else if (node == null)
				{
					tree.refresh();
				}
				else
				{
					tree.refresh(node);
				}
			}
		};

		runAndKeepTreePaths(runnable);
	}

	private void runAndKeepTreePaths(final Runnable toRun)
	{
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				boolean treeWidgetDisposed = true;
				try
				{
					treeWidgetDisposed = (tree.getControl().getDisplay() == null);
				}
				catch (Exception e)
				{
					//it means widget has been disposed
				}

				if (!treeWidgetDisposed)
				{
					try
					{
						tree.setComparer(nodeContentComparer); // make tree paths ignore
						// instances and use only
						// node contents
						TreePath[] oldPath = tree.getExpandedTreePaths();
						toRun.run();
						// try to restore the old path
						tree.setExpandedTreePaths(oldPath);
						list.refresh();
					}
					finally
					{
						tree.setComparer(null);
					}
				}
			}
		};
		if (Display.getCurrent() != null)
		{
			runnable.run();
		}
		else
		{
			Display.getDefault().asyncExec(runnable);
		}
	}

	/**
	 * Refresh the outline list.
	 */
	public void refreshList()
	{
		Runnable refresher = new Runnable()
		{
			public void run()
			{
				if (list != null && list.getControl() != null && !list.getControl().isDisposed())
				{
					ISelection sel = list.getSelection();
					list.refresh();
					list.setSelection(sel);
				}
			}
		};
		if (Display.getCurrent() == null)
		{
			Display.getDefault().asyncExec(refresher);
		}
		else
		{
			refresher.run();
		}
	}

	/**
	 * Collapses the tree, refreshes it's contents and then tries to expand it back to the way it was. Also clears the cache of the detail list.
	 */
	public void refreshView()
	{
		((SolutionExplorerListContentProvider)list.getContentProvider()).clearCache();
		refreshTreeCompletely();
	}

	/**
	 * Returns the current selection. u
	 * 
	 * @return the current selection.
	 */
	protected ISelection getSelection()
	{
		StructuredViewer viewerInFocus = fSelectionProviderMediator.getViewerInFocus();
		if (viewerInFocus != null)
		{
			return viewerInFocus.getSelection();
		}
		return StructuredSelection.EMPTY;
	}

	/**
	 * Returns the current selection in the (outline) list.
	 * 
	 * @return the current selection in the (outline) list.
	 */
	public IStructuredSelection getListSelection()
	{
		return (IStructuredSelection)list.getSelection();
	}

	/**
	 * Returns the current selection in the tree.
	 * 
	 * @return the current selection in the tree.
	 */
	public ITreeSelection getTreeSelection()
	{
		return (ITreeSelection)tree.getSelection();
	}

	/**
	 * Returns the node that is selected in the (outline) list, if there is exactly one node selected. If more than 1 node is selected, returns the first node
	 * from the selection.
	 * 
	 * @return the node that is selected in the (outline) list, if there is exactly one node selected. If more than 1 node is selected, returns the first node
	 *         from the selection.
	 */
	public SimpleUserNode getSelectedListNode()
	{
		SimpleUserNode ret = null;
		Object obj = ((IStructuredSelection)list.getSelection()).getFirstElement();
		if (obj instanceof SimpleUserNode)
		{
			ret = (SimpleUserNode)obj;
		}
		return ret;
	}

	/**
	 * Returns the node that is selected in the tree, if there is exactly one node selected. If more than 1 node is selected, returns the first node from the
	 * selection.
	 * 
	 * @return the node that is selected in the tree, if there is exactly one node selected. If more than 1 node is selected, returns the first node from the
	 *         selection.
	 */
	public SimpleUserNode getSelectedTreeNode()
	{
		SimpleUserNode ret = null;
		Object obj = ((ITreeSelection)tree.getSelection()).getFirstElement();
		if (obj instanceof SimpleUserNode)
		{
			ret = (SimpleUserNode)obj;
		}
		return ret;
	}

	public void refreshSelection()
	{
		tree.setSelection(tree.getSelection());
	}

	private void addPartListener()
	{
		fPartListener = new ActiveEditorTracker(getViewSite().getPage().getActiveEditor())
		{
			@Override
			public void partClosed(IWorkbenchPart part)
			{
				super.partClosed(part);
				if (PART_ID.equals(part.getSite().getId())) saveViewSettings();
			}

			@Override
			public void partDeactivated(IWorkbenchPart part)
			{
				super.partDeactivated(part);
				if (PART_ID.equals(part.getSite().getId())) saveViewSettings();
			}
		};
		fPartListener.addActiveEditorListener(moveCode);
		fPartListener.addActiveEditorListener(moveSample);

		moveCode.activeEditorChanged(fPartListener.getActiveEditor());
		moveSample.activeEditorChanged(fPartListener.getActiveEditor());

		// the contents of the tree are different when a calculation.js file is
		// being edited;
		// so we must monitor the active editor
		SolutionExplorerTreeContentProvider treeContentProvider = (SolutionExplorerTreeContentProvider)tree.getContentProvider();
		CalculationTracker ct = new CalculationTracker(treeContentProvider);
		fPartListener.addActiveEditorListener(ct);
		ct.activeEditorChanged(fPartListener.getActiveEditor());

		// update needed nodes in tree if the editor changes (for example active form highlighting)
		fPartListener.addActiveEditorListener(new HighlightNodeUpdater(tree, treeContentProvider));

		getViewSite().getPage().addPartListener(fPartListener);
	}

	/**
	 * Saves splitter ratio and orientation.
	 */
	protected void saveViewSettings()
	{
		saveSplitterRatio();
		fDialogSettings.put(DIALOGSTORE_VIEWORIENTATION, fCurrentOrientation);
		fDialogSettings.put(USE_OPEN_AS_DEFAULT, openModeToggleButton.getSelection());
		fDialogSettings.put(INCLUDE_ENTRIES_FROM_MODULES, includeModulesToggleButton.getSelection());
	}

	private void saveSplitterRatio()
	{
		if (fSplitter != null && !fSplitter.isDisposed())
		{
			int[] weigths = fSplitter.getWeights();
			int ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
			String key = DIALOGSTORE_RATIO + fSplitter.getOrientation();
			fDialogSettings.put(key, ratio);
		}
	}

	private void restoreSplitterRatio()
	{
		String ratio = fDialogSettings.get(DIALOGSTORE_RATIO + fSplitter.getOrientation());
		if (ratio == null) return;
		int intRatio = Integer.parseInt(ratio);
		fSplitter.setWeights(new int[] { intRatio, 1000 - intRatio });
	}

	private void initOrientation()
	{
		int savedOrientation;
		try
		{
			savedOrientation = fDialogSettings.getInt(DIALOGSTORE_VIEWORIENTATION);

			if ((savedOrientation < 0) || (savedOrientation > 3))
			{
				savedOrientation = VIEW_ORIENTATION_AUTOMATIC;
			}
		}
		catch (NumberFormatException e)
		{
			savedOrientation = VIEW_ORIENTATION_AUTOMATIC;
		}

		// force the update
		fCurrentOrientation = -1;
		setOrientation(savedOrientation);
		updateOrientationState(); // mark as checked the according action in the
		// toolbar menu
	}

	/**
	 * Called from OrientationAction.
	 * 
	 * @param orientation VIEW_ORIENTATION_HORIZONTAL, VIEW_ORIENTATION_AUTOMATIC or VIEW_ORIENTATION_VERTICAL
	 */
	public void setOrientation(int orientation)
	{
		if (orientation != VIEW_ORIENTATION_VERTICAL && orientation != VIEW_ORIENTATION_HORIZONTAL && orientation != VIEW_ORIENTATION_AUTOMATIC)
		{
			orientation = VIEW_ORIENTATION_AUTOMATIC;
		}

		if (fCurrentOrientation != orientation)
		{
			if (fCurrentOrientation >= 0)
			{
				saveSplitterRatio();
			}
			if (fSplitter != null && !fSplitter.isDisposed())
			{
				int swtOrientation;
				if (orientation == VIEW_ORIENTATION_HORIZONTAL)
				{
					swtOrientation = SWT.HORIZONTAL;
				}
				else if (orientation == VIEW_ORIENTATION_VERTICAL)
				{
					swtOrientation = SWT.VERTICAL;
				}
				else
				{
					swtOrientation = computeDesiredOrientation();
				}
				fSplitter.setOrientation(swtOrientation);
				fSplitter.layout();
			}

			fCurrentOrientation = orientation;
			fDialogSettings.put(DIALOGSTORE_VIEWORIENTATION, fCurrentOrientation);
			restoreSplitterRatio();
		}
	}

	private void updateOrientationState()
	{
		for (OrientationAction element : fToggleOrientationActions)
		{
			element.setChecked(fCurrentOrientation == element.getOrientation());
		}
	}

	private void createSplitter(Composite parent)
	{
		fSplitter = new SashForm(parent, SWT.NONE);
	}

	private KeyListener createKeyListener()
	{
		KeyListener keyListener = new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent event)
			{
				handleKeyEvent(event);
			}
		};

		return keyListener;
	}

	/**
	 * Handles some key events. (used for actions such as refresh)
	 * 
	 * @param event the key event.
	 */
	protected void handleKeyEvent(KeyEvent event)
	{
		if (event.stateMask == 0)
		{
			if (event.keyCode == SWT.F5)
			{
				if ((fRefreshAction != null) && fRefreshAction.isEnabled())
				{
					fRefreshAction.run();
					return;
				}
			}
		}
	}

	private void createListViewer(Composite parent)
	{
		ViewForm viewForm = new ViewForm(parent, SWT.NONE);
		list = new TableViewer(viewForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		ColumnViewerToolTipSupport.enableFor(list);
		list.setContentProvider(new SolutionExplorerListContentProvider(this));
		list.setLabelProvider(labelProvider);
		viewForm.setContent(list.getControl());

		listToolBar = new ToolBar(viewForm, SWT.FLAT | SWT.WRAP);
		viewForm.setTopCenter(listToolBar);

		// create pull down menu slot for the list
		final ToolBar listMenuToolbar = new ToolBar(viewForm, SWT.FLAT | SWT.WRAP);
		ToolBarManager listMenuToolbarManager = new ToolBarManager(listMenuToolbar);
		viewForm.setTopRight(listMenuToolbar);

		// create pull down menu & link to button in the listMenuToolbar
		listDropDownMenu = new Menu(listMenuToolbar);
		Action pullDown = new Action("Menu", IAction.AS_PUSH_BUTTON) //$NON-NLS-1$
		{
			@Override
			public void run()
			{
				Point pt = listMenuToolbar.toDisplay(0, listMenuToolbar.getSize().y);
				listDropDownMenu.setLocation(pt.x, pt.y);
				listDropDownMenu.setVisible(true);
			}
		};
		Image dropDownImage = UIUtils.paintViewMenuImage();
		if (dropDownImage != null)
		{
			ImageDescriptor dropDownImageDescriptor = ImageDescriptor.createFromImage(dropDownImage);
			pullDown.setImageDescriptor(dropDownImageDescriptor);
			pullDown.setDisabledImageDescriptor(dropDownImageDescriptor);
			pullDown.setHoverImageDescriptor(dropDownImageDescriptor);
		}
		pullDown.setToolTipText("More options"); //$NON-NLS-1$
		listMenuToolbarManager.add(pullDown);
		listMenuToolbarManager.update(true);

		list.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK, new Transfer[] { FormElementTransfer.getInstance() },
			new UserNodeListDragSourceListener(list, FormElementTransfer.getInstance()));

		list.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				boolean openEnabled = openAction.isEnabled();
				boolean moveEnabled = moveCode.isEnabled();

				if (openEnabled && moveEnabled)
				{
					if (openModeToggleButton.getSelection())
					{
						openAction.run(); // open in script mode
					}
					else
					{
						moveCode.run(); // move code mode
					}
				}
				else if (openEnabled)
				{
					openAction.run();
				}
				else if (moveEnabled)
				{
					moveCode.run();
				}
			}
		});
	}

	private void createTreeViewer(Composite parent)
	{
		tree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		ColumnViewerToolTipSupport.enableFor(tree);
		drillDownAdapter = new DrillDownAdapter(tree);
		tree.setContentProvider(new SolutionExplorerTreeContentProvider(this));
		tree.addSelectionChangedListener(this);

		ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
		decoratingLabelProvider = new DecoratingColumnLabelProvider(labelProvider, decorator);
		labelProviderListener = new ILabelProviderListener()
		{
			public void labelProviderChanged(LabelProviderChangedEvent event)
			{
				Object[] elements = event.getElements();
				if (elements != null)
				{
					SolutionExplorerTreeContentProvider cp = (SolutionExplorerTreeContentProvider)tree.getContentProvider();
					ArrayList<SimpleUserNode> changedUserNodeA = new ArrayList<SimpleUserNode>();
					SimpleUserNode simpleUserNode;

					for (Object e : elements)
					{
						simpleUserNode = null;
						if (e instanceof SimpleUserNode)
						{
							simpleUserNode = (SimpleUserNode)e;
						}
						else if (e instanceof IResource)
						{
							IResource adaptableResource = (IResource)e;

							if (adaptableResource.exists())
							{
								simpleUserNode = SolutionExplorerView.this.resourceToSimpleUserNode(adaptableResource);
							}
						}

						if (simpleUserNode != null)
						{
							SimpleUserNode servoyProjectNode = simpleUserNode.getAncestorOfType(ServoyProject.class);
							if (servoyProjectNode != null)
							{
								SimpleUserNode solutionNodeFromAllSolutions = cp.getSolutionFromAllSolutionsNode(servoyProjectNode.getName());
								if (solutionNodeFromAllSolutions != null && changedUserNodeA.indexOf(solutionNodeFromAllSolutions) == -1) changedUserNodeA.add(solutionNodeFromAllSolutions);
							}

							changedUserNodeA.add(simpleUserNode);
						}
					}
					if (changedUserNodeA.size() > 0) tree.update(changedUserNodeA.toArray(), null);
				}
				else
				{
					tree.refresh();
				}
			}

		};
		decoratingLabelProvider.addListener(labelProviderListener);
		tree.setLabelProvider(decoratingLabelProvider);

		// comparer that sees SimpleUserNode instances equal if their important
		// content is equal.
		// useful for storing restoring expanded three paths after tree node
		// reloads and such;
		// careful - when using single nodes in order to get/set stuff (not tree
		// paths) this comparer
		// must be temporarely removed; for example: double clicking a "Globals"
		// node will read the
		// expanded state of that node and expand/collapse it programatically -
		// but if the comparer
		// is used, then all these actions will be applied to the first "Globals"
		// node found in the tree,
		// not to exactly the one we are searching for...
		nodeContentComparer = new IElementComparer()
		{
			public int hashCode(Object obj)
			{
				if (obj == null) return -1;
				return UserNodeComparer.hashCode(obj);
			}

			public boolean equals(Object obj, Object b)
			{
				if (obj == null) return obj == b;
				return UserNodeComparer.equals(obj, b);
			}
		};

		tree.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK, new Transfer[] { FormElementTransfer.getInstance() },
			new UserNodeListDragSourceListener(tree, FormElementTransfer.getInstance()));
	}

	private void initTreeViewer()
	{
		((SolutionExplorerTreeContentProvider)tree.getContentProvider()).flushCachedData();
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject[] roots = servoyModel.getServoyProjects();

		if (persistChangeListener == null)
		{
			persistChangeListener = new IPersistChangeListener()
			{
				public void persistChanges(Collection<IPersist> changes)
				{
					Set<IPersist> parents = new HashSet<IPersist>();
					for (IPersist persist : changes)
					{
						if (!(persist instanceof ISupportChilds) && persist.getParent() != null)
						{
							parents.add(persist.getParent());
						}
					}
					for (IPersist persist : changes)
					{
						if (persist instanceof ISupportChilds && !parents.contains(persist) && persist.getParent() != null)
						{
							if (persist instanceof Relation)
							{
								// don't send the solution as refresh object, then would have to refresh everything (forms + relations)
								parents.add(persist);
							}
							else
							{
								parents.add(persist.getParent());
							}
						}
					}
					for (IPersist persist : parents)
					{
						((SolutionExplorerTreeContentProvider)tree.getContentProvider()).refreshContent(persist);
					}
					((SolutionExplorerListContentProvider)list.getContentProvider()).persistChanges(changes);
					((SolutionExplorerListContentProvider)list.getContentProvider()).refreshContent();
				}
			};
			servoyModel.addPersistChangeListener(true, persistChangeListener);
		}

		if (solutionMetaDataChangeListener == null)
		{
			solutionMetaDataChangeListener = new ISolutionMetaDataChangeListener()
			{
				public void solutionMetaDataChanged(Solution changedSolution)
				{
					ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
					if (activeProject != null && activeProject.getSolution().getSolutionID() == changedSolution.getSolutionID()) SolutionExplorerView.this.refreshTreeCompletely();
				}

			};
			servoyModel.addSolutionMetaDataChangeListener(solutionMetaDataChangeListener);
		}

		tree.setInput(roots);
		drillDownAdapter.reset();
		treeRoots = roots;
		selectionChanged(new SelectionChangedEvent(tree, tree.getSelection())); // set
		// initial
		// state
		// for
		// actions
	}

	private String getResourcesProjectName(ServoyProject sp)
	{
		if (sp == null) return Messages.TreeStrings_NoActiveResourcesProject;
		ServoyResourcesProject srp = sp.getResourcesProject();
		if (srp != null)
		{
			return Messages.TreeStrings_FromProject + srp.getProject().getName();
		}
		return Messages.TreeStrings_NoActiveResourcesProject;
	}

	private void addActiveProjectListener()
	{
		ServoyProject initialActiveProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		((SolutionExplorerTreeContentProvider)tree.getContentProvider()).getResourcesNode().setToolTipText(getResourcesProjectName(initialActiveProject));
		tree.refresh();
		activeProjectListener = new IActiveProjectListener()
		{
			/**
			 * @see com.servoy.eclipse.model.nature.IActiveProjectListener#activeProjectWillChange(com.servoy.eclipse.model.nature.ServoyProject,
			 *      com.servoy.eclipse.model.nature.ServoyProject)
			 */
			public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
			{
				return true;
			}

			public void activeProjectChanged(final ServoyProject activeProject)
			{
				((SolutionExplorerTreeContentProvider)tree.getContentProvider()).getResourcesNode().setToolTipText(getResourcesProjectName(activeProject));
				refreshTreeCompletely();

				// expand and select new active project node
				Runnable runnable = new Runnable()
				{
					public void run()
					{
						String activeProjectName;
						if (activeProject != null)
						{
							activeProjectName = activeProject.getProject().getName();
						}
						else
						{
							activeProjectName = ""; //$NON-NLS-1$
						}
						Object activeProjectNode = ((SolutionExplorerTreeContentProvider)tree.getContentProvider()).getSolutionNode(activeProjectName);
						if (activeProjectNode != null)
						{
							tree.expandToLevel(activeProjectNode, 1);
							ISelection newSelection = new StructuredSelection(activeProjectNode);
							// force selection change, as it may have not been changed if import place holder project was active before
							// causing solution properties view to be not updated
							if (newSelection.equals(tree.getSelection()))
							{
								tree.setSelection(null);
							}
							tree.setSelection(newSelection, false);


							// try to make the solution's contents visible (if they fit in the tree area);
							// if they do not fit scroll to make the active solution the first visible node
							Object[] children = ((SolutionExplorerTreeContentProvider)tree.getContentProvider()).getChildren(activeProjectNode);
							if (children != null && children.length > 0)
							{
								tree.reveal(children[children.length - 1]);
							}
							TreeItem[] treeSelection = tree.getTree().getSelection();
							if (treeSelection.length == 1 && treeSelection[0].getBounds().y < tree.getTree().getVerticalBar().getSelection())
							{
								tree.getTree().setTopItem(treeSelection[0]);
							}
							Object AllSolutionsNode = ((SolutionExplorerTreeContentProvider)tree.getContentProvider()).getAllSolutionsNode();
							tree.collapseToLevel(AllSolutionsNode, 1);


							DesignerPreferences dp = new DesignerPreferences();
							Form firstForm = null;

							if (dp.getOpenFirstFormDesigner())
							{
								Solution activeSolution = activeProject.getSolution();
								firstForm = activeSolution.getForm(activeSolution.getFirstFormID());
								if (firstForm == null)
								{
									Iterator<Form> formIterator = activeSolution.getForms(null, false);
									if (formIterator.hasNext())
									{
										firstForm = formIterator.next();
									}
								}
							}
							if (firstForm != null)
							{
								EditorUtil.openFormDesignEditor(firstForm);
							}
						}
						((SolutionExplorerTreeContentProvider)tree.getContentProvider()).setScriptingNodesEnabled(activeProject != null);
						((SolutionExplorerTreeContentProvider)tree.getContentProvider()).setResourceNodesEnabled(activeProject != null);
					}
				};
				if (Display.getCurrent() != null)
				{
					runnable.run();
				}
				else
				{
					Display.getDefault().asyncExec(runnable);
				}
			}

			public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
			{
				if (updateInfo == IActiveProjectListener.MODULES_UPDATED)
				{
					refreshTreeCompletely();
				}
				else if (updateInfo == IActiveProjectListener.RESOURCES_UPDATED_BECAUSE_ACTIVE_PROJECT_CHANGED ||
					updateInfo == IActiveProjectListener.STYLES_ADDED_OR_REMOVED || updateInfo == IActiveProjectListener.TEMPLATES_ADDED_OR_REMOVED)
				{
					refreshList();
				}
				else if (updateInfo == IActiveProjectListener.RESOURCES_UPDATED_ON_ACTIVE_PROJECT)
				{
					((SolutionExplorerTreeContentProvider)tree.getContentProvider()).getResourcesNode().setToolTipText(getResourcesProjectName(activeProject));
					refreshList();
				}
			}

		};
		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(activeProjectListener);
	}

	private void addResourceListener()
	{
		// we monitor the changes to eclipse projects in order to keep the list of
		// projects in the tree up to date
		resourceChangeListener = new IResourceChangeListener()
		{
			public void resourceChanged(IResourceChangeEvent event)
			{
				if ((event.getType() & IResourceChangeEvent.POST_CHANGE) != 0)
				{
					boolean mustRefresh = false;
					IResourceDelta[] affectedChildren = event.getDelta().getAffectedChildren();
					for (IResourceDelta element : affectedChildren)
					{
						IResource resource = element.getResource();
						if (resource instanceof IProject)
						{
							// see if it is a Servoy project that changed
							try
							{
								IProject project = (IProject)resource;
								if ((!project.isOpen()) || (!project.hasNature(ServoyUpdatingProject.NATURE_ID)))
								{
									if (element.getKind() != IResourceDelta.REMOVED && project.isOpen() && project.hasNature(ServoyProject.NATURE_ID))
									{
										// if it is not already in the tree then add it
										if (!isSolutionInTree(resource))
										{
											mustRefresh = true;
											break;
										}
									}
									else
									{
										// see if it was in the tree (if it was a Servoy
										// Project) and must be removed
										if (isSolutionInTree(resource))
										{
											mustRefresh = true;
											break;
										}
									}
								}
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
					if (mustRefresh)
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								// we are in UI thread, but we must wait for ServoyModel to be updated by a fellow resources listener (so we do this by running the
								// update)
								try
								{
									ServoyModel.getWorkspace().run(new IWorkspaceRunnable() // TODO this should be done nicer by controlling the sequence resource listeners execute; maybe add a proxy resource listener mechanism to ServoyModel that is able to do that
										{

											public void run(IProgressMonitor monitor) throws CoreException
											{
												refreshTreeCompletely();
											}
										}, null);
								}
								catch (CoreException e)
								{
									ServoyLog.logError("Cannot update SolEx content", e); //$NON-NLS-1$
								}
							}
						});
					}
				}
				else if ((event.getType() & IResourceChangeEvent.POST_BUILD) != 0)
				{
					ProblemDecorator problemDecorator = (ProblemDecorator)PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(
						ProblemDecorator.ID);
					if (problemDecorator != null)
					{
						IMarkerDelta[] markersDelta = event.findMarkerDeltas(IMarker.PROBLEM, true);
						ArrayList<IResource> changedProblemResources = new ArrayList<IResource>();
						for (IMarkerDelta md : markersDelta)
						{
							IResource r = md.getResource();
							do
							{
								if (changedProblemResources.indexOf(r) == -1) changedProblemResources.add(r);
								r = r.getParent();
							}
							while (r.getType() != IResource.ROOT);
						}

						problemDecorator.fireChanged(changedProblemResources.toArray(new IResource[changedProblemResources.size()]));
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.POST_BUILD);
	}

	private void addServerAndTableListeners()
	{
		IServerManagerInternal serverManager = ServoyModel.getServerManager();
		tableListener = new ITableListener()
		{
			public void tablesAdded(IServer server, String[] tableNames)
			{
				((SolutionExplorerListContentProvider)list.getContentProvider()).refreshServer(((IServerInternal)server).getName());
			}

			public void tablesRemoved(IServer server, Table[] tables, boolean deleted)
			{
				if (tables != null && tables.length > 0 && ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
				{
					List<String> names = new ArrayList<String>();
					for (Table table : tables)
					{
						names.add(table.getName());
					}
					FlattenedSolution editingSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingFlattenedSolution();
					Iterator<Form> it = editingSolution.getForms((Table)null, false);
					while (it.hasNext())
					{
						Form form = it.next();
						if (names.contains(form.getTableName()))
						{
							form.clearTable();
						}
					}
				}
				((SolutionExplorerListContentProvider)list.getContentProvider()).refreshServer(((IServerInternal)server).getName());
			}

			public void serverStateChanged(IServer server, int oldState, int newState)
			{
				((SolutionExplorerListContentProvider)list.getContentProvider()).refreshServer(((IServerInternal)server).getName());
				if ((oldState & ITableListener.VALID) == ITableListener.VALID && (newState & ITableListener.VALID) != ITableListener.VALID)
				{
					SolutionExplorerTreeContentProvider treeContentProvider = (SolutionExplorerTreeContentProvider)tree.getContentProvider();
					final Object serverNode = treeContentProvider.getServers();
					UIUtils.runInUI(new Runnable()
					{
						public void run()
						{
							tree.setExpandedState(serverNode, true);
						}
					}, false);
				}
			}

			public void tableInitialized(Table t)
			{
				// not interested in this
			}

		};
		// add listeners to initial server list
		String[] array = serverManager.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			((IServerInternal)serverManager.getServer(server_name, false, false)).addTableListener(tableListener);
		}

		// monitor changes in server list
		serverListener = new IServerListener()
		{

			public void serverAdded(IServer s)
			{
				((SolutionExplorerTreeContentProvider)tree.getContentProvider()).refreshServerList();
				((IServerInternal)s).addTableListener(tableListener);
			}

			public void serverRemoved(IServer s)
			{
				((SolutionExplorerTreeContentProvider)tree.getContentProvider()).refreshServerList();
				((IServerInternal)s).removeTableListener(tableListener);
			}
		};
		serverManager.addServerListener(serverListener);
	}

	private void addI18NChangeListener()
	{
		i18nChangeListener = new I18NChangeListener()
		{
			PlatformSimpleUserNode i18nFilesNode = ((SolutionExplorerTreeContentProvider)tree.getContentProvider()).getI18NFilesNode();

			public void i18nChanged()
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						if (i18nFilesNode.equals(getSelectedTreeNode())) refreshList();
					}
				});
			}
		};
		ServoyModelManager.getServoyModelManager().getServoyModel().addI18NChangeListener(i18nChangeListener);
	}

	public boolean isNonEmptyPlugin(SimpleUserNode un)
	{
		if (list != null && list.getContentProvider() instanceof SolutionExplorerListContentProvider)
		{
			return ((SolutionExplorerListContentProvider)list.getContentProvider()).isNonEmptyPlugin(un);
		}
		return true;
	}

	private boolean isSolutionInTree(IResource resource)
	{
		if (treeRoots != null)
		{
			for (ServoyProject servoyProject : treeRoots)
			{
				if (servoyProject.getProject() == resource)
				{
					return true;
				}
			}
		}
		return false;
	}

	private void addResizeListener(Composite parent)
	{
		resizeListener = new ControlListener()
		{
			public void controlMoved(ControlEvent e)
			{ /* not used */
			}

			public void controlResized(ControlEvent e)
			{
				if (fCurrentOrientation == VIEW_ORIENTATION_AUTOMATIC)
				{
					int current = fSplitter.getOrientation();
					int computed = computeDesiredOrientation();

					if (current != computed)
					{
						saveSplitterRatio();
						fSplitter.setOrientation(computed);
						fSplitter.layout();
						restoreSplitterRatio();
					}
				}
			}
		};
		parent.addControlListener(resizeListener);
	}

	/**
	 * Returns preferred split orientation of this view (for automatic splitting).
	 * 
	 * @return SWT.HORIZONTAL or SWT.VERTICAL
	 */
	public int computeDesiredOrientation()
	{
		Point size = fParent.getSize();
		if (size.x != 0 && size.y != 0)
		{
			if (size.x > size.y) return SWT.HORIZONTAL;
			else return SWT.VERTICAL;
		}
		return SWT.VERTICAL;
	}

	private void hookContextMenu()
	{
		MenuManager menuMgr = new MenuManager("#TreePopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener()
		{
			public void menuAboutToShow(IMenuManager manager)
			{
				SolutionExplorerView.this.fillTreeContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(tree.getControl());
		tree.getControl().setMenu(menu);

		getSite().registerContextMenu(PART_ID + ".tree", menuMgr, tree); //$NON-NLS-1$

		menuMgr = new MenuManager("#ListPopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener()
		{
			public void menuAboutToShow(IMenuManager manager)
			{
				SolutionExplorerView.this.fillListContextMenu(manager);
			}
		});
		menu = menuMgr.createContextMenu(list.getControl());
		list.getControl().setMenu(menu);

		getSite().registerContextMenu(PART_ID + ".list", menuMgr, list); //$NON-NLS-1$
	}

	private void contributeToActionBars()
	{
		ToolBarManager lowertbmanager = new ToolBarManager(listToolBar);
		fillListToolbar(lowertbmanager);
		fillListMenu(listDropDownMenu);

		IActionBars bars = getViewSite().getActionBars();
		fillViewMenu(bars.getMenuManager());
		fillViewToolBar(bars.getToolBarManager());
	}

	private void fillListToolbar(final ToolBarManager lowertbmanager)
	{
		lowertbmanager.add(openAction);
		lowertbmanager.add(newActionInListPrimary);
		final ActionContributionItem imfContributionItem = new ActionContributionItem(importMediaFolder);
		imfContributionItem.setVisible(importMediaFolder.isEnabled());
		importMediaFolder.addPropertyChangeListener(new IPropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent event)
			{
				String propertyName = event.getProperty();
				if (propertyName == null || propertyName.equals(IAction.ENABLED))
				{
					imfContributionItem.setVisible(importMediaFolder.isEnabled());
					lowertbmanager.update(true);
					listToolBar.getParent().layout();
				}
			}
		});
		lowertbmanager.add(imfContributionItem);
		lowertbmanager.add(deleteActionInList);
		lowertbmanager.add(new Separator());
		lowertbmanager.add(moveSample);
		lowertbmanager.add(moveCode);

		lowertbmanager.update(true);
	}

	private void fillListMenu(Menu lowertbmenu)
	{
		openModeToggleButton = new MenuItem(lowertbmenu, SWT.CHECK);
		openModeToggleButton.setText("Use 'open' as default action"); //$NON-NLS-1$
		openModeToggleButton.setSelection(fDialogSettings.getBoolean(USE_OPEN_AS_DEFAULT));
		openModeToggleButton.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
			}

			public void widgetSelected(SelectionEvent e)
			{
				fDialogSettings.put(USE_OPEN_AS_DEFAULT, openModeToggleButton.getSelection());
			}
		});

		includeModulesToggleButton = new MenuItem(lowertbmenu, SWT.CHECK);
		includeModulesToggleButton.setText("Include entries from modules"); //$NON-NLS-1$
		includeModulesToggleButton.setSelection(fDialogSettings.getBoolean(INCLUDE_ENTRIES_FROM_MODULES));
		((SolutionExplorerListContentProvider)list.getContentProvider()).setIncludeModules(fDialogSettings.getBoolean(INCLUDE_ENTRIES_FROM_MODULES));
		includeModulesToggleButton.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
			}

			public void widgetSelected(SelectionEvent e)
			{
				((SolutionExplorerListContentProvider)list.getContentProvider()).setIncludeModules(includeModulesToggleButton.getSelection());
				refreshList();
				fDialogSettings.put(INCLUDE_ENTRIES_FROM_MODULES, includeModulesToggleButton.getSelection());
			}
		});
	}

	private void fillViewMenu(IMenuManager viewMenu)
	{
		MenuManager layoutSubMenu = new MenuManager("Layout"); //$NON-NLS-1$
		for (OrientationAction element : fToggleOrientationActions)
		{
			layoutSubMenu.add(element);
		}
		viewMenu.add(layoutSubMenu);
		viewMenu.add(new Separator());
	}

	private void fillTreeContextMenu(IMenuManager manager)
	{
		SimpleUserNode selectedTreeNode = getSelectedTreeNode();

		if (setActive.isEnabled()) manager.add(setActive);
		manager.add(new Separator());

		if (openActionInTree.isEnabled()) manager.add(openActionInTree);
		if (openSqlEditorAction.isEnabled()) manager.add(openSqlEditorAction);
		// extra open actions contributions
		manager.add(new Separator(IWorkbenchActionConstants.OPEN_EXT));
		if (selectedTreeNode != null && selectedTreeNode.getType() == UserNodeType.FORM)
		{
			manager.add(openFormEditorAction);
		}
		manager.add(new Separator());
		if (selectedTreeNode != null && selectedTreeNode.getType() == UserNodeType.SERVERS)
		{
			MenuManager submenu = new MenuManager("Connect to existing database", "newServer"); //$NON-NLS-1$ //$NON-NLS-2$
			for (Map.Entry<String, ServerConfig> template : ServerConfig.TEMPLATES.entrySet())
			{
				submenu.add(new NewServerAction(template.getKey(), template.getValue()));
			}
			manager.add(submenu);
		}
		else
		{
			if (newActionInTreePrimary.isEnabled()) manager.add(newActionInTreePrimary);
			if (newActionInTreeSecondary.isEnabled()) manager.add(newActionInTreeSecondary);
		}
		manager.add(new Separator());
		if (toggleFormCommandsActions.isEnabled()) manager.add(toggleFormCommandsActions);
		if (changeResourcesProjectAction.isEnabled()) manager.add(changeResourcesProjectAction);
		if (replaceServerAction.isEnabled()) manager.add(replaceServerAction);
		if (replaceActionInTree.isEnabled()) manager.add(replaceActionInTree);
		if (removeSolutionProtectionAction.isEnabled()) manager.add(removeSolutionProtectionAction);
		if (duplicateServer.isEnabled()) manager.add(duplicateServer);

		if (selectedTreeNode.getRealType() == UserNodeType.SERVERS)
		{
			MenuManager createDBSubmenu = new MenuManager("Create new database", "newDatabase"); //$NON-NLS-1$ //$NON-NLS-2$

			createDBSubmenu.add(newPostgresqlDatabase);
			createDBSubmenu.add(newSybaseDatabase);
			manager.add(createDBSubmenu);
		}

		if (enableServer.isEnabled()) manager.add(enableServer);
		manager.add(new Separator());
		if (synchronizeTablesWithDBAction.isEnabled()) manager.add(synchronizeTablesWithDBAction);
		if (reloadTablesOfServerAction.isEnabled()) manager.add(reloadTablesOfServerAction);
		if (updateServoySequencesAction.isEnabled()) manager.add(updateServoySequencesAction);
		if (loadRelationsAction.isEnabled()) manager.add(loadRelationsAction);
		// Other plug-ins can contribute their actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		if (selectedTreeNode != null && selectedTreeNode.getType() == UserNodeType.SOLUTION && exportActiveSolutionAction.isEnabled() &&
			selectedTreeNode.getRealObject() != null)
		{
			manager.add(new Separator());
			manager.add(exportActiveSolutionAction);
		}

		if (selectedTreeNode != null && selectedTreeNode.getType() == UserNodeType.ALL_SOLUTIONS && importSolutionAction.isEnabled())
		{
			manager.add(new Separator());
			manager.add(importSolutionAction);
		}


		if (selectedTreeNode != null && selectedTreeNode.getType() == UserNodeType.SERVER && suggestForeignTypes.isEnabled() &&
			selectedTreeNode.getRealObject() != null)
		{
			manager.add(new Separator());
			manager.add(suggestForeignTypes);
		}

		if (selectedTreeNode != null && selectedTreeNode.getType() == UserNodeType.I18N_FILES)
		{
			manager.add(new Separator());
			manager.add(i18nCreateFromDBAction);
			manager.add(i18nWriteToDBAction);
		}

		if (selectedTreeNode != null && selectedTreeNode.isEnabled() && selectedTreeNode.getType() == UserNodeType.SOLUTION)
		{
			manager.add(new Separator());
			manager.add(i18nExternalizeAction);
		}

		manager.add(new Separator());
		if (addAsModuleAction.isEnabled()) manager.add(addAsModuleAction);
		if (removeModuleAction.isEnabled()) manager.add(removeModuleAction);
		if (moveFormAction.isEnabled()) manager.add(moveFormAction);
		if (duplicateFormAction.isEnabled()) manager.add(duplicateFormAction);
		if (renameFormAction.isEnabled()) manager.add(renameFormAction);
		if (deleteActionInTree.isEnabled()) manager.add(deleteActionInTree);
		if (renameSolutionAction.isEnabled()) manager.add(renameSolutionAction);
		manager.add(new Separator());
		manager.add(cutAction);
		manager.add(copyAction);
		manager.add(pasteAction);
		manager.add(new Separator());
		if (searchTreeAction.isEnabled()) manager.add(searchTreeAction);
		manager.add(new Separator());
		manager.add(fRefreshAction);
		manager.add(expandNodeAction);
		manager.add(collapseTreeAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void fillListContextMenu(IMenuManager manager)
	{
		if (moveCode.isEnabled()) manager.add(moveCode);
		if (moveSample.isEnabled()) manager.add(moveSample);
		manager.add(new Separator());
		if (openAction.isEnabled()) manager.add(openAction);
		if (editVariableAction.isEnabled()) manager.add(editVariableAction);
		if (debugMethodAction.isMethodSelected()) manager.add(debugMethodAction);
		if (openSqlEditorAction.isEnabled()) manager.add(openSqlEditorAction);
		if (searchListAction.isEnabled()) manager.add(searchListAction);

		manager.add(new Separator());
		if (newActionInListPrimary.isEnabled()) manager.add(newActionInListPrimary);
		if (copyTable.isEnabled()) manager.add(copyTable);
		if (newActionInListSecondary.isEnabled()) manager.add(newActionInListSecondary);
		if (overrideMethod.isEnabled()) manager.add(overrideMethod);
		if (importMediaFolder.isEnabled()) manager.add(importMediaFolder);
		if (renameMediaAction.isEnabled()) manager.add(renameMediaAction);
		if (movePersistAction.isEnabled()) manager.add(movePersistAction);
		if (duplicatePersistAction.isEnabled()) manager.add(duplicatePersistAction);
		if (deleteActionInList.isEnabled()) manager.add(deleteActionInList);
		// Other plug-ins can contribute their actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillViewToolBar(IToolBarManager manager)
	{
		IContributionItem searchLabel = new ControlContribution("searchLabel") //$NON-NLS-1$
		{
			@Override
			protected Control createControl(Composite parent)
			{
				// vertically center the label
				Composite labelPositioner = new Composite(parent, SWT.NONE);
				RowLayout la = new RowLayout(SWT.VERTICAL);
				la.wrap = false;
				la.pack = true;
				la.fill = true;
				la.justify = true;
				labelPositioner.setLayout(la);

				Label l = new Label(labelPositioner, SWT.NONE);
				l.setText(Messages.SolutionExplorerView_filter);

				return labelPositioner;
			}
		};
		IContributionItem searchField = new ControlContribution("searchField") //$NON-NLS-1$
		{
			@Override
			protected Control createControl(Composite parent)
			{
				final Text searchFld = new Text(parent, SWT.SEARCH);
				final FilterDelayJob filterDelayJob = new FilterDelayJob(SolutionExplorerView.this, TREE_FILTER_DELAY_TIME, "Applying Solution Explorer filter"); //$NON-NLS-1$
				searchFld.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent e)
					{
						String text = searchFld.getText();
						if (text != null && text.trim().length() > 0)
						{
							text += '*';
						}
						filterDelayJob.setFilterText(text);
					}
				});
				searchFld.addFocusListener(new FocusListener()
				{
					public void focusGained(FocusEvent e)
					{
						Display.getCurrent().asyncExec(new Runnable()
						{
							public void run()
							{
								searchFld.selectAll();
							}
						});
					}

					public void focusLost(FocusEvent e)
					{
						// not used
					}
				});
				textActionHandler.addText(searchFld);

				// Hack to deactivate the text control from the textActionHandler
				// Create temp swt text for the purposes of adding and removing it from textactionhandler
				// This will cause the activeTextController to be set to null
				Text aText = new Text(parent, SWT.NONE);
				textActionHandler.addText(aText);
				textActionHandler.removeText(aText);

				return searchFld;
			}

			@Override
			protected int computeWidth(Control control)
			{
				return control.computeSize(DEFAULT_SEARCH_FIELD_WIDTH, SWT.DEFAULT, true).x;
			}
		};
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		manager.add(searchLabel);
		manager.add(searchField);
		manager.add(new Separator());
		manager.add(fRefreshAction);
		manager.add(collapseTreeAction);
		manager.add(linkWithEditorAction);
	}

	public void filter(String filterValue)
	{
		filterTree(filterValue);
	}

	private void filterTree(final String text)
	{
		final boolean wasNull;
		if (treeFilter == null)
		{
			treeFilter = new TextFilter(labelProvider, true, false);
			treeFilter.setSupplementalContentProvider((IStructuredContentProvider)list.getContentProvider());
			treeFilter.setText(text);
			// cache contents as it may take a while the first time... (filter once outside of SWT UI thread - so we can show progress dialog)
			treeFilter.filter(tree, tree.getInput(), ((IStructuredContentProvider)tree.getContentProvider()).getElements(tree.getInput()));
			wasNull = true;
		}
		else
		{
			treeFilter.setText(text);
			wasNull = false;
		}

		Runnable updateUI = new Runnable()
		{
			public void run()
			{
				ISelection selection = tree.getSelection();
				if (wasNull) tree.setFilters(new ViewerFilter[] { treeFilter });
				else tree.refresh();
				if (text != null && text.trim().length() > 0)
				{
					// if for the filtered tree structure, all leafs have the same parent,
					// expand to the leafs, otherwise
					// expand two levels
					SimpleUserNode toExpand = (SimpleUserNode)treeFilter.getFirstMatchingNode();
					ArrayList<Object> expandedElements = new ArrayList<Object>();
					while (toExpand != null)
					{
						expandedElements.add(toExpand);
						toExpand = toExpand.parent;
					}
					tree.setExpandedElements(expandedElements.toArray());
				}
				else
				{
					tree.collapseAll();
				}
				tree.setSelection(selection);
				tree.reveal(selection);
			}
		};
		if (Display.getCurrent() != null)
		{
			updateUI.run();
		}
		else
		{
			Display.getDefault().asyncExec(updateUI);
		}
	}

	private void createActions()
	{
		moveSample = new MoveTextAction(this, true);
		moveCode = new MoveTextAction(this, false);
		searchListAction = new SearchAction();
		searchTreeAction = new SearchAction();
		moveFormAction = new MovePersistAction(getSite().getShell());
		duplicateFormAction = new DuplicatePersistAction(getSite().getShell());
		renameFormAction = new RenamePersistAction();
		changeResourcesProjectAction = new ChangeResourcesProjectAction(getSite().getShell());
		removeSolutionProtectionAction = new RemoveSolutionProtectionAction(getSite().getShell());
		reloadTablesOfServerAction = new ReloadTablesAction();
		updateServoySequencesAction = new UpdateServoySequencesAction();
		synchronizeTablesWithDBAction = new SynchronizeTablesAction();
		loadRelationsAction = new LoadRelationsAction(this);

		newActionInTreePrimary = new ContextAction(this, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD),
			"New"); //$NON-NLS-1$
		newActionInTreeSecondary = new ContextAction(this, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD),
			"New"); //$NON-NLS-1$
		newActionInListSecondary = new ContextAction(this, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD),
			"New"); //$NON-NLS-1$

		IAction newMethod = new NewMethodAction(this);
		overrideMethod = new OverrideMethodAction(this);
		IAction newVariable = new NewVariableAction(this);
		IAction newValueList = new NewValueListAction(this);
		IAction newTable = new NewTableAction(this);
		newPostgresqlDatabase = new NewPostgresDbAction(this);
		newPostgresqlDatabase.setEnabledStatus();
		newSybaseDatabase = new NewSybaseDbAction(this);
		newSybaseDatabase.setEnabledStatus();
		ServoyModel.getServerManager().addServerConfigListener(new SoltuionExplorerServerConfigSync());
		duplicateServer = new DuplicateServerAction(this);
		enableServer = new EnableServerAction(getSite().getShell());
		toggleFormCommandsActions = new ToggleFormCommandsAction(this);

		replaceActionInTree = new ReplaceTableAction(this);
		replaceServerAction = new ReplaceServerAction(this);

		openSqlEditorAction = new OpenSqlEditorAction();

		IAction newForm = new OpenNewFormWizardAction();
		IAction newSolution = new OpenWizardAction(NewSolutionWizard.class, Activator.loadImageDescriptorFromBundle("solution_icon.gif"), "Create new solution"); //$NON-NLS-1$ //$NON-NLS-2$
		IAction newStyle = new OpenWizardAction(NewStyleWizard.class, Activator.loadImageDescriptorFromBundle("styles.gif"), "Create new style"); //$NON-NLS-1$//$NON-NLS-2$
		exportActiveSolutionAction = new OpenWizardAction(ExportSolutionWizard.class, Activator.loadImageDescriptorFromOldLocations("export_wiz.gif"), //$NON-NLS-1$
			"Export solution"); //$NON-NLS-1$
		importSolutionAction = new OpenWizardAction(ImportSolutionWizard.class, Activator.loadImageDescriptorFromOldLocations("import_wiz.gif"), //$NON-NLS-1$
			"Import solution"); //$NON-NLS-1$
		i18nExternalizeAction = new I18NExternalizeAction();
		i18nCreateFromDBAction = new I18NCreateFromDBAction();
		i18nWriteToDBAction = new I18NWriteToDBAction();

		suggestForeignTypes = new SuggestForeignTypesAction(this);

		openFormEditorAction = new OpenFormEditorAction();

		IAction newRelation = new NewRelationAction(this);
		IAction importMedia = new ImportMediaAction(this);
		importMediaFolder = new ImportMediaFolderAction(this);
		renameMediaAction = new RenameMediaAction(this);
		movePersistAction = new MovePersistAction(this.getSite().getShell());
		duplicatePersistAction = new DuplicatePersistAction(this.getSite().getShell());

		newActionInTreePrimary.registerAction(UserNodeType.FORM, newMethod);
		newActionInTreePrimary.registerAction(UserNodeType.GLOBALS_ITEM, newMethod);
		newActionInTreePrimary.registerAction(UserNodeType.GLOBAL_VARIABLES, newVariable);
		newActionInTreePrimary.registerAction(UserNodeType.FORM_VARIABLES, newVariable);
		newActionInTreePrimary.registerAction(UserNodeType.VALUELISTS, newValueList);
		newActionInTreePrimary.registerAction(UserNodeType.RELATIONS, newRelation);
		newActionInTreePrimary.registerAction(UserNodeType.GLOBALRELATIONS, newRelation);
		newActionInTreePrimary.registerAction(UserNodeType.ALL_RELATIONS, newRelation);
		newActionInTreePrimary.registerAction(UserNodeType.MEDIA, importMedia);
		newActionInTreePrimary.registerAction(UserNodeType.SERVER, newTable);
		newActionInTreePrimary.registerAction(UserNodeType.FORMS, newForm);
		newActionInTreePrimary.registerAction(UserNodeType.SOLUTION, newSolution);
		newActionInTreePrimary.registerAction(UserNodeType.ALL_SOLUTIONS, newSolution);
		newActionInTreePrimary.registerAction(UserNodeType.STYLES, newStyle);

		newActionInTreeSecondary.registerAction(UserNodeType.MEDIA, importMediaFolder);
		importMediaFolder = new ImportMediaFolderAction(this);
		importMediaFolder.setEnabled(false);

		newActionInTreeSecondary.registerAction(UserNodeType.FORM, new OpenWizardAction(NewFormWizard.class,
			Activator.loadImageDescriptorFromBundle("designer.gif"), "Create new sub form")); //$NON-NLS-1$ //$NON-NLS-2$
		newActionInTreeSecondary.registerAction(UserNodeType.SOLUTION, newForm);

		newActionInListPrimary = new ContextAction(this, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD),
			"New"); //$NON-NLS-1$
		newMethod = new NewMethodAction(this);
		newVariable = new NewVariableAction(this);
		newValueList = new NewValueListAction(this);
		newTable = new NewTableAction(this);


		newStyle = new OpenWizardAction(NewStyleWizard.class, Activator.loadImageDescriptorFromBundle("styles.gif"), "Create new style"); //$NON-NLS-1$ //$NON-NLS-2$
		importMedia = new ImportMediaAction(this);
		newRelation = new NewRelationAction(this);

		newActionInListPrimary.registerAction(UserNodeType.FORM, newMethod);
		newActionInListPrimary.registerAction(UserNodeType.GLOBALS_ITEM, newMethod);
		newActionInListPrimary.registerAction(UserNodeType.GLOBAL_VARIABLES, newVariable);
		newActionInListPrimary.registerAction(UserNodeType.FORM_VARIABLES, newVariable);
		newActionInListPrimary.registerAction(UserNodeType.VALUELISTS, newValueList);
		newActionInListPrimary.registerAction(UserNodeType.MEDIA, importMedia);
		newActionInListPrimary.registerAction(UserNodeType.SERVER, newTable);

		newActionInListPrimary.registerAction(UserNodeType.STYLES, newStyle);
		newActionInListPrimary.registerAction(UserNodeType.ALL_RELATIONS, newRelation);
		newActionInListPrimary.registerAction(UserNodeType.RELATIONS, newRelation);

		newActionInListSecondary.registerAction(UserNodeType.TABLE, newForm);
		newActionInListSecondary.registerAction(UserNodeType.VIEW, newForm);

		openAction = new ContextAction(this, Activator.loadImageDescriptorFromBundle("open.gif"), "Open"); //$NON-NLS-1$ //$NON-NLS-2$
		IAction openScript = new OpenScriptAction();
		IAction openStyle = new OpenStyleAction(this);
		IAction openValueList = new OpenValueListAction(this);
		IAction openTable = new OpenTableAction(this);
		IAction openRelation = new OpenRelationAction();
		IAction openMedia = new OpenMediaAction();
		IAction openI18N = new OpenI18NAction(this);

		openAction.registerAction(UserNodeType.FORM_METHOD, openScript);
		openAction.registerAction(UserNodeType.GLOBAL_METHOD_ITEM, openScript);
		openAction.registerAction(UserNodeType.CALCULATIONS_ITEM, openScript);
		openAction.registerAction(UserNodeType.GLOBAL_VARIABLE_ITEM, openScript);
		openAction.registerAction(UserNodeType.FORM_VARIABLE_ITEM, openScript);
		openAction.registerAction(UserNodeType.FORM_VARIABLE_ITEM, openScript);
		openAction.registerAction(UserNodeType.STYLE_ITEM, openStyle);
		openAction.registerAction(UserNodeType.VALUELIST_ITEM, openValueList);
		openAction.registerAction(UserNodeType.TABLE, openTable);
		openAction.registerAction(UserNodeType.VIEW, openTable);
		openAction.registerAction(UserNodeType.RELATION, openRelation);
		openAction.registerAction(UserNodeType.MEDIA_IMAGE, openMedia);
		openAction.registerAction(UserNodeType.I18N_FILE_ITEM, openI18N);

		deleteActionInList = new ContextAction(this, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE), "Delete"); //$NON-NLS-1$
		IAction deleteMedia = new DeletePersistAction(UserNodeType.MEDIA_IMAGE, "Delete media"); //$NON-NLS-1$
		IAction deleteValueList = new DeletePersistAction(UserNodeType.VALUELIST_ITEM, "Delete value list"); //$NON-NLS-1$
		IAction deleteTable = new DeleteTableAction(getSite().getShell());
		IAction deleteStyle = new DeletePersistAction(UserNodeType.STYLE_ITEM, "Delete style"); //$NON-NLS-1$
		IAction deleteTemplate = new DeletePersistAction(UserNodeType.TEMPLATE_ITEM, "Delete template"); //$NON-NLS-1$
		IAction deleteRelation = new DeletePersistAction(UserNodeType.RELATION, "Delete relation"); //$NON-NLS-1$
		IAction deleteFormScript = new DeleteScriptAction(UserNodeType.FORM_METHOD, "Delete method", this); //$NON-NLS-1$
		IAction deleteGlobalScript = new DeleteScriptAction(UserNodeType.GLOBAL_METHOD_ITEM, "Delete method", this); //$NON-NLS-1$
		IAction deleteFormVariable = new DeleteScriptAction(UserNodeType.FORM_VARIABLE_ITEM, "Delete variable", this); //$NON-NLS-1$
		IAction deleteGlobalVariable = new DeleteScriptAction(UserNodeType.GLOBAL_VARIABLE_ITEM, "Delete variable", this); //$NON-NLS-1$
		IAction deleteI18N = new DeleteI18NAction(getSite().getShell());

		deleteActionInList.registerAction(UserNodeType.FORM_METHOD, deleteFormScript);
		deleteActionInList.registerAction(UserNodeType.GLOBAL_METHOD_ITEM, deleteGlobalScript);
		deleteActionInList.registerAction(UserNodeType.FORM_VARIABLE_ITEM, deleteFormVariable);
		deleteActionInList.registerAction(UserNodeType.GLOBAL_VARIABLE_ITEM, deleteGlobalVariable);
		// deleteAction.registerAction(UserNodeType.GLOBAL_ITEM, deleteVariable);
		deleteActionInList.registerAction(UserNodeType.VALUELIST_ITEM, deleteValueList);
		deleteActionInList.registerAction(UserNodeType.MEDIA_IMAGE, deleteMedia);
		deleteActionInList.registerAction(UserNodeType.TABLE, deleteTable); /// not UserNodeType.VIEW
		deleteActionInList.registerAction(UserNodeType.STYLE_ITEM, deleteStyle);
		deleteActionInList.registerAction(UserNodeType.TEMPLATE_ITEM, deleteTemplate);
		deleteActionInList.registerAction(UserNodeType.RELATION, deleteRelation);
		deleteActionInList.registerAction(UserNodeType.I18N_FILE_ITEM, deleteI18N);

		copyTable = new CopyTableAction(getSite().getShell());
		editVariableAction = new EditVariableAction(this);

		debugMethodAction = new DebugMethodAction(this);

		openActionInTree = new ContextAction(this, Activator.loadImageDescriptorFromBundle("open.gif"), "Open"); //$NON-NLS-1$ //$NON-NLS-2$
		openRelation = new OpenRelationAction(); // must be another instance
		// (in order to use only
		// selections from the tree)
		openActionInTree.registerAction(UserNodeType.RELATION, openRelation);
		openActionInTree.registerAction(UserNodeType.CALC_RELATION, openRelation);
		openActionInTree.registerAction(UserNodeType.SERVER, new OpenServerAction(this));
		openActionInTree.registerAction(UserNodeType.USER_GROUP_SECURITY, new EditSecurityAction());
		openActionInTree.registerAction(UserNodeType.I18N_FILES, new EditI18nAction());

		deleteActionInTree = new ContextAction(this, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE), "Delete"); //$NON-NLS-1$
		IAction deleteForm = new DeletePersistAction(UserNodeType.FORM, "Delete form"); //$NON-NLS-1$
		deleteRelation = new DeletePersistAction(UserNodeType.RELATION, "Delete relation"); //$NON-NLS-1$
		IAction deleteSolution = new DeleteSolutionAction(getSite().getShell());
		IAction deleteServer = new DeleteServerAction(this);
		deleteActionInTree.registerAction(UserNodeType.FORM, deleteForm);
		deleteActionInTree.registerAction(UserNodeType.RELATION, deleteRelation);
		deleteActionInTree.registerAction(UserNodeType.SOLUTION_ITEM, deleteSolution);
		deleteActionInTree.registerAction(UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE, deleteSolution);
		deleteActionInTree.registerAction(UserNodeType.SERVER, deleteServer);

		addAsModuleAction = new AddAsModuleAction(getSite().getShell());
		renameSolutionAction = new RenameSolutionAction(this);
		removeModuleAction = new RemoveModuleAction(getSite().getShell());

		expandNodeAction = new ExpandNodeAction(tree);

		setActive = new ActivateSolutionAction();

		// let the actions decide when they are enabled or disabled
		list.addSelectionChangedListener(moveCode);
		list.addSelectionChangedListener(moveSample);
		list.addSelectionChangedListener(deleteActionInList);
		list.addSelectionChangedListener(openAction);
		list.addSelectionChangedListener(editVariableAction);
		list.addSelectionChangedListener(debugMethodAction);
		list.addSelectionChangedListener(newActionInListSecondary);
		list.addSelectionChangedListener(renameMediaAction);
		list.addSelectionChangedListener(searchListAction);
		list.addSelectionChangedListener(movePersistAction);
		list.addSelectionChangedListener(duplicatePersistAction);
		list.addSelectionChangedListener(copyTable);
		list.addSelectionChangedListener(overrideMethod);
		list.addSelectionChangedListener(openSqlEditorAction);

		tree.addSelectionChangedListener(importMediaFolder);
		tree.addSelectionChangedListener(synchronizeTablesWithDBAction);
		tree.addSelectionChangedListener(loadRelationsAction);
		tree.addSelectionChangedListener(newActionInTreePrimary);
		tree.addSelectionChangedListener(newActionInTreeSecondary);

		tree.addSelectionChangedListener(newActionInListPrimary);

		tree.addSelectionChangedListener(openActionInTree);
		tree.addSelectionChangedListener(searchTreeAction);
		tree.addSelectionChangedListener(deleteActionInTree);
		tree.addSelectionChangedListener(addAsModuleAction);
		tree.addSelectionChangedListener(renameSolutionAction);
		tree.addSelectionChangedListener(removeModuleAction);
		tree.addSelectionChangedListener(setActive);
		tree.addSelectionChangedListener(replaceActionInTree);
		tree.addSelectionChangedListener(replaceServerAction);
		tree.addSelectionChangedListener(openSqlEditorAction);
		tree.addSelectionChangedListener(duplicateFormAction);
		tree.addSelectionChangedListener(moveFormAction);
		tree.addSelectionChangedListener(renameFormAction);
		tree.addSelectionChangedListener(changeResourcesProjectAction);
		tree.addSelectionChangedListener(removeSolutionProtectionAction);
		tree.addSelectionChangedListener(reloadTablesOfServerAction);
		tree.addSelectionChangedListener(updateServoySequencesAction);
		tree.addSelectionChangedListener(duplicateServer);
		tree.addSelectionChangedListener(enableServer);
//		tree.addSelectionChangedListener(newDatabase);
//		tree.addSelectionChangedListener(newSybaseDatabase);
		tree.addSelectionChangedListener(toggleFormCommandsActions);
		tree.addSelectionChangedListener(expandNodeAction);

		tree.addSelectionChangedListener(openFormEditorAction);

		fRefreshAction = new RefreshAction(this);
		collapseTreeAction = new CollapseTreeAction(tree);
		selectAllActionInTree = new SelectAllAction(tree);
		selectAllActionInlist = new SelectAllAction(list);

		linkWithEditorAction = new LinkWithEditorAction(tree, list);

		fToggleOrientationActions = new OrientationAction[] { new OrientationAction(this, VIEW_ORIENTATION_VERTICAL), new OrientationAction(this,
			VIEW_ORIENTATION_HORIZONTAL), new OrientationAction(this, VIEW_ORIENTATION_AUTOMATIC) };
	}

	private void hookTreeDoubleClickAction()
	{
		tree.getTree().addMouseListener(new MouseListener()
		{
			public void mouseDoubleClick(MouseEvent e)
			{
				IStructuredSelection originalSelection = (IStructuredSelection)tree.getSelection();
				TreeItem doubleClickedTreeItem = ((Tree)e.getSource()).getItem(new Point(e.x, e.y));
				if (doubleClickedTreeItem == null)
				{
					return;
				}
				SimpleUserNode doubleClickedItem = (SimpleUserNode)doubleClickedTreeItem.getData();

				// CTRL + double click on any node that is able to be opened will
				// open that node;
				// simple double click on a expandable node will only do
				// collapse/expand;
				// simple double click on a leaf will try to perform open (if
				// available)
				// CTRL + double click on an expandable node that does not
				// support open will expand/collapse that node.
				// NOTE: For MAC, not CTRL but the CMD key is used.

				tree.setComparer(null);

				boolean ctrlPressed = (e.stateMask == SWT.MOD1);
				boolean expandable = tree.isExpandable(doubleClickedItem);

				// CTRL + double click might result in a unselection of the
				// double clicked element (CTRL + click toggles the selection
				// state);
				// this is why we need to use the selection given by the event
				// instead of the selection given by the tree
				boolean isForm = (doubleClickedItem.getType() == UserNodeType.FORM); // form open action was moved to the designer plugin, so we must make a special case for it (it is no longer part of openActionInTree)
				boolean isGlobals = (doubleClickedItem.getType() == UserNodeType.GLOBALS_ITEM);
				openActionInTree.selectionChanged(new SelectionChangedEvent(tree, new StructuredSelection(doubleClickedItem)));
				Preferences store = Activator.getDefault().getPluginPreferences();
				String formDblClickOption = store.getString(SolutionExplorerPreferences.FORM_DOUBLE_CLICK_ACTION);
				String globalsDblClickOption = store.getString(SolutionExplorerPreferences.GLOBALS_DOUBLE_CLICK_ACTION);
				boolean formDblClickOptionDefined = (SolutionExplorerPreferences.DOUBLE_CLICK_OPEN_FORM_EDITOR.equals(formDblClickOption)) ||
					(SolutionExplorerPreferences.DOUBLE_CLICK_OPEN_FORM_SCRIPT.equals(formDblClickOption));
				boolean globalsDblClickOptionDefined = (SolutionExplorerPreferences.DOUBLE_CLICK_OPEN_GLOBAL_SCRIPT.equals(globalsDblClickOption));
				if (isForm && (ctrlPressed || formDblClickOptionDefined))
				{
					if (ctrlPressed || SolutionExplorerPreferences.DOUBLE_CLICK_OPEN_FORM_EDITOR.equals(formDblClickOption))
					{
						EditorUtil.openFormDesignEditor((Form)doubleClickedItem.getRealObject());
					}
					else
					{
						EditorUtil.openScriptEditor((Form)doubleClickedItem.getRealObject(), true);
					}
				}
				else if (isGlobals && (ctrlPressed || globalsDblClickOptionDefined))
				{
					EditorUtil.openScriptEditor((Solution)doubleClickedItem.getRealObject(), true);
				}
				else if (doubleClickedItem.getType() == UserNodeType.GLOBAL_VARIABLES)
				{
					EditorUtil.openScriptEditor((Solution)doubleClickedItem.getRealObject(), true);
				}
				else if (doubleClickedItem.getType() == UserNodeType.FORM_VARIABLES)
				{
					EditorUtil.openScriptEditor((Form)doubleClickedItem.getRealObject(), true);
				}
				else if (doubleClickedItem.getType() == UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE)
				{
					Object clickedRealObject = doubleClickedItem.getRealObject();
					if (clickedRealObject instanceof ServoyProject) ServoyModelManager.getServoyModelManager().getServoyModel().setActiveProject(
						(ServoyProject)clickedRealObject, true);
				}
				else if ((!expandable || ctrlPressed) && openActionInTree.isEnabled())
				{
					// execute open option
					openActionInTree.run();
				}
				else if (expandable)
				{
					// expand/collapse anyway - if we cannot open
					if (tree.getExpandedState(doubleClickedItem))
					{
						tree.collapseToLevel(doubleClickedItem, AbstractTreeViewer.ALL_LEVELS);
					}
					else
					{
						tree.expandToLevel(doubleClickedItem, 1);
					}
				}
				openActionInTree.selectionChanged(new SelectionChangedEvent(tree, originalSelection));
			}

			public void mouseDown(MouseEvent e)
			{
			}

			public void mouseUp(MouseEvent e)
			{
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus()
	{
		StructuredViewer focusedView = fSelectionProviderMediator.getViewerInFocus();
		if (focusedView != null)
		{
			focusedView.getControl().setFocus();
		}
		else
		{
			tree.getControl().setFocus();
		}
	}

	@Override
	public void dispose()
	{
		beanCache.clear();

		if (resourceChangeListener != null)
		{
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
			resourceChangeListener = null;
		}
		if (fPartListener != null)
		{
			getViewSite().getPage().removePartListener(fPartListener);
			fPartListener = null;
		}

		if (statusBarUpdater != null)
		{
			statusBarUpdater.dispose();
			fSelectionProviderMediator.removeSelectionChangedListener(statusBarUpdater);
			statusBarUpdater = null;
		}

		if (resizeListener != null && fParent != null && !fParent.isDisposed())
		{
			fParent.removeControlListener(resizeListener);
		}

		if (activeProjectListener != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(activeProjectListener);
		}

		if (i18nChangeListener != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().removeI18NChangeListener(i18nChangeListener);
		}

		if (serverListener != null)
		{
			ServoyModel.getServerManager().removeServerListener(serverListener);
			serverListener = null;
		}

		if (tableListener != null)
		{
			IServerManagerInternal serverManager = ServoyModel.getServerManager();
			// add listeners to initial server list
			String[] array = serverManager.getServerNames(false, false, true, true);
			for (String server_name : array)
			{
				IServerInternal server = (IServerInternal)serverManager.getServer(server_name, false, false);
				server.removeTableListener(tableListener);
			}
			tableListener = null;
		}

		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(true, persistChangeListener);
		ServoyModelManager.getServoyModelManager().getServoyModel().removeSolutionMetaDataChangeListener(solutionMetaDataChangeListener);
//		ServoyProject[] currentRoots = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
//		registerPersistListener(currentRoots, null);

		yellow.dispose();

		labelProvider.dispose();
		if (decoratingLabelProvider != null) decoratingLabelProvider.removeListener(labelProviderListener);
		super.dispose();
	}

	@Override
	public Object getAdapter(Class type)
	{
		if (type == org.eclipse.ui.views.properties.IPropertySheetPage.class)
		{
			PropertySheetPage page = new ModifiedPropertySheetPage();
			page.setRootEntry(new ModifiedPropertySheetEntry());
			return page;
		}

		return super.getAdapter(type);
	}

	protected boolean setInput(ISelection selection)
	{
		if (selection instanceof IStructuredSelection)
		{
			UUID uuid = null;
			IStructuredSelection treeSelection = (IStructuredSelection)selection;
			Object firstElement = treeSelection.getFirstElement();

			if (firstElement instanceof IFile)
			{
				return showFile((IFile)firstElement);
			}
		}
		return false;
	}

	/**
	 * @param firstElement
	 */
	private boolean showFile(IFile resourceFile)
	{
		if (resourceFile == null) return false;

		UUID uuid;
		File workspace = resourceFile.getWorkspace().getRoot().getLocation().toFile();
		File f = resourceFile.getRawLocation().toFile();
		uuid = SolutionDeserializer.getUUID(f);
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(resourceFile.getProject().getName());
		IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), uuid);
		if (!(persist instanceof Form))
		{
			File parentFile = SolutionSerializer.getParentFile(workspace, f);
			if (parentFile != null) uuid = SolutionDeserializer.getUUID(parentFile);
		}

		if (uuid != null)
		{
			IContentProvider contentProvider = tree.getContentProvider();
			TreePath path = ((SolutionExplorerTreeContentProvider)contentProvider).getTreePath(uuid);
			if (path != null)
			{
				tree.setSelection(new TreeSelection(path), true);
				return true;
			}
		}
		return false;
	}

	public ShowInContext getShowInContext()
	{
		ISelection selection = this.getSelection();
		IStructuredSelection structSelection = new StructuredSelection();

		if (selection instanceof TreeSelection)
		{
			TreeSelection treeSelection = (TreeSelection)selection;

			if (treeSelection.getFirstElement() instanceof SimpleUserNode)
			{
				SimpleUserNode simpleUserNode = (SimpleUserNode)treeSelection.getFirstElement();

				if (simpleUserNode.getRealObject() instanceof IPersist)
				{
					IPersist persist = (IPersist)simpleUserNode.getRealObject();
					Solution solution = (Solution)persist.getRootObject();

					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solution.getName());
					//retrieve the project
					IResource project = servoyProject.getProject();


					Pair<String, String> pair = SolutionSerializer.getFilePath(persist, false);
					String left = pair.getLeft();
					int index = left.indexOf('/');
					String solutionName = left.substring(0, index);
					if (solutionName.equals(project.getName()))
					{
						left = left.substring(index + 1);
					}

					String right = pair.getRight();

					//retrieve the file;
					IResource file = ((IProject)project).getFile(new Path(left + right));

					structSelection = new StructuredSelection(new Object[] { file });
				}
			}
		}

		return new ShowInContext(null, structSelection);
	}

	public boolean show(ShowInContext context)
	{
		ISelection selection = context.getSelection();

		if (selection == null || !this.setInput(selection))
		{
			Object input = context.getInput();
			if (input instanceof IFileEditorInput)
			{
				showFile(((IFileEditorInput)input).getFile());
			}
		}
		return false;
	}

	/**
	 * IResource to SimpleUserNode adapter
	 * 
	 * @param resource
	 * @return SE node for the the resource
	 */
	private SimpleUserNode resourceToSimpleUserNode(IResource resource)
	{
		SolutionExplorerTreeContentProvider cp = (SolutionExplorerTreeContentProvider)tree.getContentProvider();
		IPath folderPath;
		String[] segments;
		ServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = sm.getActiveProject();
		ServoyResourcesProject resourcesProject = sm.getActiveResourcesProject();

		switch (resource.getType())
		{
			case IResource.PROJECT :
				try
				{
					if (((IProject)resource).isOpen() && ((IProject)resource).hasNature(ServoyProject.NATURE_ID)) // it is a servoy project
					{
						String projectName = resource.getName();
						SimpleUserNode solutionNode = cp.getSolutionNode(projectName);
						return solutionNode;
					}
					else if (resourcesProject != null && resourcesProject.getProject() == resource)
					{
						return cp.getResourcesNode(); // it is the resource project
					}
				}
				catch (CoreException ex)
				{
					ServoyLog.logError(ex);
				}
				break;
			case IResource.FOLDER :
				folderPath = resource.getFullPath();
				segments = folderPath.segments();
				if (segments.length == 2) // for finding folders under resource project
				{
					if (activeProject != null && segments[0].equals(activeProject.getProject().getName()))
					{
						if (segments[1].equals(SolutionSerializer.FORMS_DIR))
						{
							return cp.getForms();
						}
						else if (segments[1].equals(SolutionSerializer.RELATIONS_DIR))
						{
							return cp.getRelations();
						}
						else if (segments[1].equals(SolutionSerializer.VALUELISTS_DIR))
						{
							return cp.getValuelists();
						}
						else if (segments[1].equals(SolutionSerializer.MEDIAS_DIR))
						{
							return cp.getMedia();
						}
					}

					if (resourcesProject != null)
					{
						String resourceProjectName = resourcesProject.getProject().getName();
						if (segments[0].equals(resourceProjectName)) // it is the resource project
						{
							if (segments[1].equals(StringResourceDeserializer.STYLES_DIR_NAME))
							{
								return cp.getStylesNode();
							}
							else if (segments[1].equals(StringResourceDeserializer.TEMPLATES_DIR_NAME))
							{
								return cp.getTemplatesNode();
							}
							else if (segments[1].equals(SolutionSerializer.DATASOURCES_DIR_NAME))
							{
								return cp.getServers();
							}
							else if (segments[1].equals(WorkspaceUserManager.SECURITY_DIR))
							{
								return cp.getUserGroupSecurityNode();
							}
							else if (segments[1].equals(EclipseMessages.MESSAGES_DIR))
							{
								return cp.getI18NFilesNode();
							}
						}
					}
				}
				else if (segments.length == 3) // for finding folders under solution/forms
				{
					String solutionProjectName = null;
					if (activeProject != null) solutionProjectName = activeProject.getProject().getName();
					if (solutionProjectName != null && segments[0].equals(solutionProjectName))
					{
						if (segments[1].equals(SolutionSerializer.FORMS_DIR)) // if the forms node 
						{
							SimpleUserNode formsNode = cp.getForms();
							if (formsNode != null && formsNode.children != null)
							{
								for (SimpleUserNode un : formsNode.children) // find the form
								{
									if (un.getName().equals(segments[2])) return un;
								}
							}
						}
					}
				}
				break;
			case IResource.FILE :
				// we only handle files under solution/forms
				folderPath = resource.getFullPath();
				segments = folderPath.segments();
				String solutionProjectName = null;
				if (activeProject != null) solutionProjectName = activeProject.getProject().getName();
				if (solutionProjectName != null && segments[0].equals(solutionProjectName))
				{
					if (segments[1].equals(SolutionSerializer.FORMS_DIR)) // if the forms node 
					{
						SimpleUserNode formsNode = cp.getForms();
						if (formsNode != null && formsNode.children != null)
						{
							for (SimpleUserNode un : formsNode.children) // find the form
							{
								if (((un.getName() + SolutionSerializer.FORM_FILE_EXTENSION).equals(segments[2]) || (un.getName() + SolutionSerializer.JS_FILE_EXTENSION).equals(segments[2])) &&
									segments.length == 3) return un;
							}
						}
					}
					else if (segments[1].equals(SolutionSerializer.GLOBALS_FILE))
					{
						PlatformSimpleUserNode globalsFolder = cp.getGlobalsFolder();
						if (globalsFolder.getType() == UserNodeType.GLOBALS_ITEM) return globalsFolder;
					}
				}
				break;
		}

		return null;
	}

	public void setIncludeModulesOption(boolean includeModulesOptionStatus)
	{
		((SolutionExplorerListContentProvider)list.getContentProvider()).setIncludeModules(includeModulesOptionStatus);
		refreshList();
		includeModulesToggleButton.setSelection(includeModulesOptionStatus);
	}

	public void setOpenAsDefaultOption(boolean openAsdefaultOptionStatus)
	{
		openModeToggleButton.setSelection(openAsdefaultOptionStatus);
	}

	public void enablePostgresDBCreation()
	{
		newPostgresqlDatabase.setEnabledStatus();
	}

	public void enableSybaseDBCreation()
	{
		newSybaseDatabase.setEnabledStatus();
	}
}
