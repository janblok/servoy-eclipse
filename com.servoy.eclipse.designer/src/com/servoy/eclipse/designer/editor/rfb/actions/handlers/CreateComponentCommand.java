/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import static com.servoy.eclipse.core.util.UIUtils.getActiveShell;
import static com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentHandler.autoshowWizard;
import static java.util.Arrays.asList;
import static org.eclipse.jface.dialogs.MessageDialog.openQuestion;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.rfb.palette.PaletteCommonsHandler;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.autowizard.FormComponentTreeSelectDialog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.ChildWebComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistCloneable;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

/**
 * Command to create a component.
 *
 * @author rgansevles
 */
public class CreateComponentCommand extends BaseRestorableCommand
{
	private static final AtomicInteger id = new AtomicInteger();
	private static final Dimension EMPTY_SIZE = new Dimension(0, 0);

	private final CreateComponentOptions args;
	private final IStructuredSelection[] newSelection;
	private IPersist[] newPersist;
	private final BaseVisualFormEditor editorPart;

	public CreateComponentCommand(BaseVisualFormEditor editorPart, CreateComponentOptions args, IStructuredSelection[] newSelection)
	{
		super("createComponent");
		this.editorPart = editorPart;
		this.args = args;
		this.newSelection = newSelection;
	}

