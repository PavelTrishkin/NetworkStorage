package controllers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    public static ServerRightPanelController getServerPanel() {
        return serverPanel;
    }


    private static String selectedFile;
    private static String actualClientPath;
    private static String login;
    private static String pass;
    private static final byte AUTH_BYTE_OK = 20;
    public static boolean authOk = false;

    public static ClientLeftPanelController getClientPanel() {
        return clientPanel;
    }


    @FXML
    VBox leftPanel, rightPanel;
    @FXML
    HBox buttonBlock;
    @FXML
    VBox loginBox;
    @FXML
    HBox tablePanel;
    @FXML
    Label loginLabel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;

    @FXML
    ProgressBar progressBar;

    private static void updateCallBack() {
        clientPanel.globalUpdateList();
    }

    public void btnExitAction() {
        Network.getInstance().stop();
        Platform.exit();
    }

    public void btnLoginAction() throws Exception {
        login = loginField.getText();
        pass = passwordField.getText();
        if (login.isEmpty() || pass.isEmpty()) {
            loginLabel.setText("Все поля должны быть заполнены");
        } else {
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
                if (authOk){
                    System.out.println("Авторизация прошла успешно");
                    loginLabel.setText("Авторизация прошла успешно");
                    clientPanel = (ClientLeftPanelController) leftPanel.getProperties().get("ctrl");
                    serverPanel = (ServerRightPanelController) rightPanel.getProperties().get("ctrl");
                    clientPanel.create();
                    serverPanel.create();
                    afterAuthorise();
                    Network.getInstance().setOnUpdateCallBack(() ->{
                        serverPanel.updateFileList(ClientHandler.fileList);
                    });
                    break;
                }
                else {
                    System.out.println("Авторизация провалена");
                    break;
                }
            }

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
                        Files.delete(Paths.get(clientPanel.getCurrentPath(),clientPanel.getSelectedFilename()));
                        clientPanel.updateList(Paths.get(clientPanel.pathField.getText()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    byte[] filenameBytes = serverPanel.getSelectedFilename().getBytes(StandardCharsets.UTF_8);
                    ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length);
                    buf.writeByte((byte)60);
                    System.out.println("Записали сигнальный байт");
                    buf.writeInt(filenameBytes.length);
                    System.out.println("Записали длину имени файла" + filenameBytes.length);
                    buf.writeBytes(filenameBytes);
                    System.out.println("Записали имя файла" + filenameBytes);
                    System.out.println("Отправили сигнальный байт и данные файла для удаления");
                    Network.getInstance().getCurrentChannel().writeAndFlush(buf);
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
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        tablePanel.setVisible(true);
        tablePanel.setManaged(true);
        buttonBlock.setVisible(true);
        buttonBlock.setManaged(true);
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
        Network.getInstance().setOnUpdateCallBack(MainController::updateCallBack);
        Network.getInstance().setOnFinishCallBack(this::finishProcess);
}

    public void btnUploadAction(ActionEvent actionEvent) throws IOException {
        if (clientPanel.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не выбран!");
            alert.showAndWait();
            return;
        }
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
    }

    public void btnCreateFolderAction(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Пока не реалтзовано, задел на будущее");
    }
}
