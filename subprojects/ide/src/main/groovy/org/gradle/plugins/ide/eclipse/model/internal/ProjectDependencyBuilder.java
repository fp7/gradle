/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.plugins.ide.eclipse.model.internal;

import org.gradle.api.Project;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.ProjectDependency;
import org.gradle.util.DeprecationLogger;

public class ProjectDependencyBuilder {
    public ProjectDependency build(Project project, final String declaredConfigurationName) {
        return buildProjectDependency(determineProjectName(project), project.getPath(), declaredConfigurationName);
    }

    private String determineProjectName(Project project) {
        String name;
        if (project.getPlugins().hasPlugin(EclipsePlugin.class)) {
            name = project.getExtensions().getByType(EclipseModel.class).getProject().getName();
        } else {
            name = project.getName();
        }
        return name;
    }

    private ProjectDependency buildProjectDependency(String name, String projectPath, final String declaredConfigurationName) {
        final ProjectDependency out = new ProjectDependency("/" + name, projectPath);
        out.setExported(false);
        DeprecationLogger.whileDisabled(new Runnable() {
            @Override
            public void run() {
                out.setDeclaredConfigurationName(declaredConfigurationName);
            }

        });
        return out;
    }
}
