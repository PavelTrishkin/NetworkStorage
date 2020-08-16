package controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import util.ClientHandler;
import util.ServerFileInfo;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerRightPanelController {

    List<ServerFileInfo> serverFiles = new ArrayList<>();
    @FXML
    TableView<ServerFileInfo> filesTable;


    @FXML
    TextField pathField;

    public void create() {
        TableColumn<ServerFileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        filenameColumn.setPrefWidth(200);

        TableColumn<ServerFileInfo, Long> sizeColumn = new TableColumn<>("Размер");
        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        sizeColumn.setPrefWidth(120);

        sizeColumn.setCellFactory(column -> {
            return new TableCell<ServerFileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });

        pathField.setText("storage");
        filesTable.getColumns().addAll(filenameColumn, sizeColumn);
        filesTable.getSortOrder().add(filenameColumn);
        serverFiles.clear();
        updateFileList(ClientHandler.fileList);
    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFileName();
    }

    public void updateFileList(String fileList) {
        System.out.println("Читаем список файлов: " + fileList);
        if (fileList.isEmpty()){
            System.out.println("Список файлов пуст");
            filesTable.getItems().clear();
            return;
        }
        else {
            String[] list = fileList.split("NEXT_FILE");
            System.out.println("В массиве List данные: " + Arrays.toString(list));
            String[] files;
            serverFiles.clear();
            filesTable.getItems().clear();

            pathField.setText("storage");
            for (int i = 0; i < list.length; i++) {
                files = list[i].split("SEPARATOR");
                System.out.println("В массиве files данные: " + Arrays.toString(files));
                serverFiles.add(new ServerFileInfo(files[0], Long.valueOf(files[1])));
            }
            filesTable.getItems().addAll(serverFiles);
            filesTable.sort();
        }
    }

}
