package intellij.zeppelin;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class ZeppelinConfigurable implements SearchableConfigurable {
  private JPanel mainPanel;
  private JBTextField usernameField;
  private JPasswordField passwordField;
  private JPanel configPanel;
  private JBTextField hostTextField;
  private JBTextField proxyField;

    private final Project myProject;

  public ZeppelinConfigurable(@NotNull Project project) {
    myProject = project;

    usernameField.addFocusListener(createInitialTextFocusAdapter(usernameField, DEFAULT_USERNAME_TEXT));
    ZeppelinConnection connection = ZeppelinConnection$.MODULE$.connectionFor(project);
    setInitialText(usernameField, connection.getUsername(), DEFAULT_USERNAME_TEXT);
    passwordField.setText(connection.getPassword());
    hostTextField.setText(connection.getHostUrl());
    proxyField.setText(connection.getProxyUrl());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Zeppelin Notebook";
  }

  @Override
  public String getHelpTopic() {
    return "";
  }

  @Override
  public JComponent createComponent() {
    return mainPanel;
  }

  @NotNull
  @Override
  public String getId() {
    return "ZeppelinConfigurable";
  }

  private static final String DEFAULT_USERNAME_TEXT = "Leave empty for anonymous access";



  public void apply() {
    final ZeppelinConnection connection = ZeppelinConnection$.MODULE$.connectionFor(myProject);

    if (configPanel.isVisible()) {
      final String oldUsername = connection.getUsername();
      final String oldPassword = connection.getPassword();
      final String oldHost = connection.getHostUrl();
      final String newUsername = getUsername();
      final String newPassword = String.valueOf(passwordField.getPassword());
      final String newHostUrl = hostTextField.getText();
      final String newProxyUrl = proxyField.getText();

      if (!oldUsername.equals(newUsername) || !oldPassword.equals(newPassword) || !oldHost.equals(newHostUrl)) {
        connection.setUsername(newUsername);
        connection.setPassword(newPassword);
        connection.setHostUrl(newHostUrl);
        connection.setProxyUrl(newProxyUrl);
        connection.resetApi();
      }
    }
  }

  public void reset() {
    final ZeppelinConnection connection = ZeppelinConnection$.MODULE$.connectionFor(myProject);
    if (configPanel.isVisible()) {
      setInitialText(usernameField, connection.getUsername(), DEFAULT_USERNAME_TEXT);
      passwordField.setText(connection.getPassword());
      hostTextField.setText(connection.getHostUrl());
    }
  }

  public boolean isModified() {
    final ZeppelinConnection connection = ZeppelinConnection$.MODULE$.connectionFor(myProject);
    if (configPanel.isVisible()) {
      final String oldUsername = connection.getUsername();
      final String oldPassword = connection.getPassword();
      final String oldHost = connection.getHostUrl();

      final String newPassword = String.valueOf(passwordField.getPassword());
      final String newUsername = getUsername();
      final String newHost = hostTextField.getText();

      return !oldPassword.equals(newPassword) || !oldUsername.equals(newUsername) || !oldHost.equals(newHost) ;
    }
    return false;
  }

  private String getUsername() {
    final String usernameText = usernameField.getText();
    return DEFAULT_USERNAME_TEXT.equals(usernameText) ? "" : usernameText;
  }

  @NotNull
  private static FocusAdapter createInitialTextFocusAdapter(@NotNull JBTextField field, @NotNull String initialText) {
    return new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (field.getText().equals(initialText)) {
          field.setForeground(UIUtil.getActiveTextColor());
          field.setText("");
        }
      }

      @Override
      public void focusLost(FocusEvent e) {
        if (field.getText().isEmpty()) {
          field.setForeground(UIUtil.getInactiveTextColor());
          field.setText(initialText);
        }
      }
    };
  }

  private static void setInitialText(@NotNull JBTextField field,
                                     @NotNull String savedValue,
                                     @NotNull String defaultText) {
    if (savedValue.isEmpty()) {
      field.setForeground(UIUtil.getInactiveTextColor());
      field.setText(defaultText);
    }
    else {
      field.setForeground(UIUtil.getActiveTextColor());
      field.setText(savedValue);
    }
  }
}

