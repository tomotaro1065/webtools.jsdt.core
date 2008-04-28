/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;

public interface ISourceConstraintVariable {

	public IJavaScriptUnit getCompilationUnit();

	public Object getData(String name);

	public ITypeSet getTypeEstimate();

	public void setCompilationUnit(IJavaScriptUnit cu);

	public void setData(String name, Object data);
}