package sk.upjs.kopr.file_copy.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import sk.upjs.kopr.file_copy.FileInfo;
import sk.upjs.kopr.file_copy.client.FileCopyController;
import sk.upjs.kopr.file_copy.client.FileInfoReceiver;
import sk.upjs.kopr.file_copy.client.FileReceiveTask;
import sk.upjs.kopr.file_copy.server.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileCopyApp extends Application {
    private ProgressBar copyProgressBar;
    private Spinner<Integer> threadCountSpinner;
    private Button startButton;
    private Button continueButton;
    private Button cancelButton;
    private ExecutorService executorService;
    private int threadCount;
    private FileCopyController fcc;
    private File subor = new File("oSubore");



    @Override
    public void start(Stage primaryStage) throws IOException {

        Pane pane = new Pane();

        Label threadCountLabel = new Label("Počet vlákien");
        threadCountLabel.setLayoutX(265);
        threadCountLabel.setLayoutY(95);
        pane.getChildren().add(threadCountLabel);


        threadCountSpinner = new Spinner<>();
        threadCountSpinner.setLayoutX(226);
        threadCountSpinner.setLayoutY(124);

        pane.getChildren().add(threadCountSpinner);

        startButton = new Button("Začať kopírovanie");
        startButton.setLayoutX(139);
        startButton.setLayoutY(200);
        startButton.setPrefSize(144, 25);
        pane.getChildren().add(startButton);

        continueButton = new Button("Pokračovať v kopírovaní");
        continueButton.setLayoutX(333);
        continueButton.setLayoutY(200);
        pane.getChildren().add(continueButton);

        cancelButton = new Button("Zrušiť kopírovanie");
        cancelButton.setLayoutX(240);
        cancelButton.setLayoutY(250);
        cancelButton.setPrefSize(144, 25);
        cancelButton.setDisable(true);
        pane.getChildren().add(cancelButton);

        copyProgressBar = new ProgressBar(0);
        copyProgressBar.setLayoutX(200);
        copyProgressBar.setLayoutY(334);
        copyProgressBar.setPrefWidth(200);
        pane.getChildren().add(copyProgressBar);

        Label progressLabel = new Label("Priebeh kopírovania");
        progressLabel.setLayoutX(247);
        progressLabel.setLayoutY(303);
        pane.getChildren().add(progressLabel);

        fileChecker();


        startButton.setOnAction(e -> {
            int threadCount = threadCountSpinner.getValue();
            fcc = new FileCopyController(copyProgressBar, threadCount);
            fcc.start();
            startButton.setDisable(true);
            cancelButton.setDisable(false);
            threadCountSpinner.setDisable(true);
            fcc.setOnSucceeded(a -> {
                cancelButton.setDisable(true);
            });

        });

        cancelButton.setOnAction(e -> {
            cancelButton.setDisable(true);
            startButton.setDisable(false);
            continueButton.setDisable(false);
            fcc.cancel();
            System.out.println("Kopírovanie bolo prerušené pri kliknutí na prerušenie.");
            System.exit(0);

        });
        continueButton.setOnAction(e -> {
            continueButton.setDisable(true);
            int threadCount = threadCountSpinner.getValue();
            fcc = new FileCopyController(copyProgressBar, threadCount);
            fcc.start();


        });
        primaryStage.setOnCloseRequest(event -> {
            if(fcc==null){
                System.exit(0);
            }else{
                fcc.cancel();
                System.out.println("Aplikácia bola ukončená");
                System.exit(0);
            }


        });

        Scene scene = new Scene(pane, 600, 400);
        primaryStage.setTitle("Kopírovanie súborov");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    private void fileChecker() throws IOException {
        try{
            if (subor.createNewFile() || subor.length() == 0) { // new copy
                copyProgressBar.setVisible(false);
                startButton.setDisable(false);
                continueButton.setDisable(true);
                cancelButton.setDisable(true);
                threadCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
                threadCountSpinner.setEditable(true);
            } else {

                continueButton.setDisable(false);
                cancelButton.setDisable(true);
                startButton.setDisable(true);


                BufferedReader br = new BufferedReader(new FileReader(subor));
                String line = br.readLine();
                while(line!= null) {
                    if(line.contains("vlakna")) {
                        threadCount=Integer.parseInt(line.split(" ")[1]);
                        threadCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(threadCount, threadCount, threadCount));
                        threadCountSpinner.setEditable(false);
                    }
                    line = br.readLine();
                }
                br.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

    }






    public static void main(String[] args) {
        launch(args);
    }
}




