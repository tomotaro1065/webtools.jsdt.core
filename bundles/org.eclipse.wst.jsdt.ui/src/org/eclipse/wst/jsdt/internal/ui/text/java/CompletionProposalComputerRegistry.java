/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * A registry for all extensions to the
 * <code>org.eclipse.wst.jsdt.ui.javaCompletionProposalComputer</code>
 * extension point.
 * 
 * @since 3.2
 */
public final class CompletionProposalComputerRegistry {

	private static final String EXTENSION_POINT= "javaCompletionProposalComputer"; //$NON-NLS-1$
	
	/** The singleton instance. */
	private static CompletionProposalComputerRegistry fgSingleton= null;
	
	/**
	 * Returns the default computer registry.
	 * <p>
	 * TODO keep this or add some other singleton, e.g. JavaScriptPlugin?
	 * </p>
	 * 
	 * @return the singleton instance
	 */
	public static synchronized CompletionProposalComputerRegistry getDefault() {
		if (fgSingleton == null) {
			fgSingleton= new CompletionProposalComputerRegistry();
		}
		
		return fgSingleton;
	}
	
	/**
	 * The sets of descriptors, grouped by partition type (key type:
	 * {@link String}, value type:
	 * {@linkplain List List&lt;CompletionProposalComputerDescriptor&gt;}).
	 */
	private final Map fDescriptorsByPartition= new HashMap();
	/**
	 * Unmodifiable versions of the sets stored in
	 * <code>fDescriptorsByPartition</code> (key type: {@link String},
	 * value type:
	 * {@linkplain List List&lt;CompletionProposalComputerDescriptor&gt;}).
	 */
	private final Map fPublicDescriptorsByPartition= new HashMap();
	/**
	 * All descriptors (element type:
	 * {@link CompletionProposalComputerDescriptor}).
	 */
	private final List fDescriptors= new ArrayList();
	/**
	 * Unmodifiable view of <code>fDescriptors</code>
	 */
	private final List fPublicDescriptors= Collections.unmodifiableList(fDescriptors);
	
	private final List fCategories= new ArrayList();
	private final List fPublicCategories= Collections.unmodifiableList(fCategories);
	/**
	 * <code>true</code> if this registry has been loaded.
	 */
	private boolean fLoaded= false;

	/**
	 * Creates a new instance.
	 */
	public CompletionProposalComputerRegistry() {
	}

	/**
	 * Returns the list of {@link CompletionProposalComputerDescriptor}s describing all extensions
	 * to the <code>javaCompletionProposalComputer</code> extension point for the given partition
	 * type.
	 * <p>
	 * A valid partition is either one of the constants defined in
	 * {@link org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions} or
	 * {@link org.eclipse.jface.text.IDocument#DEFAULT_CONTENT_TYPE}. An empty list is returned if
	 * there are no extensions for the given partition.
	 * </p>
	 * <p>
	 * The returned list is read-only and is sorted in the order that the extensions were read in.
	 * There are no duplicate elements in the returned list. The returned list may change if plug-ins
	 * are loaded or unloaded while the application is running or if an extension violates the API
	 * contract of {@link org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer}. When
	 * computing proposals, it is therefore imperative to copy the returned list before iterating
	 * over it.
	 * </p>
	 * 
	 * @param partition
	 *        the partition type for which to retrieve the computer descriptors
	 * @return the list of extensions to the <code>javaCompletionProposalComputer</code> extension
	 *         point (element type: {@link CompletionProposalComputerDescriptor})
	 */
	List getProposalComputerDescriptors(String partition) {
		ensureExtensionPointRead();
		List result= (List) fPublicDescriptorsByPartition.get(partition);
		return result != null ? result : Collections.EMPTY_LIST;
	}

	/**
	 * Returns the list of {@link CompletionProposalComputerDescriptor}s describing all extensions
	 * to the <code>javaCompletionProposalComputer</code> extension point.
	 * <p>
	 * The returned list is read-only and is sorted in the order that the extensions were read in.
	 * There are no duplicate elements in the returned list. The returned list may change if plug-ins
	 * are loaded or unloaded while the application is running or if an extension violates the API
	 * contract of {@link org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer}. When
	 * computing proposals, it is therefore imperative to copy the returned list before iterating
	 * over it.
	 * </p>
	 * 
	 * @return the list of extensions to the <code>javaCompletionProposalComputer</code> extension
	 *         point (element type: {@link CompletionProposalComputerDescriptor})
	 */
	List getProposalComputerDescriptors() {
		ensureExtensionPointRead();
		return fPublicDescriptors;
	}
	
