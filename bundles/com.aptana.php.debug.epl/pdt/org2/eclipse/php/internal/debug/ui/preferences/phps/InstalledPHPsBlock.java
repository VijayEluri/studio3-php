/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zend and IBM - Initial implementation
 *******************************************************************************/
package org2.eclipse.php.internal.debug.ui.preferences.phps;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org2.eclipse.php.internal.debug.core.interpreter.preferences.PHPexeItem;
import org2.eclipse.php.internal.debug.core.preferences.PHPDebuggersRegistry;
import org2.eclipse.php.internal.debug.core.preferences.PHPexes;
import org2.eclipse.php.internal.debug.ui.PHPDebugUIMessages;
import org2.eclipse.php.internal.debug.ui.wizard.ClosableWizardDialog;
import org2.eclipse.php.internal.debug.ui.wizard.exe.PHPExeEditDialog;
import org2.eclipse.php.internal.debug.ui.wizard.exe.PHPExeWizard;
import org2.eclipse.php.util.SWTUtil;

import com.aptana.core.util.StringUtil;
import com.aptana.editor.php.internal.ui.PHPPluginImages;
import com.aptana.php.debug.epl.PHPDebugEPLPlugin;

/**
 * A composite that displays installed PHP's in a table. PHPs can be added, removed, edited, and searched for.
 * <p>
 * This block implements ISelectionProvider - it sends selection change events when the checked PHP in the table
 * changes, or when the "use default" button check state changes.
 * </p>
 * 
 * @author seva, shalom
 */
public class InstalledPHPsBlock
{

	/**
	 * Content provider to show a list of PHPs
	 */
	class PHPsContentProvider implements IStructuredContentProvider
	{

		public void dispose()
		{
		}

		public Object[] getElements(final Object input)
		{
			return fPHPexes.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput)
		{
		}

	}

	/**
	 * Label provider for installed PHPs table.
	 */
	class PHPExeLabelProvider extends LabelProvider implements ITableLabelProvider, IFontProvider
	{

		/**
		 * @see ITableLabelProvider#getColumnImage(Object, int)
		 */
		public Image getColumnImage(final Object element, final int columnIndex)
		{
			if (columnIndex == 0 && element instanceof PHPexeItem)
			{
				// Display an error icon in cases where we cannot locate the ini.
				PHPexeItem item = (PHPexeItem) element;
				if (item.isEditable())
				{
					File iniLocation = item.getINILocation();
					if (iniLocation == null || !iniLocation.exists())
					{
						return PHPPluginImages.get(PHPPluginImages.IMG_OBJS_FIXABLE_ERROR);
					}
				}
			}
			return null;
		}

		/**
		 * @see ITableLabelProvider#getColumnText(Object, int)
		 */
		public String getColumnText(final Object element, final int columnIndex)
		{
			if (element instanceof PHPexeItem)
			{
				final PHPexeItem phpExe = (PHPexeItem) element;
				switch (columnIndex)
				{
					case 0:
						if (isDefault(element))
						{
							return phpExe.getName() + ' ' + PHPDebugUIMessages.PHPsPreferencePage_WorkspaceDefault;
						}
						return phpExe.getName();
					case 1:
						String debuggerName = PHPDebuggersRegistry.getDebuggerName(phpExe.getDebuggerID());
						if (debuggerName == null)
						{
							debuggerName = StringUtil.EMPTY;
						}
						return debuggerName;
					case 2:
						return (phpExe.getExecutable() != null) ? phpExe.getExecutable().getAbsolutePath()
								: PHPDebugUIMessages.InstalledPHPsBlock_unknown;
				}
			}
			return element.toString();
		}

		public Font getFont(Object element)
		{
			if (isDefault(element))
			{
				return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
			}
			return null;
		}

		private boolean isDefault(Object element)
		{
			if (element instanceof PHPexeItem)
			{
				return ((PHPexeItem) element).isDefault();
			}
			return false;
		}

	}

	private static final String[] PHP_CANDIDATE_BIN = { "php", "php-cli", "php-cgi", "php.exe", "php-cli.exe", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"php-cgi.exe" }; //$NON-NLS-1$

