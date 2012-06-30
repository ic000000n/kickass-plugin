package org.lyllo.kickassplugin.prefs;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.lyllo.kickassplugin.Constants;

public class ProjectPrefenceHelper {

	public static ScopedPreferenceStore getStore(IProject project){
		
		ProjectScope projectScope = new ProjectScope(project); 
				
		ScopedPreferenceStore store = new ScopedPreferenceStore(projectScope, "org.lyllo.kickassplugin");
		store.setDefault(Constants.PROJECT_PREFS_SRC_DIRECTORY_KEY, "src");
		store.setDefault(Constants.PROJECT_PREFS_BUILD_DIRECTORY_KEY, "build");
		store.setDefault(Constants.PROJECT_PREFS_LIBDIR_DIRECTORY_KEY, "");
		return store;
	}
	
	public static String getBuildDir(IProject project){
		return getStore(project).getString(Constants.PROJECT_PREFS_BUILD_DIRECTORY_KEY);
	}
	
	public static String[] getSourceDirs(IProject project){
		return getStore(project).getString(Constants.PROJECT_PREFS_SRC_DIRECTORY_KEY).split(File.pathSeparator);
	}
	
	public static String[] getLibDirs(IProject project){
		return getStore(project).getString(Constants.PROJECT_PREFS_LIBDIR_DIRECTORY_KEY).split(File.pathSeparator);
	}
}
