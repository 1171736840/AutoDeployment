package cn.xuyanwu.autodeployment.window;

import cn.xuyanwu.autodeployment.AutoDeployment;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AutoDeploymentWindow {
    private final Project project;
    private final ConsoleView console;
    private AutoDeployment autoDeployment;
    private final JPanel rootPanel;

    public AutoDeploymentWindow(Project project) {
        this.project = project;
        rootPanel = new JPanel(new BorderLayout());
        console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        rootPanel.add(console.getComponent(),BorderLayout.CENTER);
        Toolbar toolbar = new Toolbar(project,console,this::deployment,this::closeAutoDeployment);
        rootPanel.add(toolbar.getPanel(),BorderLayout.NORTH);
    }

    /**
     * 开始自动部署
     */
    private void deployment(File file) {
        closeAutoDeployment();
        console.clear();
        autoDeployment = new AutoDeployment(project,file,str -> console.print(str,ConsoleViewContentType.NORMAL_OUTPUT));
        autoDeployment.deployment();
    }

    /**
     * 关闭 AutoDeployment （日志输出流）
     */
    private void closeAutoDeployment() {
        if (autoDeployment != null) {
            autoDeployment.close();
            console.print("自动部署已关闭！",ConsoleViewContentType.SYSTEM_OUTPUT);
            autoDeployment = null;
        }
    }

    public JPanel getComponent() {
        return rootPanel;
    }
}
