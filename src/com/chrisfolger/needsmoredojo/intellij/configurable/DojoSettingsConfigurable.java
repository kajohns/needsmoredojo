package com.chrisfolger.needsmoredojo.intellij.configurable;

import com.chrisfolger.needsmoredojo.core.amd.SourcesAutoDetector;
import com.chrisfolger.needsmoredojo.core.settings.DojoSettings;
import com.chrisfolger.needsmoredojo.core.util.AMDUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

public class DojoSettingsConfigurable implements Configurable {
    private JComponent myComponent;
    private JButton autoDetectDojoSources;
    private JPanel myPanel;
    private TextFieldWithBrowseButton projectSourcesText;
    private TextFieldWithBrowseButton dojoSourcesText;
    private JTable moduleExceptionsTable;
    private JButton addMapping;
    private JTextField addModuleText;
    private JTextField addParameterText;
    private JButton removeMapping;
    private JCheckBox preferRelativePathsWhenCheckBox;
    private JButton autoDetectProjectSources;
    private Project project;
    private String dojoSourceString;
    private String projectSourceString;
    private DojoSettings settingsService;

    public String getDisplayName() {
        return "Needs More Dojo";
    }

    public boolean isModified() {
        return true;
    }

    private class ProjectSourcesChosen extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>
    {
        public ProjectSourcesChosen(@Nullable String title, @Nullable String description, FileChooserDescriptor fileChooserDescriptor) {
            super(title, description, projectSourcesText, null, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);

            projectSourceString = projectSourcesText.getText().replace('\\', '/');;
            projectSourcesText.setText(projectSourceString);
        }
    }

    private class AutoDetectDojoSources implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            autoDetectDojoSources.setEnabled(false);
            VirtualFile directory = AMDUtil.getDojoSourcesDirectory(project, false);
            dojoSourcesText.setText(directory.getCanonicalPath());
            dojoSourceString = directory.getCanonicalPath();
            autoDetectDojoSources.setEnabled(true);
        }
    }

    private class DojoSourcesChosen extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>
    {
        public DojoSourcesChosen(@Nullable String title, @Nullable String description, FileChooserDescriptor fileChooserDescriptor) {
            super(title, description, dojoSourcesText, null, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);

            dojoSourceString = dojoSourcesText.getText().replace('\\', '/');
            dojoSourcesText.setText(dojoSourceString);
        }
    }

    public JComponent createComponent() {
        myComponent = (JComponent) myPanel;
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();

        projectSourcesText.addBrowseFolderListener(null, new ProjectSourcesChosen("Project Sources", "Select the root of your project's sources to support certain features of Needs More Dojo", descriptor));
        dojoSourcesText.addBrowseFolderListener(null, new DojoSourcesChosen("Dojo Sources", "Select the root of the dojo library sources to support certain features of Needs More Dojo", descriptor));

        autoDetectDojoSources.addActionListener(new AutoDetectDojoSources());

        // don't know how else to get the current project???
        DataContext context = DataManager.getInstance().getDataContext();
        final Project project = DataKeys.PROJECT.getData(context);
        this.project = project;

        settingsService = ServiceManager.getService(project, DojoSettings.class);

        dojoSourceString = settingsService.getDojoSourcesDirectory();
        dojoSourcesText.setText(dojoSourceString);

        projectSourceString =settingsService.getProjectSourcesDirectory();
        projectSourcesText.setText(projectSourceString);

        final ExceptionsTableBuilder builder = new ExceptionsTableBuilder(moduleExceptionsTable, project);

        addMapping.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                builder.getTableModel().addRow(new String[] { addModuleText.getText(), addParameterText.getText()});
            }
        });

        removeMapping.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                builder.getTableModel().removeRow(moduleExceptionsTable.getSelectedRow());
            }
        });

        autoDetectProjectSources.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoDetectProjectSources.setEnabled(false);
                addAutoDetectedSource(new SourcesAutoDetector().getPossibleSourceRoots(project));
                autoDetectProjectSources.setEnabled(true);
            }
        });

        preferRelativePathsWhenCheckBox.setSelected(settingsService.isPreferRelativeImports());
        preferRelativePathsWhenCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsService.setPreferRelativeImports(preferRelativePathsWhenCheckBox.isSelected());
            }
        });

        return myComponent;
    }

    public Icon getIcon()
    {
        return null;
    }

    private void addAutoDetectedSource(Set<String> possibleSourceRoots)
    {
        String[] choices = possibleSourceRoots.toArray(new String[0]);

        if(choices.length == 0)
        {
            Messages.showInfoMessage("Could not find any source roots via auto-detection", "Auto-detect Project Sources");
            return;
        }

        String result = choices[0];
        if(choices.length > 1)
        {
            result = Messages.showEditableChooseDialog("Found these possible source roots: ", "Auto-detect Project Sources", null, choices, choices[0], null);
        }

        projectSourceString = result;
        projectSourcesText.setText(result);
    }

    public void apply()
    {
        if(dojoSourcesText.getText() != null)
        {
           settingsService.setDojoSourcesDirectory(dojoSourcesText.getText());
        }

        if(projectSourcesText.getText() != null)
        {
           settingsService.setProjectSourcesDirectory(projectSourcesText.getText());
        }
    }

    public void disposeUIResources() {

    }

    public String getHelpTopic() {
        return "";
    }

    public void reset()
    {
        dojoSourceString = settingsService.getDojoSourcesDirectory();
        dojoSourcesText.setText(dojoSourceString);

        projectSourceString =settingsService.getProjectSourcesDirectory();
        projectSourcesText.setText(projectSourceString);
    }
}
