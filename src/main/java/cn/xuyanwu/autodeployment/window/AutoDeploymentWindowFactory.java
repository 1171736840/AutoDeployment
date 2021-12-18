package cn.xuyanwu.autodeployment.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class AutoDeploymentWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project,@NotNull ToolWindow toolWindow) {
        AutoDeploymentWindow window = new AutoDeploymentWindow(project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(window.getComponent(),"",false);
        toolWindow.getContentManager().addContent(content);
    }
}