	/**
	 * Returns the list of proposal categories contributed to the
	 * <code>javaCompletionProposalComputer</code> extension point.
	 * <p>
	 * <p>
	 * The returned list is read-only and is sorted in the order that the extensions were read in.
	 * There are no duplicate elements in the returned list. The returned list may change if
	 * plug-ins are loaded or unloaded while the application is running.
	 * </p>
	 * 
	 * @return list of proposal categories contributed to the
	 *         <code>javaCompletionProposalComputer</code> extension point (element type:
	 *         {@link CompletionProposalCategory})
	 */
	public List getProposalCategories() {
		ensureExtensionPointRead();
		return fPublicCategories;
	}

	/**
	 * Ensures that the extensions are read and stored in
	 * <code>fDescriptorsByPartition</code>.
	 */
	private void ensureExtensionPointRead() {
		boolean reload;
		synchronized (this) {
			reload= !fLoaded;
			fLoaded= true;
		}
		if (reload)
			reload();
	}

	/**
	 * Reloads the extensions to the extension point.
	 * <p>
	 * This method can be called more than once in order to reload from
	 * a changed extension registry.
	 * </p>
	 */
	public void reload() {
		IExtensionRegistry registry= Platform.getExtensionRegistry();
		List elements= new ArrayList(Arrays.asList(registry.getConfigurationElementsFor(JavaScriptPlugin.getPluginId(), EXTENSION_POINT)));
		
		Map map= new HashMap();
		List all= new ArrayList();
		
		List categories= getCategories(elements);
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			IConfigurationElement element= (IConfigurationElement) iter.next();
			try {
				CompletionProposalComputerDescriptor desc= new CompletionProposalComputerDescriptor(element, this, categories);
				Set partitions= desc.getPartitions();
				for (Iterator it= partitions.iterator(); it.hasNext();) {
					String partition= (String) it.next();
					List list= (List) map.get(partition);
					if (list == null) {
						list= new ArrayList();
						map.put(partition, list);
					}
					list.add(desc);
				}
				all.add(desc);
				
			} catch (InvalidRegistryObjectException x) {
				/*
				 * Element is not valid any longer as the contributing plug-in was unloaded or for
				 * some other reason. Do not include the extension in the list and inform the user
				 * about it.
				 */
				Object[] args= {element.toString()};
				String message= Messages.format(JavaTextMessages.CompletionProposalComputerRegistry_invalid_message, args);
				IStatus status= new Status(IStatus.WARNING, JavaScriptPlugin.getPluginId(), IStatus.OK, message, x);
				informUser(status);
			}
		}
		
