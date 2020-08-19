package controllers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import network.FileSender;
import util.ClientHandler;
import util.Network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class MainController {

    private static ClientLeftPanelController clientPanel;
    private static ServerRightPanelController serverPanel;
    public VBox mainWindow;

    private static String selectedFile;
    private static String actualClientPath;
    private static String login;
    private static String pass;
    private static final byte AUTH_BYTE_OK = 20;
    public static boolean authOk = false;
    public static boolean isLogin = false;


    public static ClientLeftPanelController getClientPanel() {
        return clientPanel;
    }

    public static String getLogin() {
        return login;
    }

    @FXML
    VBox leftPanel, rightPanel;
    @FXML
    HBox buttonBlock;
    @FXML
    VBox loginBox;
    @FXML
    VBox regBox;
    @FXML
    HBox tablePanel;
    @FXML
    Label loginLabel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    Label regLabel;
    @FXML
    TextField newLoginField;
    @FXML
    PasswordField firstPasswordField;
    @FXML
    PasswordField secondPasswordField;

    @FXML
    ProgressBar progressBar;

    private static void update() {
        clientPanel.globalUpdateList();
    }

    public void btnExitAction() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение");
        alert.setHeaderText("Вы действительно хотите выйти?");
        ButtonType exit = new ButtonType("Выйти");
        ButtonType cancel = new ButtonType("Остаться");

        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(exit, cancel);

        Optional<ButtonType> optional = alert.showAndWait();

        if (optional.get() == exit) {
            try {
                Network.getInstance().stop();
                Platform.exit();
            } catch (Exception e) {
                System.out.println("Exception: " + e);
            } finally {
                Platform.exit();
            }
        }
    }

    public void btnLoginAction() throws Exception {
        login = loginField.getText();
        pass = passwordField.getText();
        if (login.isEmpty() || pass.isEmpty()) {
            loginLabel.setText("Все поля должны быть заполнены");
        } else {
            authorize(login, pass);
        }

    }

    public void btnUpdateAction() {
        Path path = Paths.get(clientPanel.pathField.getText());
        clientPanel.updateList(path);
        serverPanel.updateFileList(ClientHandler.fileList);
    }

    private void waitProcess() {
        buttonBlock.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
    }

    private void finishProcess() {
        buttonBlock.setDisable(false);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
    }

    public void btnDeleteAction() {
        if (checkPanel()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Действительно удалить файл?");
        alert.getDialogPane().setHeaderText(null);
        Optional<ButtonType> option = alert.showAndWait();

        if (option.isPresent()) {
            if (option.get() == ButtonType.OK) {
                if (clientPanel.getSelectedFilename() != null) {
                    try {
                        System.out.println("Файл " + clientPanel.getSelectedFilename() + " был удален");
                        Files.delete(Paths.get(clientPanel.getCurrentPath(), clientPanel.getSelectedFilename()));
                        clientPanel.updateList(Paths.get(clientPanel.pathField.getText()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    byte[] filenameBytes = serverPanel.getSelectedFilename().getBytes(StandardCharsets.UTF_8);
                    ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length);
                    buf.writeByte((byte) 60);
                    System.out.println("Записали сигнальный байт");
                    buf.writeInt(filenameBytes.length);
                    System.out.println("Записали длину имени файла" + filenameBytes.length);
                    buf.writeBytes(filenameBytes);
                    System.out.println("Записали имя файла" + filenameBytes);
                    System.out.println("Отправили сигнальный байт и данные файла для удаления");
                    Network.getInstance().getCurrentChannel().writeAndFlush(buf);
                    Network.getInstance().setOnUpdateCallBack(() -> serverPanel.updateFileList(ClientHandler.fileList));
                }
            }
        }
    }

    private boolean checkPanel() {
        if (clientPanel.getSelectedFilename() == null && serverPanel.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.getDialogPane().setHeaderText(null);
            alert.showAndWait();
            return true;
        }
        return false;
    }

    private void afterAuthorise() {
        Stage stage = (Stage) mainWindow.getScene().getWindow();
        stage.hide();
        stage.centerOnScreen();
        stage.setResizable(true);
        stage.setWidth(1200);
        stage.setHeight(600);
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        tablePanel.setVisible(true);
        tablePanel.setManaged(true);
        buttonBlock.setVisible(true);
        buttonBlock.setManaged(true);
        stage.show();
    }

    public void btnDownloadAction(ActionEvent actionEvent) {
        if (serverPanel.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.getDialogPane().setHeaderText(null);
            alert.showAndWait();
        }
        waitProcess();
        actualClientPath = getClientPanel().pathField.getText();
        selectedFile = serverPanel.getSelectedFilename();
        byte[] filenameBytes = selectedFile.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length);
        buf.writeByte((byte) 15);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        System.out.println("Оправили сигнальный байт на загрузку и данные о запрашиваемом файле");

        Network.getInstance().getCurrentChannel().writeAndFlush(buf);
        Network.getInstance().setOnUpdateCallBack(MainController::update);
        Network.getInstance().setOnFinishCallBack(this::finishProcess);
    }

    public void btnUploadAction(ActionEvent actionEvent) throws IOException {
        if (clientPanel.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не выбран!");
            alert.showAndWait();
            return;
        }
        waitProcess();
        Path srsPath = Paths.get(clientPanel.getCurrentPath(), clientPanel.getSelectedFilename());
        System.out.println(srsPath.toString());

        FileSender.sendFile(srsPath, Network.getInstance().getCurrentChannel(), channelFuture -> {
            if (!channelFuture.isSuccess()) {
                channelFuture.cause().printStackTrace();
            }
            if (channelFuture.isSuccess()) {
                System.out.println("Файл успешно передан!");
            }
        });
        Network.getInstance().setOnUpdateCallBack(() -> {
            serverPanel.updateFileList(ClientHandler.fileList);
            finishProcess();
        });
    }

    public void btnCreateFolderAction(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Пока не реалтзовано, задел на будущее");
    }

    public void btnRegAction(ActionEvent actionEvent) {
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        regBox.setVisible(true);
        regBox.setManaged(true);
    }

    public void btnReg(ActionEvent actionEvent) {
        String newLogin = newLoginField.getText();
        String firstPass = firstPasswordField.getText();
        String secondPass = secondPasswordField.getText();

        if (newLogin.isEmpty() || firstPass.isEmpty() || secondPass.isEmpty()) {
            regLabel.setText("Все поля должны быть заполнены");
        } else if (!firstPass.equals(secondPass)) {
            regLabel.setText("Пароли не совпадают");
        } else {
            CountDownLatch networkStarter = new CountDownLatch(1);
            new Thread(() -> Network.getInstance().start(networkStarter)).start();
            try {
                networkStarter.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ClientHandler.registration(newLogin, firstPass, Network.getInstance().getCurrentChannel());
            Network.getInstance().setOnRegistrationFailedCallBack(() -> {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Ошибка регистрации");
                    alert.setHeaderText("Данный пользователь уже зарегистрирован.\n" +
                            "Пожалуйста придумайте новый логин или авторизуйтесь.");
                    alert.showAndWait();
                });
                Network.getInstance().stop();
            });
            Network.getInstance().setOnRegistrationOkCallBack(() -> {
                System.out.println("Регистрация прошла успешно");
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Регистрация прошла успешно");
                    alert.setHeaderText("Регистарция прошла успешно.\n" +
                            "Авторизуйтесь используя логин и пароль.");
                    alert.showAndWait();
                });
                regBox.setVisible(false);
                regBox.setManaged(false);
                loginBox.setVisible(true);
                loginBox.setManaged(true);
                Network.getInstance().stop();
            });
        }
    }

    public void btnBack(ActionEvent actionEvent) {
        regBox.setVisible(false);
        regBox.setManaged(false);
        loginBox.setVisible(true);
        loginBox.setManaged(true);
    }

    public void authorize(String login, String pass) throws InterruptedException {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        try {
            networkStarter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Авторизация...");
        ClientHandler.authorization(login, pass, Network.getInstance().getCurrentChannel());
        while (!authOk) {
                /*
                Как решить проблему без слипа, чтобы пока не пришел ответ, main ждал
                 */
            Thread.sleep(200);
            if (authOk) {
                System.out.println("Авторизация прошла успешно");
                loginLabel.setText("Авторизация прошла успешно");
                clientPanel = (ClientLeftPanelController) leftPanel.getProperties().get("ctrl");
                serverPanel = (ServerRightPanelController) rightPanel.getProperties().get("ctrl");
                clientPanel.create();
                serverPanel.create();
                afterAuthorise();
                Network.getInstance().setOnUpdateCallBack(() -> {
                    serverPanel.updateFileList(ClientHandler.fileList);
                });
            } else if (isLogin) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Идентификация пользователя");
                alert.setHeaderText("Данный пользователь уже активен.\n" +
                        "Пожалуйста завершите активную сессию и повторите вход.");

                alert.showAndWait();
                System.out.println("Вы уже залогинились");
                break;
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка авторизации");
                alert.setHeaderText("Ошибка авторизации.\n" +
                        "Проверьте логин и пароль.");
                alert.showAndWait();
                System.out.println("Ошибка авторизации");
                break;
            }
        }
    }
}
