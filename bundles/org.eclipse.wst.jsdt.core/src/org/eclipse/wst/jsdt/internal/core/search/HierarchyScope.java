/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.hierarchy.TypeHierarchy;

/**
 * Scope limited to the subtype and supertype hierarchy of a given type.
 */
public class HierarchyScope extends AbstractSearchScope implements SuffixConstants {

	public IType focusType;
	private String focusPath;
	private WorkingCopyOwner owner;

	private ITypeHierarchy hierarchy;
	private IType[] types;
	private HashSet resourcePaths;
	private IPath[] enclosingProjectsAndJars;

	protected IResource[] elements;
	protected int elementCount;

	public boolean needsRefresh;

	/* (non-Javadoc)
	 * Adds the given resource to this search scope.
	 */
	public void add(IResource element) {
		if (this.elementCount == this.elements.length) {
			System.arraycopy(
				this.elements,
				0,
				this.elements = new IResource[this.elementCount * 2],
				0,
				this.elementCount);
		}
		elements[elementCount++] = element;
	}

	/* (non-Javadoc)
	 * Creates a new hiearchy scope for the given type.
	 */
	public HierarchyScope(IType type, WorkingCopyOwner owner) throws JavaScriptModelException {
		this.focusType = type;
		this.owner = owner;

		this.enclosingProjectsAndJars = this.computeProjectsAndJars(type);

		// resource path
		IPackageFragmentRoot root = (IPackageFragmentRoot)type.getPackageFragment().getParent();
//		if (root.isArchive()) {
//			IPath jarPath = root.getPath();
//			Object target = JavaModel.getTarget(ResourcesPlugin.getWorkspace().getRoot(), jarPath, true);
//			String zipFileName;
//			if (target instanceof IFile) {
//				// internal jar
//				zipFileName = jarPath.toString();
//			} else if (target instanceof File) {
//				// external jar
//				zipFileName = ((File)target).getPath();
//			} else {
//				return; // unknown target
//			}
//			this.focusPath =
//				zipFileName
//					+ JAR_FILE_ENTRY_SEPARATOR
//					+ type.getFullyQualifiedName().replace('.', '/')
//					+ SUFFIX_STRING_class;
//		}
//		else 
		{
			this.focusPath = type.getPath().toString();
		}

		this.needsRefresh = true;

		//disabled for now as this could be expensive
		//JavaModelManager.getJavaModelManager().rememberScope(this);
	}
	private void buildResourceVector() {
		HashMap resources = new HashMap();
		HashMap paths = new HashMap();
		this.types = this.hierarchy.getAllTypes();
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		for (int i = 0; i < this.types.length; i++) {
			IType type = this.types[i];
			IResource resource = type.getResource();
			if (resource != null && resources.get(resource) == null) {
				resources.put(resource, resource);
				add(resource);
			}
			IPackageFragmentRoot root =
				(IPackageFragmentRoot) type.getPackageFragment().getParent();
//			if (root instanceof JarPackageFragmentRoot) {
//				// type in a jar
//				JarPackageFragmentRoot jar = (JarPackageFragmentRoot) root;
//				IPath jarPath = jar.getPath();
//				Object target = JavaModel.getTarget(workspaceRoot, jarPath, true);
//				String zipFileName;
//				if (target instanceof IFile) {
//					// internal jar
//					zipFileName = jarPath.toString();
//				} else if (target instanceof File) {
//					// external jar
//					zipFileName = ((File)target).getPath();
//				} else {
//					continue; // unknown target
//				}
//				String resourcePath =
//					zipFileName
//						+ JAR_FILE_ENTRY_SEPARATOR
//						+ type.getFullyQualifiedName().replace('.', '/')
//						+ SUFFIX_STRING_class;
//
//				this.resourcePaths.add(resourcePath);
//				paths.put(jarPath, type);
//			} else 
			{
				// type is a project
				paths.put(type.getJavaProject().getProject().getFullPath(), type);
			}
		}
		this.enclosingProjectsAndJars = new IPath[paths.size()];
		int i = 0;
		for (Iterator iter = paths.keySet().iterator(); iter.hasNext();) {
			this.enclosingProjectsAndJars[i++] = (IPath) iter.next();
		}
	}
	/*
	 * Computes the paths of projects and jars that the hierarchy on the given type could contain.
	 * This is a super set of the project and jar paths once the hierarchy is computed.
	 */
	private IPath[] computeProjectsAndJars(IType type) throws JavaScriptModelException {
		HashSet set = new HashSet();
		IPackageFragmentRoot root = (IPackageFragmentRoot)type.getPackageFragment().getParent();
		if (root.isArchive()) {
			// add the root
			set.add(root.getPath());
			// add all projects that reference this archive and their dependents
			IPath rootPath = root.getPath();
			IJavaScriptModel model = JavaModelManager.getJavaModelManager().getJavaModel();
			IJavaScriptProject[] projects = model.getJavaProjects();
			HashSet visited = new HashSet();
			for (int i = 0; i < projects.length; i++) {
				JavaProject project = (JavaProject) projects[i];
				IIncludePathEntry entry = project.getClasspathEntryFor(rootPath);
				if (entry != null) {
					// add the project and its binary pkg fragment roots
					IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
					set.add(project.getPath());
					for (int k = 0; k < roots.length; k++) {
						IPackageFragmentRoot pkgFragmentRoot = roots[k];
						if (pkgFragmentRoot.getKind() == IPackageFragmentRoot.K_BINARY) {
							set.add(pkgFragmentRoot.getPath());
						}
					}
					// add the dependent projects
					this.computeDependents(project, set, visited);
				}
			}
		} else {
			// add all the project's pkg fragment roots
			IJavaScriptProject project = (IJavaScriptProject)root.getParent();
			IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot pkgFragmentRoot = roots[i];
				if (pkgFragmentRoot.getKind() == IPackageFragmentRoot.K_BINARY) {
					set.add(pkgFragmentRoot.getPath());
				} else {
					set.add(pkgFragmentRoot.getParent().getPath());
				}
			}
			// add the dependent projects
			this.computeDependents(project, set, new HashSet());
		}
		IPath[] result = new IPath[set.size()];
		set.toArray(result);
		return result;
	}
	private void computeDependents(IJavaScriptProject project, HashSet set, HashSet visited) {
		if (visited.contains(project)) return;
		visited.add(project);
		IProject[] dependents = project.getProject().getReferencingProjects();
		for (int i = 0; i < dependents.length; i++) {
			try {
				IJavaScriptProject dependent = JavaScriptCore.create(dependents[i]);
				IPackageFragmentRoot[] roots = dependent.getPackageFragmentRoots();
				set.add(dependent.getPath());
				for (int j = 0; j < roots.length; j++) {
					IPackageFragmentRoot pkgFragmentRoot = roots[j];
					if (pkgFragmentRoot.isArchive()) {
						set.add(pkgFragmentRoot.getPath());
					}
				}
				this.computeDependents(dependent, set, visited);
			} catch (JavaScriptModelException e) {
				// project is not a java project
			}
		}
	}
	/* (non-Javadoc)
	 * @see IJavaScriptSearchScope#encloses(String)
	 */
	public boolean encloses(String resourcePath) {
		if (this.hierarchy == null) {
			if (resourcePath.equals(this.focusPath)) {
				return true;
			} else {
				if (this.needsRefresh) {
					try {
						this.initialize();
					} catch (JavaScriptModelException e) {
						return false;
					}
				} else {
					// the scope is used only to find enclosing projects and jars
					// clients is responsible for filtering out elements not in the hierarchy (see SearchEngine)
					return true;
				}
			}
		}
		if (this.needsRefresh) {
			try {
				this.refresh();
			} catch(JavaScriptModelException e) {
				return false;
			}
		}
		int separatorIndex = resourcePath.indexOf(JAR_FILE_ENTRY_SEPARATOR);
		if (separatorIndex != -1) {
			return this.resourcePaths.contains(resourcePath);
		} else {
			for (int i = 0; i < this.elementCount; i++) {
				if (resourcePath.startsWith(this.elements[i].getFullPath().toString())) {
					return true;
				}
			}
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see IJavaScriptSearchScope#encloses(IJavaScriptElement)
	 */
	public boolean encloses(IJavaScriptElement element) {
		if (this.hierarchy == null) {
			if (this.focusType.equals(element.getAncestor(IJavaScriptElement.TYPE))) {
				return true;
			} else {
				if (this.needsRefresh) {
					try {
						this.initialize();
					} catch (JavaScriptModelException e) {
						return false;
					}
				} else {
					// the scope is used only to find enclosing projects and jars
					// clients is responsible for filtering out elements not in the hierarchy (see SearchEngine)
					return true;
				}
			}
		}
		if (this.needsRefresh) {
			try {
				this.refresh();
			} catch(JavaScriptModelException e) {
				return false;
			}
		}
		IType type = null;
		if (element instanceof IType) {
			type = (IType) element;
		} else if (element instanceof IMember) {
			type = ((IMember) element).getDeclaringType();
		}
		if (type != null) {
			if (this.hierarchy.contains(type)) {
				return true;
			} else {
				// be flexible: look at original element (see bug 14106 Declarations in Hierarchy does not find declarations in hierarchy)
				IType original;
				if (!type.isBinary()
						&& (original = (IType)type.getPrimaryElement()) != null) {
					return this.hierarchy.contains(original);
				}
			}
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see IJavaScriptSearchScope#enclosingProjectsAndJars()
	 * @deprecated
	 */
	public IPath[] enclosingProjectsAndJars() {
		if (this.needsRefresh) {
			try {
				this.refresh();
			} catch(JavaScriptModelException e) {
				return new IPath[0];
			}
		}
		return this.enclosingProjectsAndJars;
	}
	protected void initialize() throws JavaScriptModelException {
		this.resourcePaths = new HashSet();
		this.elements = new IResource[5];
		this.elementCount = 0;
		this.needsRefresh = false;
		if (this.hierarchy == null) {
			this.hierarchy = this.focusType.newTypeHierarchy(this.owner, null);
		} else {
			this.hierarchy.refresh(null);
		}
		this.buildResourceVector();
	}
	/*
	 * @see AbstractSearchScope#processDelta(IJavaScriptElementDelta)
	 */
	public void processDelta(IJavaScriptElementDelta delta) {
		if (this.needsRefresh) return;
		this.needsRefresh = this.hierarchy == null ? false : ((TypeHierarchy)this.hierarchy).isAffected(delta);
	}
	protected void refresh() throws JavaScriptModelException {
		if (this.hierarchy != null) {
			this.initialize();
		}
	}
	public String toString() {
		return "HierarchyScope on " + ((JavaElement)this.focusType).toStringWithAncestors(); //$NON-NLS-1$
	}

}