	@Override
	public void execute()
	{
		try
		{
			List<IPersist> changedPersists = new ArrayList<IPersist>();
			newPersist = createComponent(editorPart, args, changedPersists);
			if (newPersist != null)
			{
				changedPersists.addAll(asList(newPersist));
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changedPersists);
				if (newSelection != null && !args.isKeepOldSelection())
				{
					newSelection[0] = new StructuredSelection(
						newPersist.length > 0 ? PersistContext.create(newPersist[0], editorPart.getForm()) : newPersist);
				}
				if (newPersist.length == 1 && newPersist[0] instanceof LayoutContainer &&
					CSSPositionUtils.isCSSPositionContainer((LayoutContainer)newPersist[0]))
				{
					if (openQuestion(getActiveShell(), "Edit css position container",
						"Do you want to zoom into the layout container so you can edit it ?"))
					{
						BaseVisualFormEditor editor = DesignerUtil.getActiveEditor();
						if (editor != null)
						{
							BaseVisualFormEditorDesignPage activePage = editor.getGraphicaleditor();
							if (activePage instanceof RfbVisualFormEditorDesignPage)
								((RfbVisualFormEditorDesignPage)activePage).showContainer((LayoutContainer)newPersist[0]);
						}
					}
				}
			}
		}
		catch (JSONException | RepositoryException ex)
		{
			Debug.error(ex);
		}
	}

	static IPersist[] createComponent(BaseVisualFormEditor editorPart, CreateComponentOptions args, List<IPersist> extraChangedPersists)
		throws JSONException, RepositoryException
	{
		if (args.getType() != null)
		{
			// a ghost dragged from the palette. it is defined in the "types" section of the .spec file
			IPersist next = PersistFinder.INSTANCE.searchForPersist(editorPart, args.getDropTargetUUID());
			int arrayIndex = -1;
			if (next instanceof IChildWebObject)
			{
				arrayIndex = ((IChildWebObject)next).getIndex();
				if (args.isAddAfterTarget())
				{
					arrayIndex++;
				}
				next = next.getParent();
			}
			else if (args.isPrepend())
			{
				arrayIndex = 0;
			}
			if (next instanceof BaseComponent)
			{
				if (next instanceof IBasicWebComponent)
				{
					IBasicWebComponent parentBean = (IBasicWebComponent)next;
					String propertyName = args.getGhostPropertyName();
					String compName = "component_" + id.incrementAndGet();
					while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
					{
						compName = "component_" + id.incrementAndGet();
					}
					parentBean = (IBasicWebComponent)ElementUtil.getOverridePersist(PersistContext.create(parentBean, editorPart.getForm()));
					WebCustomType bean = AddContainerCommand.addCustomType(parentBean, propertyName, compName, arrayIndex, null);
					AddContainerCommand.showDataproviderDialog(bean.getPropertyDescription().getProperties(), bean, editorPart);
					return new IPersist[] { bean };
				}
				else if (args.getType().equals("tab"))
				{
					if (next instanceof ISupportChilds)
					{
						ISupportChilds iSupportChilds = (ISupportChilds)next;
						iSupportChilds = (ISupportChilds)ElementUtil.getOverridePersist(PersistContext.create(iSupportChilds, editorPart.getForm()));
						Tab newTab = (Tab)editorPart.getForm().getRootObject().getChangeHandler().createNewObject(iSupportChilds, IRepository.TABS);
						String tabName = "tab_" + id.incrementAndGet();
						while (!PersistFinder.INSTANCE.checkName(editorPart, tabName))
						{
							tabName = "tab_" + id.incrementAndGet();
						}
						newTab.setText(tabName);
						newTab.setLocation(args.getLocation());
						iSupportChilds.addChild(newTab);
						return new IPersist[] { newTab };
					}
				}
			}
		}
		else if (args.getName() != null || args.getUuid() != null)
		{
			ISupportFormElements parentSupportingElements = editorPart.getForm();
			IPersist dropTarget = null;
			IPersist initialDropTarget = null;
			if (args.getDropTargetUUID() != null)
			{
				dropTarget = PersistFinder.INSTANCE.searchForPersist(editorPart, args.getDropTargetUUID());
				if (dropTarget != null)
				{
					initialDropTarget = dropTarget;
					dropTarget = ElementUtil.getOverridePersist(PersistContext.create(dropTarget, editorPart.getForm()));
				}
				if (dropTarget != null)
				{
					IPersist p = dropTarget;
					while (!(p instanceof ISupportFormElements) && p != null)
					{
						p = p.getParent();
					}
					if (p instanceof ISupportFormElements)
					{
						parentSupportingElements = (ISupportFormElements)p;
					}
				}
			}

			if (args.getName() != null)
			{
				String name = args.getName();
				if (args.getPackageName() != null) // ghost components has no packageName
				{
					PaletteCommonsHandler.getInstance().updateComponentCounter(name);
				}
				if (dropTarget instanceof WebComponent)
				{
					// see if target has a 'component' or 'component[]' typed property
					WebComponent parentWebComponent = (WebComponent)dropTarget;
					PropertyDescription propertyDescription = ((WebObjectImpl)parentWebComponent.getImplementation()).getPropertyDescription();

					// TODO add a visual way for the user to drop to a specific property (if there is more then one property that supports components)
					// TODO also add a way of adding to a specific index in a component array and also just moving component ghosts in a component array property
					for (String propertyName : new TreeSet<String>(propertyDescription.getAllPropertiesNames()))
					{
						PropertyDescription property = propertyDescription.getProperty(propertyName);
						if (property != null)
						{
							if (property.getType() instanceof ComponentPropertyType)
							{
								// simple component type
								return new IPersist[] { createNestedWebComponent(editorPart, parentWebComponent, property, name, propertyName, -1,
									args.getLocation(), args.getSize()) };
							}
							else if (PropertyUtils.isCustomJSONArrayPropertyType(property.getType()) &&
								((CustomJSONArrayType< ? , ? >)property.getType()).getCustomJSONTypeDefinition().getType() instanceof ComponentPropertyType)
							{
								// array of component types
								int index = 0;
								IChildWebObject[] arrayOfChildComponents = (IChildWebObject[])parentWebComponent.getProperty(propertyName);
								if (arrayOfChildComponents != null) index = arrayOfChildComponents.length;
								return new IPersist[] { createNestedWebComponent(editorPart, parentWebComponent,
									((CustomJSONArrayType< ? , ? >)property.getType()).getCustomJSONTypeDefinition(), name, propertyName, index,
									args.getLocation(), args.getSize()) };
							}
						}
					} // if we found no property to drop to, just continue with code below - it will be dropped on form
				}

				if ("servoydefault-button".equals(name))
				{
					GraphicalComponent gc = parentSupportingElements.createNewGraphicalComponent(args.getLocation());
					gc.setText("button");
					gc.setOnActionMethodID(-1);
					gc.setRolloverCursor(Cursor.HAND_CURSOR);
					if (args.getStyleClass() != null)
					{
						gc.setStyleClass(args.getStyleClass());
					}
					return singlePersistWithLocationAndSize(gc, args);
				}
				else if ("servoydefault-label".equals(name))
				{
					GraphicalComponent gc = parentSupportingElements.createNewGraphicalComponent(args.getLocation());
					gc.setText(args.getText() != null ? args.getText() : "label");
					if (args.getStyleClass() != null)
					{
						gc.setStyleClass(args.getStyleClass());
					}
					return singlePersistWithLocationAndSize(gc, args);
				}
				else if ("servoydefault-combobox".equals(name))
				{
					return createField(parentSupportingElements, Field.COMBOBOX, args);
				}
				else if ("servoydefault-textfield".equals(name))
				{
					return createField(parentSupportingElements, Field.TEXT_FIELD, args);
				}
				else if ("servoydefault-textarea".equals(name))
				{
					return createField(parentSupportingElements, Field.TEXT_AREA, args);
				}
				else if ("servoydefault-password".equals(name))
				{
					return createField(parentSupportingElements, Field.PASSWORD, args);
				}
				else if ("servoydefault-calendar".equals(name))
				{
					Field field = parentSupportingElements.createNewField(args.getLocation());
					field.setDisplayType(Field.CALENDAR);
					CSSPositionUtils.setLocation(field, args.getLocation());
					if (!EMPTY_SIZE.equals(args.getSize())) CSSPositionUtils.setSize(field, args.getSize());
					return new IPersist[] { field };
				}
				else if ("servoydefault-typeahead".equals(name))
				{
					return createField(parentSupportingElements, Field.TYPE_AHEAD, args);
				}
				else if ("servoydefault-spinner".equals(name))
				{
					return createField(parentSupportingElements, Field.SPINNER, args);
				}
				else if ("servoydefault-check".equals(name) || "servoydefault-checkgroup".equals(name))
				{
					return createField(parentSupportingElements, Field.CHECKS, args);
				}
				else if ("servoydefault-radio".equals(name) || "servoydefault-radiogroup".equals(name))
				{
					return createField(parentSupportingElements, Field.RADIOS, args);
				}
				else if ("servoydefault-imagemedia".equals(name))
				{
					return createField(parentSupportingElements, Field.IMAGE_MEDIA, args);
				}
				else if ("servoydefault-listbox".equals(name))
				{
					return createField(parentSupportingElements, Field.LIST_BOX, args);
				}
				else if ("servoydefault-htmlarea".equals(name))
				{
					Field field = parentSupportingElements.createNewField(args.getLocation());
					field.setDisplayType(Field.HTML_AREA);
					field.setEditable(true);
					return singlePersistWithLocationAndSize(field, args);
				}
				else if ("servoydefault-tabpanel".equals(name))
				{
					String compName = "tabpanel_" + id.incrementAndGet();
					while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
					{
						compName = "tabpanel_" + id.incrementAndGet();
					}
					TabPanel tabPanel = null;
					if (parentSupportingElements instanceof AbstractContainer)
					{
						tabPanel = ((AbstractContainer)parentSupportingElements).createNewTabPanel(compName);
					}
					else
					{
						tabPanel = editorPart.getForm().createNewTabPanel(compName);
					}
					return singlePersistWithLocationAndSize(tabPanel, args);
				}
				else if ("servoydefault-splitpane".equals(name))
				{
					String compName = "tabpanel_" + id.incrementAndGet();
					while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
					{
						compName = "tabpanel_" + id.incrementAndGet();
					}
					TabPanel tabPanel = null;
					if (parentSupportingElements instanceof AbstractContainer)
					{
						tabPanel = ((AbstractContainer)parentSupportingElements).createNewTabPanel(compName);
					}
					else
					{
						tabPanel = editorPart.getForm().createNewTabPanel(compName);
					}
					tabPanel.setTabOrientation(TabPanel.SPLIT_HORIZONTAL);
					return singlePersistWithLocationAndSize(tabPanel, args);
				}
				else if ("servoycore-portal".equals(name))
				{
					String compName = "portal_" + id.incrementAndGet();
					while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
					{
						compName = "portal_" + id.incrementAndGet();
					}
					Portal portal = null;
					if (parentSupportingElements instanceof AbstractContainer)
					{
						portal = ((AbstractContainer)parentSupportingElements).createNewPortal(compName, args.getLocation());
					}
					else
					{
						portal = editorPart.getForm().createNewPortal(compName, args.getLocation());
					}
					return singlePersistWithLocationAndSize(portal, args);
				}
				else if ("servoydefault-rectangle".equals(name))
				{
					RectShape shape = editorPart.getForm().createNewRectangle(args.getLocation());
					shape.setLineSize(1);
					return singlePersistWithLocationAndSize(shape, args);
				}
				else
				{
					WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(name);
					if (spec != null)
					{
						String compName = null;
						String componentName = spec.getDisplayName().replaceAll("\\s", "").toLowerCase();
						componentName = componentName.replaceAll("-", "_");
						compName = componentName + "_" + id.incrementAndGet();
						while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
						{
							compName = componentName + "_" + id.incrementAndGet();
						}

						WebComponent webComponent = null;
						if (parentSupportingElements instanceof Portal)
						{
							Portal portal = (Portal)parentSupportingElements;
							webComponent = (WebComponent)editorPart.getForm().getRootObject().getChangeHandler().createNewObject(portal,
								IRepository.WEBCOMPONENTS);
							webComponent.setProperty("text", compName); //default
							if (args.getText() != null)
							{
								webComponent.setProperty("text", args.getText());
							}

							webComponent.setTypeName(name);
							portal.addChild(webComponent);
						}
						else if (parentSupportingElements instanceof AbstractContainer)
						{
							webComponent = ((AbstractContainer)parentSupportingElements).createNewWebComponent(compName, name);

						}
						CSSPositionUtils.setLocation(webComponent, args.getLocation());
						CSSPositionUtils.setSize(webComponent, args.getSize());
						PropertyDescription description = spec.getProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
						if (EMPTY_SIZE.equals(args.getSize()) && description != null && description.getDefaultValue() instanceof JSONObject)
						{
							CSSPositionUtils.setSize(webComponent, ((JSONObject)description.getDefaultValue()).optInt("width", 80),
								((JSONObject)description.getDefaultValue()).optInt("height", 80));
						}
						else if (EMPTY_SIZE.equals(args.getSize()))
						{
							CSSPositionUtils.setSize(webComponent, 200, 100);
						}
						Collection<String> allPropertiesNames = spec.getAllPropertiesNames();
						for (String propertyName : allPropertiesNames)
						{
							PropertyDescription property = spec.getProperty(propertyName);
							if (property != null)
							{
								if (args.hasOtherProperty(propertyName) && webComponent.getProperty(propertyName) == null)
								{
									webComponent.setProperty(propertyName, args.getOtherProperty(propertyName));
									if (property.getType() == FormComponentPropertyType.INSTANCE)
									{
										FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(webComponent);
										Form form = FormComponentPropertyType.INSTANCE.getForm(args.getOtherProperty(propertyName), flattenedSolution);
										if (form != null)
										{
											Dimension size = form.getSize();
											if (size.height == 0)
											{
												size.height = CSSPositionUtils.getSize(webComponent).height;
											}
											CSSPositionUtils.setSize(webComponent, size.width, size.height);
										}
									}
								}
								else if (property.getInitialValue() != null)
								{
									Object initialValue = property.getInitialValue();
									if (initialValue != null) webComponent.setProperty(propertyName, initialValue);
								}
								if ("autoshow".equals(property.getTag("wizard")))
								{
									if (property.getType() == FormComponentPropertyType.INSTANCE && property.getConfig() instanceof ComponentTypeConfig &&
										((ComponentTypeConfig)property.getConfig()).forFoundset != null)
									{
										// list form component
										FormComponentTreeSelectDialog.selectFormComponent(webComponent, editorPart.getForm());
									}
									else
									{
										autoshowWizard(parentSupportingElements, spec, webComponent, property, editorPart, id);
									}
								}
							}
						}
						AddContainerCommand.showDataproviderDialog(spec.getProperties(), webComponent, editorPart);
						List<IPersist> changes = new ArrayList<>();
						boolean addSiblingsToChanges = true;
						if (editorPart.getForm().isResponsiveLayout() || webComponent.getParent() instanceof CSSPositionLayoutContainer)
						{
							if (initialDropTarget != null &&
								!initialDropTarget.getUUID().equals(webComponent.getParent().getUUID()))
							{
								ISupportChilds parent = webComponent.getParent();
								changes.add(webComponent.getParent());
								addSiblingsToChanges = false;//no need to mark the siblings as changed because the whole parent was overridden

								FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(webComponent);
								parent = PersistHelper.getFlattenedPersist(flattenedSolution, editorPart.getForm(), parent);
								Iterator<IPersist> it = parent.getAllObjects();
								while (it.hasNext())
								{
									IPersist child = it.next();
									IPersist overridePersist = ElementUtil.getOverridePersist(PersistContext.create(child, editorPart.getForm()));
									if (!overridePersist.getUUID().equals(child.getUUID()))
									{
										parent.removeChild(child);
										// do not add the override again, the getOverridePersist should already create it in the right place (probably directly on form)
										//parent.addChild(overridePersist);
									}
								}
							}
							else
							{
								changes.add(webComponent);
							}
							webComponent.setLocation(getLocationAndShiftSiblings(editorPart, webComponent.getParent(), args, extraChangedPersists));
							// we don't need to add the changed components
							//if (addSiblingsToChanges) changes.addAll(extraChangedPersists);
						}
						else
						{
							changes.add(webComponent);
						}
						return changes.toArray(new IPersist[changes.size()]);
					}
					else
					{
						PackageSpecification<WebLayoutSpecification> specifications = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications()
							.get(
								args.getPackageName());
						if (specifications != null)
						{
							WebLayoutSpecification layoutSpec = specifications.getSpecification(name);
							if (layoutSpec != null)
							{
								Iterator<IPersist> childContainersIte = parentSupportingElements.getObjects(IRepositoryConstants.LAYOUTCONTAINERS);
								LayoutContainer sameTypeChildContainer = null;
								if (!"div".equals(layoutSpec.getName()))
								{
									while (childContainersIte.hasNext())
									{
										LayoutContainer childContainer = (LayoutContainer)childContainersIte.next();
										if (layoutSpec.getName().equals(childContainer.getSpecName()))
										{
											sameTypeChildContainer = childContainer;
										}
									}
								}
								JSONObject config = layoutSpec.getConfig() instanceof String ? new JSONObject((String)layoutSpec.getConfig()) : null;
								boolean fullRefreshNeeded = initialDropTarget != null && !initialDropTarget.equals(dropTarget) &&
									initialDropTarget.getParent() instanceof Form;
								// this is a fix for dropping the responsive container on csspos
								List<IPersist> res = createLayoutContainer(editorPart, parentSupportingElements, layoutSpec, sameTypeChildContainer, config,
									args.getLocation(), specifications, args.getPackageName());
								if (dropTarget != null && !dropTarget.equals(initialDropTarget))
								{
									res.add(dropTarget);
								}
//								else if (!fullRefreshNeeded && !res.isEmpty() && res.get(0).getParent() instanceof Form)
//								{
//									LayoutContainer layoutContainer = (LayoutContainer)res.get(0);
//									List<IPersist> children = new ArrayList<>(((AbstractContainer)layoutContainer.getParent()).getAllObjectsAsList());
//									Collections.sort(children, PositionComparator.XY_PERSIST_COMPARATOR);
//									//only refresh if it's not the last element
//									fullRefreshNeeded = !layoutContainer.getUUID().equals(children.get(children.size() - 1).getUUID());
//								}
								IPersist[] result = res.toArray(new IPersist[0]);
								if (fullRefreshNeeded)
								{
									IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
									for (IEditorReference editorRef : editorRefs)
									{
										IEditorPart editor = editorRef.getEditor(false);
										if (editor instanceof BaseVisualFormEditor)
										{
											BaseVisualFormEditorDesignPage activePage = ((BaseVisualFormEditor)editor).getGraphicaleditor();
											if (activePage instanceof RfbVisualFormEditorDesignPage)
												((RfbVisualFormEditorDesignPage)activePage).refreshContent();
											break;
										}
									}
								}
								return result;
							}
						}
						else
						{
							for (IRootObject template : ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES))
							{
								if (template.getName().equals(name))
								{
									Point p = getLocationAndShiftSiblings(editorPart, parentSupportingElements, args, extraChangedPersists);
									Object[] applyTemplate = ElementFactory.applyTemplate(parentSupportingElements,
										new TemplateElementHolder((Template)template), new org.eclipse.swt.graphics.Point(p.x, p.y), false);
									if (applyTemplate.length > 0)
									{
										if (applyTemplate[0] instanceof FormElementGroup)
										{
											Iterator<IFormElement> elements = ((FormElementGroup)applyTemplate[0]).getElements();
											//convert iterator to []
											ArrayList<IFormElement> list = new ArrayList<>();
											while (elements.hasNext())
											{
												IFormElement next = elements.next();
												list.add(next);
											}
											return list.toArray(new IPersist[list.size()]);
										}
										else
										{ //Object[] to IPersist[]
											return asList(applyTemplate).toArray(new IPersist[applyTemplate.length]);
										}
									}
								}
							}
						}
					}
				}
			}
			else if (args.getUuid() != null)
			{
				IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart, args.getUuid());
				if (persist instanceof AbstractBase)
				{
					IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
					ISupportChilds parent = dropTarget instanceof ISupportChilds ? (ISupportChilds)dropTarget : persist.getParent();
					IPersist newPersist = ((AbstractBase)persist).cloneObj(parent, true, validator, true, true, true);
					Point p = getLocationAndShiftSiblings(editorPart, parent, args, extraChangedPersists);
					CSSPositionUtils.setLocation((ISupportBounds)newPersist, p.x, p.y);
					if (!EMPTY_SIZE.equals(args.getSize())) CSSPositionUtils.setSize((ISupportBounds)newPersist, args.getSize());

					final ArrayList<IPersist> newPersists = new ArrayList<IPersist>();
					newPersist.acceptVisitor(new IPersistVisitor()
					{
						@Override
						public Object visit(IPersist o)
						{
							if (o instanceof IPersistCloneable)
							{
								newPersists.add(o);
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});

					return newPersists.toArray(new IPersist[newPersists.size()]);
				}
			}
		}

		return null;
	}

	private static IPersist[] createField(ISupportFormElements parentSupportingElements, int displayType, CreateComponentOptions args)
		throws RepositoryException
	{
		Field field = parentSupportingElements.createNewField(args.getLocation());
		field.setDisplayType(displayType);
		return singlePersistWithLocationAndSize(field, args);
	}

	private static IPersist[] singlePersistWithLocationAndSize(IFormElement persist, CreateComponentOptions args)
	{
		CSSPositionUtils.setLocation(persist, args.getLocation());
		if (!EMPTY_SIZE.equals(args.getSize())) CSSPositionUtils.setSize(persist, args.getSize());
		return new IPersist[] { persist };
	}

	private static List<IPersist> createLayoutContainer(BaseVisualFormEditor editorPart, ISupportFormElements parent, WebLayoutSpecification layoutSpec,
		LayoutContainer sameTypeChildContainer,
		JSONObject config, Point location, PackageSpecification<WebLayoutSpecification> specifications, String packageName)
		throws RepositoryException, JSONException
	{
		List<IPersist> newPersists = new ArrayList<IPersist>();
		int type = parent.getAncestor(IRepository.CSSPOS_LAYOUTCONTAINERS) == null && layoutSpec.getName().equals("servoycore-responsivecontainer")
			? IRepository.CSSPOS_LAYOUTCONTAINERS : IRepository.LAYOUTCONTAINERS;
		LayoutContainer container = (LayoutContainer)editorPart.getForm().getRootObject().getChangeHandler().createNewObject(parent,
			type);
		container.setSpecName(layoutSpec.getName());
		container.setPackageName(packageName);
		parent.addChild(container);
		if (container instanceof CSSPositionLayoutContainer)
		{
			((CSSPositionLayoutContainer)container)
				.setCssPosition(new CSSPosition(Integer.toString(location.y), "-1", "-1", Integer.toString(location.x), "200", "200"));
		}
		else
		{
			container.setLocation(new Point(location.x, location.x));
			if (CSSPositionUtils.isCSSPositionContainer(layoutSpec)) container.setSize(new Dimension(200, 200));
		}
		newPersists.add(container);
		if (config != null)
		{
			// if this is a composite try to set the actual layoutname (so a row combination with columns becomes here just a row)
			String layoutName = config.optString("layoutName", null);
			if (layoutName != null) container.setSpecName(layoutName);
			Iterator keys = config.keys();
			while (keys.hasNext())
			{
				String key = (String)keys.next();
				Object value = config.get(key);
				if ("children".equals(key))
				{
					// special key to create children instead of a attribute set.
					JSONArray array = (JSONArray)value;
					for (int i = 0; i < array.length(); i++)
					{
						JSONObject jsonObject = array.getJSONObject(i);
						if (jsonObject.has("layoutName"))
						{
							WebLayoutSpecification spec = specifications.getSpecification(jsonObject.getString("layoutName"));
							newPersists.addAll(
								createLayoutContainer(editorPart, container, spec, null, jsonObject.optJSONObject("model"), new Point(i + 1, i + 1),
									specifications,
									packageName));
						}
						else if (jsonObject.has("componentName"))
						{
							String compName = "component_" + id.incrementAndGet();
							WebComponent component = container.createNewWebComponent(compName, jsonObject.getString("componentName"));
							newPersists.add(component);
							WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
								jsonObject.getString("componentName"));
							if (spec != null)
							{
								Collection<String> allPropertiesNames = spec.getAllPropertiesNames();
								for (String string : allPropertiesNames)
								{
									PropertyDescription property = spec.getProperty(string);
									if (property != null && property.getInitialValue() != null)
									{
										Object initialValue = property.getInitialValue();
										if (initialValue != null) component.setProperty(string, initialValue);
									}
								}
							}
						}
					}
				} // children and layoutName are special
				else if (!"layoutName".equals(key))
				{
					container.putAttribute(key, sameTypeChildContainer != null ? sameTypeChildContainer.getAttribute(key) : value.toString());
				}
			}
		}
		return newPersists;
	}

	private static ChildWebComponent createNestedWebComponent(BaseVisualFormEditor editorPart, WebComponent parentWebComponent, PropertyDescription pd,
		String componentSpecName,
		String propertyName, int indexIfInArray, Point location, Dimension size)
	{
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(componentSpecName);
		if (spec != null)
		{
			String compName = null;
			String componentName = spec.getDisplayName().replaceAll("\\s", "").toLowerCase();
			componentName = componentName.replaceAll("-", "_");
			compName = componentName + "_" + id.incrementAndGet();
			while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
			{
				compName = componentName + "_" + id.incrementAndGet();
			}

			ChildWebComponent webComponent = ChildWebComponent.createNewInstance(parentWebComponent, pd, propertyName, indexIfInArray);
			webComponent.setTypeName(componentSpecName);

			// not sure if location and size are still needed to be set in children here... maybe it is (if parent wants to use them at runtime)
			int xRelativeToParent = Math.max(0, (int)(location.x - parentWebComponent.getLocation().getX()));
			int yRelativeToParent = Math.max(0, (int)(location.y - parentWebComponent.getLocation().getY()));
			webComponent.setLocation(new Point(xRelativeToParent, yRelativeToParent));
			webComponent.setSize(size);
			PropertyDescription description = spec.getProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
			if (description != null && description.getDefaultValue() instanceof JSONObject)
			{
				webComponent.setSize(new Dimension(((JSONObject)description.getDefaultValue()).optInt("width", 80),
					((JSONObject)description.getDefaultValue()).optInt("height", 80)));
			}
			parentWebComponent.insertChild(webComponent);

			return webComponent;
		}
		return null;
	}

	private static Point getLocationAndShiftSiblings(BaseVisualFormEditor editorPart, ISupportChilds parent, CreateComponentOptions args,
		List<IPersist> extraChangedPersists) throws RepositoryException
	{
		if ((editorPart.getForm().isResponsiveLayout() || parent instanceof CSSPositionLayoutContainer) && !CSSPositionUtils.isCSSPositionContainer(
			parent instanceof LayoutContainer ? (LayoutContainer)parent : null))
		{
			List<IPersist> children = new ArrayList<IPersist>();
			Iterator<IPersist> it = PersistHelper.getFlattenedPersist(ModelUtils.getEditingFlattenedSolution(editorPart.getForm()), editorPart.getForm(),
				parent).getAllObjects();
			while (it.hasNext())
			{
				IPersist persist = it.next();
				if (persist instanceof ISupportBounds)
				{
					children.add(persist);
				}
			}

			// default place it as the first element.
			int x = 1;
			int y = 1;
			if (children.size() > 0)
			{
				IPersist[] childArray = children.toArray(new IPersist[0]);
				Arrays.sort(childArray, PositionComparator.XY_PERSIST_COMPARATOR);
				if (args.getRightSibling() != null)
				{
					IPersist rightSibling = PersistFinder.INSTANCE.searchForPersist(editorPart, args.getRightSibling());
					if (rightSibling == null && editorPart.getForm().getExtendsForm() != null)
					{

						Form f = editorPart.getForm();
						do
						{
							f = f.getExtendsForm();
							rightSibling = AbstractRepository.searchPersist(f, UUID.fromString(args.getRightSibling()));
						}
						while (f.getExtendsForm() != null);
						if (rightSibling != null)
						{
							rightSibling = ElementUtil.getOverridePersist(PersistContext.create(rightSibling, editorPart.getForm()));
						}
					}
					if (rightSibling != null)
					{
						int counter = 1;
						for (IPersist element : childArray)
						{
							if (element.getUUID().equals(rightSibling.getUUID()))
							{
								x = counter;
								y = counter;
								// i don't think we need this, because we are doing the increment at line 940
								counter++;
							}
							((ISupportBounds)element).setLocation(new Point(counter, counter));
							if (extraChangedPersists != null && element.isChanged()) extraChangedPersists.add(element);
							counter++;
						}
					}
					else
					{
						Debug.log("Could not find rightsibling with uuid '" + args.getRightSibling() + "', inserting on last position.");
						Point location = ((ISupportBounds)childArray[childArray.length - 1]).getLocation();
						x = location.x + 1;
						y = location.y + 1;
					}
				}
				else
				{
					// insert as last element in flow layout because no right/bottom sibling was given
					Point location = ((ISupportBounds)childArray[childArray.length - 1]).getLocation();
					x = location.x + 1;
					y = location.y + 1;
				}
			}
			return new Point(x, y);
		}
		return args.getLocation() == null ? new Point(1, 1) : args.getLocation();
	}

	@Override
	public void undo()
	{
		try
		{
			if (newPersist != null)
			{
				for (IPersist iPersist : newPersist)
				{
					((IDeveloperRepository)iPersist.getRootObject().getRepository()).deleteObject(iPersist);
				}

				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, asList(newPersist));
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Could not undo create elements", e);
		}
	}

	public static class CreateComponentOptions
	{
		private final JSONObject allProperties;
		private Point location;
		private Dimension size;
		private String rightSibling;
		private String text;
		private String styleClass;
		private String packageName;
		private String uuid;
		private String name;
		private Object type;
		private String ghostPropertyName;
		private String dropTargetUUID;
		private boolean keepOldSelection;
		private boolean addAfterTarget;
		private boolean prepend;

		public CreateComponentOptions()
		{
			this(null);
		}

		/**
		 * Constructor with optional original json for dynamic webcomponent properties
		 */
		public CreateComponentOptions(JSONObject allProperties)
		{
			this.allProperties = allProperties;
		}

		public boolean hasOtherProperty(String propertyName)
		{
			return allProperties != null && allProperties.has(propertyName);
		}

		public Object getOtherProperty(String propertyName)
		{
			return allProperties == null ? null : allProperties.opt(propertyName);
		}

		/**
		 * @param location the location to set
		 */
		public void setLocation(Point location)
		{
			this.location = location;
		}

		/**
		 * @param rightSibling the rightSibling to set
		 */
		public void setRightSibling(String rightSibling)
		{
			this.rightSibling = rightSibling;
		}

		/**
		 * @param text the text to set
		 */
		public void setText(String text)
		{
			this.text = text;
		}

		/**
		 * @param styleClass the styleClass to set
		 */
		public void setStyleClass(String styleClass)
		{
			this.styleClass = styleClass;
		}

		/**
		 * @param packageName the packageName to set
		 */
		public void setPackageName(String packageName)
		{
			this.packageName = packageName;
		}

		/**
		 * @param uuid the uuid to set
		 */
		public void setUuid(String uuid)
		{
			this.uuid = uuid;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name)
		{
			this.name = name;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(Object type)
		{
			this.type = type;
		}

		/**
		 * @param ghostPropertyName the ghostPropertyName to set
		 */
		public void setGhostPropertyName(String ghostPropertyName)
		{
			this.ghostPropertyName = ghostPropertyName;
		}

		/**
		 * @param size the size to set
		 */
		public void setSize(Dimension size)
		{
			this.size = size;
		}

		/**
		 * @param dropTargetUUID the dropTargetUUID to set
		 */
		public void setDropTargetUUID(String dropTargetUUID)
		{
			this.dropTargetUUID = dropTargetUUID;
		}

		/**
		 * @param addAfterTarget the addAfterTarget to set
		 */
		public void setAddAfterTarget(boolean addAfterTarget)
		{
			this.addAfterTarget = addAfterTarget;
		}

		/**
		 * @param prepend the prepend to set
		 */
		public void setPrepend(boolean prepend)
		{
			this.prepend = prepend;
		}

		/**
		 * @param keepOldSelection the keepOldSelection to set
		 */
		public void setKeepOldSelection(boolean keepOldSelection)
		{
			this.keepOldSelection = keepOldSelection;
		}

		public Point getLocation()
		{
			return location;
		}

		public Dimension getSize()
		{
			return size;
		}

		public String getRightSibling()
		{
			return rightSibling;
		}

		public String getText()
		{
			return text;
		}

		public String getStyleClass()
		{
			return styleClass;
		}

		public String getPackageName()
		{
			return packageName;
		}

		public String getUuid()
		{
			return uuid;
		}

		public String getName()
		{
			return name;
		}

		public Object getType()
		{
			return type;
		}

		public String getGhostPropertyName()
		{
			return ghostPropertyName;
		}

		public String getDropTargetUUID()
		{
			return dropTargetUUID;
		}

		public boolean isAddAfterTarget()
		{
			return addAfterTarget;
		}

		public boolean isPrepend()
		{
			return prepend;
		}

		public boolean isKeepOldSelection()
		{
			return keepOldSelection;
		}

		/**
		 * @param args
		 * @return
		 */
		public static CreateComponentOptions fromJson(JSONObject args)
		{
			CreateComponentOptions options = new CreateComponentOptions(args);

			options.rightSibling = args.optString("rightSibling", null);
			options.text = args.optString("text", null);
			options.styleClass = args.optString("styleClass", null);
			options.packageName = args.optString("packageName", null);
			options.uuid = args.optString("uuid", null);
			options.name = args.optString("name", null);
			options.type = args.optString("type", null);
			options.ghostPropertyName = args.optString("ghostPropertyName", null);
			options.dropTargetUUID = args.optString("dropTargetUUID", null);
			options.addAfterTarget = args.optBoolean("addAfterTarget", false);
			options.prepend = args.optBoolean("prepend", false);
			options.keepOldSelection = args.optBoolean("keepOldSelection", false);

			options.location = new Point(args.optInt("x"), args.optInt("y"));
			options.size = new Dimension(args.optInt("w"), args.optInt("h"));
			return options;
		}
	}
}