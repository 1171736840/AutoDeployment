package cn.xuyanwu.autodeployment.window;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Toolbar {
    private final Project project;
    private final ConsoleView console;
    private final JPanel panel;
    private ActionToolbar actionToolbar;
    private JComboBox<String> configComboBox;

    public Toolbar(Project project,ConsoleView console,Consumer<File> run,Runnable stop) {
        this.project = project;
        this.console = console;
        panel = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        createActionToolbar(console,run,stop);
        createConfigComboBox();
        panel.add(configComboBox,BorderLayout.EAST);
        panel.add(actionToolbar.getComponent(),BorderLayout.WEST);
    }

    /**
     * 创建基本的工具条
     */
    private void createActionToolbar(ConsoleView console,Consumer<File> run,Runnable stop) {
        DefaultActionGroup actions = new DefaultActionGroup();
        actions.addAction(new RunAction(run,this::getConfig));
        actions.addAction(new StopAction(stop));

        actions.addSeparator();

        AnAction[] consoleActions = console.createConsoleActions();
        actions.addAll(consoleActions);
        actionToolbar = ActionManager.getInstance().createActionToolbar("AutoDeploymentToolbar",actions,true);
    }

    /**
     * 创建配置文件下拉列表框
     */
    private void createConfigComboBox() {
        configComboBox = new JComboBox<>();
        refreshConfigComboBox();
        configComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                refreshConfigComboBox();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
            }
        });
    }

    /**
     * 刷新配置文件下拉列表框
     */
    public void refreshConfigComboBox() {
        String selectedItem = (String) configComboBox.getSelectedItem();
        configComboBox.removeAllItems();
        String path = project.getBasePath() + "/autoDeployment";
        if(!FileUtil.exist(path)){
            console.print("没有扫描到部署脚本，请在项目的 autoDeployment 目录中创建部署脚本\n",ConsoleViewContentType.ERROR_OUTPUT);
            return;
        }
        List<String> list = FileUtil.listFileNames(path).stream()
                .filter(f -> {
                    String extName = FileNameUtil.extName(f);
                    return "sh".equals(extName) || "json".equals(extName);
                })
                .collect(Collectors.toList());

        list.forEach(configComboBox::addItem);
        if (selectedItem != null && list.contains(selectedItem)) {
            configComboBox.setSelectedItem(selectedItem);
        }
    }

    /**
     * 刷新配置文件下拉列表框
     */
    public File getConfig() {
        String item = (String) configComboBox.getSelectedItem();
        return item == null ? null : new File(project.getBasePath() + "/autoDeployment",item);
    }

    public Component getPanel() {
        return panel;
    }

    /**
     * 开始部署按钮
     */
    public static class RunAction extends AnAction implements DumbAware {
        private final Consumer<File> callback;
        private final Supplier<File> fileSupplier;

        public RunAction(Consumer<File> callback,Supplier<File> fileSupplier) {
            super("部署","开始部署",AllIcons.Actions.Execute);
            this.callback = callback;
            this.fileSupplier = fileSupplier;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            callback.accept(fileSupplier.get());
        }
    }

    /**
     * 停止部署按钮
     */
    public static class StopAction extends AnAction implements DumbAware {
        private final Runnable runnable;

        public StopAction(Runnable runnable) {
            super("停止部署","停止部署",AllIcons.Actions.Suspend);
            this.runnable = runnable;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
//            e.getPresentation().setIcon();
            runnable.run();
        }
    }

}