		synchronized (this) {
			fCategories.clear();
			fCategories.addAll(categories);
			
			Set partitions= map.keySet();
			fDescriptorsByPartition.keySet().retainAll(partitions);
			fPublicDescriptorsByPartition.keySet().retainAll(partitions);
			for (Iterator it= partitions.iterator(); it.hasNext();) {
				String partition= (String) it.next();
				List old= (List) fDescriptorsByPartition.get(partition);
				List current= (List) map.get(partition);
				if (old != null) {
					old.clear();
					old.addAll(current);
				} else {
					fDescriptorsByPartition.put(partition, current);
					fPublicDescriptorsByPartition.put(partition, Collections.unmodifiableList(current));
				}
			}
			
			fDescriptors.clear();
			fDescriptors.addAll(all);
		}
	}

	private List getCategories(List elements) {
		IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
		String preference= store.getString(PreferenceConstants.CODEASSIST_EXCLUDED_CATEGORIES);
		Set disabled= new HashSet();
		StringTokenizer tok= new StringTokenizer(preference, "\0");  //$NON-NLS-1$
		while (tok.hasMoreTokens())
			disabled.add(tok.nextToken());
		Map ordered= new HashMap();
		preference= store.getString(PreferenceConstants.CODEASSIST_CATEGORY_ORDER);
		tok= new StringTokenizer(preference, "\0"); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			StringTokenizer inner= new StringTokenizer(tok.nextToken(), ":"); //$NON-NLS-1$
			String id= inner.nextToken();
			int rank= Integer.parseInt(inner.nextToken());
			ordered.put(id, new Integer(rank));
		}
		
		List categories= new ArrayList();
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			IConfigurationElement element= (IConfigurationElement) iter.next();
			try {
				if (element.getName().equals("proposalCategory")) { //$NON-NLS-1$
					iter.remove(); // remove from list to leave only computers
					
					CompletionProposalCategory category= new CompletionProposalCategory(element, this);
					categories.add(category);
					category.setIncluded(!disabled.contains(category.getId()));
					Integer rank= (Integer) ordered.get(category.getId());
					if (rank != null) {
						int r= rank.intValue();
						boolean separate= r < 0xffff;
						category.setSeparateCommand(separate);
						category.setSortOrder(r);
					}
				}
			} catch (InvalidRegistryObjectException x) {
				/*
				 * Element is not valid any longer as the contributing plug-in was unloaded or for
				 * some other reason. Do not include the extension in the list and inform the user
				 * about it.
				 */
				Object[] args= {element.toString()};
				String message= Messages.format(JavaTextMessages.CompletionProposalComputerRegistry_invalid_message, args);
				IStatus status= new Status(IStatus.WARNING, JavaScriptPlugin.getPluginId(), IStatus.OK, message, x);
				informUser(status);
			}
		}
		return categories;
	}

	/**
	 * Log the status and inform the user about a misbehaving extension.
	 * 
	 * @param descriptor the descriptor of the misbehaving extension
	 * @param status a status object that will be logged
	 */
	void informUser(CompletionProposalComputerDescriptor descriptor, IStatus status) {
		JavaScriptPlugin.log(status);
        String title= JavaTextMessages.CompletionProposalComputerRegistry_error_dialog_title;
        CompletionProposalCategory category= descriptor.getCategory();
        IContributor culprit= descriptor.getContributor();
        Set affectedPlugins= getAffectedContributors(category, culprit);
        
		final String avoidHint;
		final String culpritName= culprit == null ? null : culprit.getName();
		if (affectedPlugins.isEmpty())
			avoidHint= Messages.format(JavaTextMessages.CompletionProposalComputerRegistry_messageAvoidanceHint, new Object[] {culpritName, category.getDisplayName()});
		else
			avoidHint= Messages.format(JavaTextMessages.CompletionProposalComputerRegistry_messageAvoidanceHintWithWarning, new Object[] {culpritName, category.getDisplayName(), toString(affectedPlugins)});
        
		String message= status.getMessage();
        // inlined from MessageDialog.openError
        MessageDialog dialog = new MessageDialog(JavaScriptPlugin.getActiveWorkbenchShell(), title, null /* default image */, message, MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0) {
        	protected Control createCustomArea(Composite parent) {
        		Link link= new Link(parent, SWT.NONE);
        		link.setText(avoidHint);
        		link.addSelectionListener(new SelectionAdapter() {
        			public void widgetSelected(SelectionEvent e) {
        				PreferencesUtil.createPreferenceDialogOn(getShell(), "org.eclipse.wst.jsdt.ui.preferences.CodeAssistPreferenceAdvanced", null, null).open(); //$NON-NLS-1$
        			}
        		});
        		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        		gridData.widthHint= this.getMinimumMessageWidth();
				link.setLayoutData(gridData);
        		return link;
        	}
        };
        dialog.open();
	}

	/**
	 * Returns the names of contributors affected by disabling a category. 
	 * 
	 * @param category the category that would be disabled
	 * @param culprit the culprit plug-in, which is not included in the returned list
	 * @return the names of the contributors other than <code>culprit</code> that contribute to <code>category</code> (element type: {@link String})
	 */
	private Set getAffectedContributors(CompletionProposalCategory category, IContributor culprit) {
	    Set affectedPlugins= new HashSet();
        for (Iterator it= getProposalComputerDescriptors().iterator(); it.hasNext();) {
	        CompletionProposalComputerDescriptor desc= (CompletionProposalComputerDescriptor) it.next();
	        CompletionProposalCategory cat= desc.getCategory();
	        if (cat.equals(category)) {
	        	IContributor contributor= desc.getContributor();
	        	if (contributor != null && !culprit.equals(contributor))
	        		affectedPlugins.add(contributor.getName());
	        }
        }
	    return affectedPlugins;
    }

    private Object toString(Collection collection) {
    	// strip brackets off AbstractCollection.toString()
    	String string= collection.toString();
    	return string.substring(1, string.length() - 1);
    }

	private void informUser(IStatus status) {
		JavaScriptPlugin.log(status);
		String title= JavaTextMessages.CompletionProposalComputerRegistry_error_dialog_title;
		String message= status.getMessage();
		MessageDialog.openError(JavaScriptPlugin.getActiveWorkbenchShell(), title, message);
	}
}
