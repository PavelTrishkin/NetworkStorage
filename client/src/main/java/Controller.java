import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import util.Network;
import network.FileSender;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class Controller implements Initializable {
    public ListView<String> lv;
    public TextField txt;
    public Button send;
    private DataInputStream is;
    private DataOutputStream os;
    private final String clientFilesPath = "./src/main/resources/clientFiles";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        try {
            networkStarter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        File dir = new File(clientFilesPath);
        for (String file : dir.list()) {
            lv.getItems().add(file);
        }
    }

    // ./download fileName
    // ./upload fileName
    public void sendCommand(ActionEvent actionEvent) {
        String command = txt.getText();
        String [] op = command.split(" ");
        if (op[0].equals("./download")) {
            try {
                os.writeBytes("10");
                os.writeUTF(op[1]);
                byte response = is.readByte();
                System.out.println("resp: " + response);
                if (response == (byte)25) {
                    File file = new File(clientFilesPath + "/" + op[1]);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    long len = is.readLong();
                    byte [] buffer = new byte[1024];
                    try(FileOutputStream fos = new FileOutputStream(file)) {
                        if (len < 1024) {
                            int count = is.read(buffer);
                            fos.write(buffer, 0, count);
                        } else {
                            for (long i = 0; i < len / 1024; i++) {
                                int count = is.read(buffer);
                                fos.write(buffer, 0, count);
                            }
                        }
                        fos.flush();
                    }
                    lv.getItems().add(op[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if(op[0].equals("./upload")) {
            System.out.println(op[1]);
            try {
                FileSender.sendFile(Paths.get(clientFilesPath + "/" + op[1]), Network.getInstance().getCurrentChannel(), future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        System.out.println("Файл успешно передан");
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
