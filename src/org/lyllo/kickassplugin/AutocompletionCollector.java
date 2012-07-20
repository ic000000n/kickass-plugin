package org.lyllo.kickassplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.lyllo.kickassplugin.editor.TreeObject;

public class AutocompletionCollector implements IResourceChangeListener, IResourceDeltaVisitor, IResourceVisitor{

	public void resourceChanged(IResourceChangeEvent event) {
		IResource resource = event.getResource();

		switch (event.getType()){
		case IResourceChangeEvent.PRE_DELETE:
			break;
		case IResourceChangeEvent.POST_CHANGE:
		case IResourceChangeEvent.POST_BUILD:
		case IResourceChangeEvent.PRE_REFRESH:
			try {
				event.getDelta().accept(this);
			} catch (CoreException ex){
				//error
				ex.printStackTrace();
			}
			break;
		}
	}


	public boolean visit(IResourceDelta delta) throws CoreException {

		if (delta.getResource() == null || delta.getResource().getProject() == null){
			return true;
		}

		IResource resource = delta.getResource();
		
		if (delta.getKind() == IResourceDelta.REMOVED){
			return true;
		}  

		return visit(resource);

	}


	private void scanfile(final IFile file, final String project) throws CoreException {
		
		WorkspaceJob scanFileJob = 
				new WorkspaceJob("Scanning file " + file.getName()) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {


				if (monitor == null){
					monitor = new NullProgressMonitor();
				}

				BufferedReader reader = null;
				List<String> labels = new ArrayList<String>();
				List<String> macros = new ArrayList<String>();
				Set<String> imports = new HashSet<String>();
				
				try {
					reader = new BufferedReader(new InputStreamReader(file.getContents(true)));
					String line = null;
					while ( (line = reader.readLine()) != null){
						if (line.indexOf(":") > -1 || line.indexOf(".label") > -1){
							Matcher matcher = Constants.LABEL_PATTERN.matcher(line);
							if (matcher.matches()){
								labels.add(matcher.group(1).replaceAll("\\s*=\\s*\\S+", ""));
							} else {
								matcher =  Constants.LABEL_PATTERN_ALT.matcher(line);
								if (matcher.find()){
									labels.add(matcher.group(1));
								}
							}
						}
						
						if (line.toLowerCase().indexOf(".macro") > -1) {
							Pattern pattern = Constants.MACRO_PATTERN;
							Matcher matcher = pattern.matcher(line);

							if (matcher.find()) {
								macros.add(":"+matcher.group(1));
							}
						}
						
						if (line.toLowerCase().indexOf(".import") > -1) {
							Pattern pattern = Constants.IMPORT_SOURCE_PATTERN;
							Matcher matcher = pattern.matcher(line);
							
							if (matcher.matches()) {
								IPath importPath = file.getParent().getProjectRelativePath().append(matcher.group(1));
								imports.add(importPath.toString());
							}
						}

					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (reader != null){
						try {
							reader.close();
						} catch (IOException e) {
						}
					}
				}
				Collections.sort(labels);
				file.setSessionProperty(Constants.LABELS_SESSION_KEY, labels);

				Collections.sort(macros);
				file.setSessionProperty(Constants.MACROS_SESSION_KEY, macros);

				file.setSessionProperty(Constants.IMPORTS_SESSION_KEY, imports);
				
				return Status.OK_STATUS;
			}
		};

		scanFileJob.schedule();
	}

	public void init() {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		try {
			workspace.getRoot().accept(this);
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	public boolean visit(IResource resource) throws CoreException {

		if (resource.getProject() == null){
			return true;
		}
		
		if (!resource.getProject().isAccessible() || !resource.getProject().hasNature(Constants.NATURE_ID)){
			return false;
		}

		String project = resource.getProject().getName();

		if (resource.getType() != IResource.FILE)
			return true;

		IFile file = (IFile) resource;
		
		String ext = file.getFileExtension();
		if (ext != null && Constants.EXTENSION_PATTERN_ALL.matcher(ext).matches())
			scanfile(file, project);

		return true;
	}

}