	// Action buttons
	private Button fAddButton;

	/**
	 * This block's control
	 */
	private Composite fControl;
	private Button fEditButton;
	/**
	 * The main list control
	 */
	private TableViewer fPHPExeList;
	/**
	 * VMs being displayed
	 */
	private final List<PHPexeItem> fPHPexes = new ArrayList<PHPexeItem>();

	private Button fRemoveButton;

	// ignore column re-sizing when the table is being resized
	private boolean fResizingTable = false;

	private Button fSearchButton;
	private Button fSetDefaultButton;
	/**
	 * Selection listeners (checked PHP changes)
	 */
	private final ListenerList fSelectionListeners = new ListenerList(ListenerList.IDENTITY);

	// index of column used for sorting
	private int fSortColumn = 0;

	// column weights
	private float fWeight1 = 3 / 8F;
	private float fWeight2 = 2 / 8F;
	private float fWeight3 = 3 / 8F;

	PHPexes phpExes;

	public InstalledPHPsBlock()
	{
		this.phpExes = PHPexes.getInstance();
	}

	/**
	 * Bring up a dialog that lets the user create a new VM definition.
	 */
	private void addPHPexe()
	{
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		NullProgressMonitor monitor = new NullProgressMonitor();
		PHPexeItem phpExeItem = null;
		PHPExeWizard wizard = new PHPExeWizard(phpExes.getAllItems());
		ClosableWizardDialog dialog = new ClosableWizardDialog(shell, wizard);
		if (dialog.open() == Window.CANCEL)
		{
			monitor.setCanceled(true);
			return;
		}
		phpExeItem = (PHPexeItem) wizard.getRootFragment().getWizardModel().getObject(PHPExeWizard.MODEL);
		fPHPexes.add(phpExeItem);
		phpExes.addItem(phpExeItem);
		fPHPExeList.refresh();
		commitChanges();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.
	 * ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(final ISelectionChangedListener listener)
	{
		fSelectionListeners.add(listener);
	}

	/**
	 * Correctly resizes the table so no phantom columns appear
	 */
	protected void configureTableResizing(final Composite parent, final Composite buttons, final Table table,
			final TableColumn column1, final TableColumn column2, final TableColumn column3)
	{
		parent.addControlListener(new ControlAdapter()
		{
			public void controlResized(final ControlEvent e)
			{
				resizeTable(parent, buttons, table, column1, column2, column3);
			}
		});
		table.addListener(SWT.Paint, new Listener()
		{
			public void handleEvent(final Event event)
			{
				table.removeListener(SWT.Paint, this);
				resizeTable(parent, buttons, table, column1, column2, column3);
			}
		});
		column1.addControlListener(new ControlAdapter()
		{
			public void controlResized(final ControlEvent e)
			{
				if (column1.getWidth() > 0 && !fResizingTable)
					fWeight1 = getColumnWeight(0);
			}
		});
		column2.addControlListener(new ControlAdapter()
		{
			public void controlResized(final ControlEvent e)
			{
				if (column2.getWidth() > 0 && !fResizingTable)
					fWeight2 = getColumnWeight(1);
			}
		});

		column3.addControlListener(new ControlAdapter()
		{
			public void controlResized(final ControlEvent e)
			{
				if (column3.getWidth() > 0 && !fResizingTable)
					fWeight3 = getColumnWeight(2);
			}
		});
	}

	/**
	 * Creates this block's control in the given control.
	 * 
	 * @param ancestor
	 *            containing control the user that opens the installed PHPs pref page for PHP management, or to provide
	 *            'add, remove, edit, and search' buttons.
	 */
	public void createControl(final Composite ancestor)
	{

		final Composite parent = new Composite(ancestor, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		parent.setLayout(layout);
		final Font font = ancestor.getFont();
		parent.setFont(font);
		fControl = parent;

		GridData data;

		final Label tableLabel = new Label(parent, SWT.NONE);
		tableLabel.setText(PHPDebugUIMessages.InstalledPHPsBlock_15);
		data = new GridData();
		data.horizontalSpan = 2;
		tableLabel.setLayoutData(data);
		tableLabel.setFont(font);

		final Table table = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

		data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
		table.setFont(font);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		final TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);

		final TableColumn column1 = new TableColumn(table, SWT.NULL);
		column1.setText(PHPDebugUIMessages.InstalledPHPsBlock_0);
		column1.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(final SelectionEvent e)
			{
				sortByName();
			}
		});

		final TableColumn column2 = new TableColumn(table, SWT.NULL);
		column2.setText(PHPDebugUIMessages.InstalledPHPsBlock_17);
		column2.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(final SelectionEvent e)
			{
				sortByDebugger();
			}
		});

		final TableColumn column3 = new TableColumn(table, SWT.NULL);
		column3.setText(PHPDebugUIMessages.InstalledPHPsBlock_1);
		column3.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(final SelectionEvent e)
			{
				sortByLocation();
			}
		});

		fPHPExeList = new CheckboxTableViewer(table);
		fPHPExeList.setLabelProvider(new PHPExeLabelProvider());
		fPHPExeList.setContentProvider(new PHPsContentProvider());
		fPHPExeList.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(final SelectionChangedEvent evt)
			{
				enableButtons();
			}
		});

		fPHPExeList.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(final DoubleClickEvent e)
			{
				if (!fPHPExeList.getSelection().isEmpty())
					editPHPexe();
			}
		});
		table.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(final KeyEvent event)
			{
				if (event.character == SWT.DEL && event.stateMask == 0)
					removePHPexes();
			}
		});

		final Composite buttons = new Composite(parent, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttons.setLayout(layout);
		buttons.setFont(font);

		fAddButton = createPushButton(buttons, PHPDebugUIMessages.InstalledPHPsBlock_3);
		fAddButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(final Event evt)
			{
				addPHPexe();
			}
		});

		fEditButton = createPushButton(buttons, PHPDebugUIMessages.InstalledPHPsBlock_4);
		fEditButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(final Event evt)
			{
				editPHPexe();
			}
		});

		fRemoveButton = createPushButton(buttons, PHPDebugUIMessages.InstalledPHPsBlock_5);
		fRemoveButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(final Event evt)
			{
				removePHPexes();
			}
		});

		fSetDefaultButton = createPushButton(buttons, PHPDebugUIMessages.InstalledPHPsBlock_setDefault);
		fSetDefaultButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				PHPexeItem defaultItem = (PHPexeItem) ((IStructuredSelection) fPHPExeList.getSelection())
						.getFirstElement();
				phpExes.setDefaultItem(defaultItem);
				commitChanges();
				setPHPs(phpExes.getAllItems());
				// Preferences prefs = PHPProjectPreferences.getModelPreferences();
				// prefs.setValue(PHPDebugCorePreferenceNames.DEFAULT_PHP, defaultItem.getName());
			}
		});

		// copied from ListDialogField.CreateSeparator()
		final Label separator = new Label(buttons, SWT.NONE);
		separator.setVisible(false);
		final GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.BEGINNING;
		gd.heightHint = 4;
		separator.setLayoutData(gd);

		fSearchButton = createPushButton(buttons, PHPDebugUIMessages.InstalledPHPsBlock_6);
		fSearchButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(final Event evt)
			{
				search();
			}
		});

		configureTableResizing(parent, buttons, table, column1, column2, column3);

		fillWithWorkspacePHPs();
		// by default, sort by the debugger type
		sortByDebugger();
		enableButtons();
	}

	protected Button createPushButton(final Composite parent, final String label)
	{
		return SWTUtil.createPushButton(parent, label, null);
	}

	private void editPHPexe()
	{
		final IStructuredSelection selection = (IStructuredSelection) fPHPExeList.getSelection();
		final PHPexeItem phpExe = (PHPexeItem) selection.getFirstElement();
		if (phpExe == null)
		{
			return;
		}
		PHPexeItem phpExeToEdit = new PHPexeItem(phpExe.getName(), phpExe.getExecutable(), phpExe.getINILocation(),
				phpExe.getDebuggerID(), phpExe.isEditable());
		PHPExeEditDialog dialog = new PHPExeEditDialog(getShell(), phpExeToEdit, phpExes.getAllItems());
		dialog.setTitle(PHPDebugUIMessages.InstalledPHPsBlock_8);
		if (dialog.open() != Window.OK)
		{
			return;
		}
		boolean needReAdding = false;
		if (!phpExe.getDebuggerID().equals(phpExeToEdit.getDebuggerID())
				|| !phpExe.getName().equals(phpExeToEdit.getName()))
		{
			// In this case, we need to remove this exe from the PHPExes and re-add it.
			// This way, the PHPExes will update the mapping for this exe.
			phpExes.removeItem(phpExe);
			needReAdding = true;
		}
		phpExe.setName(phpExeToEdit.getName());
		phpExe.setExecutable(phpExeToEdit.getExecutable());
		phpExe.setINILocation(phpExeToEdit.getINILocation());
		phpExe.setDebuggerID(phpExeToEdit.getDebuggerID());
		if (needReAdding)
		{
			// Re-add the item in case needed
			phpExes.addItem(phpExe);
		}
		commitChanges();
		setPHPs(phpExes.getAllItems());
	}

	private void enableButtons()
	{
		IStructuredSelection selection = (IStructuredSelection) fPHPExeList.getSelection();
		Object[] elements = selection.toArray();
		boolean canRemoveOrEdit = true;
		for (int i = 0; canRemoveOrEdit && i < elements.length; i++)
		{
			PHPexeItem item = (PHPexeItem) elements[i];
			canRemoveOrEdit &= item.isEditable();
		}
		final int selectionCount = selection.size();
		fRemoveButton.setEnabled(canRemoveOrEdit && selectionCount > 0);
		PHPexeItem selectedItem = (PHPexeItem) ((IStructuredSelection) fPHPExeList.getSelection()).getFirstElement();
		fSetDefaultButton.setEnabled(selectionCount == 1 && selectedItem != null && !selectedItem.isDefault());
	}

	/**
	 * Populates the PHP table with existing PHPs defined in the workspace.
	 */
	protected void fillWithWorkspacePHPs()
	{
		// fill with PHPs
		final PHPexeItem[] items = phpExes.getAllItems();
		setPHPs(items);
	}

	private float getColumnWeight(final int col)
	{
		final Table table = fPHPExeList.getTable();
		final int tableWidth = table.getSize().x;
		final int columnWidth = table.getColumn(col).getWidth();
		if (tableWidth > columnWidth)
			return (float) columnWidth / tableWidth;
		return 1 / 3F;
	}

	/**
	 * Returns this block's control
	 * 
	 * @return control
	 */
	public Control getControl()
	{
		return fControl;
	}

	/**
	 * Returns the PHPs currently being displayed in this block
	 * 
	 * @return PHPs currently being displayed in this block
	 */
	public PHPexeItem[] getPHPs()
	{
		return fPHPexes.toArray(new PHPexeItem[fPHPexes.size()]);
	}

	protected Shell getShell()
	{
		return getControl().getShell();
	}

	/**
	 * @see IAddPHPexeDialogRequestor#isDuplicateName(String)
	 */
	public boolean isDuplicateName(final String name)
	{
		for (int i = 0; i < fPHPexes.size(); i++)
		{
			final PHPexeItem phpExe = fPHPexes.get(i);
			if (phpExe.getName().equals(name))
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void removePHPexes()
	{
		final IStructuredSelection selection = (IStructuredSelection) fPHPExeList.getSelection();
		final PHPexeItem[] phpExes = new PHPexeItem[selection.size()];
		final Iterator iter = selection.iterator();
		int i = 0;
		while (iter.hasNext())
		{
			phpExes[i] = (PHPexeItem) iter.next();
			i++;
		}
		removePHPs(phpExes);
		commitChanges();
	}

	public void commitChanges()
	{
		phpExes.save();
	}

	public void removePHPs(final PHPexeItem[] phpExes)
	{
		for (PHPexeItem element : phpExes)
		{
			fPHPexes.remove(element);
			this.phpExes.removeItem(element);
		}
		fPHPExeList.refresh();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.
	 * ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(final ISelectionChangedListener listener)
	{
		fSelectionListeners.remove(listener);
	}

	private void resizeTable(final Composite parent, final Composite buttons, final Table table,
			final TableColumn column1, final TableColumn column2, final TableColumn column3)
	{
		fResizingTable = true;
		int parentWidth = -1;
		int parentHeight = -1;
		if (parent.isVisible())
		{
			final Rectangle area = parent.getClientArea();
			parentWidth = area.width;
			parentHeight = area.height;
		}
		else
		{
			final Point parentSize = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			parentWidth = parentSize.x;
			parentHeight = parentSize.y;
		}
		final Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		int width = parentWidth - 2 * table.getBorderWidth();
		if (preferredSize.y > parentHeight)
		{
			// Subtract the scrollbar width from the total column width
			// if a vertical scrollbar will be required
			final Point vBarSize = table.getVerticalBar().getSize();
			width -= vBarSize.x;
		}
		width -= buttons.getSize().x;
		final Point oldSize = table.getSize();
		if (oldSize.x > width)
		{
			// table is getting smaller so make the columns
			// smaller first and then resize the table to
			// match the client area width
			column1.setWidth(Math.round(width * fWeight1));
			column2.setWidth(Math.round(width * fWeight2));
			column3.setWidth(Math.round(width * fWeight3));
			table.setSize(width, parentHeight);
		}
		else
		{
			// table is getting bigger so make the table
			// bigger first and then make the columns wider
			// to match the client area width
			table.setSize(width, parentHeight);
			column1.setWidth(Math.round(width * fWeight1));
			column2.setWidth(Math.round(width * fWeight2));
			column3.setWidth(Math.round(width * fWeight3));
		}
		fResizingTable = false;
	}

	/**
	 * Restore table settings from the given dialog store using the given key.
	 * 
	 * @param settings
	 *            dialog settings store
	 * @param qualifier
	 *            key to restore settings from
	 */
	public void restoreColumnSettings(final IDialogSettings settings, final String qualifier)
	{
		fWeight1 = restoreColumnWeight(settings, qualifier, 0);
		fWeight2 = restoreColumnWeight(settings, qualifier, 1);
		fWeight3 = restoreColumnWeight(settings, qualifier, 2);
		fPHPExeList.getTable().layout(true);
		try
		{
			fSortColumn = settings.getInt(qualifier + ".sortColumn"); //$NON-NLS-1$
		}
		catch (final NumberFormatException e)
		{
			fSortColumn = 1;
		}
		switch (fSortColumn)
		{
			case 1:
				sortByName();
				break;
			case 2:
				sortByDebugger();
				break;
			case 3:
				sortByLocation();
				break;
			case 4:
				sortByType();
				break;
		}
	}

	private float restoreColumnWeight(final IDialogSettings settings, final String qualifier, final int col)
	{
		try
		{
			return settings.getFloat(qualifier + ".column" + col); //$NON-NLS-1$
		}
		catch (final NumberFormatException e)
		{
			switch (col)
			{
				case 1:
					return 2 / 8F;
				default:
					return 3 / 8F;
			}
		}

	}

	/**
	 * Persist table settings into the give dialog store, prefixed with the given key.
	 * 
	 * @param settings
	 *            dialog store
	 * @param qualifier
	 *            key qualifier
	 */
	public void saveColumnSettings(final IDialogSettings settings, final String qualifier)
	{
		for (int i = 0; i < 3; i++)
			// persist the first 2 column weights
			settings.put(qualifier + ".column" + i, getColumnWeight(i)); //$NON-NLS-1$
		settings.put(qualifier + ".sortColumn", fSortColumn); //$NON-NLS-1$
	}

	/**
	 * Search for installed VMs in the file system
	 */
	protected void search()
	{

		// choose a root directory for the search
		final DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage(PHPDebugUIMessages.InstalledPHPsBlock_9);
		dialog.setText(PHPDebugUIMessages.InstalledPHPsBlock_10);
		final String path = dialog.open();
		if (path == null)
			return;

		// ignore installed locations
		final Set<File> exstingLocations = new HashSet<File>();
		Iterator<PHPexeItem> iter = fPHPexes.iterator();
		while (iter.hasNext())
			exstingLocations.add(iter.next().getExecutable().getParentFile());

		// search
		final File rootDir = new File(path);
		final List<File> locations = new ArrayList<File>();

		final IRunnableWithProgress r = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor)
			{
				monitor.beginTask(PHPDebugUIMessages.InstalledPHPsBlock_11, IProgressMonitor.UNKNOWN);
				search(rootDir, locations, exstingLocations, monitor);
				monitor.done();
			}
		};

		try
		{
			final ProgressMonitorDialog progress = new ProgressMonitorDialog(getShell());
			progress.run(true, true, r);
		}
		catch (final InvocationTargetException e)
		{
			PHPDebugEPLPlugin.logError(e);
		}
		catch (final InterruptedException e)
		{
			// cancelled
			return;
		}

		if (locations.isEmpty())
			MessageDialog.openInformation(getShell(), PHPDebugUIMessages.InstalledPHPsBlock_12,
					MessageFormat.format(PHPDebugUIMessages.InstalledPHPsBlock_13, new Object[] { path }));
		else
		{
			Iterator<File> iter2 = locations.iterator();
			while (iter2.hasNext())
			{
				File location = iter2.next();
				PHPexeItem phpExe = new PHPexeItem(null, location, null, PHPDebuggersRegistry.getDefaultDebuggerId(),
						true);
				String nameCopy = new String(phpExe.getName());
				int i = 1;
				while (isDuplicateName(nameCopy))
				{
					nameCopy = phpExe.getName() + '[' + i++ + ']';
				}
				// Since the search for PHP exe option does not 'know' the debugger id it should assign to the
				// PHPexeItem,
				// we call for PHPexes.getDefaultDebuggerId() - which can also return null in some cases.
				if (phpExe.getExecutable() != null)
				{
					fPHPexes.add(phpExe);
					phpExes.addItem(phpExe);
					fPHPExeList.refresh();
				}
			}
		}

	}

	/**
	 * Searches the specified directory recursively for installed PHP executables, adding each detected executable to
	 * the <code>found</code> list. Any directories specified in the <code>ignore</code> are not traversed.
	 * 
	 * @param directory
	 * @param found
	 * @param types
	 * @param ignore
	 */
	protected void search(final File directory, final List<File> found, final Set<File> ignore,
			final IProgressMonitor monitor)
	{
		if (monitor.isCanceled())
			return;

		// Search the root directory
		if (!ignore.contains(directory))
		{
			final File foundExe = findPHPExecutable(directory);
			if (foundExe != null)
				found.add(foundExe);
		}

		final String[] names = directory.list();
		if (names == null)
			return;
		final List<File> subDirs = new ArrayList<File>();
		for (String element : names)
		{
			if (monitor.isCanceled())
				return;
			final File file = new File(directory, element);
			// PHPexeItem[] vmTypes = phpExes.getEditableItems();
			if (file.isDirectory())
			{
				try
				{
					monitor.subTask(MessageFormat.format(PHPDebugUIMessages.InstalledPHPsBlock_14, new Object[] {
							Integer.toString(found.size()), file.getCanonicalPath() }));
				}
				catch (final IOException e)
				{
				}
				if (!ignore.contains(file))
				{
					if (monitor.isCanceled())
						return;
					final File foundExe = findPHPExecutable(file);
					if (foundExe != null)
					{
						found.add(foundExe);
						ignore.add(file);
					}
					subDirs.add(file);
				}
			}
		}
		while (!subDirs.isEmpty())
		{
			final File subDir = subDirs.remove(0);
			search(subDir, found, ignore, monitor);
			if (monitor.isCanceled())
				return;
		}
	}

	/**
	 * Locate a PHP executable file in the PHP location given to this method. The location should be a directory. The
	 * search is done for php and php.exe only.
	 * 
	 * @param phpLocation
	 *            A directory that might hold a PHP executable.
	 * @return A PHP executable file.
	 */
	private static File findPHPExecutable(File phpLocation)
	{

		// Try each candidate in order. The first one found wins. Thus, the order
		// of fgCandidateJavaLocations is significant.
		for (String element : PHP_CANDIDATE_BIN)
		{
			File javaFile = new File(phpLocation, element);
			if (javaFile.exists() && !javaFile.isDirectory())
			{
				return javaFile;
			}
		}
		return null;
	}

	/**
	 * Sets the PHPs to be displayed in this block
	 * 
	 * @param phpExes
	 *            PHPs to be displayed
	 */
	protected void setPHPs(final PHPexeItem[] phpExes)
	{
		fPHPexes.clear();
		for (PHPexeItem element : phpExes)
		{
			fPHPexes.add(element);
		}
		fPHPExeList.setInput(fPHPexes);
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				fPHPExeList.refresh();
			}
		});
	}

	/**
	 * Sorts by VM location.
	 */
	private void sortByLocation()
	{
		fPHPExeList.setSorter(new ViewerSorter()
		{
			public int compare(final Viewer viewer, final Object e1, final Object e2)
			{
				if (e1 instanceof PHPexeItem && e2 instanceof PHPexeItem)
				{
					final PHPexeItem left = (PHPexeItem) e1;
					final PHPexeItem right = (PHPexeItem) e2;
					return left.getExecutable().getAbsolutePath()
							.compareToIgnoreCase(right.getExecutable().getAbsolutePath());
				}
				return super.compare(viewer, e1, e2);
			}

			public boolean isSorterProperty(final Object element, final String property)
			{
				return true;
			}
		});
		fSortColumn = 2;
	}

	/**
	 * Sorts by debugger type.
	 */
	private void sortByDebugger()
	{
		fPHPExeList.setSorter(new ViewerSorter()
		{
			public int compare(final Viewer viewer, final Object e1, final Object e2)
			{
				if (e1 instanceof PHPexeItem && e2 instanceof PHPexeItem)
				{
					final PHPexeItem left = (PHPexeItem) e1;
					final PHPexeItem right = (PHPexeItem) e2;
					String leftDebugger = PHPDebuggersRegistry.getDebuggerName(left.getDebuggerID());
					String rightDebugger = PHPDebuggersRegistry.getDebuggerName(right.getDebuggerID());
					return rightDebugger.compareToIgnoreCase(leftDebugger);
				}
				return super.compare(viewer, e1, e2);
			}

			public boolean isSorterProperty(final Object element, final String property)
			{
				return true;
			}
		});
		fSortColumn = 3;
	}

	/**
	 * Sorts by VM name.
	 */
	private void sortByName()
	{
		fPHPExeList.setSorter(new ViewerSorter()
		{
			public int compare(final Viewer viewer, final Object e1, final Object e2)
			{
				if (e1 instanceof PHPexeItem && e2 instanceof PHPexeItem)
				{
					final PHPexeItem left = (PHPexeItem) e1;
					final PHPexeItem right = (PHPexeItem) e2;
					return left.getName().compareToIgnoreCase(right.getName());
				}
				return super.compare(viewer, e1, e2);
			}

			public boolean isSorterProperty(final Object element, final String property)
			{
				return true;
			}
		});
		fSortColumn = 1;
	}

	/**
	 * Sorts by VM type, and name within type.
	 */
	private void sortByType()
	{
		fPHPExeList.setSorter(new ViewerSorter()
		{
			public int compare(final Viewer viewer, final Object e1, final Object e2)
			{
				if (e1 instanceof PHPexeItem && e2 instanceof PHPexeItem)
				{
					final PHPexeItem left = (PHPexeItem) e1;
					final PHPexeItem right = (PHPexeItem) e2;
					final String leftType = left.getName();
					final String rightType = right.getName();
					final int res = leftType.compareToIgnoreCase(rightType);
					if (res != 0)
						return res;
					return left.getName().compareToIgnoreCase(right.getName());
				}
				return super.compare(viewer, e1, e2);
			}

			public boolean isSorterProperty(final Object element, final String property)
			{
				return true;
			}
		});
		fSortColumn = 3;
	}

}
